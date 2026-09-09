package com.persondic.ui.groupmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
        // One square for both axes. Scaling x by the width and y by the height while taking radii
        // from the smaller of the two stretched the layout sideways, so the gaps the layout worked
        // out were not the gaps on screen.
        val scale = min(widthPx, heightPx)
        val originX = (widthPx - scale) / 2f
        val originY = (heightPx - scale) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bubbles, positions) {
                    detectTapGestures { tap ->
                        val hit = bubbles.firstOrNull { bubble ->
                            val center = positions[bubble.tag] ?: return@firstOrNull false
                            val centerPx = Offset(originX + center.x * scale, originY + center.y * scale)
                            val radiusPx = bubbleRadius(bubble.memberIds.size, maxMembers) * scale
                            (tap - centerPx).getDistance() <= radiusPx
                        }
                        if (hit != null) onToggleTag(hit.tag)
                    }
                },
        ) {
            bubbles.forEach { bubble ->
                val center = positions[bubble.tag] ?: return@forEach
                val centerPx = Offset(originX + center.x * scale, originY + center.y * scale)
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
            // The name is given its own width instead of the circle's: a long tag squeezed into a
            // small bubble wrapped mid-word and spilled out of it.
            val labelHalfWidthPx = with(LocalDensity.current) { (LABEL_WIDTH / 2).toPx() }
            val labelHalfHeightPx = with(LocalDensity.current) { (LABEL_HEIGHT / 2).toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (originX + center.x * scale - labelHalfWidthPx).roundToInt(),
                            y = (originY + center.y * scale - labelHalfHeightPx).roundToInt(),
                        )
                    }
                    .width(LABEL_WIDTH)
                    .height(LABEL_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#${bubble.tag}\n${bubble.memberIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VennSection(selected: List<GroupBubble>, uiState: GroupMapUiState) {
    val circles = remember(selected) { vennCircles(selected) }
    val bounds = remember(selected) { vennBounds(circles) }
    val regions = remember(selected) { vennRegions(selected) }
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Text(
        text = selected.joinToString(" · ") { "#${it.tag}" },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )

    // The container is shaped like the diagram and the diagram is scaled to fill it, so none of
    // the width goes to empty margin — that is what makes the circles, and the room for names
    // inside them, as large as the screen allows.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bounds.aspectRatio),
    ) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        val scale = min(boxWidth / bounds.width, boxHeight / bounds.height)
        val originX = (boxWidth - bounds.width * scale) / 2f
        val originY = (boxHeight - bounds.height * scale) / 2f
        fun place(point: Offset) = Offset(
            originX + (point.x - bounds.minX) * scale,
            originY + (point.y - bounds.minY) * scale,
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            circles.forEach { circle ->
                val centerPx = place(circle.center)
                drawCircle(color = primary, radius = circle.radius * scale, center = centerPx, alpha = 0.16f)
                drawCircle(
                    color = primary,
                    radius = circle.radius * scale,
                    center = centerPx,
                    style = Stroke(width = 3f),
                )
            }
        }

        val density = LocalDensity.current
        regions.filter { it.memberIds.isNotEmpty() }.forEach { region ->
            val widthPx = region.box.width * scale
            val heightPx = region.box.height * scale
            val widthDp = with(density) { widthPx.toDp() }
            val heightDp = with(density) { heightPx.toDp() }
            val anchorPx = place(region.box.center)

            // Dividing by the font scale is what keeps the estimate honest when the reader has
            // text enlarged: the box is fixed, so the room has to shrink instead.
            val fontScale = density.fontScale
            val names = region.memberIds.mapNotNull { uiState.peopleById[it]?.displayName }
            val packed = packNames(names, widthDp.value / fontScale, heightDp.value / fontScale)
            val overflow = stringResource(R.string.group_map_more_members, packed.hidden)
            val text = buildString {
                append(packed.shown.joinToString(NAME_SEPARATOR))
                if (packed.showOverflowMarker && packed.hidden > 0) {
                    if (isNotEmpty()) append(NAME_SEPARATOR)
                    append(overflow)
                }
            }

            if (text.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (anchorPx.x - widthPx / 2f).roundToInt(),
                                y = (anchorPx.y - heightPx / 2f).roundToInt(),
                            )
                        }
                        .size(width = widthDp, height = heightDp)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = (heightDp.value / (NAME_LINE_HEIGHT.value * fontScale))
                            .toInt()
                            .coerceAtLeast(1),
                        overflow = TextOverflow.Clip,
                    )
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

/**
 * Names get their own width rather than the circle's. A tag squeezed into a small bubble used to
 * wrap mid-word and spill past the edge of it; the layout already keeps this much clear.
 */
private val LABEL_WIDTH = 96.dp

/** Two lines of labelMedium: the tag and the member count. Matches BubbleLayout's own estimate. */
private val LABEL_HEIGHT = 34.dp

private const val NAME_SEPARATOR = "  "

/** Line box for a name in the diagram. Must match NamePacking's own line height. */
private val NAME_LINE_HEIGHT = 15.dp