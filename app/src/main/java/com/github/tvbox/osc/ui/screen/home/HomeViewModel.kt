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
 * 核心：SourceViewModel.getSort()/getList() 是 OkGo 异步请求，
 * 结果通过 LiveData.postValue 回调。必须用 observer 正确桥接。
 *
 * ⚠️ 关键坑：MutableLiveData.observeForever() 会立刻收到残留旧值！
 *   解决：observer 只接受 sourceKey 匹配当前请求的新结果。
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
            ApiConfig.get().homeSourceBean.key  // SourceBean.getKey() 永远不返回 null
        } catch (e: Throwable) {
            Log.e(TAG, "获取 sourceKey 异常: ${e.message}")
            ""
        }
    }

    // ============================================================
    //  LiveData → suspend 桥接（关键修复）
    // ============================================================

    /**
     * 等待 sourceViewModel.getSort(sourceKey) 的结果。
     *
     * 关键修复：observer 里检查 result.sourceKey == sourceKey
     * —— 因为 observeForever() 会立刻收到 LiveData 里残留的旧值
     *    那些旧值的 sourceKey 不匹配当前请求，必须忽略！
     */
    private suspend fun awaitSortResult(sourceKey: String): AbsSortXml? {
        return try {
            val deferred = kotlinx.coroutines.CompletableDeferred<AbsSortXml?>()
            var receivedCurrent = false  // 是否已收到匹配当前请求的结果

            val observer = androidx.lifecycle.Observer<AbsSortXml?> { result ->
                val resultKey = result?.sourceKey
                Log.d(TAG, "  [sort-observer] 收到结果 sourceKey=$resultKey, 期望=$sourceKey")
                if (resultKey == sourceKey && !receivedCurrent) {
                    receivedCurrent = true
                    try { deferred.complete(result) } catch (_: Throwable) {}
                }
            }

            sourceViewModel.sortResult.observeForever(observer)
            Log.d(TAG, "  [sort] observer 已注册，开始请求...")

            try {
                sourceViewModel.getSort(sourceKey)
                Log.d(TAG, "  [sort] getSort() 已调用，等待回调...")
                // 最多等 40 秒
                withTimeoutOrNull(40000L) { deferred.await() }
            } finally {
                try { sourceViewModel.sortResult.removeObserver(observer) } catch (_: Throwable) {}
            }
        } catch (e: CancellationException) {
            Log.w(TAG, "  [sort] 被取消")
            null
        } catch (e: Throwable) {
            Log.e(TAG, "  [sort] 异常: ${e.message}")
            null
        }
    }

    private suspend fun awaitListResult(sortData: MovieSort.SortData, page: Int): AbsXml? {
        return try {
            val deferred = kotlinx.coroutines.CompletableDeferred<AbsXml?>()
            // getList 没带 sourceKey，我们用 sentinel 标记"已收到新值"
            var gotNewValue = false

            val observer = androidx.lifecycle.Observer<AbsXml?> { result ->
                if (!gotNewValue) {
                    gotNewValue = true
                    Log.d(TAG, "  [list-observer] 收到结果: ${result != null}")
                    try { deferred.complete(result) } catch (_: Throwable) {}
                }
            }

            sourceViewModel.listResult.observeForever(observer)
            Log.d(TAG, "  [list] observer 已注册，page=$page")

            try {
                sourceViewModel.getList(sortData, page)
                Log.d(TAG, "  [list] getList() 已调用，等待回调...")
                withTimeoutOrNull(40000L) { deferred.await() }
            } finally {
                try { sourceViewModel.listResult.removeObserver(observer) } catch (_: Throwable) {}
            }
        } catch (e: CancellationException) {
            null
        } catch (e: Throwable) {
            Log.e(TAG, "  [list] 异常: ${e.message}")
            null
        }
    }

    private suspend inline fun <T> withTimeoutOrNull(timeoutMs: Long, crossinline block: suspend () -> T): T? {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) { block() }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "  操作超时 ${timeoutMs}ms")
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
                Log.i(TAG, "🚀 init() 开始")
                val api = ApiConfig.get()

                // 等待 ApiConfig.sourceBeanList 非空
                var waited = 0L
                while (true) {
                    val list = try { api.sourceBeanList } catch (_: Throwable) { emptyList<SourceBean>() }
                    if (list.isNotEmpty()) {
                        Log.d(TAG, "  ApiConfig 已就绪（等待了 ${waited}ms）")
                        break
                    }
                    if (waited >= API_READY_TIMEOUT_MS) {
                        Log.w(TAG, "  ApiConfig 等待超时")
                        break
                    }
                    delay(API_READY_POLL_INTERVAL_MS)
                    waited += API_READY_POLL_INTERVAL_MS
                }

                val srcList = try { api.sourceBeanList } catch (_: Throwable) { emptyList() }
                val homeSrc = try { api.homeSourceBean } catch (_: Throwable) { null }
                val key = homeSrc?.key ?: ""

                Log.i(TAG, "📊 状态: sources=${srcList.size}, homeSrcKey='$key', homeSrcName='${homeSrc?.name}'")

                if (srcList.isEmpty() || key.isEmpty()) {
                    Log.i(TAG, "❌ 未配置视频源 → 显示引导")
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
                Log.i(TAG, "✅ 已就绪: $name (type=${homeSrc?.type})")

                loadCategories()
            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Throwable) {
                Log.e(TAG, "💥 init() 崩溃: ${e.stackTraceToString()}")
                _sourceConfigured.value = false
            }
        }
    }

    fun forceRefresh() {
        Log.i(TAG, "🔄 forceRefresh()")
        initJob?.cancel()
        loadJob?.cancel()
        init()
    }

    fun switchSource(source: SourceBean) {
        try {
            Log.i(TAG, "🔀 切换源: ${source.name}")
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
        Log.i(TAG, "📂 选中分类: ${sortData.name}")
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
                    Log.i(TAG, "📺 listResult: ${videos.size} 条视频")
                    if (videos.isNotEmpty()) {
                        _videos.value = if (currentPage == 1) videos else _videos.value + videos
                        currentPage++
                    }
                } else {
                    Log.w(TAG, "⚠️ listResult 为 null")
                }
            } catch (e: CancellationException) {
            } catch (e: Throwable) {
                Log.e(TAG, "💥 loadMore 崩溃: ${e.stackTraceToString()}")
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
            Log.w(TAG, "loadCategories: sourceKey 为空！")
            return
        }

        Log.i(TAG, "🔄 开始加载分类: sourceKey=$key")
        _loading.value = true
        _categories.value = emptyList()
        _videos.value = emptyList()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                Log.d(TAG, "  调用 awaitSortResult...")
                val sortXml = awaitSortResult(key)
                Log.d(TAG, "  awaitSortResult 返回: ${if (sortXml == null) "null" else "有数据"}")

                if (sortXml == null) {
                    Log.e(TAG, "❌ sortResult 为 null —— 源可能没返回数据")
                    _categories.value = DefaultConfig.adjustSort(key, ArrayList(), true)
                    return@launch
                }

                val rawSortList = sortXml.classes?.sortList ?: emptyList()
                Log.d(TAG, "  sortXml.classes.sortList.size = ${rawSortList.size}")
                Log.d(TAG, "  sortXml.videoList.size = ${sortXml.videoList?.size ?: 0}")

                val sortList = try {
                    DefaultConfig.adjustSort(key, rawSortList, true)
                } catch (e: Throwable) {
                    Log.e(TAG, "adjustSort 异常: ${e.message}")
                    rawSortList
                }

                Log.i(TAG, "✅ 分类处理完成: 原始 ${rawSortList.size} → 最终 ${sortList.size}")
                _categories.value = sortList

                if (sortList.isNotEmpty()) {
                    Log.i(TAG, "  自动选中第一个分类: ${sortList.first().name}")
                    selectCategory(sortList.first())
                } else if (!sortXml.videoList.isNullOrEmpty()) {
                    Log.i(TAG, "ℹ️ 无分类 Tab，使用 home 推荐: ${sortXml.videoList.size}")
                    _videos.value = sortXml.videoList
                } else {
                    Log.w(TAG, "⚠️ sortResult 里既无分类也无视频")
                }
            } catch (e: CancellationException) {
            } catch (e: Throwable) {
                Log.e(TAG, "💥 loadCategories 崩溃: ${e.stackTraceToString()}")
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
