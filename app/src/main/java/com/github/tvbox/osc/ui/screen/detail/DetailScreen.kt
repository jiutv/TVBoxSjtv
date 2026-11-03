package com.github.tvbox.osc.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.ui.theme.Colors

/**
 * 详情页 —— 接真实 SourceViewModel.getDetail()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    videoId: String = "",
    sourceKey: String = "",
    onBack: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    viewModel: DetailViewModel = viewModel()
) {
    // 自动从 ApiConfig 获取 homeSourceBean.key（如果没传）
    val actualSourceKey = sourceKey.ifBlank {
        try { ApiConfig.get().homeSourceBean.key } catch (_: Throwable) { "" }
    }

    LaunchedEffect(videoId, actualSourceKey) {
        if (videoId.isNotBlank() && actualSourceKey.isNotBlank()) {
            viewModel.loadDetail(actualSourceKey, videoId)
        }
    }

    val video by viewModel.video.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedEpisode by remember { mutableStateOf(0) }
    var descExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(video?.name ?: "详情") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("❌ 加载失败", color = Colors.Error)
                    Text(error ?: "", color = Colors.TextTertiary, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp))
                    Button(
                        onClick = { viewModel.loadDetail(actualSourceKey, videoId) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("重试") }
                }

                video != null -> DetailContent(
                    video = video!!,
                    selectedEpisode = selectedEpisode,
                    onEpisodeSelected = { selectedEpisode = it },
                    descExpanded = descExpanded,
                    onDescToggle = { descExpanded = !descExpanded },
                    isFavorite = isFavorite,
                    onFavoriteToggle = { isFavorite = !isFavorite },
                    onPlayClick = onPlayClick
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    video: Movie.Video,
    selectedEpisode: Int,
    onEpisodeSelected: (Int) -> Unit,
    descExpanded: Boolean,
    onDescToggle: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部视频区（黑色占位）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PlayArrow, contentDescription = null,
                    tint = Colors.Accent.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                Text("点击播放", color = Colors.TextTertiary, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // 影片标题
            Text(
                text = video.name.ifBlank { "(未知影片)" },
                style = MaterialTheme.typography.titleLarge,
                color = Colors.TextPrimary
            )

            // 元信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                video.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Colors.Primary, fontSize = 13.sp)
                }
                if (video.year > 0) Text("${video.year}", color = Colors.TextSecondary, fontSize = 13.sp)
                video.area?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Colors.TextSecondary, fontSize = 13.sp)
                }
                video.type?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Colors.Accent, fontSize = 13.sp)
                }
            }

            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.Primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" 立即播放", fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onFavoriteToggle,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isFavorite) Colors.Error else Colors.TextSecondary)
                    Text(" ${if (isFavorite) "已收藏" else "收藏"}", fontSize = 14.sp)
                }
            }

            // 简介
            video.actor?.takeIf { it.isNotBlank() }?.let {
                Text("主演：$it", color = Colors.TextSecondary, fontSize = 13.sp)
            }
            video.director?.takeIf { it.isNotBlank() }?.let {
                Text("导演：$it", color = Colors.TextSecondary, fontSize = 13.sp)
            }
            video.des?.takeIf { it.isNotBlank() }?.let { des ->
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = des,
                        color = Colors.TextPrimary,
                        fontSize = 13.sp,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (descExpanded) "收起" else "展开全部",
                        color = Colors.Accent,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp).clickable { onDescToggle() }
                    )
                }
            }
        }

        // 选集 —— 从 video.urlBean.infoList[0].urls 解析
        EpisodeGrid(
            video = video,
            selectedEpisode = selectedEpisode,
            onEpisodeSelected = onEpisodeSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeGrid(
    video: Movie.Video,
    selectedEpisode: Int,
    onEpisodeSelected: (Int) -> Unit
) {
    // 从 urlBean 解析剧集列表
    val episodes = remember(video.id) {
        val list = mutableListOf<Pair<String, String>>()
        video.urlBean?.infoList?.firstOrNull()?.urls?.let { urls ->
            // urls 格式: "第01集$http://xxx.m3u8#第02集$http://xxx.m3u8"
            urls.split("#").forEach { segment ->
                val parts = segment.split("$", limit = 2)
                if (parts.size >= 2) {
                    list.add(parts[0] to parts[1])
                }
            }
        }
        list
    }

    if (episodes.isEmpty()) {
        // 没有多线路剧集，可能只有单集
        return
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("选集 (${episodes.size} 集)", color = Colors.TextPrimary,
                style = MaterialTheme.typography.titleMedium)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(episodes) { index, (name, _) ->
                FilterChip(
                    selected = index == selectedEpisode,
                    onClick = { onEpisodeSelected(index) },
                    label = {
                        Text(
                            name.ifBlank { "第${index + 1}集" },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (index == selectedEpisode) Colors.Accent else Colors.SurfaceVariant,
                        labelColor = if (index == selectedEpisode) Colors.Background else Colors.TextPrimary
                    ),
                    modifier = Modifier.height(36.dp)
                )
            }
        }
    }
}
