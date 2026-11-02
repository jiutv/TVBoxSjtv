package com.github.tvbox.osc.ui.mock

/**
 * 模拟数据 —— 所有页面共用
 * 纯 Kotlin data class，不依赖任何底层 Bean
 * 后续接真实接口时把这里换掉即可
 */

data class MockVideo(
    val id: String,
    val name: String,
    val pic: String = "",           // 封面 URL，空字符串时显示占位
    val note: String = "",          // 集数/角标，如 "更新至 40 集"
    val year: Int = 2024,
    val area: String = "大陆",
    val score: String = "",
    val director: String = "",
    val actor: String = "",
    val desc: String = ""
)

data class MockCategory(
    val id: String,
    val name: String,
    val tabs: List<String> = emptyList()
)

data class MockLiveChannel(
    val id: String,
    val name: String,
    val cover: String = "",
    val status: String = "直播中"
)

data class MockEpisode(
    val name: String,
    val isSelected: Boolean = false
)

object MockData {

    // ─── 分类 Tab（首页顶部） ───
    val homeTabs = listOf(
        "推荐", "点播", "电影", "电视剧", "综艺", "动漫", "少儿", "纪录片"
    )

    // ─── 首页推荐视频（每个分类一份） ───
    private val covers = listOf(
        "https://picsum.photos/seed/tv1/400/600",
        "https://picsum.photos/seed/tv2/400/600",
        "https://picsum.photos/seed/tv3/400/600",
        "https://picsum.photos/seed/tv4/400/600",
        "https://picsum.photos/seed/tv5/400/600",
        "https://picsum.photos/seed/tv6/400/600",
        "https://picsum.photos/seed/tv7/400/600",
        "https://picsum.photos/seed/tv8/400/600"
    )

    val homeVideos: List<MockVideo> = (1..24).map { i ->
        MockVideo(
            id = "v$i",
            name = when (i % 8) {
                0 -> "狂飙 $i"
                1 -> "三体 第 $i 部"
                2 -> "长津湖"
                3 -> "你好，李焕英"
                4 -> "流浪地球 2"
                5 -> "孤注一掷"
                6 -> "消失的她"
                else -> "八角笼中"
            },
            pic = covers[i % covers.size],
            note = "更新至 ${20 + i} 集",
            year = 2022 + (i % 3),
            area = if (i % 3 == 0) "香港" else "大陆",
            score = "8.${i % 10}",
            director = "张艺谋",
            actor = "吴京, 沈腾, 黄渤",
            desc = "一个关于英雄主义与人性光辉的故事，讲述了平凡人在面对逆境时做出不平凡选择的经历。本片汇集了华语影坛最强阵容，场面震撼，情感真挚。"
        )
    }

    // ─── 分类页（左侧） ───
    val categories = listOf(
        MockCategory("c1", "推荐", listOf("全部", "高清", "蓝光", "4K")),
        MockCategory("c2", "电影", listOf("动作", "爱情", "科幻", "悬疑", "战争", "喜剧")),
        MockCategory("c3", "电视剧", listOf("大陆", "港台", "韩剧", "美剧", "日剧")),
        MockCategory("c4", "综艺", listOf("真人秀", "访谈", "音乐")),
        MockCategory("c5", "动漫", listOf("日漫", "国漫", "美漫")),
        MockCategory("c6", "少儿", listOf("动画", "儿歌")),
        MockCategory("c7", "纪录片", listOf("自然", "历史", "科技"))
    )

    val categoryVideos = homeVideos

    // ─── 直播 ───
    val liveChannels: List<MockLiveChannel> = listOf(
        MockLiveChannel("l1", "CCTV-1 综合", status = "直播中"),
        MockLiveChannel("l2", "CCTV-2 财经", status = "直播中"),
        MockLiveChannel("l3", "CCTV-3 综艺", status = "直播中"),
        MockLiveChannel("l4", "CCTV-5 体育", status = "直播中"),
        MockLiveChannel("l5", "CCTV-6 电影", status = "直播中"),
        MockLiveChannel("l6", "湖南卫视", status = "直播中"),
        MockLiveChannel("l7", "浙江卫视", status = "直播中"),
        MockLiveChannel("l8", "东方卫视", status = "直播中"),
        MockLiveChannel("l9", "北京卫视", status = "直播中"),
        MockLiveChannel("l10", "广东卫视", status = "直播中")
    )

    // ─── 详情页剧集 ───
    fun episodes(count: Int = 40): List<MockEpisode> =
        (1..count).map { MockEpisode("第${it}集", isSelected = it == 1) }

    // ─── 收藏 ───
    val collectVideos: List<MockVideo> = homeVideos.take(6).map { it.copy(note = "收藏于 2024-11-${it.id.takeLast(1)}") }

    // ─── 源配置 ───
    val sources = listOf(
        Triple("🐱 TVBox 官方源", "https://raw.githubusercontent.com/...", "启用"),
        Triple("🔥 豆瓣热播源", "https://cdn.jsdelivr.net/...", "启用"),
        Triple("📺 本地测试源", "http://192.168.1.100:9000/api.json", "已停止"),
        Triple("🌐 备用全球源", "https://example.com/config.json", "启用")
    )
}
