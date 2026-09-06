package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset

data class VennCircle(
    val tag: String,
    val center: Offset,
    val radius: Float,
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

fun vennRegions(selected: List<GroupBubble>): List<VennRegion> = when (selected.size) {
    2 -> twoSetRegions(selected[0], selected[1])
    3 -> threeSetRegions(selected[0], selected[1], selected[2])
    else -> emptyList()
}

private fun twoSetRegions(a: GroupBubble, b: GroupBubble): List<VennRegion> = listOf(
    VennRegion(
        label = "#${a.tag}만",
        memberIds = a.memberIds.filterNot { it in b.memberIds },
        center = Offset(0.20f, 0.5f),
    ),
    VennRegion(
        label = "#${a.tag} ∩ #${b.tag}",
        memberIds = a.memberIds.filter { it in b.memberIds },
        center = Offset(0.5f, 0.5f),
    ),
    VennRegion(
        label = "#${b.tag}만",
        memberIds = b.memberIds.filterNot { it in a.memberIds },
        center = Offset(0.80f, 0.5f),
    ),
)

private fun threeSetRegions(a: GroupBubble, b: GroupBubble, c: GroupBubble): List<VennRegion> {
    val sa = a.memberIds
    val sb = b.memberIds
    val sc = c.memberIds
    return listOf(
        VennRegion("#${a.tag}만", sa.filterNot { it in sb || it in sc }, Offset(0.50f, 0.20f)),
        VennRegion("#${b.tag}만", sb.filterNot { it in sa || it in sc }, Offset(0.78f, 0.68f)),
        VennRegion("#${c.tag}만", sc.filterNot { it in sa || it in sb }, Offset(0.22f, 0.68f)),
        VennRegion(
            "#${a.tag} ∩ #${b.tag}",
            sa.filter { it in sb && it !in sc },
            Offset(0.65f, 0.39f),
        ),
        VennRegion(
            "#${a.tag} ∩ #${c.tag}",
            sa.filter { it in sc && it !in sb },
            Offset(0.35f, 0.39f),
        ),
        VennRegion(
            "#${b.tag} ∩ #${c.tag}",
            sb.filter { it in sc && it !in sa },
            Offset(0.50f, 0.66f),
        ),
        VennRegion(
            "#${a.tag} ∩ #${b.tag} ∩ #${c.tag}",
            sa.filter { it in sb && it in sc },
            Offset(0.50f, 0.47f),
        ),
    )
}
