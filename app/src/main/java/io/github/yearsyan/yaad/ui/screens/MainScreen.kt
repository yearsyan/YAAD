package io.github.yearsyan.yaad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.downloader.DownloadViewModel
import io.github.yearsyan.yaad.ui.components.filemanager.FileManagerScreen
import io.github.yearsyan.yaad.ui.theme.YAADTheme

data class BottomNavItem(
    val label: Int,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems =
    listOf(
        BottomNavItem(R.string.nav_home, Icons.Default.Home, "home"),
        BottomNavItem(R.string.nav_file, Icons.Default.Folder, "file"),
        BottomNavItem(R.string.nav_tasks, Icons.Default.SaveAlt, "tasks"),
        BottomNavItem(R.string.nav_settings, Icons.Default.Settings, "settings")
    )

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    downloadViewModel: DownloadViewModel,
    lifecycleScope: LifecycleCoroutineScope
) {
    Box(modifier = modifier.fillMaxSize()) {
        // InputScreen - index 0
        AnimatedVisibility(
            visible = selectedIndex == 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            InputScreen(lifecycleScope)
        }

        // FileManagerScree - index 1
        AnimatedVisibility(
            visible = selectedIndex == 1,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FileManagerScreen(lifecycleScope)
        }

        // TasksScreen - index 2
        AnimatedVisibility(
            visible = selectedIndex == 2,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TasksScreen(lifecycleScope, downloadViewModel)
        }

        // SettingsScreen - index 2
        AnimatedVisibility(
            visible = selectedIndex == 3,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SettingsScreen()
        }
    }
}

@Composable
fun MainScreen(lifecycleScope: LifecycleCoroutineScope) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val downloadViewModel = remember { DownloadViewModel() }
    val windowInfo = LocalWindowInfo.current
    val isTablet = windowInfo.containerSize.width >= 600.dp.value
    val isLandscape =
        windowInfo.containerSize.width > windowInfo.containerSize.height
    val useLrNav = isTablet && isLandscape

    YAADTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar =
                if (useLrNav) {
                    {}
                } else {
                    {
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
                }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useLrNav) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        bottomNavItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                modifier =
                                    Modifier.padding(
                                        top = 12.dp,
                                        bottom = 12.dp
                                    ),
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
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                MainContent(
                    selectedIndex = selectedIndex,
                    downloadViewModel = downloadViewModel,
                    lifecycleScope = lifecycleScope
                )
            }
        }
    }
}
