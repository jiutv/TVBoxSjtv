package com.github.tvbox.osc.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.tvbox.osc.api.ApiConfig
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
import com.orhanobut.hawk.Hawk
import com.github.tvbox.osc.util.HawkConfig
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // === 初始化 ApiConfig（加载视频源配置） ===
        initApiConfig()

        setContent {
            TVBoxTheme {
                App()
            }
        }
    }

    private fun initApiConfig() {
        val api = ApiConfig.get()
        val apiUrl = Hawk.get(HawkConfig.API_URL, "")
        if (apiUrl.isEmpty()) {
            Log.w(TAG, "未设置 HawkConfig.API_URL，跳过 loadConfig")
            return
        }
        Log.i(TAG, "开始加载视频源配置: $apiUrl")
        lifecycleScope.launch {
            api.loadConfig(
                true,
                object : ApiConfig.LoadConfigCallback {
                    override fun notice(msg: String?) {
                        Log.i(TAG, "loadConfig notice: $msg")
                    }
                    override fun error(msg: String?) {
                        Log.e(TAG, "loadConfig error: $msg")
                    }
                    override fun success() {
                        Log.i(TAG, "✅ loadConfig 成功，源数量: ${api.sourceBeanList.size}")
                    }
                },
                this@MainActivity
            )
        }
    }

    companion object {
        private const val TAG = "TVBoxOS"
    }
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab("home", "首页", Icons.Default.Home),
    BottomTab("category", "分类", Icons.Default.ViewList),
    BottomTab("live", "直播", Icons.Default.LiveTv),
    BottomTab("collect", "收藏", Icons.Default.Favorite),
    BottomTab("profile", "我的", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute == "home" || currentRoute == "category" ||
        currentRoute == "live" || currentRoute == "collect" || currentRoute == "profile"

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
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            composable("home") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    HomeScreen(
                        onVideoClick = { navController.navigate("detail/$it") },
                        onSourceConfigClick = { navController.navigate("source_config") }
                    )
                }
            }
            composable("category") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    CategoryScreen(onVideoClick = { navController.navigate("detail/$it") })
                }
            }
            composable("live") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    LiveScreen(onChannelClick = { navController.navigate("play/$it") })
                }
            }
            composable("collect") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    CollectScreen(onVideoClick = { navController.navigate("detail/$it") })
                }
            }
            composable("profile") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProfileScreen(
                        onSourceConfigClick = { navController.navigate("source_config") },
                        onHistoryClick = { navController.navigate("history") },
                        onCollectClick = {
                            navController.navigate("collect") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onPlaySettingClick = { navController.navigate("play_setting") },
                        onUISettingClick = { navController.navigate("ui_setting") },
                        onAboutClick = { navController.navigate("about") }
                    )
                }
            }
            composable(
                route = "detail/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStack ->
                val videoId = backStack.arguments?.getString("videoId") ?: ""
                DetailScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() },
                    onPlayClick = { navController.navigate("play/$videoId") }
                )
            }
            composable(
                route = "play/{title}",
                arguments = listOf(navArgument("title") { type = NavType.StringType })
            ) { backStack ->
                val title = backStack.arguments?.getString("title") ?: "播放"
                PlayScreen(title = title, onBack = { navController.popBackStack() })
            }
            composable("source_config") {
                SourceConfigScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                PlaceholderScreen("历史记录", onBack = { navController.popBackStack() })
            }
            composable("play_setting") {
                PlaceholderScreen("播放设置", onBack = { navController.popBackStack() })
            }
            composable("ui_setting") {
                PlaceholderScreen("界面设置", onBack = { navController.popBackStack() })
            }
            composable("about") {
                PlaceholderScreen("关于", onBack = { navController.popBackStack() })
            }
        }
    }
}

/** 占位页面 —— 临时用于打通路由，后续替换真实内容 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.material3.TopAppBar(
            title = { Text(title, color = Colors.TextPrimary) },
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Home, contentDescription = "返回",
                        tint = Colors.TextPrimary
                    )
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = Colors.Background
            )
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                "$title — 功能开发中",
                color = Colors.TextTertiary,
                fontSize = 14.sp
            )
        }
    }
}
