package com.persondic.ui.persondetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.ui.common.FixedInfoBlock
import com.persondic.ui.common.SUGGESTED_ATTRIBUTE_LABELS
import com.persondic.ui.common.GroupTagEditor
import com.persondic.ui.common.PhotoPickerRow
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.deleteStoredPhoto
import com.persondic.ui.common.requirePersonDicApplication
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: UUID,
    onBack: () -> Unit,
    onBriefingClick: (UUID) -> Unit,
    onAddFact: (UUID) -> Unit,
    onEditFact: (UUID, UUID) -> Unit,
    onOpenInteraction: (UUID, UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: PersonDetailViewModel = viewModel(
        factory = ViewModelFactory { PersonDetailViewModel(application.repository, personId) },
    )
    val person by viewModel.person.collectAsStateWithLifecycle()
    val factGroups by viewModel.factGroups.collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val commitments by viewModel.commitments.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val attributes by viewModel.attributes.collectAsStateWithLifecycle()
    val allAttributeLabels by viewModel.allAttributeLabels.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var actionMenuFact by remember { mutableStateOf<Fact?>(null) }
    var showAddCommitmentDialog by rememberSaveable { mutableStateOf(false) }
    var actionMenuCommitment by remember { mutableStateOf<Commitment?>(null) }
    var showFixedInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(person?.displayName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(onClick = { onAddFact(personId) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fact_add))
                }
                2 -> FloatingActionButton(onClick = { showAddCommitmentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.commitment_add_title))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            person?.let { loaded ->
                PersonHeader(
                    person = loaded,
                    tags = tags,
                    allTags = allTags,
                    attributes = attributes,
                    onEditFixedInfo = { showFixedInfoDialog = true },
                    onEditProfile = { showProfileDialog = true },
                    onPhotoPicked = { picked ->
                        deleteStoredPhoto(loaded.photoUri)
                        viewModel.setPhoto(picked)
                    },
                    onPhotoCleared = {
                        deleteStoredPhoto(loaded.photoUri)
                        viewModel.setPhoto(null)
                    },
                    onAddTag = viewModel::addTag,
                    onRemoveTag = viewModel::removeTag,
                )
            }

            Button(
                onClick = { onBriefingClick(personId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.person_detail_view_briefing))
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.person_detail_tab_facts)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.person_detail_tab_interactions)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.person_detail_tab_commitments)) },
                )
            }

            when (selectedTab) {
                0 -> FactsTab(groups = factGroups, onLongPress = { actionMenuFact = it })
                1 -> InteractionsTab(
                    interactions = interactions,
                    onOpen = { onOpenInteraction(personId, it.id) },
                )
                else -> CommitmentsTab(
                    commitments = commitments,
                    onLongPress = { actionMenuCommitment = it },
                )
            }
        }
    }

    if (showProfileDialog) {
        person?.let { loaded ->
            ProfileEditDialog(
                person = loaded,
                onDismiss = { showProfileDialog = false },
                onConfirm = { displayName, alias, metStory ->
                    viewModel.updateProfile(displayName, alias, metStory)
                    showProfileDialog = false
                },
            )
        }
    }

    if (showFixedInfoDialog) {
        person?.let { loaded ->
            FixedInfoDialog(
                person = loaded,
                attributes = attributes,
                suggestedLabels = (allAttributeLabels + SUGGESTED_ATTRIBUTE_LABELS)
                    .distinct()
                    .filterNot { label -> attributes.any { it.label == label } },
                onDismiss = { showFixedInfoDialog = false },
                onSetBirthday = viewModel::setBirthday,
                onSetAttribute = viewModel::setAttribute,
                onRemoveAttribute = viewModel::removeAttribute,
            )
        }
    }

    actionMenuFact?.let { fact ->
        FactActionDialog(
            fact = fact,
            onDismiss = { actionMenuFact = null },
            onEdit = {
                actionMenuFact = null
                onEditFact(personId, fact.id)
            },
            onDelete = {
                actionMenuFact = null
                viewModel.deleteFact(fact)
            },
            onTogglePinned = {
                actionMenuFact = null
                viewModel.togglePinned(fact)
            },
        )
    }

    if (showAddCommitmentDialog) {
        AddCommitmentDialog(
            onDismiss = { showAddCommitmentDialog = false },
            onConfirm = { direction, body, dueOn ->
                viewModel.addCommitment(direction, body, dueOn)
                showAddCommitmentDialog = false
            },
        )
    }

    actionMenuCommitment?.let { commitment ->
        CommitmentActionDialog(
            commitment = commitment,
            onDismiss = { actionMenuCommitment = null },
            onSetStatus = { status ->
                actionMenuCommitment = null
                viewModel.setCommitmentStatus(commitment, status)
            },
            onDelete = {
                actionMenuCommitment = null
                viewModel.deleteCommitment(commitment)
            },
        )
    }
}

@Composable
private fun PersonHeader(
    person: Person,
    tags: List<String>,
    allTags: List<String>,
    attributes: List<PersonAttribute>,
    onEditFixedInfo: () -> Unit,
    onEditProfile: () -> Unit,
    onPhotoPicked: (String) -> Unit,
    onPhotoCleared: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        PhotoPickerRow(
            name = person.displayName,
            photoUri = person.photoUri,
            onPhotoPicked = onPhotoPicked,
            onPhotoCleared = onPhotoCleared,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = person.displayName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEditProfile) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.person_edit_profile))
            }
        }
        person.alias?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        person.metStory?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FixedInfoBlock(person = person, attributes = attributes)
            }
            IconButton(onClick = onEditFixedInfo) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.fixed_info_edit))
            }
        }

        GroupTagEditor(
            selectedTags = tags,
            allTags = allTags,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
        )
    }
}
