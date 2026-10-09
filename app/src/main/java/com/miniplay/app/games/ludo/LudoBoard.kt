package com.miniplay.app.games.ludo

/** The four Ludo players, listed in clockwise turn order. */
enum class LudoColor { RED, GREEN, YELLOW, BLUE }

/** An integer cell on the 15×15 Ludo grid (origin top-left). */
data class GridCell(val x: Int, val y: Int)

/**
 * Pure board geometry: the shared 52-cell ring, each colour's start offset,
 * private 5-cell home columns, safe squares, corner yards and the centre home.
 *
 * A token's position is a single `progress` integer:
 *  - `-1`            → in its yard (not yet on the board)
 *  - `0..50`         → on the shared ring; absolute ring index = (startOffset + progress) % 52
 *  - `51..55`        → in the colour's private home column (5 cells)
 *  - `56` ([FINISH]) → home (finished)
 *
 * Keeping this independent of Android lets the rules and every path be unit
 * tested off-device. [LudoGeometryTest] verifies the ring is 52 unique cells,
 * is continuous (orthogonal except the four diagonal corner "shoulders"), and
 * that every colour's home column attaches to the ring correctly.
 */
object LudoBoard {
    const val GRID = 15
    const val RING = 52
    const val LAST_RING = 50       // highest ring progress before the home column
    const val HOME_COLUMN = 5      // private lane cells (progress 51..55)
    const val FINISH = 56          // progress value meaning "home"
    const val TOKENS = 4

    private fun c(x: Int, y: Int) = GridCell(x, y)

    /** The 52 shared ring cells, clockwise. Index 0 is the top-arm cell nearest the centre. */
    val ring: List<GridCell> = listOf(
        c(6, 5), c(6, 4), c(6, 3), c(6, 2), c(6, 1), c(6, 0),           // 0-5  top arm, left lane
        c(7, 0),                                                         // 6    top tip
        c(8, 0), c(8, 1), c(8, 2), c(8, 3), c(8, 4), c(8, 5),           // 7-12 top arm, right lane
        c(9, 6), c(10, 6), c(11, 6), c(12, 6), c(13, 6), c(14, 6),      // 13-18 right arm, top lane
        c(14, 7),                                                        // 19   right tip
        c(14, 8), c(13, 8), c(12, 8), c(11, 8), c(10, 8), c(9, 8),      // 20-25 right arm, bottom lane
        c(8, 9), c(8, 10), c(8, 11), c(8, 12), c(8, 13), c(8, 14),      // 26-31 bottom arm, right lane
        c(7, 14),                                                        // 32   bottom tip
        c(6, 14), c(6, 13), c(6, 12), c(6, 11), c(6, 10), c(6, 9),      // 33-38 bottom arm, left lane
        c(5, 8), c(4, 8), c(3, 8), c(2, 8), c(1, 8), c(0, 8),          // 39-44 left arm, bottom lane
        c(0, 7),                                                         // 45   left tip
        c(0, 6), c(1, 6), c(2, 6), c(3, 6), c(4, 6), c(5, 6),          // 46-51 left arm, top lane
    )

    /** Ring index of each colour's entry (start) square. Spaced 13 apart. */
    val startOffset: Map<LudoColor, Int> = mapOf(
        LudoColor.RED to 47,     // emerges beside the top-left yard, at (1,6)
        LudoColor.GREEN to 8,    // beside the top-right yard, at (8,1)
        LudoColor.YELLOW to 21,  // beside the bottom-right yard, at (13,8)
        LudoColor.BLUE to 34,    // beside the bottom-left yard, at (6,13)
    )

    /** Protected ring squares: the four coloured start cells + four star cells. */
    val safeRingIndices: Set<Int> = setOf(8, 16, 21, 29, 34, 42, 47, 51)

    /** The five private home-column cells for each colour (progress 51..55), centre-ward last. */
    val homeColumn: Map<LudoColor, List<GridCell>> = mapOf(
        LudoColor.RED to listOf(c(1, 7), c(2, 7), c(3, 7), c(4, 7), c(5, 7)),
        LudoColor.GREEN to listOf(c(7, 1), c(7, 2), c(7, 3), c(7, 4), c(7, 5)),
        LudoColor.YELLOW to listOf(c(13, 7), c(12, 7), c(11, 7), c(10, 7), c(9, 7)),
        LudoColor.BLUE to listOf(c(7, 13), c(7, 12), c(7, 11), c(7, 10), c(7, 9)),
    )

    /** The four token rest positions inside each colour's corner yard. */
    val yardSlots: Map<LudoColor, List<GridCell>> = mapOf(
        LudoColor.RED to listOf(c(1, 1), c(4, 1), c(1, 4), c(4, 4)),
        LudoColor.GREEN to listOf(c(10, 1), c(13, 1), c(10, 4), c(13, 4)),
        LudoColor.YELLOW to listOf(c(10, 10), c(13, 10), c(10, 13), c(13, 13)),
        LudoColor.BLUE to listOf(c(1, 10), c(4, 10), c(1, 13), c(4, 13)),
    )

    /** Centre home where finished tokens rest (rendered clustered per colour). */
    val center = GridCell(7, 7)

    /** Absolute ring index for a token on the shared ring (progress 0..50). */
    fun ringIndex(color: LudoColor, progress: Int): Int {
        require(progress in 0..LAST_RING)
        return (startOffset.getValue(color) + progress) % RING
    }

    /** True if a token at [progress] stands on a protected (safe) ring square. */
    fun isSafeProgress(color: LudoColor, progress: Int): Boolean =
        progress in 0..LAST_RING && ringIndex(color, progress) in safeRingIndices

    /** Grid cell for a token at [progress] (>= 0). Finished tokens map to [center]. */
    fun cellFor(color: LudoColor, progress: Int): GridCell = when {
        progress in 0..LAST_RING -> ring[ringIndex(color, progress)]
        progress in 51..55 -> homeColumn.getValue(color)[progress - 51]
        else -> center
    }
}
