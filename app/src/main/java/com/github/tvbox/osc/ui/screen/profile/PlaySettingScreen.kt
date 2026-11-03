package com.github.tvbox.osc.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tvbox.osc.ui.theme.Colors
import com.orhanobut.hawk.Hawk
import com.github.tvbox.osc.util.HawkConfig

/**
 * 设置项枚举 —— 对应 HawkConfig 里的所有开关
 */
enum class SettingGroup(val title: String) {
    SOURCE("源与接口"),
    PLAYER("播放器"),
    LIVE("直播"),
    HISTORY("历史与收藏"),
    SEARCH("搜索"),
    DANMU("弹幕"),
    SUBTITLE("字幕"),
    OTHER("其他")
}

/**
 * 统一的设置项
 */
sealed class SettingItem {
    abstract val key: String
    abstract val title: String
    open val subtitle: String = ""

    data class Switch(
        override val key: String,
        override val title: String,
        override val subtitle: String = "",
        val default: Boolean = false
    ) : SettingItem()

    data class Select(
        override val key: String,
        override val title: String,
        override val subtitle: String = "",
        val options: List<String>,
        val default: Int = 0
    ) : SettingItem()

    data class Input(
        override val key: String,
        override val title: String,
        override val subtitle: String = "",
        val default: String = ""
    ) : SettingItem()

    data class Action(
        override val key: String,
        override val title: String,
        override val subtitle: String = "",
        val icon: ImageVector = Icons.Default.Settings
    ) : SettingItem()
}

/**
 * 所有设置项的完整定义 —— 从 HawkConfig 迁移
 */
fun allSettings(): Map<SettingGroup, List<SettingItem>> = mapOf(
    SettingGroup.SOURCE to listOf(
        SettingItem.Action("source_config", "视频源配置", "添加/管理视频源", Icons.Default.Source),
        SettingItem.Input(HawkConfig.API_URL, "视频源 API 地址", "当前: ${Hawk.get(HawkConfig.API_URL, "").ifBlank { "(未设置)" }}"),
        SettingItem.Input(HawkConfig.LIVE_API_URL, "直播源 API 地址"),
        SettingItem.Input(HawkConfig.EPG_URL, "EPG 地址"),
        SettingItem.Select(HawkConfig.HOME_REC, "首页推荐模式", "0=豆瓣热播 1=数据源推荐 2=历史", listOf("豆瓣热播", "数据源推荐", "历史")),
        SettingItem.Switch(HawkConfig.HOME_REC_STYLE, "首页推荐样式"),
        SettingItem.Switch(HawkConfig.M3U8_PURIFY, "M3U8 净化"),
        SettingItem.Input(HawkConfig.REMOTE_TVBOX, "远程 TVBox 地址"),
        SettingItem.Input(HawkConfig.DOH_URL, "DoH DNS 地址"),
        SettingItem.Input(HawkConfig.DOH_JSON, "DoH JSON 地址"),
    ),

    SettingGroup.PLAYER to listOf(
        SettingItem.Select(HawkConfig.PLAY_TYPE, "播放类型", "1=IJK 2=Exo 10=MXPlayer", listOf("IJK", "Exo", "MXPlayer")),
        SettingItem.Select(HawkConfig.PLAY_RENDER, "渲染方式", "0=Texture 2=Surface", listOf("Texture", "Surface")),
        SettingItem.Select(HawkConfig.PLAY_SCALE, "播放缩放", "选择画面缩放方式", listOf("默认", "拉伸", "裁剪", "原始")),
        SettingItem.Switch(HawkConfig.IJK_CODEC, "IJK 硬解码", "默认开启"),
        SettingItem.Switch(HawkConfig.IJK_CACHE_PLAY, "IJK 缓存播放"),
        SettingItem.Switch(HawkConfig.AUTO_SWITCH_LINE, "自动切换线路"),
        SettingItem.Select(HawkConfig.SCREEN_DISPLAY, "屏幕显示模式", "", listOf("默认")),
    ),

    SettingGroup.LIVE to listOf(
        SettingItem.Select(HawkConfig.LIVE_PLAY_TYPE, "直播播放类型", "", listOf("IJK", "Exo", "MXPlayer")),
        SettingItem.Select(HawkConfig.LIVE_PLAY_SCALE, "直播缩放", "", listOf("默认", "拉伸", "裁剪")),
        SettingItem.Switch(HawkConfig.LIVE_CHANNEL_REVERSE, "频道反转"),
        SettingItem.Switch(HawkConfig.LIVE_CROSS_GROUP, "跨组选台"),
        SettingItem.Switch(HawkConfig.LIVE_SHOW_NET_SPEED, "显示网速"),
        SettingItem.Switch(HawkConfig.LIVE_SHOW_RESOLUTION, "显示分辨率"),
        SettingItem.Switch(HawkConfig.LIVE_SHOW_TIME, "显示时间"),
        SettingItem.Switch(HawkConfig.DEFAULT_LOAD_LIVE, "启动直进直播"),
        SettingItem.Input(HawkConfig.LIVE_CONNECT_TIMEOUT, "直播连接超时(秒)"),
        SettingItem.Input(HawkConfig.LIVE_WEB_HEADER, "直播 WEB Header"),
    ),

    SettingGroup.HISTORY to listOf(
        SettingItem.Switch(HawkConfig.API_HISTORY, "API 历史记录"),
        SettingItem.Switch(HawkConfig.API_LINE_LIST, "API 线路列表"),
        SettingItem.Switch(HawkConfig.API_LINE_SOURCE, "API 线路来源"),
        SettingItem.Switch(HawkConfig.LIVE_API_HISTORY, "直播 API 历史"),
        SettingItem.Switch(HawkConfig.EPG_HISTORY, "EPG 历史"),
        SettingItem.Input(HawkConfig.HISTORY_NUM, "历史记录条数"),
        SettingItem.Switch(HawkConfig.HISTORY_MERGE, "历史合并同条目"),
    ),

    SettingGroup.SEARCH to listOf(
        SettingItem.Select(HawkConfig.SEARCH_VIEW, "搜索结果视图", "0=列表 1=缩略图", listOf("列表", "缩略图")),
        SettingItem.Switch(HawkConfig.FAST_SEARCH_MODE, "快速搜索模式"),
        SettingItem.Switch(HawkConfig.PARSE_WEBVIEW, "WebView 解析", "true=系统 WebView false=XWalk"),
        SettingItem.Switch(HawkConfig.SHOW_PREVIEW, "显示预览"),
    ),

    SettingGroup.DANMU to listOf(
        SettingItem.Switch(HawkConfig.DANMU_OPEN, "开启弹幕"),
        SettingItem.Input(HawkConfig.DANMU_MAX_LINE, "弹幕最大行数"),
        SettingItem.Select(HawkConfig.DANMU_SPEED, "弹幕速度", "选择弹幕滚动速度", listOf("很慢", "慢", "中", "快", "很快")),
        SettingItem.Input(HawkConfig.DANMU_ALPHA, "弹幕透明度(0-100)"),
        SettingItem.Input(HawkConfig.DANMU_SIZE_SCALE, "弹幕大小倍率"),
        SettingItem.Switch(HawkConfig.DANMU_RANDOM_COLOR, "随机颜色"),
        SettingItem.Input(HawkConfig.DANMU_API, "弹幕 API"),
    ),

    SettingGroup.SUBTITLE to listOf(
        SettingItem.Input(HawkConfig.SUBTITLE_TEXT_SIZE, "字幕大小"),
        SettingItem.Input(HawkConfig.SUBTITLE_TIME_DELAY, "字幕延迟(ms)"),
        SettingItem.Input(HawkConfig.SUBTITLE_EXO_SCALE, "Exo 字幕缩放"),
        SettingItem.Input(HawkConfig.SUBTITLE_EXO_POSITION, "Exo 字幕位置"),
    ),

    SettingGroup.OTHER to listOf(
        SettingItem.Switch(HawkConfig.DEBUG_OPEN, "调试模式"),
        SettingItem.Action("about", "关于 TVBoxOS", "", Icons.Default.Info),
    ),
)

/**
 * 设置页面 —— 完整展示 HawkConfig 所有开关
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaySettingScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            allSettings().forEach { (group, items) ->
                SettingGroupHeader(group.title)
                items.forEach { item ->
                    SettingItemRow(item)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Colors.Accent,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingItemRow(item: SettingItem) {
    when (item) {
        is SettingItem.Switch -> SwitchRow(item)
        is SettingItem.Select -> SelectRow(item)
        is SettingItem.Input -> InputRow(item)
        is SettingItem.Action -> ActionRow(item)
    }
}

@Composable
private fun SwitchRow(item: SettingItem.Switch) {
    var checked by remember(item.key) {
        mutableStateOf(Hawk.get(item.key, item.default))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = Colors.TextPrimary, fontSize = 15.sp)
            if (item.subtitle.isNotBlank()) {
                Text(item.subtitle, color = Colors.TextTertiary, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                Hawk.put(item.key, it)
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = Colors.Accent
            )
        )
    }
}

@Composable
private fun SelectRow(item: SettingItem.Select) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(item.key) {
        mutableStateOf(Hawk.get(item.key, item.default))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = Colors.TextPrimary, fontSize = 15.sp)
                if (item.subtitle.isNotBlank()) {
                    Text(item.subtitle, color = Colors.TextTertiary, fontSize = 12.sp)
                }
                Text(
                    item.options.getOrNull(selected) ?: "",
                    color = Colors.Accent,
                    fontSize = 13.sp
                )
            }
            TextButton(onClick = { expanded = true }) {
                Text("选择")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            item.options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        selected = index
                        Hawk.put(item.key, index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InputRow(item: SettingItem.Input) {
    var text by remember(item.key) {
        mutableStateOf(Hawk.get(item.key, item.default))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = Colors.TextPrimary, fontSize = 15.sp)
            TextField(
                value = text,
                onValueChange = { text = it; Hawk.put(item.key, it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ActionRow(item: SettingItem.Action) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { /* handle */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = Colors.Accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = Colors.TextPrimary, fontSize = 15.sp)
            if (item.subtitle.isNotBlank()) {
                Text(item.subtitle, color = Colors.TextTertiary, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Colors.TextTertiary)
    }
}

// 扩展 clickable
private fun Modifier.clickable(onClick: () -> Unit): Modifier = this
