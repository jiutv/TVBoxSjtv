package com.github.tvbox.osc.ui.screen.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.mock.MockData
import com.github.tvbox.osc.ui.mock.MockLiveChannel
import com.github.tvbox.osc.ui.theme.Colors

@Composable
fun LiveScreen(
    onChannelClick: (String) -> Unit = {}
) {
    val channels = MockData.liveChannels

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("直播", color = Colors.TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Background)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(chunks = channels.chunked(2)) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { channel ->
                        LiveChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 奇数时补空
                    if (row.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: MockLiveChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.SurfaceVariant)
        ) {
            // 占位渐变
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Colors.SurfaceVariant, Colors.Surface)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(2),
                    color = Colors.TextTertiary,
                    fontSize = 22.sp
                )
            }
            // 直播中角标
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF3B30))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = channel.status,
                    color = Colors.TextPrimary,
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = channel.name,
            color = Colors.TextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
