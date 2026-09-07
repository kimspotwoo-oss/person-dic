package com.persondic.ui.groupmap

/** The names a region can show, and how many did not fit. */
data class PackedNames(
    val shown: List<String>,
    val hidden: Int,
    /** False when the "외 N명" marker would have cost more names than it is worth. */
    val showOverflowMarker: Boolean = false,
)

/**
 * Decides how many names fit in a region's box, laid out as wrapping text.
 *
 * Estimated rather than measured: the names are drawn as one wrapped Text, and a Text cannot say
 * how much it would need before it is composed. Widths are rounded up so the estimate errs on the
 * side of showing one name fewer — a name cut in half at the edge of a circle looks broken, while
 * one extra name under "외 N명" does not.
 *
 * Sizes are in dp and match the label style the diagram uses.
 */
fun packNames(
    names: List<String>,
    widthDp: Float,
    heightDp: Float,
): PackedNames {
    val lines = (heightDp / LINE_HEIGHT_DP).toInt()
    if (lines < 1 || names.isEmpty()) return PackedNames(emptyList(), names.size)

    val fitted = fit(names, widthDp, lines, reserve = 0f)
    if (fitted.size == names.size) return PackedNames(fitted, 0)

    // Something has to be left out, so the overflow marker wants room of its own. In a small
    // region that room costs several names, which is a bad trade — the complete list is printed
    // under the diagram anyway, so the names win and the marker is dropped.
    val withMarker = fit(names, widthDp, lines, reserve = OVERFLOW_MARKER_DP)
    return if (withMarker.size >= fitted.size - 1) {
        PackedNames(withMarker, names.size - withMarker.size, showOverflowMarker = true)
    } else {
        PackedNames(fitted, names.size - fitted.size, showOverflowMarker = false)
    }
}

/** Greedy line filling: names are placed in order, wrapping when the next one no longer fits. */
private fun fit(names: List<String>, widthDp: Float, lines: Int, reserve: Float): List<String> {
    val kept = mutableListOf<String>()
    var line = 1
    var used = 0f

    for (name in names) {
        val needed = textWidthDp(name)
        // The marker sits after the last name, so only the final line has to leave room for it.
        val available = if (line == lines) widthDp - reserve else widthDp
        val withGap = if (used == 0f) needed else used + GAP_DP + needed

        when {
            withGap <= available -> {
                used = withGap
                kept += name
            }

            line < lines && needed <= (if (line + 1 == lines) widthDp - reserve else widthDp) -> {
                line++
                used = needed
                kept += name
            }

            else -> return kept
        }
    }
    return kept
}

/**
 * Hangul and other CJK glyphs are square at this size; Latin letters and digits are about half
 * that. Both are rounded up a little so a long name never overruns its region.
 */
private fun textWidthDp(text: String): Float = text.sumOf { char ->
    if (char.code >= CJK_START) FULL_WIDTH_DP.toDouble() else HALF_WIDTH_DP.toDouble()
}.toFloat()

private const val CJK_START = 0x1100
private const val FULL_WIDTH_DP = 12f
private const val HALF_WIDTH_DP = 7f
private const val GAP_DP = 8f
private const val LINE_HEIGHT_DP = 15f

/** Enough for "외 99명" at the same size. */
private const val OVERFLOW_MARKER_DP = 56f
