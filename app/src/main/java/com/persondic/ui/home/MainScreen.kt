package com.persondic.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.persondic.R
import com.persondic.ui.personlist.PersonListScreen
import java.util.UUID

/**
 * The two things the app is for, side by side at the bottom.
 *
 * Home answers "we just met" and "what do I owe anybody"; the list of people answers "who do I
 * know". Both are real questions, but only the first two get asked in a hurry, which is why the
 * app now opens on them.
 *
 * The tabs sit in a Column rather than a Scaffold's bottomBar because each screen brings its own
 * Scaffold — a bar and a top bar and a floating button — and nesting those doubles every inset.
 */
@Composable
fun MainScreen(
    onRecordMeeting: () -> Unit,
    onPersonClick: (UUID) -> Unit,
    onGroupMapClick: () -> Unit,
    onRelationMapClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAddPersonClick: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeScreen(
                    onRecordMeeting = onRecordMeeting,
                    onPersonClick = onPersonClick,
                    onGroupMapClick = onGroupMapClick,
                    onRelationMapClick = onRelationMapClick,
                    onQuickAddClick = onQuickAddClick,
                    onBackupClick = onBackupClick,
                )

                else -> PersonListScreen(
                    onPersonClick = onPersonClick,
                    onGroupMapClick = onGroupMapClick,
                    onRelationMapClick = onRelationMapClick,
                    onQuickAddClick = onQuickAddClick,
                    onBackupClick = onBackupClick,
                    onAddPersonClick = onAddPersonClick,
                )
            }
        }
        NavigationBar {
            NavigationBarItem(
                selected = tab == 0,
                onClick = { tab = 0 },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.home_tab)) },
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { tab = 1 },
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text(stringResource(R.string.home_tab_people)) },
            )
        }
    }
}
