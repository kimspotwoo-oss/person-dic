package com.persondic.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.persondic.R
import com.persondic.domain.ParsedPerson
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.categoryLabel
import com.persondic.ui.common.directionLabel
import com.persondic.ui.common.requirePersonDicApplication
import com.persondic.ui.common.volatilityLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: QuickAddViewModel = viewModel(
        factory = ViewModelFactory { QuickAddViewModel(application.repository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFormatHelp by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_add_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { showFormatHelp = !showFormatHelp }) {
                        Text(stringResource(R.string.quick_add_format_help))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showFormatHelp) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.quick_add_format_example),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            OutlinedTextField(
                value = uiState.input,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                label = { Text(stringResource(R.string.quick_add_input_label)) },
                placeholder = { Text(stringResource(R.string.quick_add_input_hint)) },
            )

            uiState.parsed.warnings.forEach { warning ->
                Text(
                    text = "⚠ $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.parsed.people.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.quick_add_preview_title, uiState.parsed.people.size),
                    style = MaterialTheme.typography.titleSmall,
                )
                uiState.parsed.people.forEach { person -> PersonPreview(person) }
            }

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.quick_add_save, uiState.parsed.people.size))
            }
        }
    }
}

/**
 * Shows what will actually be written, with the markers already resolved into their labels: the
 * point of the preview is to catch a mistyped marker before it becomes a wrong fact.
 */
@Composable
private fun PersonPreview(person: ParsedPerson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = person.displayName, style = MaterialTheme.typography.titleMedium)
                if (person.tags.isNotEmpty()) {
                    Text(
                        text = person.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            person.alias?.let {
                Text(
                    text = stringResource(R.string.quick_add_preview_alias, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            person.metStory?.let {
                Text(
                    text = stringResource(R.string.quick_add_preview_story, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            person.birthday?.let { birthday ->
                val year = birthday.year?.let { stringResource(R.string.birth_year_value, it) }
                val date = birthday.monthDay?.let { monthDay ->
                    if (birthday.isLunar) {
                        stringResource(R.string.birthday_lunar, monthDay.monthValue, monthDay.dayOfMonth)
                    } else {
                        stringResource(R.string.birthday_no_year, monthDay.monthValue, monthDay.dayOfMonth)
                    }
                }
                val text = listOfNotNull(year, date).joinToString(" ")
                if (text.isNotEmpty()) {
                    Text(
                        text = "${stringResource(R.string.birthday_label)}: $text",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            person.attributes.forEach { attribute ->
                Text(
                    text = "${attribute.label}: ${attribute.value}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val cautionLabel = stringResource(R.string.briefing_section_caution)
            person.facts.forEach { fact ->
                val markers = listOfNotNull(
                    categoryLabel(fact.category),
                    volatilityLabel(fact.volatility),
                    cautionLabel.takeIf { fact.pinned },
                )
                Text(
                    text = "· ${fact.body}  (${markers.joinToString(" / ")})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            person.commitments.forEach { commitment ->
                Text(
                    text = "· ${commitment.body}  (${directionLabel(commitment.direction)})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
