package com.persondic.ui.common

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.persondic.data.model.Sensitivity
import java.time.LocalDate

/**
 * The fixed-information inputs, shared by adding a person and editing one.
 *
 * Stateless apart from the half-typed values in its own boxes, and it reports changes as they
 * happen rather than holding a draft. That is what lets the same fields write straight to the
 * database when editing, and accumulate in memory when the person does not exist yet.
 *
 * [monthDay] carries no year: the year is [birthYear] and only that. See Person.birthday.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixedInfoFields(
    monthDay: LocalDate?,
    birthYear: Int?,
    isLunar: Boolean,
    attributes: List<AttributeDraft>,
    suggestedLabels: List<String>,
    onBirthdayChange: (LocalDate?, Int?, Boolean) -> Unit,
    onAddAttribute: (AttributeDraft) -> Unit,
    onRemoveAttribute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var newLabel by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newSensitivity by remember { mutableStateOf(Sensitivity.NORMAL) }

    // Seeded once, then owned by the box. Re-keying it on birthYear would blank the field the
    // moment a partly typed year like "199" stopped being a plausible year.
    var yearText by remember { mutableStateOf(birthYear?.toString().orEmpty()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.fixed_info_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 몇년생 is entered on its own: the year and the date are usually learned at different
        // times, and one is no use as a gate on the other.
        OutlinedTextField(
            value = yearText,
            onValueChange = { typed ->
                yearText = typed.filter { it.isDigit() }.take(4)
                onBirthdayChange(monthDay, yearText.toIntOrNull(), isLunar)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(R.string.birth_year_label)) },
            placeholder = { Text(stringResource(R.string.birth_year_hint)) },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = monthDay?.let {
                    stringResource(R.string.birthday_no_year, it.monthValue, it.dayOfMonth)
                } ?: stringResource(R.string.birthday_month_day),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    val start = monthDay ?: LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 1, 1)
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onBirthdayChange(LocalDate.of(year, month + 1, dayOfMonth), birthYear, isLunar)
                        },
                        start.year,
                        start.monthValue - 1,
                        start.dayOfMonth,
                    ).show()
                },
            ) {
                Text(stringResource(R.string.birthday_set))
            }
            if (monthDay != null) {
                TextButton(onClick = { onBirthdayChange(null, birthYear, isLunar) }) {
                    Text(stringResource(R.string.birthday_clear))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isLunar,
                onCheckedChange = { lunar -> onBirthdayChange(monthDay, birthYear, lunar) },
            )
            Text(
                text = stringResource(R.string.birthday_lunar_toggle),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (isLunar) {
            Text(
                text = stringResource(R.string.birthday_lunar_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        attributes.forEach { attribute ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.fixed_info_remove))
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
        val unusedSuggestions = suggestedLabels.filterNot { label -> attributes.any { it.label == label } }
        if (unusedSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                unusedSuggestions.forEach { suggestion ->
                    AssistChip(onClick = { newLabel = suggestion }, label = { Text(suggestion) })
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
                onAddAttribute(AttributeDraft(newLabel.trim(), newValue.trim(), newSensitivity))
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
}
