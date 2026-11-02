package com.github.tvbox.osc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * TVBoxOS 手机版主题 —— 对标 AVBox 深色风格
 *
 * 主色系：深色背景 + 紫蓝主色
 * 对标 XiaochangXu/AVBox 的视觉基调
 */
object Colors {
    // 深色背景
    val Background = Color(0xFF141418)
    val Surface = Color(0xFF1E1E24)
    val SurfaceVariant = Color(0xFF26262E)
    val Card = Color(0xFF22222A)

    // 主色 —— 紫蓝渐变感
    val Primary = Color(0xFF6C5CE7)
    val PrimaryVariant = Color(0xFF5A4BD1)
    val OnPrimary = Color(0xFFFFFFFF)

    val Accent = Color(0xFF00C8FF)        // 霓虹蓝，AVBox 风格
    val AccentPink = Color(0xFFFF6B9D)    // 点缀粉

    // 文字
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B8)
    val TextTertiary = Color(0xFF70707A)

    // 分割线
    val Divider = Color(0xFF2E2E36)

    // 状态
    val Success = Color(0xFF00C853)
    val Warning = Color(0xFFFFB300)
    val Error = Color(0xFFFF3B30)

    // 浅色版（很少用，保留）
    val LightBackground = Color(0xFFF7F7FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightPrimary = Color(0xFF6C5CE7)
}

private val DarkColorScheme = darkColorScheme(
    background = Colors.Background,
    surface = Colors.Surface,
    surfaceVariant = Colors.SurfaceVariant,
    primary = Colors.Primary,
    onPrimary = Colors.OnPrimary,
    secondary = Colors.Accent,
    tertiary = Colors.AccentPink,
    onBackground = Colors.TextPrimary,
    onSurface = Colors.TextPrimary,
    onSurfaceVariant = Colors.TextSecondary,
    error = Colors.Error,
    outline = Colors.Divider
)

private val LightColorScheme = lightColorScheme(
    background = Colors.LightBackground,
    surface = Colors.LightSurface,
    primary = Colors.LightPrimary,
    onBackground = Colors.Background,
    onSurface = Colors.Background
)

@Composable
fun TVBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
