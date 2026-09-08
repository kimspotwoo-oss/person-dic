package com.persondic.ui.personlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.domain.Reminder
import com.persondic.domain.ReminderKind
import com.persondic.ui.common.PersonAvatar
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    onPersonClick: (UUID) -> Unit,
    onGroupMapClick: () -> Unit,
    onRelationMapClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAddPersonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: PersonListViewModel = viewModel(
        factory = ViewModelFactory { PersonListViewModel(application.repository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
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
                            onClick = {
                                showMenu = false
                                onGroupMapClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.relation_map_open)) },
                            onClick = {
                                showMenu = false
                                onRelationMapClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quick_add_open)) },
                            onClick = {
                                showMenu = false
                                onQuickAddClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup_open)) },
                            onClick = {
                                showMenu = false
                                onBackupClick()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPersonClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.person_list_add))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.person_list_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = viewModel::onToggleSort) {
                    Text(
                        stringResource(
                            when (uiState.sort) {
                                PersonSort.NAME -> R.string.person_list_sort_name
                                PersonSort.LEAST_RECENT -> R.string.person_list_sort_least_recent
                            },
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(stringResource(R.string.person_list_group_toggle))
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = uiState.isGroupedByTag, onCheckedChange = { viewModel.onToggleGroupByTag() })
            }

            if (uiState.isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.person_list_empty))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Hidden while searching: these answer "who should I get in touch with",
                    // which is not the question being asked once a name has been typed.
                    if (uiState.searchQuery.isBlank() && uiState.reminders.isNotEmpty()) {
                        item(key = "reminders") {
                            ReminderBlock(
                                reminders = uiState.reminders,
                                onPersonClick = onPersonClick,
                            )
                        }
                    }
                    uiState.groups.forEach { group ->
                        if (group.label != null) {
                            item(key = "header-${group.label}") {
                                Text(
                                    text = "#${group.label}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                        items(
                            items = group.people,
                            key = { item -> "${group.label}-${item.person.id}" },
                        ) { item ->
                            PersonRow(item = item, onClick = { onPersonClick(item.person.id) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * The dated reasons to get in touch: commitments that are due and birthdays that are close.
 *
 * Only things with a date on them. Design principle 1 rules out ranking people, and a list of
 * "who you have neglected" would be exactly that; a missed deadline is a fact instead.
 */
@Composable
private fun ReminderBlock(reminders: List<Reminder>, onPersonClick: (UUID) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.person_list_reminders_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        reminders.forEach { reminder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPersonClick(reminder.personId) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reminderWhen(reminder),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (reminder.kind == ReminderKind.COMMITMENT_OVERDUE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.width(76.dp),
                )
                Text(
                    text = when (reminder.kind) {
                        ReminderKind.BIRTHDAY ->
                            stringResource(R.string.person_list_reminder_birthday, reminder.personName)
                        else -> "${reminder.personName} · ${reminder.body}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun reminderWhen(reminder: Reminder): String = when {
    reminder.daysFromToday < 0 -> stringResource(R.string.person_list_reminder_overdue, -reminder.daysFromToday)
    reminder.daysFromToday == 0L -> stringResource(R.string.person_list_reminder_today)
    else -> stringResource(R.string.person_list_reminder_in_days, reminder.daysFromToday)
}

@Composable
private fun PersonRow(item: PersonListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(name = item.person.displayName, photoUri = item.person.photoUri)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.person.displayName, style = MaterialTheme.typography.bodyLarge)
            val subtitle = listOfNotNull(
                item.tags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" },
                lastMetLabel(item.daysSinceLastInteraction),
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun lastMetLabel(days: Long?): String? = when {
    days == null -> null
    days <= 0 -> "오늘 만남"
    else -> "마지막 만남 ${days}일 전"
}
