package com.persondic.ui.groupmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

    Text(
        text = selected.joinToString(" · ") { "#${it.tag}" },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val scale = min(widthPx, heightPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            circles.forEach { circle ->
                val centerPx = Offset(circle.center.x * widthPx, circle.center.y * heightPx)
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

        regions.filter { it.memberIds.isNotEmpty() }.forEach { region ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (region.center.x * widthPx - 20.dp.toPx()).roundToInt(),
                            y = (region.center.y * heightPx - 12.dp.toPx()).roundToInt(),
                        )
                    }
                    .size(width = 40.dp, height = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = region.memberIds.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                )
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
