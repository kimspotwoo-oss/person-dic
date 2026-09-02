package com.persondic.ui.persondetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Interaction
import com.persondic.ui.common.commitmentStatusLabel
import com.persondic.ui.common.directionLabel
import com.persondic.ui.common.interactionKindLabel
import java.time.Instant
import java.time.ZoneId

@Composable
fun InteractionsTab(interactions: List<Interaction>) {
    if (interactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.person_detail_interactions_empty))
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(interactions, key = { it.id.toString() }) { interaction ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = interaction.summary?.takeIf { it.isNotBlank() } ?: interactionKindLabel(interaction.kind),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val meta = listOfNotNull(interaction.place, formatDate(interaction.metAt)).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CommitmentsTab(commitments: List<Commitment>) {
    if (commitments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.person_detail_commitments_empty))
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(commitments, key = { it.id.toString() }) { commitment ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(text = commitment.body, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${directionLabel(commitment.direction)} · ${commitmentStatusLabel(commitment.status)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return "%04d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)
}
