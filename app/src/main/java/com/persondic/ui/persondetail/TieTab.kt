package com.persondic.ui.persondetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Person
import com.persondic.domain.SUGGESTED_TIE_LABELS
import com.persondic.domain.TieView
import java.util.UUID

fun LazyListScope.tieItems(ties: List<TieView>, onOpen: (UUID) -> Unit, onLongPress: (TieView) -> Unit) {
    if (ties.isEmpty()) {
        item(key = "ties-empty") { EmptyTabMessage(R.string.person_detail_ties_empty) }
        return
    }
    items(ties, key = { "tie-${it.tieId}" }) { tie ->
        TieRow(tie = tie, onOpen = { onOpen(tie.otherPersonId) }, onLongPress = { onLongPress(tie) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TieRow(tie: TieView, onOpen: () -> Unit, onLongPress: () -> Unit) {
    Text(
        text = tie.text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/**
 * Picks the other end of a tie and labels it.
 *
 * The label describes the person being picked, not the one whose screen this is — "소개해준 사람"
 * means they introduced you. The other person's screen shows the same tie phrased the other way
 * round, so it stays true there too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTieDialog(
    candidates: List<Person>,
    suggestedLabels: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (otherPersonId: UUID, label: String) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Person?>(null) }

    val matches = remember(candidates, query) {
        val needle = query.trim()
        if (needle.isEmpty()) candidates else candidates.filter {
            it.displayName.contains(needle, ignoreCase = true) ||
                it.alias?.contains(needle, ignoreCase = true) == true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tie_add_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.tie_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.tie_label)) },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestedLabels.forEach { suggestion ->
                        AssistChip(onClick = { label = suggestion }, label = { Text(suggestion) })
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.tie_pick_person)) },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 220.dp),
                ) {
                    matches.take(MAX_CANDIDATES).forEach { person ->
                        FilterChip(
                            selected = selected?.id == person.id,
                            onClick = { selected = person },
                            label = { Text(person.displayName) },
                        )
                    }
                }
                if (matches.size > MAX_CANDIDATES) {
                    Text(
                        text = stringResource(R.string.tie_narrow_search, matches.size - MAX_CANDIDATES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onConfirm(it.id, label) } },
                enabled = selected != null && label.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun RemoveTieDialog(tie: TieView, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tie.text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Chips stop being a picker past this many; the search box takes over. */
private const val MAX_CANDIDATES = 30

/** Kept next to the tie UI so the suggestions the dialog shows have one source. */
val defaultTieLabels: List<String> get() = SUGGESTED_TIE_LABELS
