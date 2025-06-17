package io.github.yearsyan.yaad.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.R

@Composable
fun AdaptiveSettingsScreen() {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var currentRoute by rememberSaveable {
        mutableStateOf<SettingsRoute?>(null)
    }

    // 处理返回按键 - 只在手机模式下拦截返回键
    BackHandler(enabled = currentRoute != null && !isTablet) {
        currentRoute = null
    }

    val handleNavigateBack = { currentRoute = null }

    if (isTablet) {
        // 平板布局：主从布局
        Row(modifier = Modifier.fillMaxSize()) {
            // 左侧主列表
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                SettingsMainScreen(
                    onNavigateToSubSetting = { route -> currentRoute = route }
                )
            }

            // 分割线
            VerticalDivider()

            // 右侧详情页 - 添加淡入淡出动画
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                    },
                    label = "tablet_detail_transition"
                ) { route ->
                    route?.let {
                        when (it) {
                            SettingsRoute.FFmpeg -> {
                                FFmpegSettingsScreen(
                                    onNavigateBack = handleNavigateBack
                                )
                            }
                            SettingsRoute.Cookie -> {
                                CookieSettingsScreen(
                                    onNavigateBack = handleNavigateBack
                                )
                            }
                            SettingsRoute.Download -> {
                                DownloadSettingsScreen(
                                    onNavigateBack = handleNavigateBack
                                )
                            }
                            SettingsRoute.Notification -> {
                                NotificationSettingsScreen(
                                    onNavigateBack = handleNavigateBack
                                )
                            }
                            SettingsRoute.About -> {
                                AboutSettingsScreen(
                                    onNavigateBack = handleNavigateBack
                                )
                            }
                            else -> {
                                DefaultDetailPane()
                            }
                        }
                    } ?: DefaultDetailPane()
                }
            }
        }
    } else {
        // 手机布局：单页面导航 - 添加滑动动画
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                if (targetState != null) {
                    // 进入二级页面：从右滑入
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec =
                                tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                } else {
                    // 返回主页面：从左滑入
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec =
                                tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                }
            },
            label = "phone_navigation_transition"
        ) { route ->
            route?.let {
                when (it) {
                    SettingsRoute.FFmpeg -> {
                        FFmpegSettingsScreen(
                            onNavigateBack = handleNavigateBack
                        )
                    }
                    SettingsRoute.Cookie -> {
                        CookieSettingsScreen(
                            onNavigateBack = handleNavigateBack
                        )
                    }
                    SettingsRoute.Download -> {
                        DownloadSettingsScreen(
                            onNavigateBack = handleNavigateBack
                        )
                    }
                    SettingsRoute.Notification -> {
                        NotificationSettingsScreen(
                            onNavigateBack = handleNavigateBack
                        )
                    }
                    SettingsRoute.About -> {
                        AboutSettingsScreen(onNavigateBack = handleNavigateBack)
                    }
                    else -> {
                        SettingsMainScreen(
                            onNavigateToSubSetting = { newRoute ->
                                currentRoute = newRoute
                            }
                        )
                    }
                }
            }
                ?: SettingsMainScreen(
                    onNavigateToSubSetting = { newRoute ->
                        currentRoute = newRoute
                    }
                )
        }
    }
}

@Composable
private fun DefaultDetailPane() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_select_item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
