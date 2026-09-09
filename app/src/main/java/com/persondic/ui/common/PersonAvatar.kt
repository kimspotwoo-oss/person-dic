package com.persondic.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PersonAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    if (photoUri.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            val initials = avatarInitials(name)
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                // Two characters need the smaller of the two sizes to stay inside the circle.
                fontSize = (size.value / if (initials.length >= 2) 3.0f else 2.4f).sp,
                maxLines = 1,
            )
        }
    } else {
        AsyncImage(
            model = photoModel(photoUri),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun photoModel(photoUri: String): Any =
    if (photoUri.startsWith("content://") || photoUri.startsWith("file://")) photoUri else File(photoUri)
