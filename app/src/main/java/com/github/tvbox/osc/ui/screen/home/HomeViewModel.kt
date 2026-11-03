package com.github.tvbox.osc.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _videos = MutableStateFlow<List<Movie.Video>>(emptyList())
    val videos: StateFlow<List<Movie.Video>> = _videos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private fun currentSourceKey(): String = ApiConfig.get().homeSourceBean.key ?: ""

    /** 初始化 —— 加载源配置 + 首页分类 */
    fun init() {
        val api = ApiConfig.get()
        _sources.value = api.sourceBeanList
        _currentSourceName.value = api.homeSourceBean.name
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
        sourceViewModel.getSort(currentSourceKey())
        sourceViewModel.sortResult.observeForever { absSort ->
            // AbsSortXml.classes 是 MovieSort，MovieSort.sortList 是 List<SortData>
            absSort?.classes?.sortList?.let { list ->
                _categories.value = list
                if (list.isNotEmpty()) {
                    selectCategory(list.first())
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
            // listResult 类型是 AbsXml，AbsXml.movie 是 Movie，Movie.videoList 是 List<Video>
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

    private var currentSortData: MovieSort.SortData? = null
    private var currentPage = 1
}
