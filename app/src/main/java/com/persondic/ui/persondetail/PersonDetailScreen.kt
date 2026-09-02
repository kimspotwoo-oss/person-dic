package com.persondic.ui.persondetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Person
import com.persondic.ui.common.ViewModelFactory
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

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var actionMenuFact by remember { mutableStateOf<Fact?>(null) }

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
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { onAddFact(personId) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fact_add))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            person?.let { PersonHeader(it) }

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
                1 -> InteractionsTab(interactions = interactions)
                else -> CommitmentsTab(commitments = commitments)
            }
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
}

@Composable
private fun PersonHeader(person: Person) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = person.displayName, style = MaterialTheme.typography.headlineSmall)
        val subtitle = listOfNotNull(person.alias, person.groupTag).joinToString(" · ")
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
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
    }
}
