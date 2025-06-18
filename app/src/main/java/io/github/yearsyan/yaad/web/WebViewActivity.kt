package io.github.yearsyan.yaad.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.yearsyan.yaad.ui.theme.YAADTheme
import io.github.yearsyan.yaad.utils.ClipboardUtils

@Composable
fun WebViewTopBar(
    title: String,
    onBackPressed: () -> Unit,
    onClose: () -> Unit,
    showMenu: Boolean = false,
    onMenuClick: () -> Unit = {}
) {
    Column {
        Spacer(
            Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface)
        )
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis
                )
                if (showMenu) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription =
                                stringResource(
                                    io.github.yearsyan.yaad.R.string
                                        .webview_menu_more
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewMenu(
    onDismiss: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onRefresh: () -> Unit,
    onCopyLink: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            io.github.yearsyan.yaad.R.string
                                .webview_menu_open_in_browser
                        )
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                },
                modifier =
                    Modifier.clickable(onClick = onOpenInBrowser)
                        .background(MaterialTheme.colorScheme.surface)
            )
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            io.github.yearsyan.yaad.R.string
                                .webview_menu_refresh
                        )
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                },
                modifier =
                    Modifier.clickable(onClick = onRefresh)
                        .background(MaterialTheme.colorScheme.surface)
            )
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            io.github.yearsyan.yaad.R.string
                                .webview_menu_copy_link
                        )
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                },
                modifier =
                    Modifier.clickable(onClick = onCopyLink)
                        .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

class WebViewActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var webTitle by mutableStateOf("")
    private var showLoading by mutableStateOf(true)
    private var showMenu by mutableStateOf(false)
    private var showBottomSheet by mutableStateOf(false)
    private var currentUrl by mutableStateOf("")

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val allowMenu = intent.getBooleanExtra(EXTRA_ALLOW_MENU, false)
        showMenu = allowMenu

        setContent {
            YAADTheme {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        WebViewTopBar(
                            title = webTitle,
                            onBackPressed = {
                                if (webView.canGoBack()) {
                                    webView.goBack()
                                } else {
                                    finish()
                                }
                            },
                            onClose = { finish() },
                            showMenu = showMenu,
                            onMenuClick = { showBottomSheet = true }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { context ->
                                    webView = createWebView()
                                    webView.alpha = 0f
                                    return@AndroidView webView
                                },
                                update = {}
                            )

                            if (showLoading) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color =
                                            MaterialTheme.colorScheme
                                                .onBackground,
                                        strokeWidth = 4.dp
                                    )
                                }
                            }
                        }
                    }

                    if (showBottomSheet) {
                        WebViewMenu(
                            onDismiss = { showBottomSheet = false },
                            onOpenInBrowser = {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(currentUrl)
                                    )
                                startActivity(intent)
                                showBottomSheet = false
                            },
                            onRefresh = {
                                webView.reload()
                                showBottomSheet = false
                            },
                            onCopyLink = {
                                ClipboardUtils.writeText(
                                    WebViewActivity@ this,
                                    currentUrl
                                )
                                showBottomSheet = false
                            }
                        )
                    }
                }
            }
        }

        // Handle predictive back gesture
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        return WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            // Load URL from intent
            intent.getStringExtra(EXTRA_URL)?.let { url ->
                currentUrl = url
                loadUrl(url)
            }
            settings.domStorageEnabled = true
            // 设置 WebChromeClient 以获取标题
            webChromeClient =
                object : WebChromeClient() {
                    override fun onReceivedTitle(
                        view: WebView?,
                        title: String?
                    ) {
                        super.onReceivedTitle(view, title)
                        title?.let { webTitle = title }
                    }
                }
            webViewClient =
                object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (showLoading) {
                            showLoading = false
                            webView.alpha = 1.0f
                        }
                        url?.let { currentUrl = it }
                    }
                }
        }
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_ALLOW_MENU = "allow_menu"

        /**
         * launch WebViewActivity
         *
         * @param context
         * @param url
         * @param allowMenu Whether to show the menu button
         */
        fun start(context: Context, url: String, allowMenu: Boolean = true) {
            val intent =
                Intent(context, WebViewActivity::class.java).apply {
                    putExtra(EXTRA_URL, url)
                    putExtra(EXTRA_ALLOW_MENU, allowMenu)
                }
            context.startActivity(intent)
        }
    }
}
