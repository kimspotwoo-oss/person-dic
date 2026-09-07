package com.persondic.ui.persondetail

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.ui.common.birthdayLabel
import java.time.LocalDate

/**
 * Edits the information that does not change: a birthday and any number of labelled entries.
 *
 * Changes are applied as they are made rather than collected and saved at the end — each row is
 * an independent row in the database, so there is nothing to commit as a set.
 */
@Composable
fun FixedInfoDialog(
    person: Person,
    attributes: List<PersonAttribute>,
    suggestedLabels: List<String>,
    onDismiss: () -> Unit,
    onSetBirthday: (LocalDate?, Boolean, Boolean) -> Unit,
    onSetAttribute: (String, String) -> Unit,
    onRemoveAttribute: (String) -> Unit,
) {
    val context = LocalContext.current
    var newLabel by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val start = person.birthday ?: LocalDate.of(1990, 1, 1)
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onSetBirthday(
                                        LocalDate.of(year, month + 1, dayOfMonth),
                                        person.birthdayHasYear,
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
                        TextButton(onClick = { onSetBirthday(null, true, false) }) {
                            Text(stringResource(R.string.birthday_clear))
                        }
                    }
                }

                // The year is still stored when it is unknown, because SQLite has no month-day
                // type; the stand-in is a leap year so 2월 29일 survives the round trip.
                CheckRow(
                    checked = !person.birthdayHasYear,
                    label = stringResource(R.string.birthday_no_year_toggle),
                    onCheckedChange = { unknown ->
                        val current = person.birthday
                        onSetBirthday(
                            if (unknown && current != null) current.withYear(BIRTHDAY_YEAR_UNKNOWN) else current,
                            !unknown,
                            person.birthdayIsLunar,
                        )
                    },
                )
                CheckRow(
                    checked = person.birthdayIsLunar,
                    label = stringResource(R.string.birthday_lunar_toggle),
                    onCheckedChange = { lunar ->
                        onSetBirthday(person.birthday, person.birthdayHasYear, lunar)
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
                        Text(
                            text = "${attribute.label}: ${attribute.value}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
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
                    Text(
                        text = suggestedLabels.joinToString("  ") { it },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.fixed_info_attribute_value)) },
                )
                OutlinedButton(
                    onClick = {
                        onSetAttribute(newLabel, newValue)
                        newLabel = ""
                        newValue = ""
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
