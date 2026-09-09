package com.persondic.ui.relationmap

import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

/**
 * One line as it should actually be drawn.
 *
 * [bow] is how far the curve's control point sits off the straight chord, perpendicular to it and
 * measured in the same normalized units as the node positions. Positive is to the left of the
 * direction from the first end to the second. Zero draws a straight line, which is what a quadratic
 * curve with its control point on the midpoint already is — so there is no separate straight case.
 */
data class RoutedEdge(
    val edge: GraphEdge,
    val bow: Float,
)

/**
 * Works out how each line has to bend to stay readable.
 *
 * Two things go wrong with lines drawn straight between centres. A line can pass underneath a third
 * person who happens to sit between the two it connects — 김민준 stood between 양하영 and 김범채 and
 * swallowed the friendship between them, so the picture said they were not connected. And two
 * relations between the same pair land exactly on top of each other, so the second one is invisible
 * and the graph disagrees with the list that says there are two.
 *
 * So a line bends away from anyone standing in it, and lines sharing a pair fan out around wherever
 * the first one ended up. Bending is cheaper than moving people: the ring layout means something —
 * distance from the centre is how you know who is a friend of a friend — and shuffling nodes to
 * free a line would quietly destroy that.
 */
fun routeEdges(
    edges: List<GraphEdge>,
    nodes: List<GraphNode>,
    nodeRadius: Float,
): List<RoutedEdge> {
    if (edges.isEmpty()) return emptyList()
    val byId = nodes.associateBy { it.personId }

    return edges
        .groupBy { setOf(it.fromPersonId, it.toPersonId) }
        .flatMap { (_, sharingAPair) ->
            val first = sharingAPair.first()
            val from = byId[first.fromPersonId]
            val to = byId[first.toPersonId]
            if (from == null || to == null) {
                sharingAPair.map { RoutedEdge(it, 0f) }
            } else {
                val obstacles = nodes.filter {
                    it.personId != from.personId && it.personId != to.personId
                }
                val around = bowAround(from, to, obstacles, nodeRadius)
                sharingAPair.mapIndexed { index, edge ->
                    RoutedEdge(edge, around + fanOffset(index, nodeRadius))
                }
            }
        }
}

/**
 * How far off the chord the [index]th line of a pair sits: nothing, then alternating sides, opening
 * outwards. The first line keeps the direct path so a single relation is still drawn as a
 * straight one.
 */
private fun fanOffset(index: Int, nodeRadius: Float): Float {
    if (index == 0) return 0f
    val step = (index + 1) / 2
    val side = if (index % 2 == 1) 1f else -1f
    return side * step * FAN_SPACING * nodeRadius
}

/**
 * The bend needed to clear whoever is standing in the way, or zero when nobody is.
 *
 * Only people *between* the two ends count — someone off past either end is not in the way however
 * close to the line they are. The result is doubled because a quadratic curve only reaches half of
 * its control point's offset at its furthest, so clearing a node by some distance means aiming the
 * control point twice that far out.
 */
private fun bowAround(
    from: GraphNode,
    to: GraphNode,
    obstacles: List<GraphNode>,
    nodeRadius: Float,
): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = hypot(dx, dy)
    if (length < EPSILON) return 0f

    // Unit vector at right angles to the chord, pointing left of from → to.
    val perpX = -dy / length
    val perpY = dx / length
    val clearance = nodeRadius * CLEARANCE

    var needed = 0f
    var side = 1f
    obstacles.forEach { obstacle ->
        val offX = obstacle.x - from.x
        val offY = obstacle.y - from.y
        val along = (offX * dx + offY * dy) / (length * length)
        if (along <= 0f || along >= 1f) return@forEach

        val across = offX * perpX + offY * perpY
        val push = clearance - abs(across)
        if (push > needed) {
            needed = push
            // Away from it — and to the left when it sits exactly on the line, which is arbitrary
            // but has to be decided the same way every time or the picture flickers.
            side = if (across > 0f) -1f else 1f
        }
    }
    return side * needed * 2f
}

/** A node's own radius plus room for the line to read as passing by rather than touching. */
private const val CLEARANCE = 1.8f

/** Spacing between lines sharing a pair, as a multiple of the node radius. */
private const val FAN_SPACING = 1.1f

private const val EPSILON = 1e-6f
