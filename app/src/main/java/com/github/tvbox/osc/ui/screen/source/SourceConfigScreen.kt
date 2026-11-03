package com.github.tvbox.osc.ui.screen.source

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.theme.Colors

/** 源配置页 —— 只展示 UI 结构 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceConfigScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("源配置", color = Colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Colors.TextPrimary)
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Colors.Primary.copy(alpha = 0.9f))
                            .clickable { /* UI壳 */ }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp), tint = Colors.OnPrimary)
                        Text("添加源", color = Colors.OnPrimary, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Background)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "视频源列表",
                color = Colors.TextSecondary,
                fontSize = 13.sp
            )

            MockData.sources.forEach { (name, url, status) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Colors.SurfaceVariant)
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val statusColor = if (status == "启用") Colors.Success else Colors.TextTertiary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = status,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = Colors.TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = url,
                                color = Colors.TextTertiary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        FilterChip(
                            selected = status == "启用",
                            onClick = { /* UI 壳 */ },
                            label = { Text(if (status == "启用") "已启用" else "启用",
                                fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (status == "启用") Colors.Accent else Colors.Surface,
                                labelColor = if (status == "启用") Colors.Background else Colors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}
