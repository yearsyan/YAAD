package io.github.yearsyan.yaad.ui.components.preview

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yearsyan.yaad.utils.ClipboardUtils
import kotlinx.coroutines.delay
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.MessageDialog
import io.github.yearsyan.yaad.ui.components.ShimmerPlaceholder
import io.github.yearsyan.yaad.utils.WebInfo
import io.github.yearsyan.yaad.utils.getWebInfo

enum class PreviewState {
    Loading,
    Error,
    Done
}

@Composable
fun HttpPreviewCard(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(PreviewState.Loading) }
    var webInfo by remember { mutableStateOf<WebInfo?>(null) }

    LaunchedEffect(Unit) {
        delay(500)
        try {
            webInfo = getWebInfo(url)
            state = PreviewState.Done
        } catch (e: Exception) {
            state = PreviewState.Error
        }
    }

    val onClickLink: () -> Unit = {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Text(
                text = "来自二维码的链接",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp),
                onClick = { ClipboardUtils.writeText(context, url) }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    modifier = Modifier.size(20.dp),
                    contentDescription = "复制"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 动画包裹
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(), // 关键动画
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            onClick = onClickLink
        ) {
            when (state) {
                PreviewState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row (
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerPlaceholder(
                                color = Color.White,
                                modifier = Modifier.height(56.dp).width(56.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column (
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ShimmerPlaceholder(
                                    color = Color.White,
                                    modifier = Modifier.height(24.dp).fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ShimmerPlaceholder(
                                    color = Color.White,
                                    modifier = Modifier.height(24.dp).fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            Icon(
                                contentDescription = "play",
                                imageVector = Icons.Default.PlayArrow,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                PreviewState.Done -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 8.dp,
                                horizontal = 16.dp
                            )
                    ) {
                        webInfo?.let {
                            Row(
                                modifier = Modifier.height(64.dp)
                            ) {
                                AsyncImage(
                                    model = it.icon,
                                    contentDescription = "图标",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = it.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = it.description,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Spacer(
                                modifier = Modifier
                                    .height(1.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }

                PreviewState.Error -> {
                    Text(
                        text = "加载失败",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

