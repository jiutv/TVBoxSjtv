package com.github.tvbox.osc.ui.screen.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.ui.components.VideoCard
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.theme.Colors

/** 分类页 —— 左侧分类栏 + 右侧内容 */
@Composable
fun CategoryScreen(
    onVideoClick: (String) -> Unit = {}
) {
    val categories = MockData.categories
    var selectedIndex by remember { mutableStateOf(0) }
    val selectedCategory = categories[selectedIndex]
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类", color = Colors.TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Background)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 左侧分类栏
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .background(Colors.Surface)
            ) {
                androidx.compose.foundation.lazy.LazyColumn {
                    itemsIndexed(categories) { index, cat ->
                        val selected = index == selectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index; selectedTab = 0 }
                                .background(
                                    if (selected) Colors.Background
                                    else Colors.Surface
                                )
                                .padding(vertical = 14.dp, horizontal = 10.dp)
                        ) {
                            if (selected) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .background(Colors.Accent)
                                        .fillMaxWidth(0.04f)
                                )
                            }
                            Text(
                                text = cat.name,
                                color = if (selected) Colors.TextPrimary else Colors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 右侧内容
            Column(modifier = Modifier.fillMaxSize()) {
                // 子分类 Tab
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(selectedCategory.tabs) { index, tab ->
                        val selected = index == selectedTab
                        FilterChip(
                            selected = selected,
                            onClick = { selectedTab = index },
                            label = { Text(tab, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (selected) Colors.Primary.copy(alpha = 0.9f) else Colors.SurfaceVariant,
                                selectedContainerColor = Colors.Primary,
                                labelColor = if (selected) Colors.OnPrimary else Colors.TextSecondary,
                                selectedLabelColor = Colors.OnPrimary
                            )
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(MockData.categoryVideos) { video ->
                        VideoCard(video = video, onClick = { onVideoClick(video.id) })
                    }
                }
            }
        }
    }
}
