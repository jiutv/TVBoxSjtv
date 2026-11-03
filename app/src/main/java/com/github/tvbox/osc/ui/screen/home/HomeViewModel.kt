package com.github.tvbox.osc.ui.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel —— 桥接 ApiConfig + SourceViewModel 到 Compose StateFlow。
 * 核心：异步等待 ApiConfig.loadConfig() 完成，避免时序竞争。
 */
class HomeViewModel : ViewModel() {

    private val sourceViewModel = SourceViewModel()

    private val _sources = MutableStateFlow<List<SourceBean>>(emptyList())
    val sources: StateFlow<List<SourceBean>> = _sources.asStateFlow()

    private val _currentSourceName = MutableStateFlow("")
    val currentSourceName: StateFlow<String> = _currentSourceName.asStateFlow()

    private val _categories = MutableStateFlow<List<MovieSort.SortData>>(emptyList())
    val categories: StateFlow<List<MovieSort.SortData>> = _categories.asStateFlow()

    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex.asStateFlow()

    private val _videos = MutableStateFlow<List<Movie.Video>>(emptyList())
    val videos: StateFlow<List<Movie.Video>> = _videos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sourceConfigured = MutableStateFlow(false)
    val sourceConfigured: StateFlow<Boolean> = _sourceConfigured.asStateFlow()

    private var currentSortData: MovieSort.SortData? = null
    private var currentPage = 1
    private var loadJob: Job? = null
    private var initJob: Job? = null
    private var initialized = false

    companion object {
        private const val TAG = "HomeVM"
        private const val API_READY_TIMEOUT_MS = 15000L
        private const val API_READY_POLL_INTERVAL_MS = 300L
    }

    private fun currentSourceKey(): String = ApiConfig.get().homeSourceBean.key ?: ""

    /**
     * 初始化 —— 异步等待 ApiConfig 就绪后再加载分类/视频。
     * 支持被多次调用（用户添加源后回来）。
     */
    fun init() {
        if (initJob?.isActive == true) {
            Log.d(TAG, "init() already running, skip")
            return
        }
        initJob = viewModelScope.launch {
            val api = ApiConfig.get()

            // 等待 ApiConfig.sourceBeanList 非空（最多 15 秒）
            var waited = 0L
            while (api.sourceBeanList.isEmpty() && waited < API_READY_TIMEOUT_MS) {
                Log.d(TAG, "等待 ApiConfig.loadConfig() 完成... (已等 ${waited}ms)")
                delay(API_READY_POLL_INTERVAL_MS)
                waited += API_READY_POLL_INTERVAL_MS
            }

            val srcList = api.sourceBeanList
            val homeSrc = api.homeSourceBean
            Log.i(TAG, "ApiConfig ready? sources=${srcList.size}, homeSrc.key=${homeSrc.key}, homeSrc.name=${homeSrc.name}")

            if (srcList.isEmpty() || homeSrc.key.isNullOrEmpty()) {
                // 真的没配置源
                Log.i(TAG, "❌ 视频源列表为空或 homeSourceBean 未设置 → 显示未配置引导")
                _sourceConfigured.value = false
                _sources.value = emptyList()
                _currentSourceName.value = "未配置"
                return@launch
            }

            _sourceConfigured.value = true
            _sources.value = srcList
            _currentSourceName.value = homeSrc.name.ifBlank { "(未命名源)" }
            Log.i(TAG, "✅ 已就绪: ${homeSrc.name}, 共 ${srcList.size} 个源")

            initialized = true
            loadCategories()
        }
    }

    /** 强制重新初始化（用户从源配置页返回时调用） */
    fun forceRefresh() {
        initialized = false
        init()
    }

    /** 切换源 */
    fun switchSource(source: SourceBean) {
        ApiConfig.get().setSourceBean(source)
        _currentSourceName.value = source.name.ifBlank { "(未命名源)" }
        _selectedCategoryIndex.value = 0
        _videos.value = emptyList()
        _categories.value = emptyList()
        loadCategories()
    }

    /** 加载首页分类列表（homeContent） */
    private fun loadCategories() {
        val key = currentSourceKey()
        if (key.isEmpty()) {
            Log.w(TAG, "loadCategories: sourceKey 为空，跳过")
            return
        }

        Log.i(TAG, "🔄 加载分类列表: sourceKey=$key")
        _loading.value = true
        _categories.value = emptyList()
        _videos.value = emptyList()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            sourceViewModel.getSort(key)
            try {
                val deadline = System.currentTimeMillis() + 20000L
                var waited = 0L
                while (System.currentTimeMillis() < deadline) {
                    val result = sourceViewModel.sortResult.value
                    if (result != null) {
                        val sortList = result.classes?.sortList
                        if (!sortList.isNullOrEmpty()) {
                            Log.i(TAG, "✅ 分类加载成功: ${sortList.size} 个分类")
                            _categories.value = sortList
                            selectCategory(sortList.first())
                        } else {
                            // 有些源 sortList 是空的，sortResult.videoList 有数据（home 推荐）
                            if (!result.videoList.isNullOrEmpty()) {
                                Log.i(TAG, "✅ sortList 为空但有 home 推荐视频: ${result.videoList.size} 条")
                                _videos.value = result.videoList
                            } else {
                                Log.w(TAG, "⚠️ sortResult 里既无 sortList 也无 videoList")
                            }
                        }
                        break
                    }
                    delay(300)
                    waited += 300
                }
                if (System.currentTimeMillis() >= deadline && sourceViewModel.sortResult.value == null) {
                    Log.w(TAG, "⚠️ sortResult 超时（20s），可能源无响应")
                }
            } finally {
                _loading.value = false
            }
        }
    }

    /** 选中分类 —— 加载第一页视频（categoryContent） */
    fun selectCategory(sortData: MovieSort.SortData) {
        currentSortData = sortData
        currentPage = 1
        _videos.value = emptyList()
        loadMore()
    }

    /** 加载更多（分页） */
    fun loadMore() {
        val sortData = currentSortData ?: return
        _loading.value = true
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            sourceViewModel.getList(sortData, currentPage)
            try {
                val deadline = System.currentTimeMillis() + 20000L
                while (System.currentTimeMillis() < deadline) {
                    val result = sourceViewModel.listResult.value
                    if (result != null) {
                        val videos = result.movie?.videoList
                        if (!videos.isNullOrEmpty()) {
                            if (currentPage == 1) {
                                _videos.value = videos
                            } else {
                                _videos.value = _videos.value + videos
                            }
                            currentPage++
                            Log.i(TAG, "✅ 视频加载成功: page=$currentPage, count=${videos.size}")
                        }
                        break
                    }
                    delay(300)
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
