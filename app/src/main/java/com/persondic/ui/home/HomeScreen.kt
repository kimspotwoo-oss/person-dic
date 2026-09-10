package com.persondic.ui.home

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.data.local.entity.Person
import com.persondic.ui.common.ReminderRows
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import java.time.LocalDate
import java.util.UUID

/**
 * What the app opens on.
 *
 * A list of everybody was the wrong first screen: it answers "who do I know", which is a question
 * nobody opens this app to ask. The two that get asked are "we just met, let me write it down" and
 * "is there anything I owe somebody" — so those are what is here, and the list of people moved to
 * its own tab where it can be browsed on purpose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecordMeeting: () -> Unit,
    onPersonClick: (UUID) -> Unit,
    onGroupMapClick: () -> Unit,
    onRelationMapClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onBackupClick: () -> Unit,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory { HomeViewModel(application.repository) },
    )
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var addingReminder by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.group_map_open)) },
                            onClick = { showMenu = false; onGroupMapClick() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.relation_map_open)) },
                            onClick = { showMenu = false; onRelationMapClick() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quick_add_open)) },
                            onClick = { showMenu = false; onQuickAddClick() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup_open)) },
                            onClick = { showMenu = false; onBackupClick() },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // The reason the app is open at all, and the one thing that has to be quick — it gets
            // pressed on the walk home, standing up, with one hand.
            Button(
                onClick = onRecordMeeting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_record_meeting),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = stringResource(R.string.person_list_reminders_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 28.dp),
            )
            if (reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_reminders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                ReminderRows(
                    reminders = reminders,
                    onPersonClick = onPersonClick,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            OutlinedButton(
                onClick = { addingReminder = true },
                enabled = people.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.home_add_reminder),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (addingReminder) {
        AddReminderDialog(
            people = people,
            onDismiss = { addingReminder = false },
            onConfirm = { personId, body, dueOn ->
                viewModel.addReminder(personId, body, dueOn)
                addingReminder = false
            },
        )
    }
}

/**
 * Setting a reminder: somebody, something, and a date.
 *
 * All three are needed. A reminder with no date cannot be shown at the right time, and one with
 * nobody attached has nowhere to live in an app where everything hangs off a person.
 */
@Composable
private fun AddReminderDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onConfirm: (UUID, String, LocalDate) -> Unit,
) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var body by rememberSaveable { mutableStateOf("") }
    var dueOn by rememberSaveable { mutableStateOf<String?>(null) }
    val dueDate = remember(dueOn) { dueOn?.let(LocalDate::parse) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_add_reminder)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.home_reminder_body)) },
                )
                OutlinedButton(
                    onClick = {
                        val base = dueDate ?: LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dueOn = LocalDate.of(year, month + 1, dayOfMonth).toString()
                            },
                            base.year,
                            base.monthValue - 1,
                            base.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        dueDate?.let { "%d.%02d.%02d".format(it.year, it.monthValue, it.dayOfMonth) }
                            ?: stringResource(R.string.home_reminder_when),
                    )
                }
                Text(
                    text = stringResource(R.string.home_reminder_who),
                    style = MaterialTheme.typography.labelLarge,
                )
                people.forEach { person ->
                    val isSelected = selected == person.id.toString()
                    Text(
                        text = person.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = person.id.toString() }
                            .padding(vertical = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && body.isNotBlank() && dueDate != null,
                onClick = {
                    val personId = selected
                    val date = dueDate
                    if (personId != null && date != null) {
                        onConfirm(UUID.fromString(personId), body, date)
                    }
                },
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
