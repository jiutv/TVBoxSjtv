package com.github.tvbox.osc.ui.screen.collect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.theme.Colors

@Composable
fun CollectScreen(
    onVideoClick: (String) -> Unit = {}
) {
    val videos = MockData.collectVideos

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏", color = Colors.TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Background)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无收藏，快去添加喜欢的影片吧", color = Colors.TextTertiary)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(videos) { video ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVideoClick(video.id) }
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.3f)
                                .aspectRatio(0.67f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(Colors.SurfaceVariant, Colors.Surface))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(video.name.take(1), color = Colors.TextTertiary, fontSize = 24.sp)
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.7f)
                                .padding(start = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(video.name, color = Colors.TextPrimary, fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall)
                            Text("⭐ ${video.score} · ${video.year} · ${video.area}",
                                color = Colors.TextSecondary, fontSize = 12.sp)
                            Text(video.note, color = Colors.Primary, fontSize = 12.sp)
                            Text(video.desc, color = Colors.TextTertiary, fontSize = 12.sp,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.weight(w: Float): Modifier =
    this.then(androidx.compose.foundation.layout.weight(w))

// need weight import
