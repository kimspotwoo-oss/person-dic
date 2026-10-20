package com.persondic.ui.persondetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Interaction
import com.persondic.domain.TimelineMonth
import com.persondic.domain.splitCommitments
import com.persondic.ui.common.relativeDateLabel
import com.persondic.ui.common.commitmentStatusLabel
import com.persondic.ui.common.directionLabel
import com.persondic.ui.common.interactionKindLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A meeting with this person written down as one line (G36).
 *
 * The person is the page, so the one question S4 will not skip is already answered; the date is
 * now and the line is the summary. It saves the same kind of meeting S4 does, so tapping the row
 * it leaves opens S4 to add a place or facts afterwards. The line is cleared only once the save
 * has landed — clearing on tap would lose it if the save never happened.
 */
fun LazyListScope.oneLineMeetingItem(
    saving: Boolean,
    onSave: (line: String, onSaved: () -> Unit) -> Unit,
) {
    item(key = "interaction-one-line") {
        var line by rememberSaveable { mutableStateOf("") }
        val submit: () -> Unit = { if (line.isNotBlank()) onSave(line) { line = "" } }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = line,
                onValueChange = { line = it },
                placeholder = { Text(stringResource(R.string.person_detail_one_line_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = submit, enabled = line.isNotBlank() && !saving) {
                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.person_detail_one_line_save))
            }
        }
    }
}

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
    val sections = splitCommitments(commitments)
    // Every row already shows its status, so a heading only earns its place by dividing the two.
    val showHeadings = sections.open.isNotEmpty() && sections.finished.isNotEmpty()
    listOf(
        R.string.person_detail_commitments_open to sections.open,
        R.string.person_detail_commitments_finished to sections.finished,
    ).forEach { (heading, section) ->
        if (showHeadings) {
            item(key = "commitments-heading-$heading") {
                Text(
                    text = stringResource(heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(section, key = { "commitment-${it.id}" }) { commitment ->
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
}

private fun formatDueDate(date: LocalDate): String =
    "%d.%02d.%02d 마감".format(date.year, date.monthValue, date.dayOfMonth)

private fun formatDate(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return "%04d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)
}
