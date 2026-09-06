package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val MARGIN = 0.16f

fun jaccard(a: Set<UUID>, b: Set<UUID>): Float {
    if (a.isEmpty() && b.isEmpty()) return 0f
    val intersection = a.count { it in b }
    val union = a.size + b.size - intersection
    return if (union == 0) 0f else intersection.toFloat() / union
}

/**
 * Spring layout: groups that share members pull together, everything else pushes apart.
 * Deterministic (fixed start positions, no randomness) so the map doesn't jump around
 * between recompositions. Returns normalized 0..1 centers keyed by tag.
 */
fun layoutBubbles(bubbles: List<GroupBubble>, iterations: Int = 300): Map<String, Offset> {
    if (bubbles.isEmpty()) return emptyMap()
    if (bubbles.size == 1) return mapOf(bubbles.first().tag to Offset(0.5f, 0.5f))

    val n = bubbles.size
    val positions = Array(n) { index ->
        val angle = 2.0 * PI * index / n
        Offset(0.5f + 0.28f * cos(angle).toFloat(), 0.5f + 0.28f * sin(angle).toFloat())
    }

    repeat(iterations) {
        val displacement = Array(n) { Offset.Zero }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val delta = positions[i] - positions[j]
                val distance = max(delta.getDistance(), 0.001f)
                val overlap = jaccard(bubbles[i].memberIds, bubbles[j].memberIds)
                val desired = 0.55f - 0.35f * overlap
                val pull = (distance - desired) * 0.08f
                val direction = delta / distance
                displacement[i] -= direction * pull
                displacement[j] += direction * pull
            }
            displacement[i] += (Offset(0.5f, 0.5f) - positions[i]) * 0.02f
        }
        for (i in 0 until n) {
            positions[i] += displacement[i]
        }
    }

    return bubbles.indices.associate { index ->
        val position = positions[index]
        bubbles[index].tag to Offset(
            x = position.x.coerceIn(MARGIN, 1f - MARGIN),
            y = position.y.coerceIn(MARGIN, 1f - MARGIN),
        )
    }
}

fun bubbleRadius(memberCount: Int, maxMemberCount: Int): Float {
    if (maxMemberCount <= 0) return 0.08f
    val ratio = sqrt(memberCount.toFloat() / maxMemberCount.toFloat())
    return 0.07f + 0.08f * min(ratio, 1f)
}
