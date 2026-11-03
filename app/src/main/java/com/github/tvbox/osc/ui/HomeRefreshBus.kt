package com.github.tvbox.osc.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 简单的全局刷新信号 —— 用于跨页面通知 HomeScreen 重新加载。
 * 用 StateFlow 包装一个 Int tick，每次 +1 就是发一个信号。
 */
object HomeRefreshBus {
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick.asStateFlow()

    fun notifySourceChanged() {
        _tick.value = _tick.value + 1
    }
}
