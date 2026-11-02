package com.github.tvbox.osc.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.ui.theme.Colors
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.launch

/**
 * 首页 —— 对标 AVBox 手机版
 *
 * 布局：
 * ┌─────────────────────────────────┐
 * │  TVBoxOS   [源名▾] [🔍] [⚙️]    │  ← TopAppBar
 * ├─────────────────────────────────┤
 * │  推荐 | 电影 | 电视剧 | 综艺 | 动画 │  ← ScrollableTabRow
 * ├─────────────────────────────────┤
 * │  ┌────┐ ┌────┐ ┌────┐           │
 * │  │封面│ │封面│ │封面│           │  ← LazyVerticalGrid (2-3列)
 * │  │片名│ │片名│ │片名│           │
 * │  └────┘ └────┘ └────┘           │
 * │  ┌────┐ ┌────┐ ┌────┐           │
 * │  │... │ │... │ │... │           │
 * │  └────┘ └────┘ └────┘           │
 * └─────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    sourceViewModel: SourceViewModel = viewModel(),
    onVideoClick: (Movie.Video) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // 初始化 —— 只调一次
    LaunchedEffect(Unit) {
        homeViewModel.init(activity, sourceViewModel)
    }

    val homeSourceName by homeViewModel.homeSourceName.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val error by homeViewModel.error.collectAsState()
    val categories by homeViewModel.categories.collectAsState()
    val selectedIndex by homeViewModel.selectedCategoryIndex.collectAsState()
    val videosByCategory by homeViewModel.videosByCategory.collectAsState()
    val loadingMore by homeViewModel.loadingMore.collectAsState()

    var sourceMenuExpanded by remember { mutableStateOf(false) }
    val sources = remember { homeViewModel.getSwitchSources() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TVBoxOS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Colors.TextPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        // 源切换按钮（下拉）
                        Box {
                            FilterChip(
                                selected = false,
                                onClick = { sourceMenuExpanded = true },
                                label = {
                                    Text(
                                        text = homeSourceName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Colors.SurfaceVariant,
                                    selectedContainerColor = Colors.Primary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.height(32.dp)
                            )
                            DropdownMenu(
                                expanded = sourceMenuExpanded,
                                onDismissRequest = { sourceMenuExpanded = false },
                                containerColor = Colors.Surface
                            ) {
                                sources.forEach { source ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                source.name,
                                                color = if (source.key == homeViewModel.getCurrentSource()?.key)
                                                    Colors.Primary else Colors.TextPrimary
                                            )
                                        },
                                        onClick = {
                                            homeViewModel.switchSource(source)
                                            sourceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "搜索", tint = Colors.TextPrimary)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = Colors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colors.Background,
                    titleContentColor = Colors.TextPrimary,
                    actionIconContentColor = Colors.TextPrimary
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
            // 分类 Tab —— ScrollableTabRow
            if (categories.isNotEmpty()) {
                CategoryTabRow(
                    categories = categories,
                    selectedIndex = selectedIndex,
                    onSelect = { homeViewModel.selectCategory(it) }
                )
            }

            // 内容区
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && videosByCategory.isEmpty() -> {
                        LoadingState()
                    }
                    error != null -> {
                        ErrorState(
                            error = error!!,
                            onRetry = { homeViewModel.refreshCurrentCategory() }
                        )
                    }
                    categories.isEmpty() -> {
                        EmptyState("暂无分类")
                    }
                    else -> {
                        val key = categories[selectedIndex].id.ifEmpty { categories[selectedIndex].name }
                        val videos = videosByCategory[key] ?: emptyList()
                        VideoGrid(
                            videos = videos,
                            loadingMore = loadingMore,
                            onVideoClick = onVideoClick,
                            onLoadMore = { homeViewModel.loadMore() }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 分类 Tab 行
// ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryTabRow(
    categories: List<MovieSort.SortData>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(categories) { index, sortData ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { onSelect(index) },
                label = {
                    Text(
                        text = sortData.name,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (selected) Colors.Primary.copy(alpha = 0.9f) else Colors.SurfaceVariant,
                    selectedContainerColor = Colors.Primary,
                    labelColor = if (selected) Colors.OnPrimary else Colors.TextSecondary,
                    selectedLabelColor = Colors.OnPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Colors.Divider.copy(alpha = 0.3f),
                    selectedBorderColor = Colors.Primary
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 视频卡片网格
// ─────────────────────────────────────────────────────────────

@Composable
private fun VideoGrid(
    videos: List<Movie.Video>,
    loadingMore: Boolean,
    onVideoClick: (Movie.Video) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos) { video ->
            VideoCard(video = video, onClick = { onVideoClick(video) })
        }
        if (loadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        // 触发加载更多（滚动到底部）
        item {
            LaunchedEffect(Unit) {
                onLoadMore()
            }
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

/**
 * 视频卡片 —— 封面 + 片名 + 备注（集数/评分等）
 * 仿 AVBox 风格：16:9 封面、片名 2 行、备注小字
 */
@Composable
fun VideoCard(
    video: Movie.Video,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 封面 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f) // 2:3 poster ratio
                .clip(RoundedCornerShape(10.dp))
                .background(Colors.SurfaceVariant)
        ) {
            if (!video.pic.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.pic)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 占位
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Colors.SurfaceVariant, Colors.Surface)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.name?.take(1) ?: "?",
                        color = Colors.TextTertiary,
                        fontSize = 32.sp
                    )
                }
            }
            // 角标（备注 —— 集数/评分）
            if (!video.note.isNullOrEmpty()) {
                Surface(
                    color = Colors.Primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(topEnd = 10.dp, bottomStart = 8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = video.note,
                        color = Colors.OnPrimary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 片名
        Text(
            text = video.name ?: "",
            color = Colors.TextPrimary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 状态组件
// ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Colors.Primary)
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = msg, color = Colors.TextTertiary)
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = error, color = Colors.Error, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("重试", color = Colors.Primary)
            }
        }
    }
}
