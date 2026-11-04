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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 首页 ViewModel —— 完全对齐 TVBoxOS HomeActivity 的 initViewModel + loadHomeSort 流程。
 *
 * 旧代码核心流程 (HomeActivity.java):
 *   1. sourceViewModel = ViewModelProvider(this).get(SourceViewModel.class)
 *   2. sourceViewModel.sortResult.observe(this, Observer { ... })   // 持续观察
 *   3. ApiConfig.get().loadConfig(callback)                         // 加载源配置
 *   4. callback.success() → loadHomeSort()
 *      → loadingSourceKey = ApiConfig.get().homeSourceBean.key
 *      → sourceViewModel.getSort(loadingSourceKey)                  // 触发请求
 *   5. observer 收到 sortResult → adjustSort → update UI
 *
 * 新 Compose 版本:
 *   - 用 observeForever + StateFlow 桥接（跟旧 observe 一样持续观察）
 *   - 不 filter sourceKey（旧代码虽然 check 但不影响逻辑）
 *   - 直接把收到的 sortResult 转成 StateFlow 给 UI
 */
class HomeViewModel : ViewModel() {

    // 旧代码: new ViewModelProvider(this).get(SourceViewModel.class)
    // 这里直接 new —— 每个 HomeViewModel 实例有自己的 SourceViewModel
    private val sourceViewModel = SourceViewModel()

    // 持续观察的 observer —— 跟旧 HomeActivity.initViewModel() 完全一样
    private val sortResultObserver = androidx.lifecycle.Observer<AbsSortXml> { absXml ->
        Log.i(TAG, "🔔 sortResult.observe 收到: sourceKey=${absXml?.sourceKey}, classes=${absXml?.classes?.sortList?.size ?: "null"}")
        handleSortResult(absXml)
    }

    private val listResultObserver = androidx.lifecycle.Observer<AbsXml> { xml ->
        Log.d(TAG, "🔔 listResult.observe 收到: ${xml?.movie?.videoList?.size ?: "null"}")
        val videos = xml?.movie?.videoList ?: emptyList()
        _loading.value = false
        if (currentPage == 1) {
            _videos.value = videos
        } else {
            _videos.value = _videos.value + videos
        }
        currentPage++
    }

    // UI state
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

    companion object {
        private const val TAG = "HomeVM"
    }

    init {
        // 跟旧 HomeActivity.initViewModel() 一样 —— 构造时注册持续观察
        sourceViewModel.sortResult.observeForever(sortResultObserver)
        sourceViewModel.listResult.observeForever(listResultObserver)
        Log.i(TAG, "🔧 HomeViewModel init — observer 已注册")
    }

    override fun onCleared() {
        super.onCleared()
        // 跟旧 HomeActivity.onDestroy() 一样 —— 移除 observer
        sourceViewModel.sortResult.removeObserver(sortResultObserver)
        sourceViewModel.listResult.removeObserver(listResultObserver)
        Log.i(TAG, "🗑️ HomeViewModel onCleared — observer 已移除")
    }

    /**
     * 跟旧 HomeActivity.loadHomeSort() 完全对齐
     */
    private fun handleSortResult(absXml: AbsSortXml?) {
        try {
            val api = ApiConfig.get()
            val home = api.homeSourceBean  // 永远不 null，返回 emptyHome
            val homeKey = home.key
            val homeName = home.name

            if (homeName.isNotBlank()) {
                _currentSourceName.value = homeName
            }

            // 旧代码: DefaultConfig.adjustSort(homeKey, absXml.classes.sortList, true)
            val rawSortList = absXml?.classes?.sortList
            val sortList: List<MovieSort.SortData> = try {
                DefaultConfig.adjustSort(homeKey, rawSortList, true)
            } catch (e: Throwable) {
                Log.e(TAG, "adjustSort 异常: ${e.message}")
                rawSortList ?: emptyList()
            }

            Log.i(TAG, "📋 分类处理: 原始 ${rawSortList?.size ?: 0} → 最终 ${sortList.size}")
            _categories.value = sortList
            _loading.value = false

            if (sortList.isNotEmpty()) {
                // 自动选中第一个分类并加载视频（跟旧 GridFragment 一样）
                selectCategoryInternal(sortList.first())
            } else if (!absXml?.videoList.isNullOrEmpty()) {
                // HOME_REC 模式 —— 没有分类 Tab 但有推荐视频
                Log.i(TAG, "ℹ️ HOME_REC 模式，推荐视频 ${absXml?.videoList?.size} 条")
                _videos.value = absXml?.videoList ?: emptyList()
            } else {
                Log.w(TAG, "⚠️ sortResult 无分类也无视频")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "💥 handleSortResult 崩溃: ${e.stackTraceToString()}")
        }
    }

    /**
     * 公开入口 —— 跟旧 initData() → loadHomeSort() 对齐
     */
    fun init() {
        Log.i(TAG, "🚀 init() — 开始加载首页数据")
        _loading.value = true

        viewModelScope.launch {
            try {
                val api = ApiConfig.get()

                // 先检查 ApiConfig 当前状态
                val sourceList = try { api.getSourceBeanList() } catch (_: Throwable) { emptyList() }
                val home = api.homeSourceBean
                val homeKey = home.key
                val homeName = home.name

                Log.i(TAG, "📊 ApiConfig 当前: sources=${sourceList.size}, homeKey='$homeKey', homeName='$homeName'")

                _sources.value = sourceList
                _currentSourceName.value = homeName

                if (sourceList.isEmpty() || homeKey.isEmpty()) {
                    Log.w(TAG, "❌ 未配置视频源 —— 等待 loadConfig 完成...")
                    _sourceConfigured.value = false

                    // 等 loadConfig 完成 —— 最多 20 秒
                    var waited = 0L
                    while (waited < 20000L) {
                        delay(300)
                        waited += 300
                        val listNow = try { api.getSourceBeanList() } catch (_: Throwable) { emptyList() }
                        if (listNow.isNotEmpty()) {
                            Log.i(TAG, "✅ ApiConfig 就绪（等了 ${waited}ms），sources=${listNow.size}")
                            break
                        }
                    }

                    val listFinal = try { api.getSourceBeanList() } catch (_: Throwable) { emptyList() }
                    if (listFinal.isEmpty()) {
                        Log.w(TAG, "⚠️ 等了 20s 还是空 —— 可能 HawkConfig.API_URL 没设")
                        _loading.value = false
                        return@launch
                    }

                    _sources.value = listFinal
                    val homeFinal = api.homeSourceBean
                    _currentSourceName.value = homeFinal.name
                    _sourceConfigured.value = true
                } else {
                    _sourceConfigured.value = true
                    Log.i(TAG, "✅ 视频源已就绪: $homeName ($homeKey)")
                }

                // 跟旧 HomeActivity.loadHomeSort() 完全一样 —— 触发 getSort
                val key = api.homeSourceBean.key  // 永远有值（即使没配源也是 empty 字符串）
                Log.i(TAG, "🔄 sourceViewModel.getSort('$key')")
                sourceViewModel.getSort(key)
                // 结果会被上面注册的 sortResultObserver 自动收到 ✅

            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Throwable) {
                Log.e(TAG, "💥 init() 崩溃: ${e.stackTraceToString()}")
                _loading.value = false
            }
        }
    }

    fun forceRefresh() {
        Log.i(TAG, "🔄 forceRefresh()")
        init()
    }

    fun switchSource(source: SourceBean) {
        try {
            Log.i(TAG, "🔀 切换源: ${source.name}")
            ApiConfig.get().setSourceBean(source)
            _currentSourceName.value = source.name
            _videos.value = emptyList()
            _categories.value = emptyList()
            // 触发重新加载 —— 跟旧 showSiteSwitch 逻辑一样
            sourceViewModel.getSort(source.key)
        } catch (e: Throwable) {
            Log.e(TAG, "switchSource 异常: ${e.message}")
        }
    }

    fun selectCategory(sortData: MovieSort.SortData) {
        selectCategoryInternal(sortData)
    }

    private fun selectCategoryInternal(sortData: MovieSort.SortData) {
        Log.i(TAG, "📂 选中分类: ${sortData.name}")
        currentSortData = sortData
        currentPage = 1
        _videos.value = emptyList()
        _loading.value = true

        // 跟旧 GridFragment.onLoadMoreRequested() 一样 —— sourceViewModel.getList
        try {
            sourceViewModel.getList(sortData, currentPage)
            // 结果会被 listResultObserver 自动收到 ✅
        } catch (e: Throwable) {
            Log.e(TAG, "getList 异常: ${e.message}")
            _loading.value = false
        }
    }

    fun loadMore() {
        val sortData = currentSortData ?: return
        currentPage++
        _loading.value = true
        try {
            sourceViewModel.getList(sortData, currentPage)
        } catch (e: Throwable) {
            Log.e(TAG, "loadMore 异常: ${e.message}")
            _loading.value = false
        }
    }

    private suspend fun delay(ms: Long) {
        withContext(Dispatchers.Default) { kotlinx.coroutines.delay(ms) }
    }
}
