package io.github.yearsyan.yaad.ui.components.filemanager

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FileIconImage(file: IFileNodeProvider, modifier: Modifier = Modifier) {
    when (file.iconType) {
        IconType.RES_ID -> {
            Image(
                modifier = modifier.size(32.dp),
                painter = painterResource(id = file.getResIdIcon()),
                contentDescription = file.name
            )
        }
        IconType.BITMAP -> {
            Image(
                modifier = modifier.size(32.dp),
                bitmap = file.getBitmapIcon().asImageBitmap(),
                contentDescription = file.name
            )
        }
        IconType.IMAGE_VECTOR -> {
            Image(
                modifier = modifier.size(32.dp),
                imageVector = file.getImageVectorIcon(),
                contentDescription = file.name
            )
        }
        IconType.DEFAULT -> {
            Image(
                modifier = modifier.size(32.dp),
                imageVector = Icons.Default.FileOpen,
                contentDescription = file.name
            )
        }
    }
}

@Composable
fun FileItemCard(
    file: IFileNodeProvider,
    overrideTitle: String = "",
    onClick: () -> Unit = {}
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor =
        if (isPressed) Color(0xFFE0E0E0) else Color.Transparent

    Box(
        modifier =
            Modifier.background(backgroundColor)
                .fillMaxWidth()
                .combinedClickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = onClick,
                    onLongClick = {}
                )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileIconImage(file, modifier = Modifier.padding(2.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        if (overrideTitle.isEmpty()) {
                            file.name
                        } else {
                            overrideTitle
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = file.subTitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun FileList(
    fileRootProvider: () -> IFileNodeProvider,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit = {}
) {
    var fileRoot by remember { mutableStateOf<IFileNodeProvider?>(null) }
    var fileList by remember {
        mutableStateOf<List<IFileNodeProvider>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var fileStack by remember {
        mutableStateOf<List<IFileNodeProvider>>(emptyList())
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 加载根目录
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { fileRoot = fileRootProvider() }
    }

    // 加载文件列表
    LaunchedEffect(fileRoot) {
        fileRoot?.let { root ->
            loading = true
            withContext(Dispatchers.IO) {
                val files = root.listFiles()
                withContext(Dispatchers.Main) {
                    fileStack = listOf(root)
                    fileList = files.toList()
                    loading = false
                }
            }
        }
    }

    val goBack = {
        val job =
            coroutineScope.launch(Dispatchers.Main) {
                delay(50)
                loading = true
            }
        coroutineScope.launch(Dispatchers.IO) {
            val previousDir = fileStack[fileStack.size - 2]
            val files = previousDir.listFiles()
            withContext(Dispatchers.Main) {
                fileStack = fileStack.dropLast(1)
                fileList = files.toList()
                if (job.isActive) {
                    job.cancel()
                }
                loading = false
            }
        }
        Unit
    }

    val onFileClick = { file: IFileNodeProvider ->
        coroutineScope.launch(Dispatchers.IO) {
            if (file.isDirectory) {
                loading = true
                val files = file.listFiles()
                withContext(Dispatchers.Main) {
                    fileStack = fileStack + file
                    fileList = files.toList()
                    loading = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "文件点击事件", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Unit
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0, 0, 0, 10)),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(modifier = Modifier.clickable { onCloseClick() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "close",
                    modifier = Modifier.size(38.dp).padding(8.dp)
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                if (fileStack.size > 1) {
                    item {
                        FileItemCard(
                            file = fileStack.last(),
                            onClick = goBack,
                            overrideTitle = ".."
                        )
                    }
                }

                items(
                    count = fileList.size,
                    key = { index -> fileList[index].path }
                ) { index ->
                    val file = fileList[index]
                    FileItemCard(file = file, onClick = { onFileClick(file) })
                }
            }

            if (loading) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun FileEntryItem(text: String, onClick: () -> Unit = {}) {
    Box(
         modifier = Modifier.clickable(onClick = onClick)
    ) {
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
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }

}

@Composable
fun FileEntries(modifier: Modifier = Modifier, onProvideSuccess: (() ->IFileNodeProvider) -> Unit = {}) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Local File",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.onSurface))
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
        }
    }
}

@Composable
fun FileManagerScreen(scope: CoroutineScope) {
    val providers = remember { mutableStateListOf<() ->IFileNodeProvider>() }
    Row(modifier = Modifier.fillMaxWidth()) {
        if (providers.isEmpty()) {
            FileEntries (modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                providers.add(it)
            }
        }
        providers.forEach {
            FileList(
                fileRootProvider = it,
                modifier = Modifier.weight(1.0f),
                onCloseClick = {
                    providers.remove(it)
                }
            )
        }
    }

}
