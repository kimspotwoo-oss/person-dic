package com.persondic.ui.interactionlog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.ui.common.SectionLabel
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionLogScreen(
    personId: UUID,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: InteractionLogViewModel = viewModel(
        factory = ViewModelFactory { InteractionLogViewModel(application.repository, personId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interaction_log_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved = onDone) }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
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
            SectionLabel(stringResource(R.string.interaction_field_datetime))
            DateTimeField(dateTime = uiState.dateTime, onChange = viewModel::onDateTimeChange)

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.place,
                onValueChange = viewModel::onPlaceChange,
                label = { Text(stringResource(R.string.interaction_field_place)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.summary,
                onValueChange = viewModel::onSummaryChange,
                label = { Text(stringResource(R.string.interaction_field_summary)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.interaction_field_new_facts))
            uiState.newFactDrafts.forEachIndexed { index, body ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = body, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeNewFactDraft(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.newFactBody,
                    onValueChange = viewModel::onNewFactBodyChange,
                    label = { Text(stringResource(R.string.interaction_new_fact_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::addNewFactDraft) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
        }
    }
}

@Composable
private fun DateTimeField(dateTime: LocalDateTime, onChange: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onChange(LocalDateTime.of(year, month + 1, dayOfMonth, hour, minute))
                        },
                        dateTime.hour,
                        dateTime.minute,
                        true,
                    ).show()
                },
                dateTime.year,
                dateTime.monthValue - 1,
                dateTime.dayOfMonth,
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(formatDateTime(dateTime))
    }
}

private fun formatDateTime(dateTime: LocalDateTime): String =
    "%04d.%02d.%02d %02d:%02d".format(
        dateTime.year,
        dateTime.monthValue,
        dateTime.dayOfMonth,
        dateTime.hour,
        dateTime.minute,
    )
