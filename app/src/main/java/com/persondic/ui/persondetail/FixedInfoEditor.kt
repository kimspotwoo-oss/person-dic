package com.persondic.ui.persondetail

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.persondic.R
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.model.Sensitivity
import com.persondic.ui.common.AttributeDraft
import com.persondic.ui.common.FixedInfoFields
import java.time.LocalDate

/**
 * Edits the information that does not change.
 *
 * Every change is written as it is made rather than collected and saved at the end — each row is
 * an independent row in the database, so there is nothing to commit as a set.
 */
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fixed_info_edit)) },
        text = {
            FixedInfoFields(
                monthDay = person.birthday,
                birthYear = person.birthYear,
                isLunar = person.birthdayIsLunar,
                attributes = attributes.map { AttributeDraft(it.label, it.value, it.sensitivity) },
                suggestedLabels = suggestedLabels,
                onBirthdayChange = onSetBirthday,
                onAddAttribute = { draft -> onSetAttribute(draft.label, draft.value, draft.sensitivity) },
                onRemoveAttribute = onRemoveAttribute,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_save)) }
        },
    )
}
