package com.persondic.ui.personlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.persondic.ui.common.PersonAvatar
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.relativeDaysLabel
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // The add button floats over the list, and without this it sat on top of the
                    // last person instead of below them.
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
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
            Text(
                text = item.person.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // When it was last and every tag after it, this ran to two lines of text that was
            // mostly true of everyone. Now: when, then only what sets this person apart, one line.
            val subtitle = listOfNotNull(
                item.daysSinceLastInteraction?.let { relativeDaysLabel(it) },
                item.distinguishingTags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

