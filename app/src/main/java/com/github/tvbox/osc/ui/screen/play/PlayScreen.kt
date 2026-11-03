package com.github.tvbox.osc.ui.screen.play

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.tvbox.osc.ui.theme.Colors
import com.github.tvbox.osc.player.MyVideoView

/**
 * 播放页 —— 用 AndroidView 包装 MyVideoView（基于 xyz.doikki.videoplayer）
 */
@Composable
fun PlayScreen(
    title: String = "播放器",
    playUrl: String = "",
    onBack: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // 🎬 MyVideoView —— AndroidView 包装
        AndroidView(
            factory = { ctx ->
                MyVideoView(ctx).apply {
                    // 设置播放类型（从 HawkConfig 读取）
                    // PLAY_TYPE: 1=IJK, 2=Exo, 10=MXPlayer
                    setScreenScaleType(0) // 默认缩放
                    // 设置硬解码
                    // 这些设置在 PlayerHelper.init() 已经全局配置
                }
            },
            update = { videoView ->
                // 当 playUrl 变化时播放
                if (playUrl.isNotBlank()) {
                    videoView.setUrl(playUrl)
                    if (isPlaying) videoView.start()
                } else {
                    // 占位 —— 显示一个测试视频
                    videoView.setUrl("https://media.w3.org/2010/05/sintel/trailer.mp4")
                    if (isPlaying) videoView.start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部返回栏
        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
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
                    .background(Color(0x99000000))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SkipNext, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
