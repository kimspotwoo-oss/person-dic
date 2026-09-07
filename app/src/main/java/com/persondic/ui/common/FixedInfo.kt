package com.persondic.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.domain.BirthdayCalculator
import java.time.LocalDate

/**
 * How a birthday reads: "1990년 9월 21일 · 14일 뒤 36세".
 *
 * A lunar birthday gets no countdown — see [BirthdayCalculator]. Saying so beats printing a number
 * that would be weeks off.
 */
@Composable
fun birthdayLabel(person: Person, today: LocalDate = LocalDate.now()): String? {
    val birthday = person.birthday ?: return null

    val date = when {
        person.birthdayIsLunar && person.birthdayHasYear ->
            stringResource(R.string.birthday_lunar_with_year, birthday.year, birthday.monthValue, birthday.dayOfMonth)
        person.birthdayIsLunar ->
            stringResource(R.string.birthday_lunar, birthday.monthValue, birthday.dayOfMonth)
        person.birthdayHasYear ->
            stringResource(R.string.birthday_with_year, birthday.year, birthday.monthValue, birthday.dayOfMonth)
        else ->
            stringResource(R.string.birthday_no_year, birthday.monthValue, birthday.dayOfMonth)
    }

    val days = BirthdayCalculator.daysUntilBirthday(person, today) ?: return date
    val age = BirthdayCalculator.ageOnNextBirthday(person, today)

    val countdown = when {
        days == 0L && age != null -> stringResource(R.string.birthday_today_with_age, age)
        days == 0L -> stringResource(R.string.birthday_today)
        age != null -> stringResource(R.string.birthday_in_days_with_age, days, age)
        else -> stringResource(R.string.birthday_in_days, days)
    }
    return "$date · $countdown"
}

/** Read-only fixed information, shared by the person detail header and the briefing. */
@Composable
fun FixedInfoBlock(
    person: Person,
    attributes: List<PersonAttribute>,
    modifier: Modifier = Modifier,
) {
    val birthday = birthdayLabel(person)
    if (birthday == null && attributes.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        birthday?.let { FixedInfoRow(label = stringResource(R.string.birthday_label), value = it) }
        attributes.forEach { attribute ->
            FixedInfoRow(label = attribute.label, value = attribute.value)
        }
    }
}

@Composable
private fun FixedInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
