package com.persondic.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.model.Sensitivity
import com.persondic.domain.BirthdayCalculator
import java.time.LocalDate

/**
 * How a birthday reads. The year and the date are independent, so any of these can come out:
 * "1994년생", "3월 15일 · 14일 뒤", "1994년생 3월 15일 · 14일 뒤 32세".
 *
 * The whole thing rarely fits one line beside a 72dp label, so the pieces hold together with
 * non-breaking spaces and the wrap is left to fall on the " · " between them. Without that it
 * broke wherever it ran out of room — "…243일" on one line and "뒤 21세" on the next.
 *
 * A lunar birthday gets no countdown — see [BirthdayCalculator].
 */
@Composable
fun birthdayLabel(person: Person, today: LocalDate = LocalDate.now()): String? {
    val year = person.birthYear?.let { stringResource(R.string.birth_year_value, it) }
    val birthday = person.birthday

    if (birthday == null) return year

    val date = if (person.birthdayIsLunar) {
        stringResource(R.string.birthday_lunar, birthday.monthValue, birthday.dayOfMonth)
    } else {
        stringResource(R.string.birthday_no_year, birthday.monthValue, birthday.dayOfMonth)
    }

    val days = BirthdayCalculator.daysUntilBirthday(person, today)
    val age = BirthdayCalculator.ageOnNextBirthday(person, today)
    val countdown = when {
        days == null -> null
        days == 0L && age != null -> stringResource(R.string.birthday_today_with_age, age)
        days == 0L -> stringResource(R.string.birthday_today)
        age != null -> stringResource(R.string.birthday_in_days_with_age, days, age)
        else -> stringResource(R.string.birthday_in_days, days)
    }

    val head = listOfNotNull(year, date).joinToString(" ")
    return listOfNotNull(head, countdown).joinToString(" · ")
}

/**
 * Read-only fixed information, shared by the person detail header and the briefing.
 *
 * With [gateSensitive] set, entries marked PRIVATE or RESTRICTED collapse into a count that only
 * opens on a tap — the same protection facts get. The briefing turns it on because that screen is
 * read in front of the person; the detail screen leaves it off because you opened that one
 * deliberately, which is how the facts tab behaves too.
 */
@Composable
fun FixedInfoBlock(
    person: Person,
    attributes: List<PersonAttribute>,
    modifier: Modifier = Modifier,
    gateSensitive: Boolean = false,
) {
    var revealSensitive by remember { mutableStateOf(false) }

    val birthday = birthdayLabel(person)
    val open = if (gateSensitive) attributes.filter { it.sensitivity == Sensitivity.NORMAL } else attributes
    val hidden = if (gateSensitive) attributes.filterNot { it.sensitivity == Sensitivity.NORMAL } else emptyList()

    if (birthday == null && attributes.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        birthday?.let { FixedInfoRow(label = stringResource(R.string.birthday_label), value = it) }
        open.forEach { attribute -> FixedInfoRow(label = attribute.label, value = attribute.value) }

        if (hidden.isNotEmpty()) {
            if (revealSensitive) {
                hidden.forEach { attribute -> FixedInfoRow(label = attribute.label, value = attribute.value) }
            }
            Text(
                text = if (revealSensitive) {
                    stringResource(R.string.action_collapse)
                } else {
                    stringResource(R.string.fixed_info_sensitive_count, hidden.size)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { revealSensitive = !revealSensitive }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FixedInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // A minimum rather than a fixed width, so the labels line up in the ordinary case and a
        // long one takes the room it needs instead of wrapping. Since a tag can be moved across
        // and become a label, "전기전자공학부" is a label this has to handle, and at 72dp it broke
        // over two lines and pushed its own value out of line with the rows above it.
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Offered in the editor so the first entry is a tap rather than a typing exercise, and so the same
 * thing gets the same label on every person — "고향" and "출신지" as two labels would split it.
 */
val SUGGESTED_ATTRIBUTE_LABELS = listOf("고향", "출신 학교", "학번", "이름 한자", "MBTI", "혈액형")
