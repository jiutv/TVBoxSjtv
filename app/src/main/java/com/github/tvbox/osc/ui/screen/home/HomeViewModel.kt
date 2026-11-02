package com.github.tvbox.osc.ui.screen.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.AbsSortXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.Hawk
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home 页 ViewModel —— 包装 ApiConfig + SourceViewModel 为 Compose State
 * 底层解析、嗅探、播放核心逻辑完全不动，只做 UI 层数据桥接
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel : ViewModel() {

    private val _homeSourceName = MutableStateFlow("加载中…")
    val homeSourceName: StateFlow<String> = _homeSourceName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _categories = MutableStateFlow<List<MovieSort.SortData>>(emptyList())
    val categories: StateFlow<List<MovieSort.SortData>> = _categories.asStateFlow()

    /** 当前选中的分类 index */
    private val _selectedCategoryIndex = MutableStateFlow(0)
    val selectedCategoryIndex: StateFlow<Int> = _selectedCategoryIndex.asStateFlow()

    /** 每个分类的视频列表，key = sortData.id */
    private val _videosByCategory = MutableStateFlow<Map<String, List<Movie.Video>>>(emptyMap())
    val videosByCategory: StateFlow<Map<String, List<Movie.Video>>> = _videosByCategory.asStateFlow()

    /** 加载更多状态 */
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private var sourceVm: SourceViewModel? = null
    private var currentPage = mutableMapOf<String, Int>() // 每个分类的当前页
    private var hasMore = mutableMapOf<String, Boolean>()  // 每个分类是否有更多

    /** 初始化 —— 必须 Activity context（loadJar 需要） */
    fun init(activity: Activity, sourceViewModel: SourceViewModel) {
        if (sourceVm != null) return // 已初始化
        sourceVm = sourceViewModel
        setupObservers(sourceViewModel)
        loadConfigAndJar(activity, sourceViewModel)
    }

    private fun loadConfigAndJar(activity: Activity, sourceVm: SourceViewModel) {
        ApiConfig.get().loadConfig(true, object : ApiConfig.LoadConfigCallback {
            override fun success() {
                // jar 加载 —— 与原 HomeActivity 完全一致
                val spider = ApiConfig.get().spider
                if (spider.isNotEmpty()) {
                    ApiConfig.get().loadJar(useCache, spider, object : ApiConfig.LoadConfigCallback {
                        override fun success() {
                            loadHomeSort(sourceVm)
                        }
                        override fun error(msg: String?) {
                            loadHomeSort(sourceVm) // jar 失败也继续
                        }
                        override fun notice(msg: String?) {}
                    }, activity)
                } else {
                    loadHomeSort(sourceVm)
                }
            }
            override fun error(msg: String?) {
                _error.value = msg ?: "加载配置失败"
                _isLoading.value = false
            }
            override fun notice(msg: String?) {}
        }, activity)
    }

    private fun loadHomeSort(sourceVm: SourceViewModel) {
        val home = ApiConfig.get().homeSourceBean
        if (home != null) {
            _homeSourceName.value = home.name
            sourceVm.getSort(home.key)
        } else {
            sourceVm.getSort(null)
        }
    }

    private fun setupObservers(sourceVm: SourceViewModel) {
        // sortResult → categories
        sourceVm.sortResult.observeForever { absXml: AbsSortXml? ->
            val home = ApiConfig.get().homeSourceBean
            val homeKey = home?.key ?: ""
            val sortList = absXml?.classes?.sortList ?: emptyList()
            val adjusted = DefaultConfig.adjustSort(homeKey, ArrayList(sortList), true)
            _categories.value = adjusted
            _homeSourceName.value = home?.name ?: "未知源"
            _isLoading.value = false
            // 加载第一个分类的视频
            if (adjusted.isNotEmpty()) {
                loadCategoryVideos(sourceVm, adjusted[0], reset = true)
            }
        }

        // listResult → videosByCategory（按当前选中的分类匹配 sourceKey）
        sourceVm.listResult.observeForever { xml: com.github.tvbox.osc.bean.AbsXml? ->
            if (xml == null) return@observeForever
            val categories = _categories.value
            val idx = _selectedCategoryIndex.value
            if (idx >= categories.size) return@observeForever
            val currentSortData = categories[idx]

            // 只处理与当前选中分类匹配的结果
            if (currentSortData.id.isNotEmpty() && !xml.sourceKey.isNullOrEmpty()
                && xml.sourceKey != currentSortData.id) {
                return@observeForever // 不是当前分类的请求，忽略
            }

            val videos = xml.movie?.videoList ?: emptyList()
            val key = currentSortData.id.ifEmpty { currentSortData.name }
            val existing = _videosByCategory.value[key] ?: emptyList()
            val page = currentPage[key] ?: 1
            val isAppend = page > 1

            _videosByCategory.value = _videosByCategory.value + (
                key to if (isAppend) existing + videos else videos
            )
            hasMore[key] = videos.isNotEmpty()
            _loadingMore.value = false
        }
    }

    /** 切换选中的分类 → 加载视频 */
    fun selectCategory(index: Int) {
        if (index == _selectedCategoryIndex.value) return
        val categories = _categories.value
        if (index !in categories.indices) return
        _selectedCategoryIndex.value = index
        val sortData = categories[index]
        val sourceVm = sourceVm ?: return
        loadCategoryVideos(sourceVm, sortData, reset = true)
    }

    private fun loadCategoryVideos(sourceVm: SourceViewModel, sortData: MovieSort.SortData, reset: Boolean) {
        val key = sortData.id.ifEmpty { sortData.name }
        if (reset) {
            currentPage[key] = 1
            hasMore[key] = true
            _loadingMore.value = false
        }
        val page = currentPage[key] ?: 1
        sourceVm.getList(sortData, page)
    }

    /** 下拉刷新当前分类 */
    fun refreshCurrentCategory() {
        val categories = _categories.value
        val idx = _selectedCategoryIndex.value
        if (idx !in categories.indices) return
        val sourceVm = sourceVm ?: return
        loadCategoryVideos(sourceVm, categories[idx], reset = true)
    }

    /** 加载更多 */
    fun loadMore() {
        if (_loadingMore.value) return
        val categories = _categories.value
        val idx = _selectedCategoryIndex.value
        if (idx !in categories.indices) return
        val sortData = categories[idx]
        val key = sortData.id.ifEmpty { sortData.name }
        if (hasMore[key] == false) return
        val sourceVm = sourceVm ?: return
        _loadingMore.value = true
        val nextPage = (currentPage[key] ?: 1) + 1
        currentPage[key] = nextPage
        sourceVm.getList(sortData, nextPage)
    }

    /** 切换播放源 */
    fun switchSource(source: SourceBean) {
        ApiConfig.get().setSourceBean(source)
        _homeSourceName.value = source.name
        val sourceVm = sourceVm ?: return
        _videosByCategory.value = emptyMap()
        currentPage.clear()
        hasMore.clear()
        _selectedCategoryIndex.value = 0
        loadHomeSort(sourceVm)
    }

    /** 获取可切换源列表 */
    fun getSwitchSources(): List<SourceBean> {
        return ApiConfig.get().switchSourceBeanList
    }

    fun getCurrentSource(): SourceBean? {
        return ApiConfig.get().homeSourceBean
    }

    override fun onCleared() {
        super.onCleared()
        // SourceViewModel 是外部传入的，这里不处理
    }
}
