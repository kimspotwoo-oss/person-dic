package com.persondic.ui.persondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R

/**
 * Turns a group tag into a fixed-information entry.
 *
 * #고려대학교 and #06 are not groups, they are a school and a birth year — attributes wearing a
 * group's clothes, because a tag was one tap and an entry was four boxes. The entry side is as
 * cheap as the tag side now, but that does nothing for the tags already typed in, and nobody wants
 * to retype twelve of them.
 *
 * The value is the part the app already has; only the label is missing, and only a person can
 * supply it — whether 고려대학교 belongs under 학교, 대학 or 출신 학교 is a choice about how the rest
 * of the list will read, not something to guess. So it asks, offering the labels already in use.
 *
 * The tag goes when the entry lands, because the point is to stop holding the same fact twice.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveTagDialog(
    tag: String,
    suggestedLabels: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (label: String) -> Unit,
) {
    var label by rememberSaveable(tag) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_tag_title, tag)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.move_tag_hint, tag),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fixed_info_attribute_label)) },
                )
                if (suggestedLabels.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestedLabels.forEach { suggestion ->
                            AssistChip(onClick = { label = suggestion }, label = { Text(suggestion) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.trim()) },
                enabled = label.isNotBlank(),
            ) {
                Text(stringResource(R.string.move_tag_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
