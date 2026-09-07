package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset
import com.persondic.data.local.entity.Person
import java.util.UUID

data class GroupBubble(
    val tag: String,
    val memberIds: Set<UUID>,
)

data class GroupMapUiState(
    val bubbles: List<GroupBubble> = emptyList(),
    val peopleById: Map<UUID, Person> = emptyMap(),
)

/**
 * A single area of a Venn diagram, e.g. "in A and B but not C".
 *
 * [center] is where the region's names are written and [clearance] is the radius of the largest
 * circle that fits inside the region around that point — how much room the names have. Both are
 * normalized to the 0..1 drawing square.
 */
data class VennRegion(
    val label: String,
    val memberIds: List<UUID>,
    val center: Offset,
    val clearance: Float,
)
