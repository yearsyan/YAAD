package io.github.yearsyan.yaad.ui.screens.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.BuildInfo
import io.github.yearsyan.yaad.web.WebViewActivity

data class DependenceLicenseItem(
    val name: String,
    val description: String,
    val license: String,
    val url: String
)

@Composable
fun LicenseModalList() {
    val context = LocalContext.current
    var isExpand by remember { mutableStateOf(false) }
    val licenseList =
        listOf<DependenceLicenseItem>(
            DependenceLicenseItem(
                name = "mmkv",
                description =
                    "A high-performance, small size, disk efficient key-value storage framework written in pure kotlin, supports Android, iOS, macOS, Windows, Linux, etc.",
                license = "Apache License 2.0",
                url = "https://github.com/Tencent/MMKV"
            ),
            DependenceLicenseItem(
                name = "kotlinx-coroutines",
                description = "Library support for Kotlin coroutines",
                license = "Apache License 2.0",
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            DependenceLicenseItem(
                name = "ffmpeg",
                description =
                    "FFmpeg is a collection of libraries and tools with a focus on multimedia.",
                license = "LGPLv2.1",
                url = "https://github.com/FFmpeg/FFmpeg"
            ),
            DependenceLicenseItem(
                name = "kotlinx-coroutines",
                description = "Library support for Kotlin coroutines",
                license = "Apache License 2.0",
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            DependenceLicenseItem(
                name = "chaquopy",
                description =
                    "Chaquopy is a library that allows you to use Android libraries in your Java or Kotlin code.",
                license = "Apache License 2.0",
                url = "https://github.com/chaquo/chaquopy"
            ),
            DependenceLicenseItem(
                name = "kotlinx-coroutines",
                description = "Library support for Kotlin coroutines",
                license = "Apache License 2.0",
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            DependenceLicenseItem(
                name = "ktor",
                description =
                    "A framework for building asynchronous servers and clients in pure Kotlin.",
                license = "Apache License 2.0",
                url = "https://github.com/ktorio/ktor"
            )
        )

    LazyColumn(
        modifier = Modifier.padding(16.dp).fillMaxWidth().fillMaxHeight(0.8f)
    ) {
        item {
            Text(
                text = "App License",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                onClick = { isExpand = !isExpand }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).animateContentSize()
                ) {
                    Text(
                        text = BuildInfo.LICENSE_STR,
                        maxLines = if (isExpand) 100 else 5,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpand) "收起" else "展开",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Dependencies Licenses",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        items(licenseList) { item ->
            Card(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier =
                        Modifier.clickable {
                                WebViewActivity.start(context, item.url)
                            }
                            .fillMaxWidth()
                            .padding(16.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "License: ${item.license}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "URL: ${item.url}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
