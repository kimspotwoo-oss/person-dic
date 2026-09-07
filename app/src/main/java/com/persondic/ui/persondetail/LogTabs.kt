package com.persondic.ui.persondetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Interaction
import com.persondic.ui.common.commitmentStatusLabel
import com.persondic.ui.common.directionLabel
import com.persondic.ui.common.interactionKindLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun LazyListScope.interactionItems(interactions: List<Interaction>, onOpen: (Interaction) -> Unit) {
    if (interactions.isEmpty()) {
        item(key = "interactions-empty") { EmptyTabMessage(R.string.person_detail_interactions_empty) }
        return
    }
    items(interactions, key = { "interaction-${it.id}" }) { interaction ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen(interaction) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = interaction.summary?.takeIf { it.isNotBlank() } ?: interactionKindLabel(interaction.kind),
                style = MaterialTheme.typography.bodyLarge,
            )
            interaction.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.commitmentItems(commitments: List<Commitment>, onLongPress: (Commitment) -> Unit) {
    if (commitments.isEmpty()) {
        item(key = "commitments-empty") { EmptyTabMessage(R.string.person_detail_commitments_empty) }
        return
    }
    items(commitments, key = { "commitment-${it.id}" }) { commitment ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { onLongPress(commitment) })
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(text = commitment.body, style = MaterialTheme.typography.bodyLarge)
            val meta = listOfNotNull(
                directionLabel(commitment.direction),
                commitmentStatusLabel(commitment.status),
                commitment.dueOn?.let { formatDueDate(it) },
            ).joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDueDate(date: LocalDate): String =
    "%d.%02d.%02d 마감".format(date.year, date.monthValue, date.dayOfMonth)

private fun formatDate(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return "%04d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)
}
