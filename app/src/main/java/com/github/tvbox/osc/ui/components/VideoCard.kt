package com.github.tvbox.osc.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.ui.theme.Colors

// ─────────────────────────────────────────────────────────────
// 视频卡片 —— 2:3 封面 + 片名 + 角标
// ─────────────────────────────────────────────────────────────

@Composable
fun VideoCard(
    video: Movie.Video,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(14.dp))
                .background(Colors.SurfaceVariant)
        ) {
            if (video.pic.isNotEmpty()) {
                AsyncImage(
                    model = video.pic,
                    contentDescription = video.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
                        text = video.name.take(1),
                        color = Colors.TextTertiary,
                        fontSize = 36.sp
                    )
                }
            }
            // 角标
            if (video.note.isNotEmpty()) {
                Surface(
                    color = Colors.Primary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topEnd = 14.dp, bottomStart = 10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = video.note,
                        color = Colors.OnPrimary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(8.dp)
        )
        Text(
            text = video.name,
            color = Colors.TextPrimary,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
        if (video.year > 0 || video.area.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (video.year > 0) {
                    Text(
                        text = "${video.year}",
                        color = Colors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
                if (video.area.isNotEmpty()) {
                    Text(
                        text = video.area,
                        color = Colors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Shimmer 骨架屏（加载中效果）
// ─────────────────────────────────────────────────────────────

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Colors.SurfaceVariant.copy(alpha = shimmerAlpha),
                        Colors.Surface.copy(alpha = shimmerAlpha),
                        Colors.SurfaceVariant.copy(alpha = shimmerAlpha)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 0f)
                )
            )
    )
}

@Composable
fun VideoCardShimmer() {
    Column {
        ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
        )
    }
}
