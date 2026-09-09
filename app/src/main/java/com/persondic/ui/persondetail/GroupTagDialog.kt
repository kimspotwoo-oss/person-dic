package com.persondic.ui.persondetail

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.persondic.R
import com.persondic.ui.common.GroupTagEditor

/**
 * Editing groups behind a pencil, like the name and the fixed information.
 *
 * The editor used to sit open in the header, where its suggestion chips grew a row taller with
 * every new group and pushed the person's actual content off the screen. It is also the only part
 * of the header that was permanently in edit mode, which made the pencils elsewhere look arbitrary.
 */
@Composable
fun GroupTagDialog(
    selectedTags: List<String>,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_tags_edit)) },
        text = {
            GroupTagEditor(
                selectedTags = selectedTags,
                allTags = allTags,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_save)) }
        },
    )
}
