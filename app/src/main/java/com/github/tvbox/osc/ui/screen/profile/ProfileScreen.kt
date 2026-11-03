package com.github.tvbox.osc.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.theme.Colors

/** 我的 / 设置页 */
@Composable
fun ProfileScreen(
    onSourceConfigClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onCollectClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val settingsItems = listOf(
        Triple(Icons.Default.History, "源配置", "管理视频源", onSourceConfigClick),
        Triple(Icons.Default.History, "历史记录", "观看历史", onHistoryClick),
        Triple(Icons.Default.History, "收藏管理", "收藏的影片", onCollectClick),
        Triple(Icons.Default.Settings, "播放设置", "播放器、解码、清晰度", {}),
        Triple(Icons.Default.Settings, "界面设置", "主题、字体、布局", {}),
        Triple(Icons.Default.Settings, "关于", "版本信息、开源许可", onAboutClick)
    )

    Scaffold(
        containerColor = Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 头像区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Colors.Primary.copy(alpha = 0.8f), Colors.Accent.copy(alpha = 0.5f))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Colors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("TV", color = Colors.Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text("TVBox 用户", color = Color.White, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Text("TVBoxOS 手机版 · v1.0.0", color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // 设置列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(settingsItems) { (icon, title, sub, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Colors.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Colors.Accent,
                                modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(title, color = Colors.TextPrimary, fontSize = 15.sp)
                            Text(sub, color = Colors.TextTertiary, fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null,
                            tint = Colors.TextTertiary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
