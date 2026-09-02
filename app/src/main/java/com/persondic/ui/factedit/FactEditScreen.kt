package com.persondic.ui.factedit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.requirePersonDicApplication
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactEditScreen(
    personId: UUID,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: FactEditViewModel = viewModel(
        factory = ViewModelFactory { FactEditViewModel(application.repository, personId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fact_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSaved = onDone) },
                        enabled = uiState.body.isNotBlank(),
                    ) {
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
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.body,
                onValueChange = viewModel::onBodyChange,
                label = { Text(stringResource(R.string.fact_field_body)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 3,
            )

            SectionLabel(stringResource(R.string.fact_field_category))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FactCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(categoryLabel(category)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            SectionLabel(stringResource(R.string.fact_field_volatility))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Volatility.entries.forEach { volatility ->
                    FilterChip(
                        selected = uiState.volatility == volatility,
                        onClick = { viewModel.onVolatilityChange(volatility) },
                        label = { Text(volatilityLabel(volatility)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Text(
                text = expirationPreviewLabel(uiState.expiresOnPreview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            SectionLabel(stringResource(R.string.fact_field_sensitivity))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Sensitivity.entries.forEach { sensitivity ->
                    FilterChip(
                        selected = uiState.sensitivity == sensitivity,
                        onClick = { viewModel.onSensitivityChange(sensitivity) },
                        label = { Text(sensitivityLabel(sensitivity)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.fact_field_pinned),
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = uiState.pinned, onCheckedChange = viewModel::onPinnedChange)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
    )
}

private fun categoryLabel(category: FactCategory): String = when (category) {
    FactCategory.CONTEXT -> "관계/계기"
    FactCategory.PREFERENCE -> "취향"
    FactCategory.LIFE -> "가족/건강/근황"
    FactCategory.HOOK -> "다음 화제"
}

private fun volatilityLabel(volatility: Volatility): String = when (volatility) {
    Volatility.PERMANENT -> "영구"
    Volatility.SLOW -> "천천히 변함"
    Volatility.SEASONAL -> "계절성"
    Volatility.EVENT -> "일회성"
}

private fun sensitivityLabel(sensitivity: Sensitivity): String = when (sensitivity) {
    Sensitivity.NORMAL -> "보통"
    Sensitivity.PRIVATE -> "비공개"
    Sensitivity.RESTRICTED -> "제한"
}

private fun expirationPreviewLabel(expiresOn: LocalDate?): String =
    if (expiresOn == null) "만료 없음" else "${expiresOn.year}년 ${expiresOn.monthValue}월까지 유효"
