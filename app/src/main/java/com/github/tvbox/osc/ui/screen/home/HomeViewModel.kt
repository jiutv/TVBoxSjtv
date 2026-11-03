package com.github.tvbox.osc.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel —— 包装 ApiConfig + SourceViewModel，给 Compose 提供 StateFlow。
 * 底层解析/爬取逻辑完全不动，只做桥接。
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

    private val _videos = MutableStateFlow<List<com.github.tvbox.osc.bean.Movie.Video>>(emptyList())
    val videos: StateFlow<List<com.github.tvbox.osc.bean.Movie.Video>> = _videos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSortData: MovieSort.SortData? = null
    private var currentPage = 1

    /** 初始化 —— 加载源配置 + 首页分类 */
    fun init() {
        // Source 列表
        ApiConfig.get().let { api ->
            _sources.value = api.sourceBeanList
            api.mHomeSource?.let { _currentSourceName.value = it.name }
        }
        // 加载分类
        loadCategories()
    }

    /** 切换源 */
    fun switchSource(source: SourceBean) {
        ApiConfig.get().setSourceBean(source)
        _currentSourceName.value = source.name
        _selectedCategoryIndex.value = 0
        _videos.value = emptyList()
        loadCategories()
    }

    /** 加载首页分类列表 */
    private fun loadCategories() {
        _loading.value = true
        currentPage = 1
        sourceViewModel.getSort(ApiConfig.get().homeSource?.key ?: "")
        sourceViewModel.sortResult.observeForever { absSort ->
            absSort?.sortList?.let { sortList ->
                _categories.value = sortList
                if (sortList.isNotEmpty()) {
                    selectCategory(sortList.first())
                }
            }
            _loading.value = false
        }
    }

    /** 选中分类 —— 加载第一页视频 */
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
        sourceViewModel.getList(sortData, currentPage)
        sourceViewModel.listResult.observeForever { absXml ->
            absXml?.movie?.videoList?.let { newVideos ->
                if (currentPage == 1) {
                    _videos.value = newVideos
                } else {
                    _videos.value = _videos.value + newVideos
                }
                currentPage++
            }
            _loading.value = false
        }
    }
}
