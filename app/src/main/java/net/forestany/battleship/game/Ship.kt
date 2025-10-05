package net.forestany.battleship.game

data class Ship(
    val size: Int,
    val name: String,
    var placed: Boolean = false,
    var isHorizontal: Boolean = true,
    var destroyed: Boolean = false,
    var cells: MutableList<Pair<Int, Int>> = mutableListOf()
)