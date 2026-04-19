package net.forestany.battleship.game

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.battleship.GlobalInstance
import net.forestany.battleship.MainActivity.Companion.GRID_COLS
import net.forestany.battleship.MainActivity.Companion.GRID_ROWS
import net.forestany.battleship.R

class BattleshipGridAdapter(
    private var fleet: List<Ship>,
    private val cellSize: Int,
    private val cellPadding: Int
) : RecyclerView.Adapter<BattleshipGridAdapter.CellViewHolder>() {
    interface GridAdapterDelegate {
        fun afterPlacedOrRemovedShip(status: Int, currentShip: Ship? = null)
        fun afterPlacedTarget(row: Int, col: Int, valid: Boolean)
        fun afterFireTarget(row: Int, col: Int)
    }

    var delegate: GridAdapterDelegate? = null
    private var currentShipIndex = 0
    private var isHorizontal = true

    enum class CellState {
        EMPTY,      // ~
        SHIP,       // o
        HIT,        // x
        MISS,       // .
        TARGET      // #
    }

    class CellViewHolder(frame: FrameLayout, val image: ImageView) : RecyclerView.ViewHolder(frame)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val frame = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(cellSize, cellSize)
            setPadding(cellPadding, cellPadding, cellPadding, cellPadding)
        }

        val imageView = ImageView(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        frame.addView(imageView)
        return CellViewHolder(frame, imageView)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        holder.itemView.layoutParams.width = cellSize
        holder.itemView.layoutParams.height = cellSize
        holder.itemView.setPadding(cellPadding, cellPadding, cellPadding, cellPadding)

        val row = position / GRID_COLS
        val col = position % GRID_COLS
        val ownState = GlobalInstance.get().getOwnGrid()[row][col]
        val otherState = GlobalInstance.get().getOtherGrid()[row][col]

        // show recycler view tiles depending on board state and cell state
        when (GlobalInstance.get().getBoardState()) {
            BattleshipActivity.BoardState.PLACEMENT,
            BattleshipActivity.BoardState.PLACEMENT_FINISHED_SERVER,
            BattleshipActivity.BoardState.PLACEMENT_FINISHED_CLIENT -> {
                if (ownState == CellState.SHIP) {
                    holder.image.setImageResource(getOwnShipPart(row, col, false))
                } else {
                    holder.image.setImageResource(R.drawable.tile_water)
                }
            }

            BattleshipActivity.BoardState.ROUND_SERVER_TARGET,
            BattleshipActivity.BoardState.ROUND_SERVER,
            BattleshipActivity.BoardState.ROUND_SERVER_FINISHED,
            BattleshipActivity.BoardState.ROUND_SERVER_KEEP -> {
                if (!GlobalInstance.get().b_isServer)
                    when (ownState) {
                        CellState.SHIP -> holder.image.setImageResource(getOwnShipPart(row, col, false))
                        CellState.HIT -> holder.image.setImageResource(getOwnShipPart(row, col, true))
                        CellState.MISS -> holder.image.setImageResource(R.drawable.tile_miss)
                        else -> holder.image.setImageResource(R.drawable.tile_water)
                    }
                else
                    when (otherState) {
                        CellState.HIT -> holder.image.setImageResource(R.drawable.ship_single_hit)
                        CellState.MISS -> holder.image.setImageResource(R.drawable.tile_miss)
                        CellState.TARGET -> holder.image.setImageResource(R.drawable.crosshair)
                        else -> holder.image.setImageResource(R.drawable.tile_water)
                    }
            }

            BattleshipActivity.BoardState.ROUND_CLIENT_TARGET,
            BattleshipActivity.BoardState.ROUND_CLIENT,
            BattleshipActivity.BoardState.ROUND_CLIENT_FINISHED,
            BattleshipActivity.BoardState.ROUND_CLIENT_KEEP -> {
                if (GlobalInstance.get().b_isServer)
                    when (ownState) {
                        CellState.SHIP -> holder.image.setImageResource(getOwnShipPart(row, col, false))
                        CellState.HIT -> holder.image.setImageResource(getOwnShipPart(row, col, true))
                        CellState.MISS -> holder.image.setImageResource(R.drawable.tile_miss)
                        else -> holder.image.setImageResource(R.drawable.tile_water)
                    }
                else
                    when (otherState) {
                        CellState.HIT -> holder.image.setImageResource(R.drawable.ship_single_hit)
                        CellState.MISS -> holder.image.setImageResource(R.drawable.tile_miss)
                        CellState.TARGET -> holder.image.setImageResource(R.drawable.crosshair)
                        else -> holder.image.setImageResource(R.drawable.tile_water)
                    }
            }

            BattleshipActivity.BoardState.END -> {
                when (otherState) {
                    CellState.SHIP -> holder.image.setImageResource(getOtherShipPart(row, col, false))
                    CellState.HIT -> holder.image.setImageResource(getOtherShipPart(row, col, true))
                    CellState.MISS -> holder.image.setImageResource(R.drawable.tile_miss)
                    else -> holder.image.setImageResource(R.drawable.tile_water)
                }
            }

//            else -> {
//                holder.image.setImageResource(R.drawable.crosshair)
//            }
        }

        // on click listener for recycler view tile
        holder.itemView.setOnClickListener {
            when (GlobalInstance.get().getBoardState()) {
                BattleshipActivity.BoardState.PLACEMENT -> {
                    val ship = fleet.find { it.placed && it.cells.isNotEmpty() && it.cells.first() == (row to col) }

                    if (ship != null) {
                        removeShip(ship)
                    } else {
                        placeShip(row, col)
                    }
                }

                BattleshipActivity.BoardState.ROUND_SERVER_TARGET -> {
                    if (GlobalInstance.get().b_isServer)
                        targetShot(row, col)
                }

                BattleshipActivity.BoardState.ROUND_CLIENT_TARGET -> {
                    if (!GlobalInstance.get().b_isServer)
                        targetShot(row, col)
                }

                else -> {

                }
            }
        }
    }

    override fun getItemCount(): Int = GRID_ROWS * GRID_COLS

    fun overwriteFleet(newFleet: List<Ship>) {
        fleet = newFleet
        currentShipIndex = 0
    }

    fun getOwnShipsEnd(): String {
        val foo = StringBuilder("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

        /*
        fine -> hit

        qwe     asd
        r       t
        f       g
        v       b
        */

        for (ship in fleet) {
            var i = 1

            for ((r, c) in ship.cells) {
                val hit = (GlobalInstance.get().getOwnGrid()[r][c] == CellState.HIT)

                when (i) {
                    1 -> {
                        if (ship.isHorizontal) {
                            if (hit) {
                                foo[r * 10 + c] = 'a'
                            } else {
                                foo[r * 10 + c] = 'q'
                            }
                        } else {
                            if (hit) {
                                foo[r * 10 + c] = 't'
                            } else {
                                foo[r * 10 + c] = 'r'
                            }
                        }
                    }
                    ship.size -> {
                        if (ship.isHorizontal) {
                            if (hit) {
                                foo[r * 10 + c] = 'd'
                            } else {
                                foo[r * 10 + c] = 'e'
                            }
                        } else {
                            if (hit) {
                                foo[r * 10 + c] = 'b'
                            } else {
                                foo[r * 10 + c] = 'v'
                            }
                        }
                    }
                    else -> {
                        if (ship.isHorizontal) {
                            if (hit) {
                                foo[r * 10 + c] = 's'
                            } else {
                                foo[r * 10 + c] = 'w'
                            }
                        } else {
                            if (hit) {
                                foo[r * 10 + c] = 'g'
                            } else {
                                foo[r * 10 + c] = 'f'
                            }
                        }
                    }
                }

                i++
            }
        }

        for (i in 0 ..< GRID_ROWS) {
            for (j in 0 ..< GRID_COLS) {
                if (GlobalInstance.get().getOwnGrid()[i][j] == CellState.MISS) {
                    foo[i * 10 + j] = '.'
                }
            }
        }

        return foo.toString()
    }

    private fun getOwnShipPart(row: Int, col: Int, hit: Boolean): Int {
        for (ship in fleet) {
            var i = 1

            for ((r, c) in ship.cells) {
                if ((row == r) && (col == c)) {
                    when (i) {
                        1 -> {
                            return if (ship.isHorizontal) {
                                if (hit) {
                                    R.drawable.ship_bow_hit
                                } else {
                                    R.drawable.ship_bow
                                }
                            } else {
                                if (hit) {
                                    R.drawable.ship_bow_hit_vertical
                                } else {
                                    R.drawable.ship_bow_vertical
                                }
                            }
                        }
                        ship.size -> {
                            return if (ship.isHorizontal) {
                                if (hit) {
                                    R.drawable.ship_stern_hit
                                } else {
                                    R.drawable.ship_stern
                                }
                            } else {
                                if (hit) {
                                    R.drawable.ship_stern_hit_vertical
                                } else {
                                    R.drawable.ship_stern_vertical
                                }
                            }
                        }
                        else -> {
                            return if (ship.isHorizontal) {
                                if (hit) {
                                    R.drawable.ship_mid_hit
                                } else {
                                    R.drawable.ship_mid
                                }
                            } else {
                                if (hit) {
                                    R.drawable.ship_mid_hit_vertical
                                } else {
                                    R.drawable.ship_mid_vertical
                                }
                            }
                        }
                    }
                }

                i++
            }
        }

        return R.drawable.tile_water
    }

    private fun getOtherShipPart(row: Int, col: Int, hit: Boolean): Int {
        /*
        fine -> hit

        qwe     asd
        r       t
        f       g
        v       b
        */

        val foo = StringBuilder(GlobalInstance.get().getOtherGridEnd())

        return when (foo[row * 10 + col]) {
            'q' ->  if (hit) R.drawable.ship_bow_hit else R.drawable.ship_bow
            'w' ->  if (hit) R.drawable.ship_mid_hit else R.drawable.ship_mid
            'e' ->  if (hit) R.drawable.ship_stern_hit else R.drawable.ship_stern
            'a' ->  R.drawable.ship_bow_hit
            's' ->  R.drawable.ship_mid_hit
            'd' ->  R.drawable.ship_stern_hit
            'r' ->  if (hit) R.drawable.ship_bow_hit_vertical else R.drawable.ship_bow_vertical
            'f' ->  if (hit) R.drawable.ship_mid_hit_vertical else R.drawable.ship_mid_vertical
            'v' ->  if (hit) R.drawable.ship_stern_hit_vertical else R.drawable.ship_stern_vertical
            't' ->  R.drawable.ship_bow_hit_vertical
            'g' ->  R.drawable.ship_mid_hit_vertical
            'b' ->  R.drawable.ship_stern_hit_vertical
            else -> R.drawable.tile_water
        }
    }

/*
//    private fun getOtherShipPartGuessing(row: Int, col: Int, hit: Boolean): Int {
//        val start = Pair(row, col - 1)
//        val top = Pair(row - 1, col)
//        val end = Pair(row, col + 1)
//        val bottom = Pair(row + 1, col)
//
//        var startState = CellState.EMPTY
//        var topState = CellState.EMPTY
//        var endState = CellState.EMPTY
//        var bottomState = CellState.EMPTY
//
//        if (start.second >= 0) {
//            startState = GlobalInstance.get().getOtherNoTargetGridCellState(start.first, start.second)
//        }
//
//        if (top.first >= 0) {
//            topState = GlobalInstance.get().getOtherNoTargetGridCellState(top.first, top.second)
//        }
//
//        if ((end.second >= 0) && (end.second < GRID_COLS)) {
//            endState = GlobalInstance.get().getOtherNoTargetGridCellState(end.first, end.second)
//        }
//
//        if ((bottom.first >= 0) && (bottom.first < GRID_COLS)) {
//            bottomState = GlobalInstance.get().getOtherNoTargetGridCellState(bottom.first, bottom.second)
//        }
//
//        /*
//         x   x   x
//        xoo ooo oox
//         x   x   x
//
//         x   o   o
//        xox xox xox
//         o   o   x
//         */
//
//        if (
//            ((startState == CellState.EMPTY) || (startState == CellState.MISS)) &&
//            ((topState == CellState.EMPTY) || (topState == CellState.MISS)) &&
//            ((endState == CellState.SHIP) || (endState == CellState.HIT)) &&
//            ((bottomState == CellState.EMPTY) || (bottomState == CellState.MISS))
//        ) {
//            return if (hit) R.drawable.ship_bow_hit else R.drawable.ship_bow
//        } else if (
//            ((startState == CellState.SHIP) || (startState == CellState.HIT)) &&
//            ((topState == CellState.EMPTY) || (topState == CellState.MISS)) &&
//            ((endState == CellState.SHIP) || (endState == CellState.HIT)) &&
//            ((bottomState == CellState.EMPTY) || (bottomState == CellState.MISS))
//        ) {
//            return if (hit) R.drawable.ship_mid_hit else R.drawable.ship_mid
//        } else if (
//            ((startState == CellState.SHIP) || (startState == CellState.HIT)) &&
//            ((topState == CellState.EMPTY) || (topState == CellState.MISS)) &&
//            ((endState == CellState.EMPTY) || (endState == CellState.MISS)) &&
//            ((bottomState == CellState.EMPTY) || (bottomState == CellState.MISS))
//        ) {
//            return if (hit) R.drawable.ship_stern_hit else R.drawable.ship_stern
//        } else if (
//            ((startState == CellState.EMPTY) || (startState == CellState.MISS)) &&
//            ((topState == CellState.EMPTY) || (topState == CellState.MISS)) &&
//            ((endState == CellState.EMPTY) || (endState == CellState.MISS)) &&
//            ((bottomState == CellState.SHIP) || (bottomState == CellState.HIT))
//        ) {
//            return if (hit) R.drawable.ship_bow_hit_vertical else R.drawable.ship_bow_vertical
//        } else if (
//            ((startState == CellState.EMPTY) || (startState == CellState.MISS)) &&
//            ((topState == CellState.SHIP) || (topState == CellState.HIT)) &&
//            ((endState == CellState.EMPTY) || (endState == CellState.MISS)) &&
//            ((bottomState == CellState.SHIP) || (bottomState == CellState.HIT))
//        ) {
//            return if (hit) R.drawable.ship_mid_hit_vertical else R.drawable.ship_mid_vertical
//        } else if (
//            ((startState == CellState.EMPTY) || (startState == CellState.MISS)) &&
//            ((topState == CellState.SHIP) || (topState == CellState.HIT)) &&
//            ((endState == CellState.EMPTY) || (endState == CellState.MISS)) &&
//            ((bottomState == CellState.EMPTY) || (bottomState == CellState.MISS))
//        ) {
//            return if (hit) R.drawable.ship_stern_hit_vertical else R.drawable.ship_stern_vertical
//        }
//
//        return R.drawable.tile_water
//    }
*/

    private fun placeShip(startRow: Int, startCol: Int) {
        // all ships are placed
        if (fleet.all { it.placed }) {
            delegate?.afterPlacedOrRemovedShip(1)
            return
        }

        val ship = fleet[currentShipIndex]
        val cells = mutableListOf<Pair<Int, Int>>()

        // do placement on the grid, depending on ship size
        for (i in 0 until ship.size) {
            val r = if (isHorizontal) startRow else startRow + i
            val c = if (isHorizontal) startCol + i else startCol

            // ship will go off grid
            if (r !in 0 until GRID_ROWS || c !in 0 until GRID_COLS) {
                delegate?.afterPlacedOrRemovedShip(-2, fleet[currentShipIndex])
                return
            }

            // another ship is in the way
            if (GlobalInstance.get().getOwnGrid()[r][c] != CellState.EMPTY) {
                delegate?.afterPlacedOrRemovedShip(-2, fleet[currentShipIndex])
                return
            }

            // placement allowing ship edge to edge
            if (!GlobalInstance.get().b_gameAdditionalOptionTwo) {
                when (r) {
                    0 -> {
                        // look r + 1 != EMPTY
                        if (GlobalInstance.get().getOwnGrid()[1][c] != CellState.EMPTY) {
                            delegate?.afterPlacedOrRemovedShip(-3, fleet[currentShipIndex])
                            return
                        }
                    }

                    GRID_ROWS - 1 -> {
                        // look r - 1 != EMPTY
                        if (GlobalInstance.get().getOwnGrid()[r - 1][c] != CellState.EMPTY) {
                            delegate?.afterPlacedOrRemovedShip(-3, fleet[currentShipIndex])
                            return
                        }
                    }

                    else -> {
                        // look r + 1 or r - 1 != EMPTY
                        if ((GlobalInstance.get().getOwnGrid()[r + 1][c] != CellState.EMPTY) || (GlobalInstance.get().getOwnGrid()[r - 1][c] != CellState.EMPTY)) {
                            delegate?.afterPlacedOrRemovedShip(-3, fleet[currentShipIndex])
                            return
                        }
                    }
                }

                when (c) {
                    0 -> {
                        // look c + 1 != EMPTY
                        if (GlobalInstance.get().getOwnGrid()[r][1] != CellState.EMPTY) {
                            delegate?.afterPlacedOrRemovedShip(-3, fleet[currentShipIndex])
                            return
                        }
                    }

                    GRID_COLS - 1 -> {
                        // look c - 1 != EMPTY
                        if (GlobalInstance.get().getOwnGrid()[r][c - 1] != CellState.EMPTY) {
                            delegate?.afterPlacedOrRemovedShip(-3, fleet[currentShipIndex])
                            return
                        }
                    }

                    else -> {
                        // look c + 1 or c - 1 != EMPTY
                        if ((GlobalInstance.get().getOwnGrid()[r][c + 1] != CellState.EMPTY) || (GlobalInstance.get().getOwnGrid()[r][c - 1] != CellState.EMPTY)) {
                            delegate?.afterPlacedOrRemovedShip(-3, fleet[currentShipIndex])
                            return
                        }
                    }
                }
            }

            cells.add(r to c)
        }

        // place ship
        for ((r, c) in cells) {
            GlobalInstance.get().getOwnGrid()[r][c] = CellState.SHIP
        }

        ship.placed = true
        ship.isHorizontal = isHorizontal
        ship.cells = cells

        // look for next ship to be placed in fleet array
        if (!fleet.all { it.placed }) {
            do {
                currentShipIndex++

                if (currentShipIndex >= fleet.size) {
                    currentShipIndex = 0
                }
            } while (fleet[currentShipIndex].placed)
        }

        if (fleet.all { it.placed }) {
            delegate?.afterPlacedOrRemovedShip(1)
        } else {
            delegate?.afterPlacedOrRemovedShip(-1, fleet[currentShipIndex])
        }

        upgradeGridTiles()
    }

    private fun targetShot(row: Int, col: Int) {
        if ((GlobalInstance.get().getOtherGrid()[row][col] == CellState.EMPTY) || (GlobalInstance.get().getOtherGrid()[row][col] == CellState.SHIP)) {
            // remove old target coordinate
            for (i in 0 ..< GRID_ROWS) {
                for (j in 0 ..< GRID_COLS) {
                    if (GlobalInstance.get().getOtherGrid()[i][j] == CellState.TARGET) {
                        GlobalInstance.get().getOtherGrid()[i][j] = GlobalInstance.get().getOtherNoTargetGridCellState(i, j)
                    }
                }
            }

            GlobalInstance.get().getOtherGrid()[row][col] = CellState.TARGET
            delegate?.afterPlacedTarget(col, row, true)
            upgradeGridTiles()
        } else if (GlobalInstance.get().getOtherGrid()[row][col] != CellState.TARGET) {
            // remove old target coordinate
            for (i in 0 ..< GRID_ROWS) {
                for (j in 0 ..< GRID_COLS) {
                    if (GlobalInstance.get().getOtherGrid()[i][j] == CellState.TARGET) {
                        GlobalInstance.get().getOtherGrid()[i][j] = GlobalInstance.get().getOtherNoTargetGridCellState(i, j)
                    }
                }
            }

            delegate?.afterPlacedTarget(col, row, false)
            upgradeGridTiles()
        }
    }

    fun fireShot(row: Int, col: Int) {
        delegate?.afterFireTarget(col, row)
    }

    fun getShipRotation(): Boolean {
        return isHorizontal
    }

    fun setShipRotation(horizontal: Boolean) {
        isHorizontal = horizontal
    }

    fun getCurrentShip(): Ship {
        return fleet[currentShipIndex]
    }

    private fun removeShip(ship: Ship) {
        // rewind cell states to empty
        for ((r, c) in ship.cells) {
            GlobalInstance.get().getOwnGrid()[r][c] = CellState.EMPTY
        }

        ship.placed = false
        ship.cells.clear()

        // allow placing it again
        currentShipIndex = fleet.indexOf(ship)

        if (fleet.all { it.placed }) {
            delegate?.afterPlacedOrRemovedShip(1)
        } else {
            delegate?.afterPlacedOrRemovedShip(-1, fleet[currentShipIndex])
        }

        upgradeGridTiles()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun upgradeGridTiles() {
        notifyDataSetChanged()
    }

    fun checkShipDestroyed(): Ship? {
        // check fleet for destroyed ship in the last few seconds
        for (ship in fleet) {
            // skip already destroyed ships
            if (ship.destroyed) {
                continue
            }

            var isDestroyed = true

            // if one cell state of current ship is not HIT, it is not destroyed
            for ((r, c) in ship.cells) {
                if (GlobalInstance.get().getOwnGrid()[r][c] != CellState.HIT) {
                    isDestroyed = false
                }
            }

            if (isDestroyed) {
                ship.destroyed = true
                return ship
            }
        }

        return null
    }

    fun checkFleetDestroyed(): Boolean {
        var foo = true

        for (ship in fleet) {
            if (!ship.destroyed) {
                foo = false
            }
        }

        return foo
    }

    fun amountShipsNotDestroyed(): Int {
        var foo = 0

        for (ship in fleet) {
            if (!ship.destroyed) {
                foo++
            }
        }

        return foo
    }
}