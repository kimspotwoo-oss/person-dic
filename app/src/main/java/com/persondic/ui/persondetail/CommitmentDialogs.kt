package com.persondic.ui.persondetail

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Commitment
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.ui.common.directionLabel
import java.time.LocalDate

@Composable
fun AddCommitmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (direction: Direction, body: String, dueOn: LocalDate?) -> Unit,
) {
    var body by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf(Direction.I_OWE) }
    var dueOn by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.commitment_add_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.commitment_field_body)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Direction.entries.forEach { candidate ->
                        FilterChip(
                            selected = direction == candidate,
                            onClick = { direction = candidate },
                            label = { Text(directionLabel(candidate)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val base = dueOn ?: LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth -> dueOn = LocalDate.of(year, month + 1, dayOfMonth) },
                            base.year,
                            base.monthValue - 1,
                            base.dayOfMonth,
                        ).show()
                    },
                ) {
                    val label = dueOn?.let { "%d.%02d.%02d".format(it.year, it.monthValue, it.dayOfMonth) }
                        ?: stringResource(R.string.commitment_field_due_on)
                    Text(label)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(direction, body, dueOn) },
                enabled = body.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun CommitmentActionDialog(
    commitment: Commitment,
    onDismiss: () -> Unit,
    onSetStatus: (CommitmentStatus) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = commitment.body, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                if (commitment.status != CommitmentStatus.OPEN) {
                    TextButton(onClick = { onSetStatus(CommitmentStatus.OPEN) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.commitment_action_reopen), modifier = Modifier.fillMaxWidth())
                    }
                }
                if (commitment.status != CommitmentStatus.DONE) {
                    TextButton(onClick = { onSetStatus(CommitmentStatus.DONE) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.commitment_action_done), modifier = Modifier.fillMaxWidth())
                    }
                }
                if (commitment.status != CommitmentStatus.DROPPED) {
                    TextButton(onClick = { onSetStatus(CommitmentStatus.DROPPED) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.commitment_action_dropped), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
