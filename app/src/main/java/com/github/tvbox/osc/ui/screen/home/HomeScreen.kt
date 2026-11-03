package com.github.tvbox.osc.ui.screen.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.ui.components.VideoCard
import com.github.tvbox.osc.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSourceConfigClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val categories = viewModel.categories.collectAsState(initial = emptyList()).value
    val videos = viewModel.videos.collectAsState(initial = emptyList()).value
    val sources = viewModel.sources.collectAsState(initial = emptyList()).value
    val currentSourceName = viewModel.currentSourceName.collectAsState(initial = "").value
    val loading = viewModel.loading.collectAsState(initial = false).value
    val sourceConfigured = viewModel.sourceConfigured.collectAsState(initial = true).value

    LaunchedEffect(Unit) { viewModel.init() }

    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TVBoxOS",
                            style = MaterialTheme.typography.titleLarge,
                            color = Colors.Accent
                        )
                        Text(
                            text = "  手机版",
                            style = MaterialTheme.typography.titleMedium,
                            color = Colors.TextSecondary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Colors.SurfaceVariant)
                            .clickable { sourceMenuExpanded = true }
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Colors.TextSecondary
                            )
                            Text(
                                text = "  ${currentSourceName.ifBlank { "加载中..." }}",
                                color = Colors.TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(
                            expanded = sourceMenuExpanded,
                            onDismissRequest = { sourceMenuExpanded = false }
                        ) {
                            sources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source.name) },
                                    onClick = {
                                        viewModel.switchSource(source)
                                        sourceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colors.Background,
                    titleContentColor = Colors.TextPrimary
                )
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 分类 Tab —— 来自真实数据源
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(categories) { index, sortData ->
                    val selected = index == selectedCategoryIndex
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategoryIndex = index
                            viewModel.selectCategory(sortData)
                        },
                        label = {
                            Text(
                                text = sortData.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (selected) Colors.Primary else Colors.SurfaceVariant,
                            labelColor = if (selected) Colors.OnPrimary else Colors.TextSecondary
                        )
                    )
                }
            }

            // 搜索框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(Colors.SurfaceVariant)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Colors.TextTertiary
                    )
                    Text(
                        "搜索电影、电视剧、综艺...",
                        color = Colors.TextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            // 视频网格 —— 真实数据
            if (!sourceConfigured) {
                // 未配置源 —— 引导用户去设置
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "⚠️ 尚未配置视频源",
                            color = Colors.TextSecondary,
                            fontSize = 15.sp
                        )
                        Text(
                            "请先在「源配置」中添加视频源地址",
                            color = Colors.TextTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        androidx.compose.material3.TextButton(
                            onClick = onSourceConfigClick,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("去配置视频源", color = Colors.Accent)
                        }
                    }
                }
            } else if (videos.isNotEmpty() || loading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos) { video: Movie.Video ->
                        VideoCard(
                            video = video,
                            onClick = { onVideoClick(video.id) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无数据，点击源切换试试",
                        color = Colors.TextTertiary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
