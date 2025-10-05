package net.forestany.battleship.game

import net.forestany.battleship.GlobalInstance
import net.forestany.battleship.MainActivity.Companion.GRID_COLS
import net.forestany.battleship.MainActivity.Companion.GRID_ROWS
import net.forestany.battleship.game.BattleshipGridAdapter.CellState

class Message (private var command: String = "") {
    override fun toString(): String {
        var s_foo = "$command|"

        s_foo += "${GlobalInstance.get().s_gameMode}|"

        s_foo += if (GlobalInstance.get().b_gameAdditionalOptionOne) {
            "1|"
        } else {
            "0|"
        }

        s_foo += if (GlobalInstance.get().b_gameAdditionalOptionTwo) {
            "1|"
        } else {
            "0|"
        }

        s_foo += "${GlobalInstance.get().i_fleetIndex}|"

        s_foo += "${GlobalInstance.get().getBoardState()}|"
        s_foo += "${GlobalInstance.get().s_user}|"
        s_foo += "${GlobalInstance.get().getOtherBoardState()}|"

        if (GlobalInstance.get().getBoardState() != BattleshipActivity.BoardState.END) {
            for (i in 0..<GRID_ROWS) {
                for (j in 0..<GRID_COLS) {
                    s_foo += when (GlobalInstance.get().getOwnGridCellState(i, j)) {
                        CellState.EMPTY -> "~"
                        CellState.MISS -> "."
                        CellState.SHIP -> "o"
                        CellState.HIT -> "x"
                        CellState.TARGET -> "#"
                    }
                }
            }
        } else {
            s_foo += GlobalInstance.get().getOwnGridEnd()
        }

        s_foo += "|"

        for (i in 0 ..< GRID_ROWS) {
            for (j in 0 ..< GRID_COLS) {
                s_foo += when (GlobalInstance.get().getOtherGridCellState(i, j)) {
                    CellState.EMPTY -> "~"
                    CellState.MISS -> "."
                    CellState.SHIP -> "o"
                    CellState.HIT -> "x"
                    CellState.TARGET -> "#"
                }
            }
        }

        return s_foo
    }
}