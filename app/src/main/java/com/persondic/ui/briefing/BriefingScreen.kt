package com.persondic.ui.briefing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(
    personId: UUID,
    onBack: () -> Unit,
    onRecordInteraction: (UUID) -> Unit,
    onEditFact: (UUID, UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: BriefingViewModel = viewModel(
        factory = ViewModelFactory { BriefingViewModel(application.repository, personId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.personName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { onRecordInteraction(personId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.briefing_record_interaction))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (uiState.cautionFacts.isNotEmpty()) {
                item(key = "section-caution") { CautionSection(uiState.cautionFacts) }
            }
            if (uiState.openCommitmentGroups.isNotEmpty()) {
                item(key = "section-commitments") { OpenCommitmentsSection(uiState.openCommitmentGroups) }
            }
            if (uiState.hookFacts.isNotEmpty()) {
                item(key = "section-hooks") { HookSection(uiState.hookFacts) }
            }
            if (uiState.recentInteractions.isNotEmpty()) {
                item(key = "section-interactions") { RecentInteractionsSection(uiState.recentInteractions) }
            }
            if (uiState.knowledgeGroups.isNotEmpty()) {
                item(key = "section-knowledge") { KnowledgeSection(uiState.knowledgeGroups) }
            }
            if (uiState.sensitiveFacts.isNotEmpty()) {
                item(key = "section-sensitive") { SensitiveSection(uiState.sensitiveFacts) }
            }
            if (uiState.staleFacts.isNotEmpty()) {
                item(key = "section-stale-header") {
                    SectionHeader(stringResource(R.string.briefing_section_stale))
                }
                items(uiState.staleFacts, key = { "stale-${it.id}" }) { fact ->
                    StaleFactRow(
                        fact = fact,
                        onStillValid = { viewModel.markStillValid(fact) },
                        onEdit = { onEditFact(personId, fact.id) },
                        onDelete = { viewModel.deleteFact(fact) },
                    )
                }
            }
        }
    }
}
