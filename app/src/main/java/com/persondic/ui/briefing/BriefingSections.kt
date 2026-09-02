package com.persondic.ui.briefing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.ui.common.FactCategoryGroup
import com.persondic.ui.common.categoryLabel
import com.persondic.ui.common.directionLabel
import com.persondic.ui.common.interactionKindLabel
import com.persondic.ui.common.relativeDateLabel

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun CautionSection(facts: List<Fact>) {
    Column {
        SectionHeader(stringResource(R.string.briefing_section_caution))
        Surface(color = MaterialTheme.colorScheme.errorContainer) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                facts.forEach { fact ->
                    Text(
                        text = fact.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun OpenCommitmentsSection(groups: List<CommitmentDirectionGroup>) {
    Column {
        SectionHeader(stringResource(R.string.briefing_section_open_commitments))
        groups.forEach { group ->
            Text(
                text = directionLabel(group.direction),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, top = 4.dp, bottom = 4.dp),
            )
            group.commitments.forEach { commitment ->
                Text(
                    text = commitment.body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun HookSection(facts: List<Fact>) {
    Column {
        SectionHeader(stringResource(R.string.briefing_section_hooks))
        facts.forEach { fact ->
            Text(
                text = fact.body,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
internal fun RecentInteractionsSection(interactions: List<Interaction>) {
    Column {
        SectionHeader(stringResource(R.string.briefing_section_recent_interactions))
        interactions.forEach { interaction ->
            val summary = interaction.summary?.takeIf { it.isNotBlank() } ?: interactionKindLabel(interaction.kind)
            Text(
                text = "$summary · ${relativeDateLabel(interaction.metAt)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
internal fun KnowledgeSection(groups: List<FactCategoryGroup>) {
    Column {
        SectionHeader(stringResource(R.string.briefing_section_knowledge))
        groups.forEach { group ->
            var expanded by remember(group.category) { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${categoryLabel(group.category)} (${group.facts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                group.facts.forEach { fact ->
                    Text(
                        text = fact.body,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SensitiveSection(facts: List<Fact>) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.briefing_section_sensitive_count, facts.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                facts.forEach { fact ->
                    Text(
                        text = "${categoryLabel(fact.category)} · ${fact.body}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun StaleFactRow(
    fact: Fact,
    onStillValid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = fact.body, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onStillValid) {
                Text(stringResource(R.string.action_still_valid))
            }
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.action_edit))
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}
