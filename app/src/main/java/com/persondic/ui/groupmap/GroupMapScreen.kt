package com.persondic.ui.groupmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMapScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: GroupMapViewModel = viewModel(
        factory = ViewModelFactory { GroupMapViewModel(application.repository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTags by remember { mutableStateOf(emptyList<String>()) }

    val selectedBubbles = selectedTags.mapNotNull { tag -> uiState.bubbles.firstOrNull { it.tag == tag } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (selectedTags.isNotEmpty()) {
                        TextButton(onClick = { selectedTags = emptyList() }) {
                            Text(stringResource(R.string.group_map_clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.bubbles.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.group_map_empty))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.group_map_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            BubbleMap(
                bubbles = uiState.bubbles,
                selectedTags = selectedTags,
                onToggleTag = { tag ->
                    selectedTags = when {
                        tag in selectedTags -> selectedTags - tag
                        selectedTags.size >= 3 -> selectedTags.drop(1) + tag
                        else -> selectedTags + tag
                    }
                },
            )

            if (selectedBubbles.size in 2..3) {
                VennSection(selected = selectedBubbles, uiState = uiState)
            }
        }
    }
}

@Composable
private fun BubbleMap(
    bubbles: List<GroupBubble>,
    selectedTags: List<String>,
    onToggleTag: (String) -> Unit,
) {
    val positions = remember(bubbles) { layoutBubbles(bubbles) }
    val maxMembers = remember(bubbles) { bubbles.maxOfOrNull { it.memberIds.size } ?: 1 }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val scale = min(widthPx, heightPx)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bubbles, positions) {
                    detectTapGestures { tap ->
                        val hit = bubbles.firstOrNull { bubble ->
                            val center = positions[bubble.tag] ?: return@firstOrNull false
                            val centerPx = Offset(center.x * widthPx, center.y * heightPx)
                            val radiusPx = bubbleRadius(bubble.memberIds.size, maxMembers) * scale
                            (tap - centerPx).getDistance() <= radiusPx
                        }
                        if (hit != null) onToggleTag(hit.tag)
                    }
                },
        ) {
            bubbles.forEach { bubble ->
                val center = positions[bubble.tag] ?: return@forEach
                val centerPx = Offset(center.x * widthPx, center.y * heightPx)
                val radiusPx = bubbleRadius(bubble.memberIds.size, maxMembers) * scale
                val isSelected = bubble.tag in selectedTags
                drawCircle(
                    color = primary,
                    radius = radiusPx,
                    center = centerPx,
                    alpha = if (isSelected) 0.35f else 0.14f,
                )
                drawCircle(
                    color = if (isSelected) primary else outline,
                    radius = radiusPx,
                    center = centerPx,
                    style = Stroke(width = if (isSelected) 5f else 2f),
                )
            }
        }

        bubbles.forEach { bubble ->
            val center = positions[bubble.tag] ?: return@forEach
            val radiusPx = bubbleRadius(bubble.memberIds.size, maxMembers) * scale
            val diameterDp = with(LocalDensity.current) { (radiusPx * 2).toDp() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (center.x * widthPx - radiusPx).roundToInt(),
                            y = (center.y * heightPx - radiusPx).roundToInt(),
                        )
                    }
                    .size(diameterDp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#${bubble.tag}\n${bubble.memberIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun VennSection(selected: List<GroupBubble>, uiState: GroupMapUiState) {
    val circles = remember(selected) { vennCircles(selected) }
    val regions = remember(selected) { vennRegions(selected) }
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Text(
        text = selected.joinToString(" · ") { "#${it.tag}" },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )

    // The diagram is drawn into a centred square. Mapping x by width and radii by min(width,
    // height) would pull the circles apart on a wide screen until the overlaps no longer matched
    // the geometry the region anchors were computed from.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val originX = (constraints.maxWidth - side) / 2f
        val originY = (constraints.maxHeight - side) / 2f
        fun place(point: Offset) = Offset(originX + point.x * side, originY + point.y * side)

        Canvas(modifier = Modifier.fillMaxSize()) {
            circles.forEach { circle ->
                val centerPx = place(circle.center)
                drawCircle(
                    color = primary,
                    radius = circle.radius * side,
                    center = centerPx,
                    alpha = 0.16f,
                )
                drawCircle(
                    color = primary,
                    radius = circle.radius * side,
                    center = centerPx,
                    style = Stroke(width = 3f),
                )
            }
        }

        val density = LocalDensity.current
        regions.filter { it.memberIds.isNotEmpty() }.forEach { region ->
            // clearance is the radius of the largest circle that fits in the region, so the
            // largest square that fits inside it has a side of clearance * √2. Staying just under
            // that keeps the corners of the text box inside the region too, not only its middle.
            val boxSidePx = region.clearance * side * 1.40f
            val boxSideDp = with(density) { boxSidePx.toDp() }
            val anchorPx = place(region.center)

            // One line per name plus a little slack, so a region only ever shows what fits inside
            // it and says how many it had to leave out.
            val capacity = (boxSideDp / NAME_LINE_HEIGHT).toInt().coerceAtLeast(1)
            val names = region.memberIds.mapNotNull { uiState.peopleById[it]?.displayName }
            val shown = if (names.size <= capacity) names else names.take((capacity - 1).coerceAtLeast(1))
            val hidden = names.size - shown.size

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (anchorPx.x - boxSidePx / 2f).roundToInt(),
                            y = (anchorPx.y - boxSidePx / 2f).roundToInt(),
                        )
                    }
                    .size(boxSideDp)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    shown.forEach { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (hidden > 0) {
                        Text(
                            text = stringResource(R.string.group_map_more_members, hidden),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    regions.filter { it.memberIds.isNotEmpty() }.forEach { region ->
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "${region.label} (${region.memberIds.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = region.memberIds
                    .mapNotNull { uiState.peopleById[it]?.displayName }
                    .joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    Box(modifier = Modifier.height(24.dp))
}

/** Rough line box for a name in the diagram, used to decide how many fit in a region. */
private val NAME_LINE_HEIGHT = 16.dp
