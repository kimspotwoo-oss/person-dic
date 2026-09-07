package com.persondic.ui.groupmap

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
 * [box] is the space its names are written into, in the same normalized coordinates as the circles.
 */
data class VennRegion(
    val label: String,
    val memberIds: List<UUID>,
    val box: RegionBox,
)
