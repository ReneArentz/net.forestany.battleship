package net.forestany.battleship

import net.forestany.battleship.MainActivity.Companion.GAME_MODE_NO_MODE
import net.forestany.battleship.MainActivity.Companion.GRID_COLS
import net.forestany.battleship.MainActivity.Companion.GRID_ROWS
import net.forestany.battleship.game.BattleshipActivity
import net.forestany.battleship.game.BattleshipGridAdapter.CellState
import net.forestany.forestj.lib.net.sock.com.Communication
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GlobalInstance {
    companion object {
        @Volatile
        private var instance: GlobalInstance? = null

        fun get(): GlobalInstance {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = GlobalInstance()
                    }
                }
            }

            return instance!!
        }
    }

    var o_threadLobby: Thread? = null
    var o_threadBattleship: Thread? = null

    var o_communicationLobby: Communication? = null
    var o_communicationBattleship: Communication? = null

    var b_serverClosed: Boolean = false
    var b_isServer: Boolean = false
    var s_user: String = "NO_USER_SPECIFIED"
    var s_otherUser: String = "NO_OTHER_USER_SPECIFIED"
    var s_gameMode: String = GAME_MODE_NO_MODE
    var b_gameAdditionalOptionOne: Boolean = false
    var b_gameAdditionalOptionTwo: Boolean = false
    var i_fleetIndex: Int = 0

    private val o_lockPing = ReentrantLock()
    private var l_ping: Long = 0

    fun getPing(): Long {
        var l_foo: Long

        o_lockPing.withLock {
            l_foo = l_ping
        }

        return l_foo
    }

    fun setPing(p_l_value: Long) {
        o_lockPing.withLock {
            l_ping = p_l_value
        }
    }

    private var o_lockBoardState = ReentrantLock()
    private var boardState: BattleshipActivity.BoardState = BattleshipActivity.BoardState.PLACEMENT

    fun getBoardState(): BattleshipActivity.BoardState {
        o_lockBoardState.withLock {
            return boardState
        }
    }

    fun setBoardState(newBoardState: BattleshipActivity.BoardState) {
        if (boardState != newBoardState) {
            o_lockBoardState.withLock {
                boardState = newBoardState
                //android.util.Log.v("Global", "changed board state to:\t$boardState")
            }
        }
    }

    private var o_lockOtherBoardState = ReentrantLock()
    private var otherBoardState: BattleshipActivity.BoardState = BattleshipActivity.BoardState.PLACEMENT

    fun getOtherBoardState(): BattleshipActivity.BoardState {
        o_lockOtherBoardState.withLock {
            return otherBoardState
        }
    }

    fun setOtherBoardState(newOtherBoardState: BattleshipActivity.BoardState) {
        if (otherBoardState != newOtherBoardState) {
            o_lockOtherBoardState.withLock {
                otherBoardState = newOtherBoardState
            }
        }
    }

    private var o_lockOwnGrid = ReentrantLock()
    private var o_lockOtherGrid = ReentrantLock()
    private var o_lockOtherGridNoTarget = ReentrantLock()
    private var o_lockOwnGridEnd = ReentrantLock()
    private var o_lockOtherGridEnd = ReentrantLock()
    private var ownGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
    private var otherGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
    private var otherGridNoTarget: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
    private var ownGridEnd: String = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
    private var otherGridEnd: String = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

    fun getOwnGrid(): Array<Array<CellState>> {
        o_lockOwnGrid.withLock {
            return ownGrid
        }
    }

    fun setOwnGrid(newOwnGrid: Array<Array<CellState>>) {
        o_lockOwnGrid.withLock {
            ownGrid = newOwnGrid
        }
    }

    fun getOwnGridCellState(row: Int, col: Int): CellState {
        o_lockOwnGrid.withLock {
            return ownGrid[row][col]
        }
    }

    fun setOwnGridCellState(row: Int, col: Int, cellState: CellState) {
        o_lockOwnGrid.withLock {
            ownGrid[row][col] = cellState
        }
    }

    fun getOtherGrid(): Array<Array<CellState>> {
        o_lockOtherGrid.withLock {
            return otherGrid
        }
    }

    fun setOtherGrid(newOtherGrid: Array<Array<CellState>>) {
        o_lockOtherGrid.withLock {
            otherGrid = newOtherGrid
        }
    }

    fun getOtherGridCellState(row: Int, col: Int): CellState {
        o_lockOtherGrid.withLock {
            return otherGrid[row][col]
        }
    }

    fun setOtherGridCellState(row: Int, col: Int, cellState: CellState) {
        o_lockOtherGrid.withLock {
            otherGrid[row][col] = cellState
        }
    }

    fun setOtherGridNoTarget(newOtherGrid: Array<Array<CellState>>) {
        o_lockOtherGridNoTarget.withLock {
            otherGridNoTarget = newOtherGrid
        }
    }

    fun getOtherNoTargetGridCellState(row: Int, col: Int): CellState {
        o_lockOtherGridNoTarget.withLock {
            return otherGridNoTarget[row][col]
        }
    }

    fun getOwnGridEnd(): String {
        o_lockOwnGridEnd.withLock {
            return ownGridEnd
        }
    }

    fun setOwnGridEnd(newOwnGridEnd: String) {
        o_lockOwnGridEnd.withLock {
            ownGridEnd = newOwnGridEnd
        }
    }

    fun getOtherGridEnd(): String {
        o_lockOtherGridEnd.withLock {
            return otherGridEnd
        }
    }

    fun setOtherGridEnd(newOtherGridEnd: String) {
        o_lockOtherGridEnd.withLock {
            otherGridEnd = newOtherGridEnd
        }
    }

    fun resetOwnGrid() {
        o_lockOwnGrid.withLock {
            ownGrid = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
        }
    }

    fun resetOtherGrid() {
        o_lockOtherGrid.withLock {
            otherGrid = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
        }
    }

    fun resetOtherNoTargetGrid() {
        o_lockOtherGridNoTarget.withLock {
            otherGridNoTarget = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
        }
    }

    fun resetOwnGridEnd() {
        o_lockOwnGridEnd.withLock {
            ownGridEnd = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        }
    }

    fun resetOtherGridEnd() {
        o_lockOtherGridEnd.withLock {
            otherGridEnd = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
        }
    }

    private var o_lockLobbyEntries = ReentrantLock()
    private var m_clientLobbyEntries: MutableMap<java.time.LocalDateTime, String> = HashMap()

    fun getClientLobbyEntries(): MutableMap<java.time.LocalDateTime, String> {
        var m_foo: MutableMap<java.time.LocalDateTime, String>

        o_lockLobbyEntries.withLock {
            m_foo = m_clientLobbyEntries.toMutableMap()
        }

        return m_foo
    }

    fun addClientLobbyEntry(p_o_value: java.time.LocalDateTime, p_s_value: String) {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.put(p_o_value, p_s_value)
        }
    }

    fun removeClientLobbyEntry(p_o_value: java.time.LocalDateTime) {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.remove(p_o_value)
        }
    }

    fun removeClientLobbyEntryByValue(p_s_value: String) {
        o_lockLobbyEntries.withLock {
            if (m_clientLobbyEntries.containsValue(p_s_value)) {
                var o_key: java.time.LocalDateTime? = null

                for ((key, value) in m_clientLobbyEntries) {
                    if (value.contentEquals(p_s_value)) {
                        o_key = key
                    }
                }

                if (o_key != null) {
                    m_clientLobbyEntries.remove(o_key)
                }
            }
        }
    }

    fun clearClientLobbyEntries() {
        o_lockLobbyEntries.withLock {
            m_clientLobbyEntries.clear()
        }
    }

    private val o_lockPreferences = ReentrantLock()
    private val m_preferences: MutableMap<String, Any?> = HashMap()

    fun getPreferences(): MutableMap<String, Any?> {
        var m_foo: MutableMap<String, Any?>

        o_lockPreferences.withLock {
            m_foo = m_preferences.toMutableMap()
        }

        return m_foo
    }

    fun addPreference(p_s_value: String, p_o_value: Any?) {
        o_lockPreferences.withLock {
            m_preferences.put(p_s_value, p_o_value)
        }
    }

    fun clearPreferences() {
        o_lockPreferences.withLock {
            m_preferences.clear()
        }
    }

    private var o_lockMessageBox = ReentrantLock()
    private var q_messageBox: Queue<String> = LinkedList()

    fun enqueueMessageBox(p_o_foo: String) {
        o_lockMessageBox.withLock {
            q_messageBox.add(p_o_foo)
        }
    }

    fun dequeueMessageBox(): String? {
        var o_foo: String?

        o_lockMessageBox.withLock {
            o_foo = q_messageBox.poll()
        }

        return o_foo
    }

    fun currentMessage(): String? {
        var o_foo: String?

        o_lockMessageBox.withLock {
            o_foo = q_messageBox.peek()
        }

        return o_foo
    }

    fun getMessageBoxAmount(): Int {
        var i_foo: Int

        o_lockMessageBox.withLock {
            i_foo = q_messageBox.size
        }

        return i_foo
    }

    fun clearMessageBox() {
        o_lockMessageBox.withLock {
            do {
                q_messageBox.size
            } while (q_messageBox.poll() != null)
        }
    }

    private var o_lockSnackbarBox = ReentrantLock()
    private var q_snackbarBox: Queue<String> = LinkedList()

    fun enqueueSnackbarBox(p_o_foo: String) {
        o_lockSnackbarBox.withLock {
            q_snackbarBox.add(p_o_foo)
        }
    }

    fun dequeueSnackbarBox(): String? {
        var o_foo: String?

        o_lockSnackbarBox.withLock {
            o_foo = q_snackbarBox.poll()
        }

        return o_foo
    }

    fun clearSnackbarBox() {
        o_lockSnackbarBox.withLock {
            do {
                q_snackbarBox.size
            } while (q_snackbarBox.poll() != null)
        }
    }
}