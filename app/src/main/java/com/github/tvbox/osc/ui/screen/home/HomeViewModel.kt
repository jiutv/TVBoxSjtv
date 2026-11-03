package com.github.tvbox.osc.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel —— 桥接 ApiConfig + SourceViewModel 到 Compose StateFlow。
 * 不再使用 observeForever，改用一次性 Job，避免内存泄漏。
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

    private val _sourceConfigured = MutableStateFlow(true)
    val sourceConfigured: StateFlow<Boolean> = _sourceConfigured.asStateFlow()

    private var currentSortData: MovieSort.SortData? = null
    private var currentPage = 1
    private var loadJob: Job? = null

    private fun currentSourceKey(): String = ApiConfig.get().homeSourceBean.key ?: ""

    /** 初始化 —— 读取 ApiConfig 已有数据 */
    fun init() {
        val api = ApiConfig.get()
        val srcList = api.sourceBeanList
        val homeSrc = api.homeSourceBean

        // 如果没有源，标记未配置
        if (srcList.isEmpty() || homeSrc.key.isNullOrEmpty()) {
            _sourceConfigured.value = false
            _sources.value = emptyList()
            _currentSourceName.value = "未配置"
            return
        }

        _sourceConfigured.value = true
        _sources.value = srcList
        _currentSourceName.value = homeSrc.name

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
        val key = currentSourceKey()
        if (key.isEmpty()) return

        _loading.value = true
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            sourceViewModel.getSort(key)
            // 一次性读取 LiveData 结果（SourceViewModel 内部已通过 OkGo 回调更新 sortResult）
            // 这里需要等待数据到达 —— 用一个简单的协程等待
            try {
                // SourceViewModel.sortResult 通过 OkGo 异步回调更新，
                // 这里用定时轮询 LiveData 直到有数据或超时
                val deadline = System.currentTimeMillis() + 15000L // 15s timeout
                while (System.currentTimeMillis() < deadline) {
                    val result = sourceViewModel.sortResult.value
                    if (result != null) {
                        result.classes?.sortList?.let { list ->
                            _categories.value = list
                            if (list.isNotEmpty()) {
                                selectCategory(list.first())
                            }
                        }
                        break
                    }
                    kotlinx.coroutines.delay(300)
                }
            } finally {
                _loading.value = false
            }
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
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            sourceViewModel.getList(sortData, currentPage)
            try {
                val deadline = System.currentTimeMillis() + 15000L
                while (System.currentTimeMillis() < deadline) {
                    val result = sourceViewModel.listResult.value
                    if (result != null) {
                        result.movie?.videoList?.let { newVideos ->
                            if (currentPage == 1) {
                                _videos.value = newVideos
                            } else {
                                _videos.value = _videos.value + newVideos
                            }
                            currentPage++
                        }
                        break
                    }
                    kotlinx.coroutines.delay(300)
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
