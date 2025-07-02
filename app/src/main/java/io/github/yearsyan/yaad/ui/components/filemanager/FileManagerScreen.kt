package io.github.yearsyan.yaad.ui.components.filemanager

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kongzue.dialogx.dialogs.PopTip
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.filemanager.IFileNodeProvider
import io.github.yearsyan.yaad.filemanager.IconType
import io.github.yearsyan.yaad.services.SmbDiscoveryManager
import io.github.yearsyan.yaad.ui.components.formatBytes
import io.github.yearsyan.yaad.ui.player.PlayerActivity
import io.github.yearsyan.yaad.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
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
                    onLongClick = onLongClick
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
fun FileMenu(file: IFileNodeProvider) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            FileIconImage(file)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatBytes(file.fileSize),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        if (!file.isDirectory) {
            Box(Modifier.clickable(onClick = {})) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.file_op_open))
                }
            }
        }
        Box(Modifier.clickable(onClick = {})) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.file_op_delete), color = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var fileMenu by remember { mutableStateOf<IFileNodeProvider?>(null) }
    var loading by remember { mutableStateOf(true) }
    var fileStack by remember {
        mutableStateOf<List<IFileNodeProvider>>(emptyList())
    }
    val hScrollState = rememberScrollState()

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

    LaunchedEffect(fileStack) {
        delay(50)
        hScrollState.animateScrollTo(hScrollState.maxValue)
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
                try {
                    val files = file.listFiles()
                    withContext(Dispatchers.Main) {
                        fileStack = fileStack + file
                        fileList = files.toList()

                    }
                } catch (e: IOException) {
                    PopTip.show(e.message ?: "Unknown error")
                } finally {
                    withContext(Dispatchers.Main) {
                        loading = false
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (FileUtils.isVideoFile(file.name)) {
                        file.uri?.let {
                            PlayerActivity.startWithUri(context, it.toString())
                        }
                        return@withContext
                    }
                    Toast.makeText(context, "文件点击事件", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Unit
    }

    val jumpToStackIndex = jumpToStackIndex@{ index: Int ->
        if (index == fileStack.size - 1) {
            return@jumpToStackIndex
        }
        val job =
            coroutineScope.launch(Dispatchers.Main) {
                delay(50)
                loading = true
            }
        coroutineScope.launch(Dispatchers.IO) {
            val dir = fileStack[index]
            val files = dir.listFiles()
            withContext(Dispatchers.Main) {
                fileStack = fileStack.subList(0, index + 1)
                fileList = files.toList()
                if (job.isActive) {
                    job.cancel()
                }
                loading = false
            }
        }
        Unit
    }

    val onLongClick = { file: IFileNodeProvider -> fileMenu = file }

    fileMenu?.let {
        ModalBottomSheet(onDismissRequest = { fileMenu = null }) {
            FileMenu(it)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier =
                    Modifier.weight(1.0f)
                        .fillMaxHeight()
                        .horizontalScroll(hScrollState)
            ) {
                fileStack.forEachIndexed { index, fileEntry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxHeight()
                                .padding(horizontal = 4.dp)
                                .clickable { jumpToStackIndex(index) }
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            fileEntry.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        if (index < fileStack.size - 1) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
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
                    FileItemCard(
                        file = file,
                        onClick = { onFileClick(file) },
                        onLongClick = { onLongClick(file) }
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMenu() {
    var expanded by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    if (showAlert) {
        ModalBottomSheet(onDismissRequest = { showAlert = false }) {
            FileEntries()
        }
    }

    TopAppBar(
        title = { Text("File Manager") },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Add") },
                    onClick = {
                        expanded = false
                        showAlert = true
                    }
                )
            }
        }
    )
}

@Composable
fun FileManagerScreen(scope: CoroutineScope) {
    val providers = remember { mutableStateListOf<() -> IFileNodeProvider>() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        SmbDiscoveryManager.initialize(context)
        SmbDiscoveryManager.startDiscovery()
    }

    Column {
        TopBarWithMenu()
        Row(modifier = Modifier.fillMaxWidth()) {
            if (providers.isEmpty()) {
                FileEntries(modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                    providers.add(it)
                }
            }
            providers.forEach {
                FileList(
                    fileRootProvider = it,
                    modifier = Modifier.weight(1.0f),
                    onCloseClick = { providers.remove(it) }
                )
            }
        }
    }
}
