package com.persondic.ui.persondetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Fact
import com.persondic.domain.DerivedValues
import com.persondic.ui.common.categoryLabel

@Composable
fun FactsTab(groups: List<FactCategoryGroup>, onLongPress: (Fact) -> Unit) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.person_detail_facts_empty))
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "cat-${group.category}") {
                Text(
                    text = categoryLabel(group.category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(group.facts, key = { it.id.toString() }) { fact ->
                FactRow(fact = fact, onLongPress = { onLongPress(fact) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FactRow(fact: Fact, onLongPress: () -> Unit) {
    val stale = DerivedValues.isStale(fact)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (stale) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = fact.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (fact.pinned) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (stale) {
            Spacer(modifier = Modifier.width(8.dp))
            StaleBadge()
        }
    }
}

@Composable
private fun StaleBadge() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(R.string.fact_needs_review_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun FactActionDialog(
    fact: Fact,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePinned: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = fact.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column {
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.action_edit),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(onClick = onTogglePinned, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(
                            if (fact.pinned) R.string.action_unpin else R.string.action_pin,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
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
