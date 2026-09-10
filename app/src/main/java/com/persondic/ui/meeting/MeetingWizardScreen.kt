package com.persondic.ui.meeting

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.data.local.entity.Person
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Volatility
import com.persondic.domain.DraftFact
import com.persondic.domain.MeetingDraft
import com.persondic.domain.MeetingStep
import com.persondic.ui.common.PersonAvatar
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.categoryLabel
import com.persondic.ui.common.requirePersonDicApplication
import com.persondic.ui.common.volatilityLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Writing down a meeting, one question to a screen.
 *
 * The old screen put the date, the place, the summary and the facts on one page, which reads as a
 * form — and a form is what stops a meeting getting written down on the walk home. One question at
 * a time is the shape sign-up flows use to get long forms answered, and it works here for the same
 * reason: each screen asks for something you can answer without deciding anything else first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingWizardScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: MeetingWizardViewModel = viewModel(
        factory = ViewModelFactory { MeetingWizardViewModel(application.repository) },
    )
    val step by viewModel.step.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val people by viewModel.selectableStatePeople.collectAsStateWithLifecycle()

    val leave = { if (viewModel.onBack()) onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(stepTitle(step))) },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (step.previous != null) {
                    OutlinedButton(onClick = leave, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.meeting_previous))
                    }
                }
                if (step.next == null) {
                    Button(
                        onClick = { viewModel.save(onSaved) },
                        enabled = !saving,
                        modifier = Modifier.weight(2f),
                    ) {
                        Text(stringResource(R.string.meeting_save))
                    }
                } else {
                    Button(
                        onClick = viewModel::onNext,
                        enabled = draft.canLeave(step),
                        modifier = Modifier.weight(2f),
                    ) {
                        Text(stringResource(R.string.meeting_next))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            LinearProgressIndicator(
                progress = { (step.ordinal + 1f) / MeetingStep.entries.size },
                modifier = Modifier.fillMaxWidth(),
            )
            when (step) {
                MeetingStep.WHO -> WhoStep(
                    people = people,
                    draft = draft,
                    onToggle = viewModel::onToggleAttendee,
                    onAddPerson = viewModel::onAddNewPerson,
                )

                MeetingStep.WHEN_WHERE -> WhenWhereStep(
                    draft = draft,
                    onMetAtChange = viewModel::onMetAtChange,
                    onPlaceChange = viewModel::onPlaceChange,
                )

                MeetingStep.SUMMARY -> SummaryStep(
                    draft = draft,
                    onSummaryChange = viewModel::onSummaryChange,
                    onNotesChange = viewModel::onNotesChange,
                )

                MeetingStep.FACTS -> FactsStep(
                    draft = draft,
                    people = people,
                    onAddFact = viewModel::onAddFact,
                    onRemoveFact = viewModel::onRemoveFact,
                )
            }
        }
    }
}

private fun stepTitle(step: MeetingStep): Int = when (step) {
    MeetingStep.WHO -> R.string.meeting_step_who
    MeetingStep.WHEN_WHERE -> R.string.meeting_step_when_where
    MeetingStep.SUMMARY -> R.string.meeting_step_summary
    MeetingStep.FACTS -> R.string.meeting_step_facts
}

@Composable
private fun StepHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

// --- 1. who was there ---

@Composable
private fun WhoStep(
    people: List<Person>,
    draft: MeetingDraft,
    onToggle: (UUID) -> Unit,
    onAddPerson: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var addingPerson by rememberSaveable { mutableStateOf(false) }

    val matches = remember(people, query) {
        val needle = query.trim()
        if (needle.isEmpty()) people else people.filter {
            it.displayName.contains(needle, ignoreCase = true) ||
                it.alias?.contains(needle, ignoreCase = true) == true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StepHint(stringResource(R.string.meeting_who_hint))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.meeting_who_search)) },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "add-person") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addingPerson = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.meeting_who_add_person),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            items(matches, key = { "person-${it.id}" }) { person ->
                val selected = person.id in draft.attendeeIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(person.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PersonAvatar(name = person.displayName, photoUri = person.photoUri)
                    Text(
                        text = person.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.meeting_who_selected),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    if (addingPerson) {
        NewPersonDialog(
            onDismiss = { addingPerson = false },
            onConfirm = { name ->
                onAddPerson(name)
                addingPerson = false
            },
        )
    }
}

@Composable
private fun NewPersonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meeting_who_add_person)) },
        text = {
            // Only the name. Everything else about them can be filled in later, and asking for it
            // now is asking somebody to stop recording a meeting to do data entry.
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.person_field_display_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// --- 2. when and where ---

@Composable
private fun WhenWhereStep(
    draft: MeetingDraft,
    onMetAtChange: (LocalDateTime) -> Unit,
    onPlaceChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val metAt = remember(draft.metAt) { LocalDateTime.ofInstant(draft.metAt, ZoneId.systemDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        StepHint(stringResource(R.string.meeting_when_hint))
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onMetAtChange(metAt.with(LocalDate.of(year, month + 1, dayOfMonth)))
                    },
                    metAt.year,
                    metAt.monthValue - 1,
                    metAt.dayOfMonth,
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text("%d.%02d.%02d".format(metAt.year, metAt.monthValue, metAt.dayOfMonth))
        }
        OutlinedTextField(
            value = draft.place,
            onValueChange = onPlaceChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.interaction_field_place)) },
        )
    }
}

// --- 3. what happened ---

@Composable
private fun SummaryStep(
    draft: MeetingDraft,
    onSummaryChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHint(stringResource(R.string.meeting_summary_hint))
        OutlinedTextField(
            value = draft.summary,
            onValueChange = onSummaryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.interaction_field_summary)) },
        )
        OutlinedTextField(
            value = draft.notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(160.dp),
            label = { Text(stringResource(R.string.interaction_field_notes)) },
        )
    }
}

// --- 4. what you learned, per person ---

@Composable
private fun FactsStep(
    draft: MeetingDraft,
    people: List<Person>,
    onAddFact: (DraftFact) -> Unit,
    onRemoveFact: (UUID) -> Unit,
) {
    var addingFor by rememberSaveable { mutableStateOf<String?>(null) }
    val namesById = remember(people) { people.associate { it.id to it.displayName } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "hint") { StepHint(stringResource(R.string.meeting_facts_hint)) }

        draft.attendeeIds.forEach { personId ->
            item(key = "head-$personId") {
                Text(
                    text = namesById[personId].orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(draft.factsFor(personId), key = { "fact-${it.id}" }) { fact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = fact.body, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${categoryLabel(fact.category)} · ${volatilityLabel(fact.volatility)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemoveFact(fact.id) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }
            item(key = "add-$personId") {
                TextButton(
                    onClick = { addingFor = personId.toString() },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.meeting_facts_add),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }

    addingFor?.let { personId ->
        AddDraftFactDialog(
            personName = namesById[UUID.fromString(personId)].orEmpty(),
            onDismiss = { addingFor = null },
            onConfirm = { fact ->
                onAddFact(fact.copy(personId = UUID.fromString(personId)))
                addingFor = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddDraftFactDialog(
    personName: String,
    onDismiss: () -> Unit,
    onConfirm: (DraftFact) -> Unit,
) {
    var body by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(FactCategory.CONTEXT) }
    var volatility by rememberSaveable { mutableStateOf(Volatility.SLOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meeting_facts_about, personName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.fact_field_body)) },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FactCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(categoryLabel(option)) },
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Volatility.entries.forEach { option ->
                        FilterChip(
                            selected = volatility == option,
                            onClick = { volatility = option },
                            label = { Text(volatilityLabel(option)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = body.isNotBlank(),
                onClick = {
                    // personId is filled in by the caller, which is the one that knows whose
                    // plus button was tapped.
                    onConfirm(
                        DraftFact(
                            personId = UUID.randomUUID(),
                            body = body,
                            category = category,
                            volatility = volatility,
                        ),
                    )
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
