package com.persondic.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.domain.Reminder
import com.persondic.domain.ReminderKind
import java.util.UUID

/**
 * The dated reasons to get in touch, oldest deadline first.
 *
 * Deliberately only things with a date on them. Design principle 1 rules out scoring or ranking
 * people, and "who you have neglected" would be exactly that wearing a date's clothes; a missed
 * deadline is a fact instead.
 */
@Composable
fun ReminderRows(
    reminders: List<Reminder>,
    onPersonClick: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        reminders.forEach { reminder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPersonClick(reminder.personId) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reminderWhen(reminder),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (reminder.kind == ReminderKind.COMMITMENT_OVERDUE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.width(76.dp),
                )
                Text(
                    text = when (reminder.kind) {
                        ReminderKind.BIRTHDAY ->
                            stringResource(R.string.person_list_reminder_birthday, reminder.personName)
                        else -> "${reminder.personName} · ${reminder.body}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun reminderWhen(reminder: Reminder): String = when {
    reminder.daysFromToday < 0 -> stringResource(R.string.person_list_reminder_overdue, -reminder.daysFromToday)
    reminder.daysFromToday == 0L -> stringResource(R.string.person_list_reminder_today)
    else -> stringResource(R.string.person_list_reminder_in_days, reminder.daysFromToday)
}
