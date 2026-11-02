package com.github.tvbox.osc.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.theme.TVBoxTheme

/**
 * Compose 主入口 —— 替换原 HomeActivity 成为 LAUNCHER
 *
 * 阶段0 只做骨架：空的 Compose 占位 + Material 主题 + 应用主题
 * 后续阶段逐步把 HomeScreen / DetailScreen 接入 NavHost
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVBoxTheme {
                Phase0Scaffold()
            }
        }
    }
}

/** 阶段0 占位 —— 确认 Compose 骨架编译通过 */
@Composable
private fun Phase0Scaffold() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TVBoxOS · 手机版 Compose 骨架\n阶段 0 ✅ 构建脚手架就绪",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
