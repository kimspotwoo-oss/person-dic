package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset
import java.util.UUID
import kotlin.math.hypot

data class VennCircle(
    val tag: String,
    val center: Offset,
    val radius: Float,
)

/** The rectangle the diagram occupies, so it can be scaled to fill whatever space it is given. */
data class VennBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val aspectRatio: Float get() = width / height
}

/**
 * The largest axis-aligned rectangle that fits inside a region — where its names are written.
 *
 * A rectangle rather than the largest inscribed circle, because names wrap: a crescent that is
 * tall and narrow holds a stacked list, and a wide one holds names side by side. Fitting a square
 * to the inscribed circle threw away most of that width.
 */
data class RegionBox(
    val center: Offset,
    val width: Float,
    val height: Float,
)

fun vennBounds(circles: List<VennCircle>): VennBounds = VennBounds(
    minX = circles.minOf { it.center.x - it.radius },
    minY = circles.minOf { it.center.y - it.radius },
    maxX = circles.maxOf { it.center.x + it.radius },
    maxY = circles.maxOf { it.center.y + it.radius },
)

/** Fixed, readable layouts. A true Venn diagram is only drawable for 2 or 3 sets. */
fun vennCircles(selected: List<GroupBubble>): List<VennCircle> = when (selected.size) {
    2 -> listOf(
        VennCircle(selected[0].tag, Offset(0.36f, 0.5f), 0.26f),
        VennCircle(selected[1].tag, Offset(0.64f, 0.5f), 0.26f),
    )
    3 -> listOf(
        VennCircle(selected[0].tag, Offset(0.50f, 0.34f), 0.24f),
        VennCircle(selected[1].tag, Offset(0.64f, 0.58f), 0.24f),
        VennCircle(selected[2].tag, Offset(0.36f, 0.58f), 0.24f),
    )
    else -> emptyList()
}

/**
 * One region per non-empty combination of the selected groups, ordered by how many groups the
 * combination covers, so "이 그룹에만" rows come before the overlaps.
 *
 * Every person in the union lands in exactly one region: membership is read off the same
 * signature that defines the region, so the regions always partition the union.
 */
fun vennRegions(selected: List<GroupBubble>): List<VennRegion> {
    val circles = vennCircles(selected)
    if (circles.isEmpty()) return emptyList()

    val bounds = vennBounds(circles)
    val union = selected.flatMap { it.memberIds }.distinct()

    return (1 until (1 shl selected.size))
        .sortedBy { mask -> Integer.bitCount(mask) }
        .mapNotNull { mask ->
            val box = regionBox(circles, mask, bounds) ?: return@mapNotNull null
            VennRegion(
                label = regionLabel(selected, mask),
                memberIds = union.filter { id -> membershipMask(selected, id) == mask },
                box = box,
            )
        }
}

private fun membershipMask(selected: List<GroupBubble>, id: UUID): Int =
    selected.indices.fold(0) { mask, i ->
        if (id in selected[i].memberIds) mask or (1 shl i) else mask
    }

private fun regionLabel(selected: List<GroupBubble>, mask: Int): String {
    val tags = selected.indices.filter { (mask shr it) and 1 == 1 }.map { "#${selected[it].tag}" }
    return if (tags.size == 1) "${tags.single()}만" else tags.joinToString(" ∩ ")
}

/**
 * The largest axis-aligned rectangle that fits entirely inside one region.
 *
 * The region is sampled onto a grid and the classic largest-rectangle-in-a-histogram scan is run
 * over it, one row at a time. Maximising area is the right target because the names wrap: how many
 * fit depends on the area, not on how square the space is.
 */
fun regionBox(circles: List<VennCircle>, mask: Int, bounds: VennBounds): RegionBox? {
    val cellWidth = bounds.width / (REGION_GRID - 1)
    val cellHeight = bounds.height / (REGION_GRID - 1)

    val inside = Array(REGION_GRID) { iy ->
        val y = bounds.minY + iy * cellHeight
        BooleanArray(REGION_GRID) { ix ->
            val x = bounds.minX + ix * cellWidth
            circles.indices.all { i ->
                val circle = circles[i]
                val within = hypot(x - circle.center.x, y - circle.center.y) <= circle.radius
                within == ((mask shr i) and 1 == 1)
            }
        }
    }

    val heights = IntArray(REGION_GRID)
    var bestArea = 0
    var bestLeft = 0
    var bestRight = -1
    var bestTop = 0
    var bestBottom = -1

    for (iy in 0 until REGION_GRID) {
        for (ix in 0 until REGION_GRID) {
            heights[ix] = if (inside[iy][ix]) heights[ix] + 1 else 0
        }

        val stack = ArrayDeque<Int>()
        for (ix in 0..REGION_GRID) {
            val current = if (ix == REGION_GRID) 0 else heights[ix]
            while (stack.isNotEmpty() && heights[stack.last()] >= current) {
                val top = stack.removeLast()
                val barHeight = heights[top]
                val left = if (stack.isEmpty()) 0 else stack.last() + 1
                val area = barHeight * (ix - left)
                if (barHeight > 0 && area > bestArea) {
                    bestArea = area
                    bestLeft = left
                    bestRight = ix - 1
                    bestTop = iy - barHeight + 1
                    bestBottom = iy
                }
            }
            stack.addLast(ix)
        }
    }

    if (bestArea == 0) return null

    val x0 = bounds.minX + bestLeft * cellWidth
    val x1 = bounds.minX + bestRight * cellWidth
    val y0 = bounds.minY + bestTop * cellHeight
    val y1 = bounds.minY + bestBottom * cellHeight
    return RegionBox(
        center = Offset((x0 + x1) / 2f, (y0 + y1) / 2f),
        width = x1 - x0,
        height = y1 - y0,
    )
}

private const val REGION_GRID = 121
