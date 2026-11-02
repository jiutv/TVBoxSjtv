package com.github.tvbox.osc.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.screen.category.CategoryScreen
import com.github.tvbox.osc.ui.screen.collect.CollectScreen
import com.github.tvbox.osc.ui.screen.detail.DetailScreen
import com.github.tvbox.osc.ui.screen.home.HomeScreen
import com.github.tvbox.osc.ui.screen.live.LiveScreen
import com.github.tvbox.osc.ui.screen.play.PlayScreen
import com.github.tvbox.osc.ui.screen.profile.ProfileScreen
import com.github.tvbox.osc.ui.screen.source.SourceConfigScreen
import com.github.tvbox.osc.ui.theme.Colors
import com.github.tvbox.osc.ui.theme.TVBoxTheme

/**
 * Compose 主入口 —— 手机版 UI 壳
 *
 * 导航结构：
 * BottomNav(5个tab) → Home / Category / Live / Collect / Profile
 * 二级页面：Detail → Play / SourceConfig
 *
 * 所有页面用模拟数据，不接真实接口。
 * 底层解析、播放内核、嗅探、源配置逻辑 —— 全部不动。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVBoxTheme {
                App()
            }
        }
    }
}

// ─── Bottom Nav 定义 ───

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab("home", "首页", Icons.Default.Home),
    BottomTab("category", "分类", Icons.Default.ViewList),
    BottomTab("live", "直播", Icons.Default.LiveTv),
    BottomTab("collect", "收藏", Icons.Default.Favorite),
    BottomTab("profile", "我的", Icons.Default.Person)
)

// ─── 根 App ───

@Composable
private fun App() {
    val navController = rememberNavController()

    // 监听当前路由 —— 决定是否显示 BottomNav
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 二级页面（detail / play / source_config）隐藏 BottomNav
    val showBottomNav = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        containerColor = Colors.Background,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = Colors.Surface,
                    tonalElevation = 6.dp
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon, contentDescription = tab.label,
                                    tint = if (selected) Colors.Accent else Colors.TextTertiary
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    color = if (selected) Colors.Accent else Colors.TextTertiary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Colors.Accent,
                                selectedTextColor = Colors.Accent,
                                unselectedIconColor = Colors.TextTertiary,
                                unselectedTextColor = Colors.TextTertiary,
                                indicatorColor = Colors.Accent.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            // ─── 一级（Bottom Nav） ───
            composable("home") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    HomeScreen(
                        onVideoClick = { videoId ->
                            navController.navigate("detail/$videoId")
                        }
                    )
                }
            }
            composable("category") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    CategoryScreen(
                        onVideoClick = { videoId ->
                            navController.navigate("detail/$videoId")
                        }
                    )
                }
            }
            composable("live") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    LiveScreen(
                        onChannelClick = { channelId ->
                            navController.navigate("play/$channelId")
                        }
                    )
                }
            }
            composable("collect") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    CollectScreen(
                        onVideoClick = { videoId ->
                            navController.navigate("detail/$videoId")
                        }
                    )
                }
            }
            composable("profile") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProfileScreen(
                        onSourceConfigClick = { navController.navigate("source_config") },
                        onCollectClick = {
                            navController.navigate("collect") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAboutClick = { /* toast */ }
                    )
                }
            }

            // ─── 二级（隐藏 BottomNav） ───
            composable(
                route = "detail/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStack ->
                val videoId = backStack.arguments?.getString("videoId") ?: ""
                val video = MockData.homeVideos.find { it.id == videoId }
                    ?: MockData.homeVideos.first()
                DetailScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() },
                    onPlayClick = { navController.navigate("play/${video.name}") }
                )
            }
            composable("play/{title}") { backStack ->
                val title = backStack.arguments?.getString("title") ?: "播放"
                PlayScreen(
                    title = title,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("source_config") {
                SourceConfigScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
