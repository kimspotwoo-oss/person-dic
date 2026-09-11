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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
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

        val labelDensity = LocalDensity.current
        bubbles.forEach { bubble ->
            val center = positions[bubble.tag] ?: return@forEach
            // Exactly the width the layout reserved for this name, so the two agree — and required
            // rather than merely asked for. A plain width is still capped by whatever is left of
            // the parent, so a bubble near an edge had its name squeezed into the remaining space
            // and cut to "#전기전자공학...". requiredWidth lets it hang over instead, which is what
            // the layout's own margins were already keeping clear.
            //
            // Both dimensions are inflated by the reader's font-size setting. Without this, a
            // phone with enlarged system text renders every glyph bigger than the fixed dp this
            // box was sized for, and — since the box no longer yields to the parent — the text
            // wraps inside its own required space instead: every tag truncated, even a three-
            // character one, which is what an unscaled box looks like under an enlarged font.
            val fontScale = labelDensity.fontScale
            val labelWidth = (labelWidthDp(bubble.tag) * fontScale).dp
            val labelHeight = LABEL_HEIGHT_DP.dp * fontScale
            val labelHalfWidthPx = with(labelDensity) { (labelWidth / 2).toPx() }
            val labelHalfHeightPx = with(labelDensity) { (labelHeight / 2).toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (originX + center.x * scale - labelHalfWidthPx).roundToInt(),
                            y = (originY + center.y * scale - labelHalfHeightPx).roundToInt(),
                        )
                    }
                    .requiredWidth(labelWidth)
                    .requiredHeight(labelHeight),
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
    val filled = regions.filter { it.memberIds.isNotEmpty() }

    // One measurement for the whole section. The diagram box below is shaped exactly like the
    // diagram, so its scale is this one — which is what lets the list underneath know what the
    // drawing managed to fit and say only what the drawing could not.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val scale = constraints.maxWidth.toFloat() / bounds.width
        fun place(point: Offset) =
            Offset((point.x - bounds.minX) * scale, (point.y - bounds.minY) * scale)

        // How much of a circle-only region's own box its name needs, in the same normalized units
        // as the box itself — inflated by the reader's font size for the same reason the bubble
        // map's own labels are (see BubbleMap below), then converted through real pixels rather
        // than assumed to scale evenly with the diagram.
        val tagStripHeight = with(density) { (TAG_LINE_HEIGHT * density.fontScale).toPx() } / scale

        // One split per region, up front: a circle-only region gives up a strip at the top for its
        // own name and packNames gets whatever is left in the very same box; an overlap region
        // is unaffected and keeps its box whole. Splitting one rectangle instead of computing the
        // tag's spot and the names' box from two separate formulas is what keeps them from landing
        // on each other — see splitOffTagStrip.
        val placements = regions.associateWith { region -> splitOffTagStrip(region, tagStripHeight) }

        val packedByRegion = filled.associate { region ->
            val box = placements.getValue(region).namesBox
            val widthDp = with(density) { (box.width * scale).toDp() }
            val heightDp = with(density) { (box.height * scale).toDp() }
            // Dividing by the font scale is what keeps the estimate honest when the reader has
            // text enlarged: the box is fixed, so the room has to shrink instead.
            val fontScale = density.fontScale
            val names = region.memberIds.mapNotNull { uiState.peopleById[it]?.displayName }
            region.label to RegionNamesLayout(box, packNames(names, widthDp.value / fontScale, heightDp.value / fontScale))
        }

        Column {
            Text(
                text = selected.joinToString(" · ") { "#${it.tag}" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            // The container is shaped like the diagram and the diagram is scaled to fill it, so
            // none of the width goes to empty margin — that is what makes the circles, and the
            // room for names inside them, as large as the screen allows.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bounds.aspectRatio),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    circles.forEach { circle ->
                        val centerPx = place(circle.center)
                        drawCircle(
                            color = primary,
                            radius = circle.radius * scale,
                            center = centerPx,
                            alpha = 0.16f,
                        )
                        drawCircle(
                            color = primary,
                            radius = circle.radius * scale,
                            center = centerPx,
                            style = Stroke(width = 3f),
                        )
                    }
                }

                // Which circle is which. Without these the drawing shows who falls where and never
                // says what "where" is, leaving the reader to match the heading above against the
                // arrangement by eye. Every circle gets one regardless of whether anyone falls in
                // its exclusive lobe — if everybody with this tag also has the other one, the lobe
                // is empty but the circle still needs a name.
                regions.forEach { region ->
                    val tag = region.exclusiveTag ?: return@forEach
                    val placement = placements.getValue(region)
                    val at = place(placement.anchor)
                    val tagWidth = (labelWidthDp(tag) * density.fontScale).dp
                    val tagHeight = TAG_LINE_HEIGHT * density.fontScale
                    val halfWidthPx = with(density) { (tagWidth / 2).toPx() }
                    val halfHeightPx = with(density) { (tagHeight / 2).toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (at.x - halfWidthPx).roundToInt(),
                                    y = (at.y - halfHeightPx).roundToInt(),
                                )
                            }
                            .requiredWidth(tagWidth)
                            .requiredHeight(tagHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }

                filled.forEach { region ->
                    val layout = packedByRegion[region.label] ?: return@forEach
                    val packed = layout.packed
                    val box = layout.box
                    val widthPx = box.width * scale
                    val heightPx = box.height * scale
                    val widthDp = with(density) { widthPx.toDp() }
                    val heightDp = with(density) { heightPx.toDp() }
                    val anchorPx = place(box.center)
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
                                // Required, like the bubble names: a plain size is capped by
                                // what is left of the parent, so a region near the edge of the
                                // diagram got a narrower box than the packing had measured for
                                // and lost names the packing thought would fit.
                                .requiredSize(width = widthDp, height = heightDp)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = (heightDp.value / (NAME_LINE_HEIGHT.value * density.fontScale))
                                    .toInt()
                                    .coerceAtLeast(1),
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            }

            // The circles carry no labels of their own, so this is where you find out which one is
            // which — it stays. What goes is the roll call of names underneath each heading, once
            // the drawing has already shown every one of them; that was the same list printed
            // twice, one above the other.
            filled.forEach { region ->
                val packed = packedByRegion[region.label]?.packed
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        text = "${region.label} (${region.memberIds.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (packed == null || packed.hidden > 0) {
                        Text(
                            text = region.memberIds
                                .mapNotNull { uiState.peopleById[it]?.displayName }
                                .joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Box(modifier = Modifier.height(24.dp))
        }
    }
}

/** One line of labelMedium, for a circle's own name. */
private val TAG_LINE_HEIGHT = 18.dp

/** What a region's own name and its packed member names ended up sharing one box to fit into. */
private data class RegionNamesLayout(val box: RegionBox, val packed: PackedNames)

private const val NAME_SEPARATOR = "  "

/** Line box for a name in the diagram. Must match NamePacking's own line height. */
private val NAME_LINE_HEIGHT = 15.dp