package io.github.yearsyan.yaad.ui.components.filemanager

import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kongzue.dialogx.dialogs.PopTip
import io.github.yearsyan.yaad.filemanager.DownloadsFileProvider
import io.github.yearsyan.yaad.filemanager.IFileNodeProvider
import io.github.yearsyan.yaad.filemanager.LocalFileProvider
import io.github.yearsyan.yaad.filemanager.SmbFileProvider
import io.github.yearsyan.yaad.filemanager.getSmbUrlFromAddresses
import io.github.yearsyan.yaad.services.SmbState
import io.github.yearsyan.yaad.utils.PermissionUtils
import io.github.yearsyan.yaad.utils.CredentialsManager
import io.github.yearsyan.yaad.R
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbAuthException
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun FileEntryItem(text: String, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                modifier = Modifier.size(38.dp),
                contentDescription = "folder"
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun SmbCredInput(
    serviceInfo: NsdServiceInfo,
    onProvideSuccess: (() -> IFileNodeProvider) -> Unit
) {
    val context = LocalContext.current
    val credentialsManager = remember { CredentialsManager.getInstance(context) }
    val smbKey = serviceInfo.serviceName ?: ""
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var savePassword by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 获取字符串资源
    val authErrorText = stringResource(R.string.auth_error)
    
    // 加载已保存的凭据并尝试自动连接
    DisposableEffect(smbKey) {
        if (smbKey.isNotEmpty()) {
            val savedCredential = credentialsManager.loadSmbCredential(smbKey)
            if (savedCredential != null) {
                username = savedCredential.username
                password = savedCredential.password
                savePassword = true
            }
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.smb_credentials),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = savePassword,
                onCheckedChange = { savePassword = it }
            )
            Text(
                text = stringResource(R.string.save_password),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val path = getSmbUrlFromAddresses(serviceInfo.hostAddresses, serviceInfo.port)
                    val auth = NtlmPasswordAuthenticator(null, username, password)
                    val context = SingletonContext.getInstance().withCredentials(auth)
                    val res = SmbFileProvider(SmbFile(path, context), context)
                    try {
                        res.listFiles()
                        // 连接成功后保存密码
                        if (savePassword && smbKey.isNotEmpty()) {
                            credentialsManager.saveSmbCredential(smbKey, username, password)
                        }
                        with(Dispatchers.Main) {
                            onProvideSuccess {
                                res
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("SmbCredInput", "path: $path ${e.message}")
                        e.printStackTrace()
                        // TODO
                    }

                }
            },
            enabled = username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.connect))
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEntries(
    modifier: Modifier = Modifier,
    onProvideSuccess: (() -> IFileNodeProvider) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialsManager = remember { CredentialsManager.getInstance(context) }
    val hasStoragePermission = remember {
        PermissionUtils.hasStoragePermission(context)
    }
    val smb = remember { SmbState(context, scope) }
    var inputService by remember { mutableStateOf<NsdServiceInfo?>(null) }
    var isAutoConnecting by remember { mutableStateOf(false) }
    
    DisposableEffect(smb) {
        smb.start()
        onDispose { smb.stop() }
    }

    // 自动连接逻辑
    fun tryAutoConnect(serviceInfo: NsdServiceInfo) {
        val smbKey = serviceInfo.serviceName ?: ""
        val savedCredential = credentialsManager.loadSmbCredential(smbKey)
        if (savedCredential != null) {
            isAutoConnecting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val path = getSmbUrlFromAddresses(serviceInfo.hostAddresses, serviceInfo.port)
                    val auth = NtlmPasswordAuthenticator(null, savedCredential.username, savedCredential.password)
                    val context = SingletonContext.getInstance().withCredentials(auth)
                    val res = SmbFileProvider(SmbFile(path, context), context)
                    res.listFiles()
                    with(Dispatchers.Main) {
                        isAutoConnecting = false
                        onProvideSuccess { res }
                    }
                } catch (e: SmbAuthException) {
                    with(Dispatchers.Main) {
                        isAutoConnecting = false
                        PopTip.show(context.getString(R.string.auth_error)).iconError()
                        inputService = serviceInfo // 弹窗让用户手动输入
                    }
                } catch (e: Exception) {
                    with(Dispatchers.Main) {
                        isAutoConnecting = false
                        PopTip.show(context.getString(R.string.unknown_error)).iconError()
                        inputService = serviceInfo // 弹窗让用户手动输入
                    }
                }
            }
        } else {
            inputService = serviceInfo
        }
    }

    inputService?.let {
        ModalBottomSheet(onDismissRequest = { inputService = null }) {
            SmbCredInput(serviceInfo = it, onProvideSuccess = {
                inputService = null
                onProvideSuccess(it)
            })
        }
    }

    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = stringResource(R.string.local_file), style = MaterialTheme.typography.titleMedium)
        Spacer(
            modifier =
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp), // 每个 item 最小宽度 120dp
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FileEntryItem(stringResource(R.string.app_file)) {
                    onProvideSuccess { LocalFileProvider(context.filesDir) }
                }
            }
            item {
                FileEntryItem(stringResource(R.string.downloads)) {
                    if (hasStoragePermission) {
                        onProvideSuccess {
                            DownloadsFileProvider.createRootProvider(context)
                        }
                    } else {
                        // TODO
                        // 这里可以显示权限请求对话框
                        // 暂时直接调用，让系统处理权限
                        onProvideSuccess {
                            DownloadsFileProvider.createRootProvider(context)
                        }
                    }
                }
            }
        }

        if (smb.services.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.network), style = MaterialTheme.typography.titleMedium)
            Spacer(
                modifier =
                    Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    smb.services.value.size
                ) { index ->
                    FileEntryItem(smb.services.value[index].serviceName) {
                        if (isAutoConnecting) {
                            return@FileEntryItem
                        }
                        tryAutoConnect(smb.services.value[index])
                    }
                }
            }
        }
    }
}
