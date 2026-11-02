package com.github.tvbox.osc.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.ui.screen.home.HomeScreen
import com.github.tvbox.osc.ui.theme.TVBoxTheme

/**
 * Compose 主入口 —— 替换原 HomeActivity 成为 LAUNCHER
 *
 * 阶段1 完成后：首页 → 详情页（占位） → 搜索 → 设置 的导航骨架
 * 后续阶段逐步填充 DetailScreen / SearchScreen / SettingScreen
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVBoxTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
private fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onVideoClick = { video: Movie.Video ->
                    // 阶段 2 实现详情页，这里先占位
                    navController.navigate("detail/${video.id ?: ""}")
                },
                onSearchClick = {
                    // 阶段 3 实现搜索
                },
                onSettingsClick = {
                    // 阶段 3 实现设置
                }
            )
        }
        composable("detail/{videoId}") {
            // 阶段 2 占位
            Scaffold { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "详情页 · 阶段 2 实现中",
                        color = Color.White
                    )
                }
            }
        }
    }
}
