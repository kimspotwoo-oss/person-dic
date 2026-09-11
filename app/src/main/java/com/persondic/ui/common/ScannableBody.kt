package com.persondic.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.persondic.R

/**
 * A body that stays scannable.
 *
 * Anything a person writes by hand — a fact, a meeting note, a commitment — comes out as several
 * sentences of memo rather than the one line a form field suggests, and one of those fills a small
 * screen by itself. So the first few lines show and the rest opens on a tap; nothing is hidden and
 * nothing is rewritten, it just does not dominate a screen meant to be scanned rather than read.
 *
 * Shared rather than local to one screen: the briefing needs it for facts, and the meeting flow
 * needs the same clamp for the conversation notes it recaps while facts are being added — the same
 * problem showing up twice is a sign it belongs in one place.
 */
@Composable
fun ScannableBody(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    // Whether it needed clamping at all, remembered from the last measurement that clamped it —
    // once open there is no overflow to detect, and the way back has to stay on screen.
    var overflowed by remember(text) { mutableStateOf(false) }

    Column(modifier = modifier.clickable(enabled = overflowed) { expanded = !expanded }) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else SCAN_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) overflowed = layout.hasVisualOverflow },
        )
        if (overflowed) {
            Text(
                text = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val SCAN_LINES = 3
