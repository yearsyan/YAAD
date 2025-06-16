package io.github.yearsyan.yaad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadViewModel
import io.github.yearsyan.yaad.ui.theme.YAADTheme

data class BottomNavItem(
    val label: Int,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems =
    listOf(
        BottomNavItem(R.string.nav_home, Icons.Default.Home, "home"),
        BottomNavItem(R.string.nav_tasks, Icons.Default.SaveAlt, "tasks"),
        BottomNavItem(R.string.nav_settings, Icons.Default.Settings, "settings")
    )

@Composable
fun MainScreen(lifecycleScope: LifecycleCoroutineScope) {

    var selectedIndex by remember { mutableIntStateOf(0) }
    val downloadViewModel = remember { DownloadViewModel() }

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
                                Icon(
                                    item.icon,
                                    contentDescription =
                                        stringResource(item.label)
                                )
                            },
                            label = { Text(stringResource(item.label)) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // 使用 AnimatedVisibility - 优雅的动画切换，保持状态

                // InputScreen - index 0
                AnimatedVisibility(
                    visible = selectedIndex == 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    InputScreen(lifecycleScope)
                }

                // TasksScreen - index 1
                AnimatedVisibility(
                    visible = selectedIndex == 1,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TasksScreen(lifecycleScope, downloadViewModel)
                }

                // SettingsScreen - index 2
                AnimatedVisibility(
                    visible = selectedIndex == 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}
