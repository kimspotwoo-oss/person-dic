package com.persondic.ui.personadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.persondic.ui.common.FixedInfoFields
import com.persondic.ui.common.GroupTagEditor
import com.persondic.ui.common.PhotoPickerRow
import com.persondic.ui.common.SUGGESTED_ATTRIBUTE_LABELS
import com.persondic.ui.common.SectionLabel
import com.persondic.ui.common.ViewModelFactory
import com.persondic.ui.common.deleteStoredPhoto
import com.persondic.ui.common.requirePersonDicApplication
import java.util.UUID

/**
 * A full screen rather than a dialog: it carries the same fields the person detail screen edits,
 * and that no longer fits a dialog — the fixed-information part would have had to open a second
 * dialog on top of the first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAddScreen(
    onDone: () -> Unit,
    onSaved: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.requirePersonDicApplication()
    val viewModel: PersonAddViewModel = viewModel(
        factory = ViewModelFactory { PersonAddViewModel(application.repository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val allAttributeLabels by viewModel.allAttributeLabels.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.person_list_add_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Nothing was saved, so the copied photo would be orphaned in
                            // internal storage otherwise.
                            deleteStoredPhoto(uiState.photoUri)
                            onDone()
                        },
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = uiState.canSave,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhotoPickerRow(
                name = uiState.displayName,
                photoUri = uiState.photoUri,
                onPhotoPicked = { picked ->
                    deleteStoredPhoto(uiState.photoUri)
                    viewModel.onPhotoChange(picked)
                },
                onPhotoCleared = {
                    deleteStoredPhoto(uiState.photoUri)
                    viewModel.onPhotoChange(null)
                },
            )

            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = { Text(stringResource(R.string.person_field_display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.alias,
                onValueChange = viewModel::onAliasChange,
                label = { Text(stringResource(R.string.person_field_alias)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.metStory,
                onValueChange = viewModel::onMetStoryChange,
                label = { Text(stringResource(R.string.person_field_met_story)) },
                modifier = Modifier.fillMaxWidth(),
            )

            GroupTagEditor(
                selectedTags = uiState.tags,
                allTags = allTags,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionLabel(stringResource(R.string.fixed_info_label))

            FixedInfoFields(
                monthDay = uiState.monthDay,
                birthYear = uiState.birthYear,
                isLunar = uiState.isLunar,
                attributes = uiState.attributes,
                suggestedLabels = (allAttributeLabels + SUGGESTED_ATTRIBUTE_LABELS).distinct(),
                onBirthdayChange = viewModel::onBirthdayChange,
                onAddAttribute = viewModel::addAttribute,
                onRemoveAttribute = viewModel::removeAttribute,
            )
        }
    }
}
