package com.persondic.ui.persondetail

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.model.Sensitivity
import com.persondic.ui.common.SectionLabel
import com.persondic.ui.common.birthdayLabel
import com.persondic.ui.common.sensitivityLabel
import java.time.LocalDate

/**
 * Edits the information that does not change: a birthday and any number of labelled entries.
 *
 * Changes are applied as they are made rather than collected and saved at the end — each row is
 * an independent row in the database, so there is nothing to commit as a set.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixedInfoDialog(
    person: Person,
    attributes: List<PersonAttribute>,
    suggestedLabels: List<String>,
    onDismiss: () -> Unit,
    onSetBirthday: (LocalDate?, Int?, Boolean) -> Unit,
    onSetAttribute: (String, String, Sensitivity) -> Unit,
    onRemoveAttribute: (String) -> Unit,
) {
    val context = LocalContext.current
    var newLabel by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newSensitivity by remember { mutableStateOf(Sensitivity.NORMAL) }
    var yearText by remember(person.birthYear) { mutableStateOf(person.birthYear?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fixed_info_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.fixed_info_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = birthdayLabel(person) ?: stringResource(R.string.birthday_label),
                    style = MaterialTheme.typography.bodyLarge,
                )

                // 몇년생 is entered on its own: the year and the date are usually learned at
                // different times, and one is no use as a gate on the other.
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { typed ->
                        yearText = typed.filter { it.isDigit() }.take(4)
                        onSetBirthday(person.birthday, yearText.toIntOrNull(), person.birthdayIsLunar)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.birth_year_label)) },
                    placeholder = { Text(stringResource(R.string.birth_year_hint)) },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val start = person.birthday ?: LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 1, 1)
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onSetBirthday(
                                        LocalDate.of(year, month + 1, dayOfMonth),
                                        person.birthYear,
                                        person.birthdayIsLunar,
                                    )
                                },
                                start.year,
                                start.monthValue - 1,
                                start.dayOfMonth,
                            ).show()
                        },
                    ) {
                        Text(stringResource(R.string.birthday_set))
                    }
                    if (person.birthday != null) {
                        TextButton(onClick = { onSetBirthday(null, person.birthYear, false) }) {
                            Text(stringResource(R.string.birthday_clear))
                        }
                    }
                }

                CheckRow(
                    checked = person.birthdayIsLunar,
                    label = stringResource(R.string.birthday_lunar_toggle),
                    onCheckedChange = { lunar ->
                        onSetBirthday(person.birthday, person.birthYear, lunar)
                    },
                )
                if (person.birthdayIsLunar) {
                    Text(
                        text = stringResource(R.string.birthday_lunar_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                attributes.forEach { attribute ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${attribute.label}: ${attribute.value}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (attribute.sensitivity != Sensitivity.NORMAL) {
                                Text(
                                    text = sensitivityLabel(attribute.sensitivity),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        IconButton(onClick = { onRemoveAttribute(attribute.label) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.fixed_info_remove),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fixed_info_attribute_label)) },
                )
                if (suggestedLabels.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestedLabels.forEach { suggestion ->
                            AssistChip(
                                onClick = { newLabel = suggestion },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fixed_info_attribute_value)) },
                )
                SectionLabel(stringResource(R.string.fixed_info_sensitivity))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Sensitivity.entries.forEach { level ->
                        FilterChip(
                            selected = newSensitivity == level,
                            onClick = { newSensitivity = level },
                            label = { Text(sensitivityLabel(level)) },
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        onSetAttribute(newLabel, newValue, newSensitivity)
                        newLabel = ""
                        newValue = ""
                        newSensitivity = Sensitivity.NORMAL
                    },
                    enabled = newLabel.isNotBlank() && newValue.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.fixed_info_add_attribute))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_save)) }
        },
    )
}

@Composable
private fun CheckRow(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}