package com.github.tvbox.osc.ui.screen.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 详情页 ViewModel —— 桥接 SourceViewModel.getDetail()
 */
class DetailViewModel : ViewModel() {

    private val sourceViewModel = SourceViewModel()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _video = MutableStateFlow<Movie.Video?>(null)
    val video: StateFlow<Movie.Video?> = _video.asStateFlow()

    companion object {
        private const val TAG = "DetailVM"
    }

    /**
     * 加载详情 —— 从首页视频卡片传入 sourceKey + videoId
     */
    fun loadDetail(sourceKey: String, videoId: String) {
        if (sourceKey.isEmpty() || videoId.isEmpty()) {
            _loading.value = false
            _error.value = "参数缺失"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = awaitDetailResult(sourceKey, videoId)
                if (result != null) {
                    // AbsXml.movie 是 Movie 类型，里面有 videoList: List<Video>
                    // 详情接口返回的 movie.videoList 一般只有一个元素
                    val firstVideo = result.movie?.videoList?.firstOrNull()
                    if (firstVideo != null) {
                        _video.value = firstVideo
                        Log.i(TAG, "✅ 详情加载成功: ${firstVideo.name}")
                    } else {
                        _error.value = "详情数据为空"
                    }
                } else {
                    _error.value = "请求超时"
                }
            } catch (e: CancellationException) {
            } catch (e: Throwable) {
                Log.e(TAG, "详情加载崩溃: ${e.message}")
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 把 LiveData 桥接成 suspend 函数
     * 关键：observer 只接受 sourceKey 匹配的新值（过滤残留旧值）
     */
    private suspend fun awaitDetailResult(sourceKey: String, videoId: String): AbsXml? =
        withContext(Dispatchers.IO) {
            val deferred = CompletableDeferred<AbsXml?>()
            var received = false

            val observer = androidx.lifecycle.Observer<AbsXml?> { result ->
                val resultKey = result?.sourceKey
                Log.d(TAG, "  [detail-observer] sourceKey=$resultKey, expect=$sourceKey")
                if (resultKey == sourceKey && !received) {
                    received = true
                    try { deferred.complete(result) } catch (_: Throwable) {}
                }
            }

            sourceViewModel.detailResult.observeForever(observer)
            try {
                sourceViewModel.getDetail(sourceKey, videoId)
                withTimeoutOrNull(40000L) { deferred.await() }
            } finally {
                sourceViewModel.detailResult.removeObserver(observer)
            }
        }

    private suspend inline fun <T> withTimeoutOrNull(timeoutMs: Long, crossinline block: suspend () -> T): T? {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) { block() }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            null
        }
    }
}
