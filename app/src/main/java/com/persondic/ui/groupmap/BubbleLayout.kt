package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
 *
 * The spring alone is not enough. Its target distance falls to 0.20 when two groups have the same
 * members, which is right for the circles — that closeness *is* the information — but the name is
 * written at the circle's centre, so two groups holding the same people printed their names on top
 * of each other and neither could be read. #대구 and #영남고등학교 covering the same four people is
 * not an edge case, it is the ordinary case. So the springs settle first, and then [separateLabels]
 * pushes centres apart just far enough that no two names overlap.
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

    val maxMembers = bubbles.maxOf { it.memberIds.size }
    val bounds = bubbles.map { bubble ->
        // Keep the circle inside the square, and the name too when the name is the wider of the two.
        Offset(
            x = max(bubbleRadius(bubble.memberIds.size, maxMembers), labelHalfWidth(bubble.tag)),
            y = max(bubbleRadius(bubble.memberIds.size, maxMembers), LABEL_HALF_HEIGHT),
        )
    }
    clamp(positions, bounds)
    separateLabels(bubbles.map { it.tag }, positions, bounds)

    return bubbles.indices.associate { index -> bubbles[index].tag to positions[index] }
}

fun bubbleRadius(memberCount: Int, maxMemberCount: Int): Float {
    if (maxMemberCount <= 0) return 0.08f
    val ratio = sqrt(memberCount.toFloat() / maxMemberCount.toFloat())
    return 0.07f + 0.08f * min(ratio, 1f)
}

/**
 * Pushes centres apart until no two names overlap, treating each name as a box around its centre.
 *
 * Each pair is separated along whichever axis they overlap on by less, which moves the picture the
 * least. Clamping runs inside the loop rather than after it: two names shoved against the same edge
 * would otherwise land back on top of each other. Two groups at the exact same point separate
 * downwards rather than in no direction at all, so identical membership still terminates.
 */
private fun separateLabels(
    tags: List<String>,
    positions: Array<Offset>,
    bounds: List<Offset>,
    iterations: Int = 60,
) {
    val halfWidths = tags.map { labelHalfWidth(it) }
    repeat(iterations) {
        var moved = false
        for (i in tags.indices) {
            for (j in i + 1 until tags.size) {
                val dx = positions[j].x - positions[i].x
                val dy = positions[j].y - positions[i].y
                val overlapX = halfWidths[i] + halfWidths[j] - abs(dx)
                val overlapY = 2f * LABEL_HALF_HEIGHT - abs(dy)
                if (overlapX <= 0f || overlapY <= 0f) continue

                if (overlapY <= overlapX) {
                    val push = (overlapY / 2f + NUDGE) * if (dy < 0f) -1f else 1f
                    positions[i] -= Offset(0f, push)
                    positions[j] += Offset(0f, push)
                } else {
                    val push = (overlapX / 2f + NUDGE) * if (dx < 0f) -1f else 1f
                    positions[i] -= Offset(push, 0f)
                    positions[j] += Offset(push, 0f)
                }
                moved = true
            }
        }
        clamp(positions, bounds)
        if (!moved) return
    }
}

private fun clamp(positions: Array<Offset>, bounds: List<Offset>) {
    for (index in positions.indices) {
        val margin = bounds[index]
        positions[index] = Offset(
            x = positions[index].x.coerceIn(margin.x, 1f - margin.x),
            y = positions[index].y.coerceIn(margin.y, 1f - margin.y),
        )
    }
}

/**
 * The map is drawn into a square, so a normalized 1.0 is one side of it. [CANVAS_DP] is that side:
 * the bubble map is `fillMaxWidth().height(360.dp)` and scales by the smaller of the two, which is
 * the height on every phone wider than it is 360dp.
 */
private const val CANVAS_DP = 360f
private const val LABEL_LINE_DP = 17f

/**
 * Slightly wider than a Hangul glyph actually measures at labelMedium, on purpose. Estimating
 * short leaves the name a pixel too little room, and the wrap that follows costs a whole line —
 * "#전기전자공학부" broke across two and pushed the member count past the last line it was allowed.
 */
private const val LABEL_GLYPH_DP = 13f

/** The name and the member count, on two lines. */
private const val LABEL_HALF_HEIGHT = LABEL_LINE_DP / CANVAS_DP

private const val NUDGE = 0.002f

/**
 * How much room a name needs, in dp. The drawing has to lay the label out at exactly this width:
 * narrower and the name is cut short, wider and two names can touch after the layout decided they
 * would not. Hangul is monospaced enough at this size for a character count to stand in for
 * measuring; the extra character is the '#'.
 */
fun labelWidthDp(tag: String): Float = (tag.length + 1) * LABEL_GLYPH_DP

/** Two lines of labelMedium: the name and the count. */
const val LABEL_HEIGHT_DP = 2 * LABEL_LINE_DP

private fun labelHalfWidth(tag: String): Float = labelWidthDp(tag) / 2f / CANVAS_DP
