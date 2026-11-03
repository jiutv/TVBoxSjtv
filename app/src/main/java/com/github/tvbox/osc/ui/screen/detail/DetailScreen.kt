package com.github.tvbox.osc.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.mock.MockEpisode
import com.github.tvbox.osc.ui.theme.Colors

/**
 * 详情页 —— 顶部播放器黑色 UI 壳 + 剧集列表 + 简介 + 操作按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    videoId: String = "",
    onBack: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    val episodes = MockData.episodes(40)
    var selectedEpisode by remember { mutableStateOf(0) }
    var descExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("影片详情", color = Colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Colors.TextPrimary)
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
        ) {
            // ─── 顶部播放器区域（黑色 UI 壳） ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Colors.Accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "播放器区域（后续接入 MyVideoView）",
                        color = Colors.TextTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ─── 影片信息 ───
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "狂飙",
                    style = MaterialTheme.typography.titleLarge,
                    color = Colors.TextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("⭐ 8.5", color = Colors.Accent, fontSize = 13.sp)
                    Text("2024", color = Colors.TextSecondary, fontSize = 13.sp)
                    Text("大陆", color = Colors.TextSecondary, fontSize = 13.sp)
                    Text("更新至 40 集", color = Colors.Primary, fontSize = 13.sp)
                }

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Colors.Primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  立即播放", color = Colors.OnPrimary, fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(21.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite, contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isFavorite) Colors.Error else Colors.TextSecondary
                        )
                        Text(
                            "  ${if (isFavorite) "已收藏" else "收藏"}",
                            color = if (isFavorite) Colors.Error else Colors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(21.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  分享", fontSize = 14.sp, color = Colors.TextSecondary)
                    }
                }

                Text("导演：张艺谋", color = Colors.TextSecondary, fontSize = 13.sp)
                Text("主演：吴京, 沈腾, 黄渤, 张译, 周迅", color = Colors.TextSecondary, fontSize = 13.sp)

                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "一个关于英雄主义与人性光辉的故事，讲述了平凡人在面对逆境时做出不平凡选择的经历。本片汇集了华语影坛最强阵容，场面震撼，情感真挚，是 2024 年度最受期待的巨制。",
                        color = Colors.TextPrimary,
                        fontSize = 13.sp,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = if (descExpanded) "收起" else "展开全部",
                        color = Colors.Accent,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { descExpanded = !descExpanded }
                    )
                }
            }

            // ─── 选集 ───
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("选集 (共 40 集)", color = Colors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("按时间排序", color = Colors.TextSecondary, fontSize = 12.sp)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(episodes.take(18)) { index: Int, ep: MockEpisode ->
                        EpisodeChip(
                            episode = ep,
                            isSelected = index == selectedEpisode,
                            onClick = { selectedEpisode = index }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeChip(
    episode: MockEpisode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(episode.name, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isSelected) Colors.Accent else Colors.SurfaceVariant,
            labelColor = if (isSelected) Colors.Background else Colors.TextPrimary
        ),
        modifier = Modifier.height(36.dp)
    )
}
