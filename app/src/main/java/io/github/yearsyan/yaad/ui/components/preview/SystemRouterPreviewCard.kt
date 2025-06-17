package io.github.yearsyan.yaad.ui.components.preview

import android.content.Intent
import android.graphics.Canvas
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.utils.ClipboardUtils
import io.github.yearsyan.yaad.utils.ComponentInfo

@Composable
fun SystemRouterPreviewCard(
    scheme: String,
    componentInfo: ComponentInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap =
        remember(componentInfo.icon) {
            val drawable = componentInfo.icon
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            val bmp = createBitmap(width, height)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bmp
        }
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier.size(40.dp).padding(end = 12.dp)
            )
            Text(
                modifier = Modifier.weight(1f),
                text = "App Link for ${componentInfo.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { ClipboardUtils.writeText(context, scheme) }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_content)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(), // 关键动画
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.secondaryContainer
                )
        ) {
            Row(modifier = modifier.fillMaxWidth().padding(16.dp)) {
                Text(scheme)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, scheme.toUri())
                    )
                }
            ) {
                Text(stringResource(R.string.navigate_to))
            }
        }
    }
}
