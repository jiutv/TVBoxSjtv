package com.github.tvbox.osc.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.ui.theme.Colors
import com.orhanobut.hawk.Hawk
import com.github.tvbox.osc.util.HawkConfig

/**
 * 界面设置 —— 和 TVBoxOS 主题/外观相关的开关
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UISettingScreen(
    onBack: () -> Unit = {}
) {
    val items = listOf(
        Triple(HawkConfig.SEARCH_VIEW, "搜索结果视图", listOf("列表", "缩略图")),
        Triple(HawkConfig.LIVE_PLAY_SCALE, "直播缩放", listOf("默认", "拉伸", "裁剪")),
        Triple(HawkConfig.HOME_REC_STYLE, "首页推荐样式", null),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("界面设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Surface)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            items.forEach { (key, label, options) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = Colors.TextPrimary, modifier = Modifier.weight(1f))
                    if (options != null) {
                        var idx by remember(key) { mutableStateOf(Hawk.get(key, 0)) }
                        TextButton(onClick = {
                            idx = (idx + 1) % options.size
                            Hawk.put(key, idx)
                        }) {
                            Text(options[idx])
                        }
                    } else {
                        var b by remember(key) { mutableStateOf(Hawk.get(key, false)) }
                        Switch(checked = b, onCheckedChange = {
                            b = it
                            Hawk.put(key, it)
                        })
                    }
                }
                Divider(
                    color = Colors.SurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Surface)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TVBoxOS", style = MaterialTheme.typography.headlineMedium, color = Colors.Accent)
            Text("手机版 · Compose UI", color = Colors.TextTertiary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("基于 TVBox 开源项目", color = Colors.TextSecondary)
            Text("github.com/jiutv/TVMobile", color = Colors.TextTertiary)
        }
    }
}
