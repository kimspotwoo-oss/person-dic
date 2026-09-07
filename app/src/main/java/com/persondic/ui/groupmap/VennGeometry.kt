package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

data class VennCircle(
    val tag: String,
    val center: Offset,
    val radius: Float,
)

/** Where a region's label goes, and how much room it has. Both normalized to the 0..1 square. */
data class RegionAnchor(
    val center: Offset,
    val clearance: Float,
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

    val union = selected.flatMap { it.memberIds }.distinct()

    return (1 until (1 shl selected.size))
        .sortedBy { mask -> Integer.bitCount(mask) }
        .mapNotNull { mask ->
            val anchor = regionAnchor(circles, mask) ?: return@mapNotNull null
            VennRegion(
                label = regionLabel(selected, mask),
                memberIds = union.filter { id -> membershipMask(selected, id) == mask },
                center = anchor.center,
                clearance = anchor.clearance,
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
 * The best place to write a region's name list: the point inside the region that sits farthest
 * from every edge, along with how much room that leaves.
 *
 * Each region is an intersection of discs and disc complements, so the distance from a point to
 * the region's boundary is exactly the smallest distance to any circle's edge. That makes this a
 * direct scan over a grid rather than an approximation of an outline, and it means the label box
 * never has to be hand-tuned when the layout changes.
 */
fun regionAnchor(circles: List<VennCircle>, mask: Int): RegionAnchor? {
    var best: RegionAnchor? = null

    for (iy in 0 until ANCHOR_GRID) {
        val y = iy / (ANCHOR_GRID - 1f)
        for (ix in 0 until ANCHOR_GRID) {
            val x = ix / (ANCHOR_GRID - 1f)

            var clearance = Float.MAX_VALUE
            var inRegion = true
            circles.forEachIndexed { i, circle ->
                val distance = hypot(x - circle.center.x, y - circle.center.y)
                if ((distance <= circle.radius) != ((mask shr i) and 1 == 1)) inRegion = false
                clearance = minOf(clearance, abs(distance - circle.radius))
            }

            if (inRegion && clearance > (best?.clearance ?: 0f)) {
                best = RegionAnchor(Offset(x, y), clearance)
            }
        }
    }

    return best
}

private const val ANCHOR_GRID = 161
