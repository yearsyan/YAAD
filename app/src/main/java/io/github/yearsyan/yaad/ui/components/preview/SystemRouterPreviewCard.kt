package io.github.yearsyan.yaad.ui.components.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.utils.ClipboardUtils

@Composable
fun SystemRouterPreviewCard(
    scheme: String,
    content: String,
    appIconRes: Int?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (appIconRes != null) {
                Image(
                    painter = painterResource(id = appIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).padding(end = 12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$scheme 链接",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            }
            IconButton(
                onClick = { ClipboardUtils.writeText(context, content) }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制内容")
            }
        }
    }
}
