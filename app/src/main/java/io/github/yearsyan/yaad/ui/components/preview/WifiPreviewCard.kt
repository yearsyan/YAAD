package io.github.yearsyan.yaad.ui.components.preview

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.utils.ClipboardUtils
import io.github.yearsyan.yaad.utils.WifiInfo

@Composable
fun WifiPreviewCard(wifiInfo: WifiInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.wifi_info),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SSID: ${wifiInfo.ssid}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text =
                    stringResource(
                        R.string.encryption_type,
                        wifiInfo.encryption
                    ),
                style = MaterialTheme.typography.bodyMedium
            )
            wifiInfo.password?.let {
                Text(
                    text = stringResource(R.string.password, it),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { ClipboardUtils.writeText(context, it) }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription =
                                stringResource(R.string.copy_password)
                        )
                    }
                }
            }
        }
    }
}
