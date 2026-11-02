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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.components.VideoCard
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.theme.Colors

@Composable
fun HomeScreen(
    onVideoClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val tabs = MockData.homeTabs
    var selectedTab by remember { mutableStateOf(0) }
    val videos = MockData.homeVideos
    var loading by remember { mutableStateOf(false) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Colors.SurfaceVariant)
                            .clickable { loading = !loading }
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically),
                            tint = Colors.TextSecondary
                        )
                        Text(
                            text = "  TVBox官方源",
                            color = Colors.TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tabs) { index, name ->
                    val selected = index == selectedTab
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTab = index },
                        label = { Text(text = name, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (selected) Colors.Primary.copy(alpha = 0.9f) else Colors.SurfaceVariant,
                            selectedContainerColor = Colors.Primary,
                            labelColor = if (selected) Colors.OnPrimary else Colors.TextSecondary,
                            selectedLabelColor = Colors.OnPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = selected,
                            borderColor = Colors.Divider.copy(alpha = 0.3f),
                            selectedBorderColor = Colors.Primary
                        )
                    )
                }
            }

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
                    Icon(Icons.Default.Search, contentDescription = null,
                        modifier = Modifier.size(18.dp), tint = Colors.TextTertiary)
                    Text("搜索电影、电视剧、综艺...", color = Colors.TextTertiary, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos) { video ->
                    VideoCard(video = video, onClick = { onVideoClick(video.id) })
                }
            }
        }
    }
}
