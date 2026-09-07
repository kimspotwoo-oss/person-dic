package com.persondic.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.backup.BackupManager
import com.persondic.data.backup.ExportSummary
import com.persondic.data.backup.ImportFailure
import com.persondic.data.backup.ImportResult
import com.persondic.data.repository.PersonDicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** What the last export or import did, so the screen can report it instead of finishing silently. */
sealed interface BackupOutcome {
    data class Exported(val summary: ExportSummary) : BackupOutcome

    data class Imported(val people: Int, val facts: Int, val photos: Int, val skipped: Int) : BackupOutcome

    data class ImportRejected(val reason: ImportFailure) : BackupOutcome

    /** The document picker handed back something the app could not open. */
    data object StreamUnavailable : BackupOutcome

    data object Failed : BackupOutcome
}

data class BackupUiState(
    val isBusy: Boolean = false,
    val outcome: BackupOutcome? = null,
)

class BackupViewModel(
    private val repository: PersonDicRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /**
     * Streams are opened by a lambda rather than passed in already open, so the file work happens
     * entirely on the IO dispatcher and the ViewModel never has to hold a Context.
     */
    fun export(openStream: () -> OutputStream?) = runBackupJob {
        val stream = openStream() ?: return@runBackupJob BackupOutcome.StreamUnavailable
        val snapshot = repository.exportSnapshot()
        stream.use { BackupOutcome.Exported(backupManager.export(snapshot, it)) }
    }

    fun import(openStream: () -> InputStream?) = runBackupJob {
        val stream = openStream() ?: return@runBackupJob BackupOutcome.StreamUnavailable
        when (val result = stream.use { backupManager.import(it) }) {
            is ImportResult.Failure -> BackupOutcome.ImportRejected(result.reason)
            is ImportResult.Success -> {
                val dropped = repository.importSnapshot(result.snapshot)
                BackupOutcome.Imported(
                    people = result.snapshot.people.size,
                    facts = result.snapshot.facts.size,
                    photos = result.photos,
                    skipped = result.skippedRows + dropped,
                )
            }
        }
    }

    fun consumeOutcome() {
        _uiState.update { it.copy(outcome = null) }
    }

    private fun runBackupJob(block: suspend () -> BackupOutcome) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, outcome = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { block() }.getOrDefault(BackupOutcome.Failed)
            }
            _uiState.update { it.copy(isBusy = false, outcome = outcome) }
        }
    }
}

/** Default archive name, e.g. persondic-backup-20260907-1430.zip. */
fun defaultBackupFileName(now: LocalDateTime = LocalDateTime.now()): String =
    "persondic-backup-${now.format(FILE_STAMP)}.zip"

private val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
