package io.github.yearsyan.yaad.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.yearsyan.yaad.utils.SettingsManager
import java.io.File

/** 简单的Cookie文件选择对话框 */
@Composable
fun SimpleCookieSelector(
    onDismiss: () -> Unit,
    settingsManager: SettingsManager,
    currentCookieFile: String
) {
    val context = LocalContext.current
    val cookieFiles by settingsManager.cookieFiles.collectAsState()

    // 文件选择器
    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result.data?.data?.let { uri ->
                try {
                    // 复制文件到应用内部存储
                    val inputStream =
                        context.contentResolver.openInputStream(uri)
                    val fileName = "cookies_${System.currentTimeMillis()}.txt"
                    val cookieDir = settingsManager.getCookieDirectory()
                    val targetFile = File(cookieDir, fileName)

                    inputStream?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 添加到设置管理器并设为当前使用
                    settingsManager.addCookieFile(targetFile.absolutePath)
                    settingsManager.setCurrentCookieFile(
                        targetFile.absolutePath
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择Cookie文件",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 无Cookie选项
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentCookieFile.isEmpty(),
                        onClick = {
                            settingsManager.setCurrentCookieFile("")
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "不使用Cookie",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Cookie文件列表
                if (cookieFiles.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(cookieFiles) { cookieFile ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected =
                                        cookieFile.path == currentCookieFile,
                                    onClick = {
                                        settingsManager.setCurrentCookieFile(
                                            cookieFile.path
                                        )
                                        onDismiss()
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cookieFile.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        settingsManager.removeCookieFile(
                                            cookieFile.path
                                        )
                                        File(cookieFile.path).delete()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // 添加文件按钮
                Button(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "text/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                        filePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加Cookie文件")
                }
            }
        }
    }
}
