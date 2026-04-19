package net.forestany.battleship.game

import net.forestany.battleship.MainActivity.Companion.GRID_COLS
import net.forestany.battleship.MainActivity.Companion.GRID_ROWS
import net.forestany.battleship.game.BattleshipGridAdapter.CellState

class GameState {
    var timestamp: java.time.LocalDateTime? = null
    var userType: String = ""
    var gameName: String = ""
    var gameMode: String = ""
    var gameAdditionalOptionOne: Boolean = false
    var gameAdditionalOptionTwo: Boolean = false
    var gameFleetIndex: Int = 0
    var serverIp: String = ""
    var serverPort: Int = 0
    var userName: String = ""
    var ownBoardState: String = ""
    var otherBoardState: String = ""
    var amountShots: Int = 0
    var buttonState: String = ""
    var lastStatus: String = ""
    val ownGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
    val otherGrid: Array<Array<CellState>> = Array(GRID_ROWS) { Array(GRID_COLS) { CellState.EMPTY } }
    var fleetString: String = ""
}