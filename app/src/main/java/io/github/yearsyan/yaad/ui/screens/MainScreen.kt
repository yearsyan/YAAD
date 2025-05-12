package io.github.yearsyan.yaad.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.yearsyan.yaad.ui.theme.YAADTheme

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems =
    listOf(
        BottomNavItem("Home", Icons.Default.Home, "home"),
        BottomNavItem("Tasks", Icons.Default.SaveAlt, "tasks"),
        BottomNavItem("Settings", Icons.Default.Settings, "settings")
    )

@Composable
fun HomeTabScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 80.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(text = "Url", fontSize = 36.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("视频链接") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = { /* 粘贴 */},
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
            }

            FloatingActionButton(
                onClick = { /* 下载 */},
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Outlined.Download, contentDescription = "Download")
            }
        }
    }
}

@Composable
fun MainScreen(lifecycleScope: LifecycleCoroutineScope) {

    var selectedIndex by remember { mutableIntStateOf(0) }

    YAADTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedIndex) {
                    0 -> HomeTabScreen()
                    1 -> Text("Tasks Screen") // Replace with actual screen
                    2 -> Text("Settings Screen") // Replace with actual screen
                }
            }
        }
    }
}
