package net.forestany.battleship.game

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.forestany.battleship.ConfettiView
import net.forestany.battleship.GlobalInstance
import net.forestany.battleship.MainActivity
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_3_SHOTS
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_5_SHOTS
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_ALTERNATE
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_NO_MODE
import net.forestany.battleship.MainActivity.Companion.GAME_MODE_SHOT_EACH_SHIP
import net.forestany.battleship.MainActivity.Companion.GRID_COLS
import net.forestany.battleship.MainActivity.Companion.GRID_ROWS
import net.forestany.battleship.Other
import net.forestany.battleship.R
import net.forestany.battleship.Util
import net.forestany.battleship.game.BattleshipGridAdapter.CellState

class BattleshipActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "BattleshipActivity"
    }

    enum class BoardState {
        PLACEMENT,
        OWN_BOARD,
        OTHER_BOARD,
        OTHER_BOARD_TARGET,
        END
    }

    enum class ButtonState {
        CONFIRM,
        HORIZONTAL,
        VERTICAL,
        FIRE
    }

    private lateinit var battleshipActivityViewGroup: ViewGroup
    private lateinit var gridTitle: TextView
    private lateinit var gameMode: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: BattleshipGridAdapter
    private lateinit var gameButton: Button
    private lateinit var statusEditText: EditText
    private lateinit var debugViewStatus: TextView
    private lateinit var fleets: List<List<Ship>>

    private lateinit var uiThread: Thread

    private var loadGameState = false
    private var userName: String = ""
    private var gameName: String = ""
    private var storedFleetIndex: Int = 0
    private var serverIp: String = ""
    private var serverPort: Int = 0
    private var currentButtonState = ButtonState.HORIZONTAL
    private var targetCoordinate = Pair(-1, -1)
    private var amountShots = 0
    private var reloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // default settings
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_battleship)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.battleship_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fleets = listOf(
            listOf( // standard fleet
                Ship(5, getString(R.string.battleship_fleet_battleship)),
                Ship(4, getString(R.string.battleship_fleet_battlecruiser_one)),
                Ship(4, getString(R.string.battleship_fleet_battlecruiser_two)),
                Ship(3, getString(R.string.battleship_fleet_cruiser_one)),
                Ship(3, getString(R.string.battleship_fleet_cruiser_two)),
                Ship(3, getString(R.string.battleship_fleet_cruiser_three)),
                Ship(2, getString(R.string.battleship_fleet_frigate_one)),
                Ship(2, getString(R.string.battleship_fleet_frigate_two)),
                Ship(2, getString(R.string.battleship_fleet_frigate_three)),
                Ship(2, getString(R.string.battleship_fleet_frigate_four))
            ),
            listOf( // frigate fleet
                Ship(2, getString(R.string.battleship_fleet_frigate_one)),
                Ship(2, getString(R.string.battleship_fleet_frigate_two)),
                Ship(2, getString(R.string.battleship_fleet_frigate_three)),
                Ship(2, getString(R.string.battleship_fleet_frigate_four)),
                Ship(2, getString(R.string.battleship_fleet_frigate_five)),
                Ship(2, getString(R.string.battleship_fleet_frigate_six)),
                Ship(2, getString(R.string.battleship_fleet_frigate_seven)),
                Ship(2, getString(R.string.battleship_fleet_frigate_eight)),
                Ship(2, getString(R.string.battleship_fleet_frigate_nine)),
                Ship(2, getString(R.string.battleship_fleet_frigate_ten))
            )
        )

        //region intent incoming data
        loadGameState = intent.extras?.getString("LOAD_GAME_STATE").contentEquals("1")
        userName = intent.extras?.getString("GAME_USER") ?: "NO_USER_ENTERED"

        if ((userName == "NO_USER_ENTERED") || (userName.isBlank())) {
            setResult(MainActivity.RETURN_CODE_NO_USER)
            finish()
        }

        gameName = intent.extras?.getString("GAME_NAME") ?: "NO_GAME_ENTERED"

        if ((gameName == "NO_GAME_ENTERED") || (gameName.isBlank())) {
            setResult(MainActivity.RETURN_CODE_NO_GAME)
            finish()
        }

        GlobalInstance.get().s_gameMode = intent.extras?.getString("GAME_MODE") ?: GAME_MODE_NO_MODE

        if ((GlobalInstance.get().b_isServer) && ((GlobalInstance.get().s_gameMode == GAME_MODE_NO_MODE) || (GlobalInstance.get().s_gameMode.isBlank()))) {
            setResult(MainActivity.RETURN_CODE_NO_GAME_MODE)
            finish()
        }

        GlobalInstance.get().b_gameAdditionalOptionOne = intent.extras?.getString("GAME_ADDITIONAL_OPTION_ONE").contentEquals("1")
        GlobalInstance.get().b_gameAdditionalOptionTwo = intent.extras?.getString("GAME_ADDITIONAL_OPTION_TWO").contentEquals("1")
        GlobalInstance.get().i_fleetIndex = intent.extras?.getString("GAME_FLEET_INDEX")?.toInt() ?: 0
        storedFleetIndex = GlobalInstance.get().i_fleetIndex

        val s_networkInterface: String = intent.extras?.getString("NETWORK_INTERFACE") ?: getString(R.string.no_network_interfaces)

        if ( (s_networkInterface == getString(R.string.no_network_interfaces)) || (s_networkInterface.isBlank()) || (!s_networkInterface.contains(":")) ) {
            setResult(MainActivity.RETURN_CODE_NO_NETWORK_INTERFACE)
            finish()
        } else {
            val serverInfo: List<String> = s_networkInterface.split(":")
            serverIp = serverInfo[0]
            serverPort = serverInfo[1].toInt()
        }
        //endregion

        //region game state
        if (loadGameState) {
            val gameFleetString = intent.extras?.getString("GAME_FLEET_STRING") ?: ""

            if (gameFleetString.isNotBlank()) {
                val a_ships = gameFleetString.split("|")

                // load all fleet ships information
                for ((i, s_ship) in a_ships.withIndex()) {
                    val a_shipInfo = s_ship.split(";")

                    fleets[storedFleetIndex][i].isHorizontal = a_shipInfo[0].contentEquals("1")
                    fleets[storedFleetIndex][i].destroyed = a_shipInfo[1].contentEquals("1")
                    fleets[storedFleetIndex][i].placed = true

                    for (j in 2..< a_shipInfo.size) {
                        val a_shipCell = a_shipInfo[j].split("-")

                        fleets[storedFleetIndex][i].cells.add(a_shipCell[0].toInt() to a_shipCell[1].toInt())
                    }
                }
            }

            amountShots = intent.extras?.getString("AMOUNT_SHOTS")?.toInt() ?: 0

            currentButtonState = when(intent.extras?.getString("BUTTON_STATE")) {
                "VERTICAL" -> ButtonState.VERTICAL
                "CONFIRM" -> ButtonState.CONFIRM
                "FIRE" -> ButtonState.FIRE
                else -> ButtonState.HORIZONTAL
            }
        }
        //endregion

        //region activity layout elements
        battleshipActivityViewGroup = findViewById(android.R.id.content)
        gridTitle = findViewById(R.id.gridTitle)
        gameMode = findViewById(R.id.gameMode)
        recyclerView = findViewById(R.id.recyclerGrid)
        gameButton = findViewById(R.id.btnGame)
        statusEditText = findViewById(R.id.etStatus)
        debugViewStatus = findViewById(R.id.debugView)

        var cellSize = GlobalInstance.get().getPreferences()["grid_cell_size"].toString().toInt()
        var cellPadding = GlobalInstance.get().getPreferences()["grid_cell_padding"].toString().toInt()

        // convert values to dp
        cellSize = (cellSize * this.resources.displayMetrics.density).toInt()
        cellPadding = (cellPadding * this.resources.displayMetrics.density).toInt()

        // recycler view adapter
        recyclerViewAdapter = BattleshipGridAdapter(fleets[storedFleetIndex], cellSize, cellPadding)
        recyclerView.layoutManager = GridLayoutManager(this, 10)
        recyclerViewAdapter.delegate = object : BattleshipGridAdapter.GridAdapterDelegate {
            override fun afterPlacedOrRemovedShip(status: Int, currentShip: Ship?) {
                onAfterPlacedOrRemovedShip(status, currentShip)
            }

            override fun afterPlacedTarget(row: Int, col: Int, valid: Boolean) {
                onAfterPlacedTarget(row, col, valid)
            }

            override fun afterFireTarget(row: Int, col: Int) {
                onAfterFireTarget(row, col)
            }
        }
        recyclerView.adapter = recyclerViewAdapter

        val horizontalHeaderEmptyCell = findViewById<TextView>(R.id.horizontalHeaderEmptyCell)
        val horizontalHeaders = findViewById<LinearLayout>(R.id.horizontalHeaders)
        val verticalHeaders = findViewById<LinearLayout>(R.id.verticalHeaders)

        // update size
        val paramsEmptyCell = horizontalHeaderEmptyCell.layoutParams
        paramsEmptyCell.width = cellSize
        paramsEmptyCell.height = cellSize
        horizontalHeaderEmptyCell.layoutParams = paramsEmptyCell

        // update padding
        horizontalHeaderEmptyCell.setPadding(cellPadding, cellPadding, cellPadding, cellPadding)

        for (i in 0 until horizontalHeaders.childCount) {
            val child = horizontalHeaders.getChildAt(i)

            if (child is TextView) {
                // update size
                val params = child.layoutParams
                params.width = cellSize
                params.height = cellSize
                child.layoutParams = params

                // update padding
                child.setPadding(cellPadding, cellPadding, cellPadding, cellPadding)
            }
        }

        for (i in 0 until verticalHeaders.childCount) {
            val child = verticalHeaders.getChildAt(i)

            if (child is TextView) {
                // update size
                val params = child.layoutParams
                params.width = cellSize
                params.height = cellSize
                child.layoutParams = params

                // update padding
                child.setPadding(cellPadding, cellPadding, cellPadding, cellPadding)
            }
        }

        // update ui
        updateGridTitle(gridTitle, !((GlobalInstance.get().getBoardState() == BoardState.OTHER_BOARD) || (GlobalInstance.get().getBoardState() == BoardState.OTHER_BOARD_TARGET)))
        updateGameMode(gameMode)
        val currentShip = recyclerViewAdapter.getCurrentShip()
        statusEditText.setText( Util.replacePlaceholders(
            getString(R.string.battleship_status_place),
            getString(
                R.string.battleship_status_place_ship,
                currentShip.name,
                currentShip.size,
                getString(R.string.battleship_status_boxes)
            )
        ) )

        if (loadGameState) {
            statusEditText.setText( intent.extras?.getString("LAST_STATUS") ?: "" )

            if ((GlobalInstance.get().getBoardState() == BoardState.OWN_BOARD)) {
                gameButton.visibility = View.GONE
            }
        }

        // game button state on click events
        gameButton.setOnClickListener {
            when (currentButtonState) {
                ButtonState.HORIZONTAL -> {
                    recyclerViewAdapter.setShipRotation(false)
                }

                ButtonState.VERTICAL -> {
                    recyclerViewAdapter.setShipRotation(true)
                }

                ButtonState.CONFIRM -> {
                    // set all own fleet info for end state
                    GlobalInstance.get().setOwnGridEnd(recyclerViewAdapter.getOwnShipsEnd())
                    GlobalInstance.get().setBoardState(BoardState.OWN_BOARD)
                    gameButton.visibility = View.GONE
                    statusEditText.setText(getString(R.string.battleship_status_wait_other_player_finished_placement))
                }

                ButtonState.FIRE -> {
                    if ((targetCoordinate.first >= 0) && (targetCoordinate.second >= 0)) {
                        recyclerViewAdapter.fireShot(targetCoordinate.second, targetCoordinate.first)
                    }
                }
            }

            currentButtonState = when (currentButtonState) {
                ButtonState.HORIZONTAL -> ButtonState.VERTICAL
                ButtonState.VERTICAL -> ButtonState.HORIZONTAL
                else -> currentButtonState
            }

            updateButtonState(gameButton)
        }

        updateButtonState(gameButton)
        //endregion

        // handle standard back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                onBackAction()
            }
        })

        if (GlobalInstance.get().b_isServer) {
            Log.i(TAG, "created game as '$userName' with network interface '$serverIp:$serverPort'")
        } else {
            Log.i(TAG, "joined game as '$userName' with network interface '$serverIp:$serverPort'")
        }

        // start lobby server
        if ((GlobalInstance.get().b_isServer) && (GlobalInstance.get().o_communicationLobby == null)) {
            /*val wifi = getSystemService(WIFI_SERVICE) as WifiManager
            val multicastLock = wifi.createMulticastLock("multicastLockServer")
            multicastLock.setReferenceCounted(true)
            multicastLock.acquire()*/

            Other().netLobby(
                GlobalInstance.get().getPreferences()["udp_multicast_ip"].toString(), //MainActivity.UDP_MULTICAST_IP,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_port"].toString()), //MainActivity.UDP_MULTICAST_PORT,
                Integer.parseInt(GlobalInstance.get().getPreferences()["udp_multicast_ttl"].toString()), //MainActivity.UDP_MULTICAST_TTL,
                gameName,
                serverIp,
                serverPort
            )
        }

        // start network communication for bank
        if (GlobalInstance.get().o_communicationBattleship == null) {
            Other().netBattleship(serverIp, serverPort)
        }

        // start ui thread to update ui every second
        uiThread = Thread { uiThreadMethod() }
        uiThread.start()

        Log.v(TAG, "onCreate $TAG")
    }

    private fun uiThreadMethod() {
        var once = false
        var userJoined = false
        var count = 0

        while (true) {
            try {
                // server perspective
                if (GlobalInstance.get().b_isServer) {
                    if (
                        (!once) &&
                        (GlobalInstance.get().getBoardState() == BoardState.OWN_BOARD) &&
                        (GlobalInstance.get().getPreviousBoardState() == BoardState.PLACEMENT) &&
                        (GlobalInstance.get().getOtherBoardState() == BoardState.OWN_BOARD) &&
                        (GlobalInstance.get().getPreviousOtherBoardState() == BoardState.PLACEMENT)
                    ) {
                        // roll a dice to decide who to start
                        val dice = (1..6).random()

                        if (dice <= 3) {
                            GlobalInstance.get().setBoardState(BoardState.OTHER_BOARD_TARGET)
                        } else {
                            GlobalInstance.get().enqueueMessageBox("CLIENT_START")
                        }

                        once = true
                    }
                }

                runOnUiThread {
                    //region debug view
                    /*var s_ownGrid = ""

                    for (i in 0 ..< GRID_ROWS) {
                        for (j in 0 ..< GRID_COLS) {
                            s_ownGrid += when (GlobalInstance.get().getOwnGridCellState(i, j)) {
                                CellState.EMPTY -> "~"
                                CellState.MISS -> "."
                                CellState.SHIP -> "o"
                                CellState.HIT -> "x"
                                CellState.TARGET -> "#"
                            }
                        }
                    }

                    var s_otherGrid = ""

                    for (i in 0 ..< GRID_ROWS) {
                        for (j in 0 ..< GRID_COLS) {
                            s_otherGrid += when (GlobalInstance.get().getOtherGridCellState(i, j)) {
                                CellState.EMPTY -> "~"
                                CellState.MISS -> "."
                                CellState.SHIP -> "o"
                                CellState.HIT -> "x"
                                CellState.TARGET -> "#"
                            }
                        }
                    }

                    var s_otherGridNoTarget = ""

                    for (i in 0 ..< GRID_ROWS) {
                        for (j in 0 ..< GRID_COLS) {
                            s_otherGridNoTarget += when (GlobalInstance.get().getOtherNoTargetGridCellState(i, j)) {
                                CellState.EMPTY -> "~"
                                CellState.MISS -> "."
                                CellState.SHIP -> "o"
                                CellState.HIT -> "x"
                                CellState.TARGET -> "#"
                            }
                        }
                    }

                    debugViewStatus.text = """
                        boardState | previousBoardState: ${GlobalInstance.get().getBoardState()} | ${GlobalInstance.get().getPreviousBoardState()}
                        otherBoardState | previousOtherBoardState : ${GlobalInstance.get().getOtherBoardState()} | ${GlobalInstance.get().getPreviousOtherBoardState()}
                        amount shots: $amountShots
                        ${GlobalInstance.get().getOtherGridEnd()}
                        $s_ownGrid
                        $s_otherGrid
                        $s_otherGridNoTarget
                    """.trimMargin()*/
                    //endregion

                    // save game state all 10 seconds
                    if ((count != 0) && (count % 10 == 0)) {
                        saveGameState()
                    }

                    // snackbar message that other user joined the game
                    if (!userJoined) {
                        if (GlobalInstance.get().s_otherUser != "NO_OTHER_USER_SPECIFIED") {
                            Util.customSnackbar(
                                message = getString(R.string.battleship_snackbar_player_joined_game, GlobalInstance.get().s_otherUser),
                                view = findViewById(android.R.id.content),
                                length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG,
                                textColor = "#D3D3D3".toColorInt(),
                                backgroundColor = "#272757".toColorInt(),
                                actionButton = false
                            )

                            userJoined = true
                        }
                    }

                    // update fleet if global fleet index does not match with the stored one
                    if (GlobalInstance.get().getBoardState() == BoardState.PLACEMENT) {
                        if (GlobalInstance.get().i_fleetIndex != storedFleetIndex) {
                            storedFleetIndex = GlobalInstance.get().i_fleetIndex
                            recyclerViewAdapter.overwriteFleet(fleets[storedFleetIndex])
                            val currentShip = recyclerViewAdapter.getCurrentShip()
                            statusEditText.setText( Util.replacePlaceholders(
                                getString(R.string.battleship_status_place),
                                getString(
                                    R.string.battleship_status_place_ship,
                                    currentShip.name,
                                    currentShip.size,
                                    getString(R.string.battleship_status_boxes)
                                )
                            ) )
                        }
                    }

                    // state to choose a target
                    if (
                        (GlobalInstance.get().getBoardState() == BoardState.OTHER_BOARD_TARGET) &&
                        (GlobalInstance.get().getPreviousBoardState() == BoardState.OWN_BOARD) &&
                        (
                            (targetCoordinate.first < 0) ||
                            (targetCoordinate.second < 0)
                        )
                    ) {
                        if (!reloaded) {
                            amountShots = when(GlobalInstance.get().s_gameMode) {
                                GAME_MODE_ALTERNATE -> 1
                                GAME_MODE_SHOT_EACH_SHIP -> recyclerViewAdapter.amountShipsNotDestroyed()
                                GAME_MODE_3_SHOTS -> 3
                                GAME_MODE_5_SHOTS -> 5
                                else -> 0
                            }

                            reloaded = true

                            updateGridTitle(gridTitle, false)
                        }

                        statusEditText.setText(getString(R.string.battleship_status_choose_target))
                    }

                    // state to wait for other players move
                    if (
                        (GlobalInstance.get().getOtherBoardState() == BoardState.OTHER_BOARD_TARGET) &&
                        (
                            (GlobalInstance.get().getPreviousOtherBoardState() == BoardState.OWN_BOARD) ||
                            (GlobalInstance.get().getPreviousOtherBoardState() == BoardState.PLACEMENT)
                        )
                    ) {
                        statusEditText.setText(getString(R.string.battleship_status_wait_other_move))
                    }

                    // check in own board view if any ship has been destroyed completely
                    if (GlobalInstance.get().getBoardState() == BoardState.OWN_BOARD) {
                        val ship = recyclerViewAdapter.checkShipDestroyed()

                        if (ship != null) {
                            Util.customSnackbar(
                                message = getString(R.string.battleship_snackbar_ship_destroyed, ship.name),
                                view = findViewById(android.R.id.content),
                                length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG,
                                textColor = "#D3D3D3".toColorInt(),
                                backgroundColor = "#272757".toColorInt(),
                                actionButton = false
                            )

                            GlobalInstance.get().enqueueMessageBox("SNACKBAR;${getString(R.string.battleship_snackbar_ship_destroyed, ship.name)}")
                        }
                    }

                    // handle end state
                    if ((GlobalInstance.get().getBoardState() != BoardState.END) && (GlobalInstance.get().getOtherBoardState() == BoardState.END) && (!recyclerViewAdapter.checkFleetDestroyed())) {
                        GlobalInstance.get().setBoardState(BoardState.END)
                        updateGridTitle(gridTitle, false)
                        statusEditText.setText(getString(R.string.battleship_status_won))
                        gameButton.visibility = View.GONE
                        SoundManager.playSound(4)
                        runConfettiView()
                    } else if (recyclerViewAdapter.checkFleetDestroyed()) {
                        // set all own fleet info for end state
                        GlobalInstance.get().setOwnGridEnd(recyclerViewAdapter.getOwnShipsEnd())
                        GlobalInstance.get().setBoardState(BoardState.END)
                        updateGridTitle(gridTitle, false)
                        statusEditText.setText(getString(R.string.battleship_status_lost))
                        gameButton.visibility = View.GONE
                    }

                    if (GlobalInstance.get().getBoardState() == BoardState.END) {
                        // set all own fleet info for end state
                        GlobalInstance.get().setOwnGridEnd(recyclerViewAdapter.getOwnShipsEnd())
                    }

                    // check if we have a snackbar message to show
                    val s_snackbarMessage = GlobalInstance.get().dequeueSnackbarBox()

                    if (s_snackbarMessage != null) {
                        Util.customSnackbar(
                            message = s_snackbarMessage,
                            view = findViewById(android.R.id.content),
                            length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG,
                            textColor = "#D3D3D3".toColorInt(),
                            backgroundColor = "#272757".toColorInt(),
                            actionButton = false
                        )
                    }

                    // update recycler view
                    recyclerViewAdapter.upgradeGridTiles()
                    updateGameMode(gameMode)
                }

                // check for closed server flag
                if (GlobalInstance.get().b_serverClosed) {
                    // delete game state
                    MainActivity.deleteGameState(cacheDir)

                    Util.errorSnackbar(message = getString(R.string.battleship_snackbar_player_left_game, GlobalInstance.get().s_otherUser), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG, view = findViewById(android.R.id.content))

                    Thread.sleep(4000)

                    setResult(MainActivity.RETURN_CODE_OTHER_EXIT)
                    finish()
                    break
                }

                Thread.sleep(1000)
            } catch (_: InterruptedException) {
                break
            } finally {
                count++
            }
        }
    }

    private fun saveGameState() {
        val file = java.io.File(cacheDir, MainActivity.GAME_STATE_FILENAME)

        try {
            val fos = java.io.FileOutputStream(file)
            fos.channel.truncate(0)
            fos.close()

            file.printWriter().use { out ->
                out.println( net.forestany.forestj.lib.Helper.toISO8601UTC(java.time.LocalDateTime.now().withNano(0)))

                if (GlobalInstance.get().b_isServer) {
                    out.println("server")
                } else {
                    out.println("client")
                }

                out.println("${gameName}|${GlobalInstance.get().s_gameMode}|${if (GlobalInstance.get().b_gameAdditionalOptionOne) "true" else "false"}|${if (GlobalInstance.get().b_gameAdditionalOptionTwo) "true" else "false"}|${storedFleetIndex}")
                out.println("${serverIp}|${serverPort}|${userName}")
                out.println("${GlobalInstance.get().getBoardState()}|${GlobalInstance.get().getPreviousBoardState()}|${GlobalInstance.get().getOtherBoardState()}|${GlobalInstance.get().getPreviousOtherBoardState()}|$amountShots|$currentButtonState")
                out.println(statusEditText.text.toString())

                var foo = ""

                for (i in 0 ..< GRID_ROWS) {
                    for (j in 0 ..< GRID_COLS) {
                        foo += when (GlobalInstance.get().getOwnGridCellState(i, j)) {
                            CellState.EMPTY -> '~'
                            CellState.MISS -> '.'
                            CellState.SHIP -> 'o'
                            CellState.HIT -> 'x'
                            CellState.TARGET -> '~'
                        }
                    }
                }

                out.println(foo)

                foo = ""

                for (i in 0 ..< GRID_ROWS) {
                    for (j in 0 ..< GRID_COLS) {
                        foo += when (GlobalInstance.get().getOtherGridCellState(i, j)) {
                            CellState.EMPTY -> '~'
                            CellState.MISS -> '.'
                            CellState.SHIP -> 'o'
                            CellState.HIT -> 'x'
                            CellState.TARGET -> '~'
                        }
                    }
                }

                out.println(foo)

                foo = ""

                for (ship in fleets[storedFleetIndex]) {
                    foo += if (ship.isHorizontal) {
                        "1;"
                    } else {
                        "0;"
                    }

                    foo += if (ship.destroyed) {
                        "1;"
                    } else {
                        "0;"
                    }

                    for ((r, c) in ship.cells) {
                        foo += "$r-$c;"
                    }

                    foo = foo.substring(0, foo.length - 1)

                    foo += "|"
                }

                foo = foo.substring(0, foo.length - 1)

                out.println(foo)
            }
        } catch (_: java.io.IOException) {
            throw Exception(getString(R.string.main_error_access_game_state, MainActivity.GAME_STATE_FILENAME))
        }
    }

    private fun onBackAction() {
        val builder = AlertDialog.Builder(this@BattleshipActivity, R.style.AlertDialogStyle)
            .setTitle(
                if (GlobalInstance.get().b_isServer) {
                    getString(R.string.battleship_close_title)
                } else {
                    getString(R.string.battleship_exit_title)
                }
            )
            .setMessage(
                if (GlobalInstance.get().b_isServer) {
                    getString(R.string.battleship_close_message)
                } else {
                    getString(R.string.battleship_exit_message)
                }
            )
            .setPositiveButton(getString(R.string.text_yes)) { dialog, _ ->
                if (GlobalInstance.get().b_isServer) {
                    // add exit message for client
                    GlobalInstance.get().enqueueMessageBox("EXIT")

                    Thread.sleep(2500)
                } else {
                    // add exit message for server
                    GlobalInstance.get().enqueueMessageBox("EXIT")
                }

                // delete game state
                MainActivity.deleteGameState(cacheDir)

                setResult(MainActivity.RETURN_CODE_OWN_EXIT)
                finish()

                dialog.dismiss()
            }
            .setNegativeButton(
                getString(R.string.text_no)
            ) { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    private fun updateGridTitle(textView: TextView, ownFleet: Boolean) {
        if (ownFleet) {
            textView.text = getString(R.string.battle_ship_grid_title_own_fleet)
            textView.setBackgroundResource(R.color.colorPrimaryVariant)
            textView.setTextColor(getColor(R.color.colorOnPrimary))
        } else {
            if (GlobalInstance.get().s_otherUser.contentEquals("NO_OTHER_USER_SPECIFIED")) {
                textView.text = getString(R.string.battle_ship_grid_title_enemy_fleet)
            } else {
                textView.text = getString(R.string.battle_ship_grid_title_enemy_fleet_with_player, GlobalInstance.get().s_otherUser)
            }

            textView.setBackgroundResource(R.color.colorError)
            textView.setTextColor(getColor(R.color.colorOnError))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateGameMode(textView: TextView) {
        val foo = if (GlobalInstance.get().b_gameAdditionalOptionOne) {
            "\n" + getString(R.string.battleship_additional_option_one)
        } else {
            ""
        }

        when (GlobalInstance.get().s_gameMode) {
            GAME_MODE_ALTERNATE -> textView.text = getString(R.string.battleship_game_mode) + " " + getString(R.string.main_game_mode_alternate) + foo
            GAME_MODE_SHOT_EACH_SHIP -> textView.text = getString(R.string.battleship_game_mode) + " " + getString(R.string.main_game_mode_shot_each_ship) + foo
            GAME_MODE_3_SHOTS -> textView.text = getString(R.string.battleship_game_mode) + " " + getString(R.string.main_game_mode_3_shot) + foo
            GAME_MODE_5_SHOTS -> textView.text = getString(R.string.battleship_game_mode) + " " + getString(R.string.main_game_mode_5_shot) + foo
            else -> textView.text = getString(R.string.battleship_game_mode) + " " + getString(R.string.battleship_game_mode_unknown)
        }
    }

    private fun updateButtonState(button: Button) {
        when (currentButtonState) {
            ButtonState.HORIZONTAL -> {
                button.text = getString(R.string.battleship_button_state_horizontal)
                val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_arrows_horizontal)
                button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }

            ButtonState.VERTICAL -> {
                button.text = getString(R.string.battleship_button_state_vertical)
                val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_arrows_vertical)
                button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }

            ButtonState.CONFIRM -> {
                button.text = getString(R.string.battleship_button_state_confirm)
                val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_check)
                button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }

            ButtonState.FIRE -> {
                button.text = getString(R.string.battleship_button_state_fire)
                val drawable = AppCompatResources.getDrawable(this, R.drawable.ic_target)
                button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }
        }
    }

    private fun onAfterPlacedOrRemovedShip(status: Int, currentShip: Ship?) {
        when (status) {
            1 -> {
                currentButtonState = ButtonState.CONFIRM
                updateButtonState(gameButton)
                statusEditText.setText(getString(R.string.battleship_status_all_ships_placed))
            }
            -1 -> {
                if (currentButtonState == ButtonState.CONFIRM) {
                    if (recyclerViewAdapter.getShipRotation()) {
                        currentButtonState = ButtonState.HORIZONTAL
                        updateButtonState(gameButton)
                    } else {
                        currentButtonState = ButtonState.VERTICAL
                        updateButtonState(gameButton)
                    }
                }

                statusEditText.setText( Util.replacePlaceholders(
                    getString(R.string.battleship_status_place),
                    getString(
                        R.string.battleship_status_place_ship,
                        currentShip?.name,
                        currentShip?.size,
                        getString(R.string.battleship_status_boxes)
                    )
                ) )
            }
            -2 -> {
                statusEditText.setText( Util.replacePlaceholders(
                    getString(R.string.battleship_status_cannot_place),
                    getString(
                        R.string.battleship_status_place_ship,
                        currentShip?.name,
                        currentShip?.size,
                        getString(R.string.battleship_status_boxes)
                    )
                ) )
            }
            -3 -> {
                statusEditText.setText( Util.replacePlaceholders(
                    getString(R.string.battleship_status_must_not_touch),
                    getString(
                        R.string.battleship_status_place_ship,
                        currentShip?.name,
                        currentShip?.size,
                        getString(R.string.battleship_status_boxes)
                    )
                ) )
            }
        }
    }

    private fun onAfterPlacedTarget(x: Int, y: Int, valid: Boolean) {
        if (valid) {
            targetCoordinate = Pair(x, y)
            statusEditText.setText(getString(R.string.battleship_status_marked_target, printCoordinate(x, y)))
            currentButtonState = ButtonState.FIRE
            updateButtonState(gameButton)
            gameButton.visibility = View.VISIBLE
        } else {
            targetCoordinate = Pair(-1, -1)
            statusEditText.setText(getString(R.string.battleship_status_cannot_mark_target, printCoordinate(x, y)))
            gameButton.visibility = View.GONE
        }
    }

    private fun onAfterFireTarget(x: Int, y: Int) {
        targetCoordinate = Pair(-1, -1)
        GlobalInstance.get().setBoardState(BoardState.OTHER_BOARD)
        gameButton.visibility = View.GONE
        statusEditText.setText(getString(R.string.battleship_status_fired_to_target, printCoordinate(x, y)))
        GlobalInstance.get().enqueueMessageBox("FIRE;$x;$y")
        amountShots--
        SoundManager.playSound(1)

        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (GlobalInstance.get().getBoardState() != BoardState.END) {
                    if ((GlobalInstance.get().b_gameAdditionalOptionOne) && (GlobalInstance.get().getOtherGridCellState(y, x) == CellState.HIT)) {
                        amountShots++
                    }

                    if (amountShots > 0) {
                        GlobalInstance.get().setBoardState(BoardState.OTHER_BOARD_TARGET)
                        statusEditText.setText(getString(R.string.battleship_status_choose_another_target))
                        gameButton.visibility = View.VISIBLE
                    } else {
                        GlobalInstance.get().setBoardState(BoardState.OWN_BOARD)
                        updateGridTitle(gridTitle, true)
                        GlobalInstance.get().enqueueMessageBox("ROUND_FINISHED")
                        reloaded = false
                    }
                }
            }, 3500
        )
    }

    private fun printCoordinate(x: Int, y: Int): String {
        val foo = x + 1

        val bar = when (y) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            5 -> "F"
            6 -> "G"
            7 -> "H"
            8 -> "I"
            9 -> "J"
            10 -> "K"
            11 -> "L"
            12 -> "M"
            13 -> "N"
            14 -> "O"
            15 -> "P"
            16 -> "Q"
            17 -> "R"
            18 -> "S"
            19 -> "T"
            20 -> "U"
            21 -> "V"
            22 -> "W"
            23 -> "X"
            24 -> "Y"
            25 -> "Z"
            else -> ""
        }

        return "( $foo | $bar )"
    }

    private fun runConfettiView(confettiCount: Int = 100) {
        val confettiView = ConfettiView(this, confettiCount = confettiCount)

        battleshipActivityViewGroup.addView(
            confettiView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        confettiView.onFinished = {
            battleshipActivityViewGroup.removeView(confettiView)
        }

        // remove confetti after 6 seconds
        confettiView.postDelayed({
            if (confettiView.parent != null) {
                battleshipActivityViewGroup.removeView(confettiView)
            }
        }, 6000)
    }

    override fun onStart() {
        super.onStart()

        Log.v(TAG, "onStart $TAG")
    }

    override fun onResume() {
        super.onResume()

        Log.v(TAG, "onResume $TAG")
    }

    override fun onPause() {
        super.onPause()

        Log.v(TAG, "onPause $TAG")
    }

    override fun onStop() {
        super.onStop()

        Log.v(TAG, "onStop $TAG")
    }

    override fun onRestart() {
        super.onRestart()

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            uiThread.interrupt()
        } catch (_: Exception) {

        }

        if ((GlobalInstance.get().b_isServer) && (GlobalInstance.get().o_communicationLobby != null)) {
            try {
                GlobalInstance.get().o_threadLobby?.interrupt()
            } catch (_: Exception) {

            } finally {
                GlobalInstance.get().o_threadLobby = null
            }

            try {
                GlobalInstance.get().o_communicationLobby?.stop()
            } catch (_: Exception) {

            } finally {
                GlobalInstance.get().o_communicationLobby = null
            }

            GlobalInstance.get().clearClientLobbyEntries()
        }

        if (GlobalInstance.get().o_communicationBattleship != null) {
            if (GlobalInstance.get().b_isServer) {
                Thread.sleep(3000)
            } else {
                Thread.sleep(1500)
            }
        }

        try {
            GlobalInstance.get().o_threadBattleship?.interrupt()
        } catch (_: Exception) {

        } finally {
            GlobalInstance.get().o_threadBattleship = null
        }

        try {
            GlobalInstance.get().o_communicationBattleship?.stop()
        } catch (_: Exception) {

        } finally {
            GlobalInstance.get().o_communicationBattleship = null
        }

        GlobalInstance.get().b_serverClosed = false
        GlobalInstance.get().b_isServer = false
        GlobalInstance.get().s_user = "NO_USER_SPECIFIED"
        GlobalInstance.get().s_otherUser = "NO_OTHER_USER_SPECIFIED"
        GlobalInstance.get().s_gameMode = GAME_MODE_NO_MODE
        GlobalInstance.get().b_gameAdditionalOptionOne = false
        GlobalInstance.get().i_fleetIndex = 0
        GlobalInstance.get().clearClientLobbyEntries()
        GlobalInstance.get().clearMessageBox()
        GlobalInstance.get().clearSnackbarBox()

        GlobalInstance.get().setBoardState(BoardState.PLACEMENT)
        GlobalInstance.get().setPreviousBoardState(BoardState.PLACEMENT)
        GlobalInstance.get().setOtherBoardState(BoardState.PLACEMENT)
        GlobalInstance.get().setPreviousOtherBoardState(BoardState.PLACEMENT)

        GlobalInstance.get().resetOwnGrid()
        GlobalInstance.get().resetOtherGrid()
        GlobalInstance.get().resetOtherNoTargetGrid()
        GlobalInstance.get().resetOwnGridEnd()
        GlobalInstance.get().resetOtherGridEnd()

        Log.v(TAG, "onDestroy $TAG")
    }
}