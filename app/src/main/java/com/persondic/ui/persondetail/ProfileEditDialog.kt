package com.persondic.ui.persondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Person

/**
 * Corrects the name, 별명 and 계기.
 *
 * Without this a misspelled name entered once could never be fixed, which also made the 계기 field
 * on the add screen write-once.
 */
@Composable
fun ProfileEditDialog(
    person: Person,
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, alias: String?, metStory: String?) -> Unit,
) {
    var displayName by remember { mutableStateOf(person.displayName) }
    var alias by remember { mutableStateOf(person.alias.orEmpty()) }
    var metStory by remember { mutableStateOf(person.metStory.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.person_edit_profile)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.person_field_display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(stringResource(R.string.person_field_alias)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = metStory,
                    onValueChange = { metStory = it },
                    label = { Text(stringResource(R.string.person_field_met_story)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        displayName,
                        alias.trim().takeIf { it.isNotEmpty() },
                        metStory.trim().takeIf { it.isNotEmpty() },
                    )
                },
                enabled = displayName.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
