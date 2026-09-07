package com.persondic.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.persondic.data.backup.ImportFailure
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.requirePersonDicApplication()
    val viewModel: BackupViewModel = viewModel(
        factory = ViewModelFactory { BackupViewModel(application.repository, application.backupManager) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            viewModel.export { context.contentResolver.openOutputStream(uri) }
        }
    }

    // Some file pickers report a .zip as octet-stream, so the filter stays wide and the archive
    // itself decides whether the file is a backup.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.import { context.contentResolver.openInputStream(uri) }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { exportLauncher.launch(defaultBackupFileName()) },
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_export))
            }

            Text(
                text = stringResource(R.string.backup_import_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { importLauncher.launch(IMPORT_MIME_FILTER) },
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_import))
            }

            if (uiState.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.outcome?.let { outcome ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = outcomeMessage(outcome),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun outcomeMessage(outcome: BackupOutcome): String = when (outcome) {
    is BackupOutcome.Exported -> stringResource(
        R.string.backup_export_done,
        outcome.summary.people,
        outcome.summary.facts,
        outcome.summary.photos,
    )

    is BackupOutcome.Imported -> {
        val base = stringResource(
            R.string.backup_import_done,
            outcome.people,
            outcome.facts,
            outcome.photos,
        )
        if (outcome.skipped > 0) {
            base + " " + stringResource(R.string.backup_import_skipped, outcome.skipped)
        } else {
            base
        }
    }

    is BackupOutcome.ImportRejected -> when (outcome.reason) {
        ImportFailure.NOT_A_BACKUP -> stringResource(R.string.backup_error_not_a_backup)
        ImportFailure.UNSUPPORTED_VERSION -> stringResource(R.string.backup_error_unsupported_version)
        ImportFailure.UNREADABLE -> stringResource(R.string.backup_error_unreadable)
    }

    BackupOutcome.StreamUnavailable -> stringResource(R.string.backup_error_no_stream)

    BackupOutcome.Failed -> stringResource(R.string.backup_error_generic)
}

private const val BACKUP_MIME_TYPE = "application/zip"
private val IMPORT_MIME_FILTER = arrayOf("application/zip", "application/octet-stream", "*/*")
