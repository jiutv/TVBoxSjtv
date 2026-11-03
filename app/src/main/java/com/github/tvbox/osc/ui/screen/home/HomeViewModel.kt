package com.github.tvbox.osc.ui.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.AbsSortXml
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel —— 桥接 ApiConfig + SourceViewModel 到 Compose StateFlow。
 *
 * 实现要点（和旧 HomeActivity 对齐）：
 * 1. SourceViewModel.getSort()/getList() 是 OkGo 异步请求，
 *    结果通过 LiveData.postValue 回调 —— 必须用 Observer 观察
 * 2. ApiConfig.loadConfig() 必须先完成，homeSourceBean 才有值
 * 3. 分类列表要过 DefaultConfig.adjustSort() 处理
 */
class HomeViewModel : ViewModel() {

    private val sourceViewModel = SourceViewModel()

    private val _sources = MutableStateFlow<List<SourceBean>>(emptyList())
    val sources: StateFlow<List<SourceBean>> = _sources.asStateFlow()

    private val _currentSourceName = MutableStateFlow("")
    val currentSourceName: StateFlow<String> = _currentSourceName.asStateFlow()

    private val _categories = MutableStateFlow<List<MovieSort.SortData>>(emptyList())
    val categories: StateFlow<List<MovieSort.SortData>> = _categories.asStateFlow()

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

    companion object {
        private const val TAG = "HomeVM"
        private const val API_READY_TIMEOUT_MS = 15000L
        private const val API_READY_POLL_INTERVAL_MS = 300L
    }

    private fun safeCurrentSourceKey(): String {
        return try {
            ApiConfig.get().homeSourceBean.key
        } catch (e: Throwable) {
            Log.e(TAG, "获取 sourceKey 异常: ${e.message}")
            ""
        }
    }

    // ============================================================
    //  LiveData → suspend 桥接（observeForever + CompletableDeferred）
    // ============================================================

    private suspend fun awaitSortResult(sourceKey: String): AbsSortXml? {
        return try {
            val deferred = kotlinx.coroutines.CompletableDeferred<AbsSortXml?>()
            val observer = androidx.lifecycle.Observer<AbsSortXml?> { result ->
                if (!deferred.isCompleted) try { deferred.complete(result) } catch (_: Throwable) {}
            }
            sourceViewModel.sortResult.observeForever(observer)
            try {
                sourceViewModel.getSort(sourceKey)
                // 有界等待：最多 35s
                withTimeoutOrNull(35000L) { deferred.await() }
            } finally {
                try { sourceViewModel.sortResult.removeObserver(observer) } catch (_: Throwable) {}
            }
        } catch (e: CancellationException) {
            null
        } catch (e: Throwable) {
            Log.e(TAG, "awaitSortResult 异常: ${e.message}")
            null
        }
    }

    private suspend fun awaitListResult(sortData: MovieSort.SortData, page: Int): AbsXml? {
        return try {
            val deferred = kotlinx.coroutines.CompletableDeferred<AbsXml?>()
            val observer = androidx.lifecycle.Observer<AbsXml?> { result ->
                if (!deferred.isCompleted) try { deferred.complete(result) } catch (_: Throwable) {}
            }
            sourceViewModel.listResult.observeForever(observer)
            try {
                sourceViewModel.getList(sortData, page)
                withTimeoutOrNull(35000L) { deferred.await() }
            } finally {
                try { sourceViewModel.listResult.removeObserver(observer) } catch (_: Throwable) {}
            }
        } catch (e: CancellationException) {
            null
        } catch (e: Throwable) {
            Log.e(TAG, "awaitListResult 异常: ${e.message}")
            null
        }
    }

    private suspend inline fun <T> withTimeoutOrNull(timeoutMs: Long, crossinline block: suspend () -> T): T? {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) { block() }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            null
        }
    }

    // ============================================================
    //  公开方法
    // ============================================================

    fun init() {
        if (initJob?.isActive == true) {
            Log.d(TAG, "init() 已在运行中，跳过")
            return
        }
        initJob = viewModelScope.launch {
            try {
                val api = ApiConfig.get()

                // 等待 ApiConfig.sourceBeanList 非空
                var waited = 0L
                while (true) {
                    val list = try { api.sourceBeanList } catch (_: Throwable) { emptyList<SourceBean>() }
                    if (list.isNotEmpty()) break
                    if (waited >= API_READY_TIMEOUT_MS) break
                    delay(API_READY_POLL_INTERVAL_MS)
                    waited += API_READY_POLL_INTERVAL_MS
                }

                val srcList = try { api.sourceBeanList } catch (_: Throwable) { emptyList() }
                val homeSrc = try { api.homeSourceBean } catch (_: Throwable) { null }
                val key = homeSrc?.key ?: ""

                Log.i(TAG, "ApiConfig 就绪: sources=${srcList.size}, homeSrcKey='$key'")

                if (srcList.isEmpty() || key.isEmpty()) {
                    Log.i(TAG, "❌ 视频源列表为空 → 显示未配置引导")
                    _sourceConfigured.value = false
                    _sources.value = emptyList()
                    _currentSourceName.value = "未配置"
                    _categories.value = emptyList()
                    _videos.value = emptyList()
                    return@launch
                }

                _sourceConfigured.value = true
                _sources.value = srcList
                val name = homeSrc?.name?.ifBlank { "(未命名源)" } ?: "(未命名源)"
                _currentSourceName.value = name
                Log.i(TAG, "✅ 当前源: $name")

                loadCategories()
            } catch (e: CancellationException) {
                // 正常取消，不处理
            } catch (e: Throwable) {
                Log.e(TAG, "init() 崩溃，保护应用: ${e.stackTraceToString()}")
                _sourceConfigured.value = false
            }
        }
    }

    fun forceRefresh() {
        initJob?.cancel()
        loadJob?.cancel()
        init()
    }

    fun switchSource(source: SourceBean) {
        try {
            ApiConfig.get().setSourceBean(source)
            _currentSourceName.value = source.name.ifBlank { "(未命名源)" }
            _videos.value = emptyList()
            _categories.value = emptyList()
            loadCategories()
        } catch (e: Throwable) {
            Log.e(TAG, "switchSource 异常: ${e.message}")
        }
    }

    fun selectCategory(sortData: MovieSort.SortData) {
        currentSortData = sortData
        currentPage = 1
        _videos.value = emptyList()
        loadMore()
    }

    fun loadMore() {
        val sortData = currentSortData ?: return
        _loading.value = true
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = awaitListResult(sortData, currentPage)
                if (result != null) {
                    val videos = result.movie?.videoList ?: emptyList()
                    if (videos.isNotEmpty()) {
                        _videos.value = if (currentPage == 1) videos else _videos.value + videos
                        currentPage++
                        Log.i(TAG, "✅ 视频加载: page=${currentPage - 1}, count=${videos.size}")
                    } else {
                        Log.w(TAG, "⚠️ listResult 里没有视频")
                    }
                } else {
                    Log.w(TAG, "⚠️ listResult 返回 null")
                }
            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Throwable) {
                Log.e(TAG, "loadMore 崩溃: ${e.stackTraceToString()}")
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================================
    //  私有 —— 加载分类
    // ============================================================

    private fun loadCategories() {
        val key = safeCurrentSourceKey()
        if (key.isEmpty()) {
            Log.w(TAG, "loadCategories: sourceKey 为空")
            return
        }

        Log.i(TAG, "🔄 加载分类: sourceKey=$key")
        _loading.value = true
        _categories.value = emptyList()
        _videos.value = emptyList()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val sortXml = awaitSortResult(key)

                if (sortXml == null) {
                    Log.e(TAG, "❌ sortResult 为 null")
                    _categories.value = DefaultConfig.adjustSort(key, ArrayList(), true)
                    return@launch
                }

                val rawSortList = sortXml.classes?.sortList ?: emptyList()
                val sortList = try {
                    DefaultConfig.adjustSort(key, rawSortList, true)
                } catch (e: Throwable) {
                    Log.e(TAG, "adjustSort 异常: ${e.message}")
                    rawSortList
                }

                Log.i(TAG, "✅ 分类加载: ${sortList.size} 个")
                _categories.value = sortList

                if (sortList.isNotEmpty()) {
                    selectCategory(sortList.first())
                } else if (!sortXml.videoList.isNullOrEmpty()) {
                    Log.i(TAG, "ℹ️ 无分类，使用 home 推荐: ${sortXml.videoList.size}")
                    _videos.value = sortXml.videoList
                }
            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Throwable) {
                Log.e(TAG, "loadCategories 崩溃: ${e.stackTraceToString()}")
            } finally {
                _loading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        initJob?.cancel()
    }
}
