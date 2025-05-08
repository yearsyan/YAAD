package io.github.yearsyan.yaad.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.yearsyan.yaad.ui.theme.YAADTheme
import io.github.yearsyan.yaad.utils.YtDlpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(lifecycleScope: LifecycleCoroutineScope) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    YAADTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text)
                Button({
                    lifecycleScope.launch(Dispatchers.IO){
                        text = YtDlpUtil.callYtDlp(context, arrayOf("--skip-download","-J", "https://www.bilibili.com/bangumi/play/ss425"))
                    }
                }) {
                    Text("Req")
                }
            }
        }
    }
}