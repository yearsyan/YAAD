package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 线程数设置对话框
 */
@Composable
fun ThreadCountDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue.toString()) }
    var isError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "设置下载线程数",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { 
                        inputValue = it
                        val intValue = it.toIntOrNull()
                        isError = intValue == null || intValue < 1 || intValue > 32
                    },
                    label = { Text("线程数") },
                    supportingText = { 
                        Text("建议范围：1-16，最大32")
                    },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = inputValue.toIntOrNull()
                            if (value != null && value in 1..32) {
                                onConfirm(value)
                                onDismiss()
                            }
                        },
                        enabled = !isError && inputValue.isNotEmpty()
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

/**
 * BT限速设置对话框
 */
@Composable
fun SpeedLimitDialog(
    currentDownloadSpeed: Long,
    currentUploadSpeed: Long,
    onDismiss: () -> Unit,
    onConfirm: (downloadSpeed: Long, uploadSpeed: Long) -> Unit
) {
    var downloadInput by remember { 
        mutableStateOf(if (currentDownloadSpeed == 0L) "" else currentDownloadSpeed.toString()) 
    }
    var uploadInput by remember { 
        mutableStateOf(if (currentUploadSpeed == 0L) "" else currentUploadSpeed.toString()) 
    }
    var downloadError by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "BT限速设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = downloadInput,
                    onValueChange = { 
                        downloadInput = it
                        if (it.isEmpty()) {
                            downloadError = false
                        } else {
                            val longValue = it.toLongOrNull()
                            downloadError = longValue == null || longValue < 0
                        }
                    },
                    label = { Text("下载限速 (KB/s)") },
                    supportingText = { 
                        Text("留空表示无限制")
                    },
                    isError = downloadError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Download, contentDescription = null)
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = uploadInput,
                    onValueChange = { 
                        uploadInput = it
                        if (it.isEmpty()) {
                            uploadError = false
                        } else {
                            val longValue = it.toLongOrNull()
                            uploadError = longValue == null || longValue < 0
                        }
                    },
                    label = { Text("上传限速 (KB/s)") },
                    supportingText = { 
                        Text("留空表示无限制")
                    },
                    isError = uploadError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Upload, contentDescription = null)
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val downloadSpeed = if (downloadInput.isEmpty()) 0L else downloadInput.toLongOrNull() ?: 0L
                            val uploadSpeed = if (uploadInput.isEmpty()) 0L else uploadInput.toLongOrNull() ?: 0L
                            
                            if (!downloadError && !uploadError) {
                                onConfirm(downloadSpeed, uploadSpeed)
                                onDismiss()
                            }
                        },
                        enabled = !downloadError && !uploadError
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
} 