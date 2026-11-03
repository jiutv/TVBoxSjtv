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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 首页 ViewModel —— 桥接 ApiConfig + SourceViewModel 到 Compose StateFlow。
 *
 * 关键实现要点（来自旧 HomeActivity 流程）：
 * 1. SourceViewModel 是业务核心，getSort()/getList() 是异步 OkGo 请求，
 *    结果通过 LiveData.postValue 回调 —— 必须用 Observer 观察，不能轮询 .value
 * 2. ApiConfig.loadConfig() 必须先完成，homeSourceBean 才有值
 * 3. sortResult.classes.sortList 要过 DefaultConfig.adjustSort() 处理
 */
class HomeViewModel : ViewModel() {

    // 和旧 Java HomeActivity 一样 —— SourceViewModel 是业务核心
    // 直接 new 出来用（每个 HomeViewModel 实例自己的 SourceViewModel）
    // 不通过 ViewModelProvider 共享，因为我们是 Compose 新栈
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

    companion object {
        private const val TAG = "HomeVM"
        private const val API_READY_TIMEOUT_MS = 15000L
        private const val API_READY_POLL_INTERVAL_MS = 300L
        private const val SORT_TIMEOUT_SECONDS = 30L
        private const val LIST_TIMEOUT_SECONDS = 30L
    }

    private fun currentSourceKey(): String = ApiConfig.get().homeSourceBean.key ?: ""

    // ============================================================
    //  核心：把 SourceViewModel 的 LiveData 包装成挂起函数
    //  用 observeForever + CompletableDeferred，一次性收到结果就取消
    // ============================================================

    /**
     * 异步等待 sourceViewModel.getSort(sourceKey) 的结果。
     * 用 observeForever 桥接 LiveData → suspend，正确接收 postValue 的回调。
     */
    private suspend fun awaitSortResult(sourceKey: String): AbsSortXml? =
        withContext(Dispatchers.IO) {
            val deferred = CompletableDeferred<AbsSortXml?>()
            val observer = androidx.lifecycle.Observer<AbsSortXml?> { result ->
                if (!deferred.isCompleted) {
                    deferred.complete(result)
                }
            }
            sourceViewModel.sortResult.observeForever(observer)
            try {
                sourceViewModel.getSort(sourceKey)
                // 等待结果，超时返回 null
                deferred.await().also {
                    Log.d(TAG, "sortResult 返回: ${if (it == null) "null" else "有数据"}")
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "awaitSortResult 超时")
                null
            } finally {
                sourceViewModel.sortResult.removeObserver(observer)
            }
        }

    /** 异步等待 sourceViewModel.getList(sortData, page) 的结果 */
    private suspend fun awaitListResult(sortData: MovieSort.SortData, page: Int): AbsXml? =
        withContext(Dispatchers.IO) {
            val deferred = CompletableDeferred<AbsXml?>()
            val observer = androidx.lifecycle.Observer<AbsXml?> { result ->
                if (!deferred.isCompleted) {
                    deferred.complete(result)
                }
            }
            sourceViewModel.listResult.observeForever(observer)
            try {
                sourceViewModel.getList(sortData, page)
                deferred.await()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "awaitListResult 超时")
                null
            } finally {
                sourceViewModel.listResult.removeObserver(observer)
            }
        }

    // ============================================================
    //  公开方法
    // ============================================================

    /**
     * 初始化 —— 等待 ApiConfig 就绪后加载分类。
     */
    fun init() {
        if (initJob?.isActive == true) {
            Log.d(TAG, "init() 已在运行中，跳过")
            return
        }
        initJob = viewModelScope.launch {
            val api = ApiConfig.get()

            // 轮询等待 ApiConfig.sourceBeanList 非空
            var waited = 0L
            while (api.sourceBeanList.isEmpty() && waited < API_READY_TIMEOUT_MS) {
                Log.d(TAG, "⏳ 等待 ApiConfig.loadConfig() 完成... (已等 ${waited}ms)")
                delay(API_READY_POLL_INTERVAL_MS)
                waited += API_READY_POLL_INTERVAL_MS
            }

            val srcList = api.sourceBeanList
            val homeSrc = api.homeSourceBean
            Log.i(TAG, "ApiConfig 就绪: sources=${srcList.size}, homeSrc=${homeSrc.key ?: "(null)"}")

            if (srcList.isEmpty() || homeSrc.key.isNullOrEmpty()) {
                Log.i(TAG, "❌ 视频源列表为空 → 显示未配置引导")
                _sourceConfigured.value = false
                _sources.value = emptyList()
                _currentSourceName.value = "未配置"
                return@launch
            }

            _sourceConfigured.value = true
            _sources.value = srcList
            _currentSourceName.value = homeSrc.name.ifBlank { "(未命名源)" }
            Log.i(TAG, "✅ 当前源: ${homeSrc.name} (${homeSrc.type})")

            loadCategories()
        }
    }

    /** 强制重新初始化（源配置变更后调用） */
    fun forceRefresh() {
        initJob?.cancel()
        loadJob?.cancel()
        init()
    }

    /** 切换当前视频源 */
    fun switchSource(source: SourceBean) {
        ApiConfig.get().setSourceBean(source)
        _currentSourceName.value = source.name.ifBlank { "(未命名源)" }
        _selectedCategoryIndex.value = 0
        _videos.value = emptyList()
        _categories.value = emptyList()
        loadCategories()
    }

    /** 选中分类 —— 加载该分类视频列表 */
    fun selectCategory(sortData: MovieSort.SortData) {
        currentSortData = sortData
        currentPage = 1
        _videos.value = emptyList()
        loadMore()
    }

    /** 加载更多视频（分页） */
    fun loadMore() {
        val sortData = currentSortData ?: return
        _loading.value = true
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = awaitListResult(sortData, currentPage)
            _loading.value = false
            if (result != null) {
                val videos = result.movie?.videoList
                if (!videos.isNullOrEmpty()) {
                    if (currentPage == 1) {
                        _videos.value = videos
                    } else {
                        _videos.value = _videos.value + videos
                    }
                    currentPage++
                    Log.i(TAG, "✅ 视频加载成功: page=${currentPage - 1}, count=${videos.size}")
                } else {
                    Log.w(TAG, "⚠️ listResult 里没有视频")
                }
            } else {
                Log.w(TAG, "⚠️ listResult 返回 null")
            }
        }
    }

    // ============================================================
    //  私有 —— 加载首页分类
    // ============================================================

    private fun loadCategories() {
        val key = currentSourceKey()
        if (key.isEmpty()) {
            Log.w(TAG, "loadCategories: sourceKey 为空，跳过")
            return
        }

        Log.i(TAG, "🔄 开始加载分类列表: sourceKey=$key")
        _loading.value = true
        _categories.value = emptyList()
        _videos.value = emptyList()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val sortXml = withTimeout(SORT_TIMEOUT_SECONDS * 1000) {
                awaitSortResult(key)
            }

            _loading.value = false

            if (sortXml == null) {
                Log.e(TAG, "❌ sortResult 为 null —— 源可能无响应或网络问题")
                _categories.value = DefaultConfig.adjustSort(key, ArrayList(), true)
                return@launch
            }

            // 分类 Tab：用 DefaultConfig.adjustSort 处理（和旧 HomeActivity 一样）
            val rawSortList = sortXml.classes?.sortList
            val sortList = DefaultConfig.adjustSort(key, rawSortList, true)
            Log.i(TAG, "✅ 分类加载成功: 原始 ${rawSortList?.size ?: 0} → 处理后 ${sortList.size}")

            _categories.value = sortList

            if (sortList.isNotEmpty()) {
                // 自动选中第一个分类并加载视频
                selectCategory(sortList.first())
            } else if (!sortXml.videoList.isNullOrEmpty()) {
                // 有些源没有分类 Tab，但 sortResult.videoList 就是推荐视频（home 推荐）
                Log.i(TAG, "ℹ️ 没有分类 Tab，使用 home 推荐视频: ${sortXml.videoList.size} 条")
                _videos.value = sortXml.videoList
            } else {
                Log.w(TAG, "⚠️ sortResult 里既无分类也无视频")
            }
        }
    }

    private suspend fun <T> withTimeout(timeoutMs: Long, block: suspend () -> T): T {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) { block() }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "操作超时 ${timeoutMs}ms")
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 不在这里调 removeObserver —— 每个 await 函数自己 remove
        loadJob?.cancel()
        initJob?.cancel()
    }
}
