package com.persondic.ui.relationmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RelationMapScreen(
    onBack: () -> Unit,
    onPersonClick: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: RelationMapViewModel = viewModel(
        factory = ViewModelFactory { RelationMapViewModel(application.repository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.relation_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.relation_map_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (!uiState.hasSelf) {
                SelfPrompt(onCreate = { name -> viewModel.createSelf(name) })
            }

            RelationCanvas(graph = uiState.graph, onPersonClick = onPersonClick)

            // 전체 first, then one button per label that exists.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = uiState.selectedLabel == null,
                    onClick = { viewModel.onSelectLabel(null) },
                    label = { Text(stringResource(R.string.relation_map_all)) },
                )
                uiState.allLabels.forEach { label ->
                    FilterChip(
                        selected = uiState.selectedLabel == label,
                        onClick = { viewModel.onSelectLabel(label) },
                        label = { Text(label) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.relation_map_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SelfPrompt(onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.relation_map_no_self),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.relation_map_self_name)) },
        )
        Button(
            onClick = { onCreate(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.relation_map_create_self))
        }
    }
}

@Composable
private fun RelationCanvas(graph: RelationGraph, onPersonClick: (UUID) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val side = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val originX = (constraints.maxWidth - side) / 2f
        val originY = (constraints.maxHeight - side) / 2f
        fun place(x: Float, y: Float) = Offset(originX + x * side, originY + y * side)

        val positions = graph.nodes.associate { it.personId to place(it.x, it.y) }
        // The layout sized the nodes to the rings it produced; drawing at any other size puts
        // circles back on top of each other or off the edge.
        val nodeRadiusPx = graph.nodeRadius * side

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(graph) {
                    detectTapGestures { tap ->
                        graph.nodes.firstOrNull { node ->
                            val centre = positions[node.personId] ?: return@firstOrNull false
                            !node.isSelf && (tap - centre).getDistance() <= nodeRadiusPx
                        }?.let { onPersonClick(it.personId) }
                    }
                },
        ) {
            graph.edges.forEach { edge ->
                val from = positions[edge.fromPersonId] ?: return@forEach
                val to = positions[edge.toPersonId] ?: return@forEach

                // Stop the line at the edge of each circle so it does not run under the names.
                val angle = atan2(to.y - from.y, to.x - from.x)
                val start = Offset(
                    from.x + nodeRadiusPx * cos(angle),
                    from.y + nodeRadiusPx * sin(angle),
                )
                val end = Offset(
                    to.x - nodeRadiusPx * cos(angle),
                    to.y - nodeRadiusPx * sin(angle),
                )
                drawLine(color = outline, start = start, end = end, strokeWidth = 2f)

                if (edge.symmetric) {
                    // Plain dots on both ends: the label reads the same either way.
                    drawCircle(color = outline, radius = 5f, center = start)
                    drawCircle(color = outline, radius = 5f, center = end)
                } else {
                    drawCircle(color = outline, radius = 5f, center = start)
                    val head = ARROW_HEAD_PX
                    listOf(angle + ARROW_SPREAD, angle - ARROW_SPREAD).forEach { side ->
                        drawLine(
                            color = outline,
                            start = end,
                            end = Offset(end.x - head * cos(side), end.y - head * sin(side)),
                            strokeWidth = 3f,
                        )
                    }
                }
            }

            graph.nodes.forEach { node ->
                val centre = positions[node.personId] ?: return@forEach
                drawCircle(
                    color = if (node.isSelf) primary else surface,
                    radius = nodeRadiusPx,
                    center = centre,
                    alpha = if (node.isSelf) 0.35f else 1f,
                )
                drawCircle(
                    color = if (node.isSelf) primary else outline,
                    radius = nodeRadiusPx,
                    center = centre,
                    style = Stroke(width = if (node.isSelf) 4f else 2f),
                )
            }
        }

        val diameter = with(LocalDensity.current) { (nodeRadiusPx * 2).toDp() }
        graph.nodes.forEach { node ->
            val centre = positions[node.personId] ?: return@forEach
            val label = graphNodeLabel(node.name, diameter.value)
            if (label.isEmpty()) return@forEach
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (centre.x - nodeRadiusPx).roundToInt(),
                            y = (centre.y - nodeRadiusPx).roundToInt(),
                        )
                    }
                    .size(diameter),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private const val ARROW_HEAD_PX = 18f
private const val ARROW_SPREAD = 0.5f
