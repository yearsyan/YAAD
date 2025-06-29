package io.github.yearsyan.yaad.ui.components.filemanager

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.filemanager.DownloadsFileProvider
import io.github.yearsyan.yaad.filemanager.IFileNodeProvider
import io.github.yearsyan.yaad.filemanager.IFileProvider
import io.github.yearsyan.yaad.filemanager.LocalFileProvider
import io.github.yearsyan.yaad.utils.PermissionUtils

@Composable
fun FileEntryItem(text: String, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxSize(),
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
fun FileEntries(
    modifier: Modifier = Modifier,
    onProvideSuccess: (() -> IFileNodeProvider) -> Unit = {}
) {
    val context = LocalContext.current
    val hasStoragePermission = remember { PermissionUtils.hasStoragePermission(context) }
    val smbList = remember {
        mutableStateOf<List<IFileProvider<NsdServiceInfo>>>(emptyList())
    }
    val smbListener = remember {
        { newValue: List<NsdServiceInfo> ->
            smbList.value =
                newValue.map {
                    object : IFileProvider<NsdServiceInfo> {
                        override fun requestCreate(
                            data: NsdServiceInfo,
                            onResult: (IFileNodeProvider) -> Unit
                        ) {}
                    }
                }
        }
    }
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Local File", style = MaterialTheme.typography.titleMedium)
        Spacer(
            modifier =
                Modifier.height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 64.dp), // 每个 item 最小宽度 120dp
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                FileEntryItem("App file") {
                    onProvideSuccess { LocalFileProvider(context.filesDir) }
                }
            }
            item {
                FileEntryItem("Downloads") {
                    if (hasStoragePermission) {
                        onProvideSuccess { DownloadsFileProvider.createRootProvider(context) }
                    } else {
                        // TODO
                        // 这里可以显示权限请求对话框
                        // 暂时直接调用，让系统处理权限
                        onProvideSuccess { DownloadsFileProvider.createRootProvider(context) }
                    }
                }
            }
        }
    }
}
