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
import com.persondic.domain.TimelineMonth
import com.persondic.ui.common.relativeDateLabel
import com.persondic.ui.common.commitmentStatusLabel
import com.persondic.ui.common.directionLabel
import com.persondic.ui.common.interactionKindLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The meeting history as a timeline.
 *
 * Months are the structure, because when is what a person scans this by — the date used to be the
 * last line of every row, which is the one place it cannot be scanned from. Each row leads with how
 * long ago instead ("3주 전"), since that is the form the question takes.
 */
fun LazyListScope.interactionItems(
    months: List<TimelineMonth>,
    onOpen: (Interaction) -> Unit,
) {
    if (months.isEmpty()) {
        item(key = "interactions-empty") { EmptyTabMessage(R.string.person_detail_interactions_empty) }
        return
    }
    months.forEach { month ->
        item(key = "month-${month.year}-${month.month}") {
            Text(
                text = stringResource(R.string.timeline_month, month.year, month.month),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
            )
        }
        items(month.entries, key = { "interaction-${it.interaction.id}" }) { entry ->
            val interaction = entry.interaction
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(interaction) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = listOfNotNull(
                        relativeDateLabel(interaction.metAt),
                        formatDate(interaction.metAt),
                        interaction.place?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = interaction.summary?.takeIf { it.isNotBlank() }
                        ?: interactionKindLabel(interaction.kind),
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
                if (entry.factCount > 0) {
                    Text(
                        text = stringResource(R.string.timeline_facts_from_here, entry.factCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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
