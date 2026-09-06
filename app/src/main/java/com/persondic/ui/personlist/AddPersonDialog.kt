package com.persondic.ui.personlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.ui.common.GroupTagEditor
import com.persondic.ui.common.PhotoPickerRow
import com.persondic.ui.common.deleteStoredPhoto

@Composable
fun AddPersonDialog(
    allTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, alias: String?, tags: List<String>, photoUri: String?) -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var tags by remember { mutableStateOf(emptyList<String>()) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.person_list_add_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PhotoPickerRow(
                    name = displayName,
                    photoUri = photoUri,
                    onPhotoPicked = { picked ->
                        deleteStoredPhoto(photoUri)
                        photoUri = picked
                    },
                    onPhotoCleared = {
                        deleteStoredPhoto(photoUri)
                        photoUri = null
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.person_field_display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(stringResource(R.string.person_field_alias)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                GroupTagEditor(
                    selectedTags = tags,
                    allTags = allTags,
                    onAddTag = { tag -> if (tag !in tags) tags = tags + tag },
                    onRemoveTag = { tag -> tags = tags - tag },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(displayName, alias, tags, photoUri) },
                enabled = displayName.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    deleteStoredPhoto(photoUri)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
