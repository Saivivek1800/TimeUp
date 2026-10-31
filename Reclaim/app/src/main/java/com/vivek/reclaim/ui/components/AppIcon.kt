package com.vivek.reclaim.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An installed app's launcher icon, loaded off the main thread. Shows a soft placeholder until ready. */
@Composable
fun AppIcon(
    packageName: String,
    loadIcon: (String) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.Default) { loadIcon(packageName) }
    }
    val shape = RoundedCornerShape(size * 0.3f)
    val current = icon
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = modifier.size(size).clip(shape))
    } else {
        Box(modifier = modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer))
    }
}
