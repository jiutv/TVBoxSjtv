package com.github.tvbox.osc.ui.screen.source

import androidx.lifecycle.ViewModel
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.SourceBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 源配置页 ViewModel —— 直接桥接 ApiConfig。
 * 不依赖 Room/Hawk，ApiConfig 本身就是数据源。
 */
class SourceConfigViewModel : ViewModel() {

    private val _sources = MutableStateFlow<List<SourceBean>>(emptyList())
    val sources: StateFlow<List<SourceBean>> = _sources.asStateFlow()

    private val _currentSourceKey = MutableStateFlow("")
    val currentSourceKey: StateFlow<String> = _currentSourceKey.asStateFlow()

    fun refresh() {
        val api = ApiConfig.get()
        _sources.value = api.sourceBeanList
        _currentSourceKey.value = api.homeSourceBean.key ?: ""
    }

    fun switchTo(source: SourceBean) {
        ApiConfig.get().setSourceBean(source)
        _currentSourceKey.value = source.key
    }
}
