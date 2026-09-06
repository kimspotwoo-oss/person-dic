package com.persondic.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.persondic.R
import kotlinx.coroutines.launch

@Composable
fun PhotoPickerRow(
    name: String,
    photoUri: String?,
    onPhotoPicked: (String) -> Unit,
    onPhotoCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                copyImageToInternalStorage(context, uri)?.let(onPhotoPicked)
            }
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(name = name, photoUri = photoUri, size = 56.dp)
        Spacer(modifier = Modifier.width(12.dp))
        TextButton(
            onClick = {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        ) {
            Text(stringResource(if (photoUri.isNullOrBlank()) R.string.photo_add else R.string.photo_change))
        }
        if (!photoUri.isNullOrBlank()) {
            TextButton(onClick = onPhotoCleared) {
                Text(stringResource(R.string.photo_remove))
            }
        }
    }
}
