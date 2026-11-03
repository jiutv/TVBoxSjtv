package com.github.tvbox.osc.ui.screen.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.theme.Colors

/**
 * 播放页 —— 全屏黑色 + 顶部返回 + 底部控制栏
 * 播放器是黑色 UI 壳，后续接入 MyVideoView
 */
@Composable
fun PlayScreen(
    title: String = "播放器 UI 壳",
    onBack: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0.35f) }
    var controlsVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // 中间播放占位
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Colors.Accent.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "播放器区域（后续接入 MyVideoView）",
                color = Colors.TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // 顶部返回栏
        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp)
                    .background(Color(0x66000000)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回",
                        tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 底部控制栏
        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0x66000000))
                    .padding(12.dp)
            ) {
                // 进度条
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("14:23", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Colors.Accent,
                            activeTrackColor = Colors.Accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Text("40:00", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }

                // 控制按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
