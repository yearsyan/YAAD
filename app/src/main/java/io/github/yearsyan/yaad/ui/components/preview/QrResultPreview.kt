package io.github.yearsyan.yaad.ui.components.preview

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yearsyan.yaad.services.UrlHandler
import io.github.yearsyan.yaad.utils.ClipboardUtils
import io.github.yearsyan.yaad.utils.getComponentInfo
import io.github.yearsyan.yaad.utils.toWifiOrNull

enum class PreviewType {
    Http,
    SystemRouter,
    Wifi,
    Text
}

fun getPreviewType(context: Context, data: String): PreviewType {
    if (UrlHandler.isHttpLink(data)) {
        return PreviewType.Http
    } else if (UrlHandler.canRoute(context, data)) {
        return PreviewType.SystemRouter
    } else if (UrlHandler.isWifiShare(data)) {
        return PreviewType.Wifi
    }
    return PreviewType.Text
}

@Composable
fun TextPreview(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "来自二维码文本",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { ClipboardUtils.writeText(context, text) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ContentCopy,
                        modifier = Modifier.size(20.dp),
                        contentDescription = "复制"
                    )
                    Text("复制")
                }
            }
        }
    }
}

@Composable
fun QrResultPreview(data: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val previewType = getPreviewType(context, data)
    Box(
        modifier =
            modifier.padding(
                PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = 0.dp,
                    bottom = 12.dp
                )
            )
    ) {
        when (previewType) {
            PreviewType.Http -> HttpPreviewCard(data)
            PreviewType.SystemRouter -> {
                val comp = UrlHandler.getComponent(context, data)
                comp?.getComponentInfo(context)?.let {
                    SystemRouterPreviewCard(data, it)
                }
            }
            PreviewType.Wifi -> {
                data.toWifiOrNull()?.let { WifiPreviewCard(it) }
            }
            PreviewType.Text -> TextPreview(data)
        }
    }
}
