package com.persondic.ui.persondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.ui.common.GroupTagEditor
import com.persondic.ui.common.SectionLabel

/**
 * Editing groups behind a pencil, like the name and the fixed information.
 *
 * The editor used to sit open in the header, where its suggestion chips grew a row taller with
 * every new group and pushed the person's actual content off the screen. It is also the only part
 * of the header that was permanently in edit mode, which made the pencils elsewhere look arbitrary.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupTagDialog(
    selectedTags: List<String>,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onMoveTag: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_tags_edit)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GroupTagEditor(
                    selectedTags = selectedTags,
                    allTags = allTags,
                    onAddTag = onAddTag,
                    onRemoveTag = onRemoveTag,
                )

                // Where a tag turns out to have been an attribute all along. Offered here rather
                // than in the fixed-information editor because this is where the tags are, and the
                // tag is the half the app already knows.
                if (selectedTags.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.move_tag_move))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        selectedTags.forEach { tag ->
                            AssistChip(
                                onClick = { onMoveTag(tag) },
                                label = { Text("#$tag →") },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_save)) }
        },
    )
}
