package com.github.tvbox.osc.ui.screen.source

import android.app.Activity
import android.text.InputType
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.ui.HomeRefreshBus
import com.github.tvbox.osc.ui.theme.Colors
import com.github.tvbox.osc.util.HawkConfig
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 源配置页 —— 连接真实 ApiConfig，支持添加/切换视频源 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceConfigScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SourceConfigViewModel = viewModel()

    val sources by viewModel.sources.collectAsState()
    val currentKey by viewModel.currentSourceKey.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newApiUrl by remember { mutableStateOf("") }
    var newApiName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var loadMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("源配置", color = Colors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Colors.TextPrimary)
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Colors.Primary.copy(alpha = 0.9f))
                            .clickable { showAddDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp), tint = Colors.OnPrimary)
                        Text("添加源", color = Colors.OnPrimary, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.Background)
            )
        },
        containerColor = Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(loadMessage.ifBlank { "加载中..." }, color = Colors.TextTertiary)
                }
            }

            if (sources.isEmpty() && !loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无视频源", color = Colors.TextSecondary, fontSize = 15.sp)
                        Text(
                            "点击右上角「添加源」输入 API 地址",
                            color = Colors.TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                sources.forEach { source ->
                    val isCurrent = source.key == currentKey
                    SourceItemRow(
                        source = source,
                        isCurrent = isCurrent,
                        onEnableClick = {
                            viewModel.switchTo(source)
                        }
                    )
                }
            }
        }
    }

    // 添加源弹窗
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加视频源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "输入视频源配置 API 地址（JSON/XML格式）",
                        color = Colors.TextTertiary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newApiName,
                        onValueChange = { newApiName = it },
                        label = { Text("源名称（可选）") },
                        placeholder = { Text("自动从配置读取") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newApiUrl,
                        onValueChange = { newApiUrl = it },
                        label = { Text("API 地址 *") },
                        placeholder = { Text("https://example.com/config.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newApiUrl.isNotBlank() && !loading,
                    onClick = {
                        showAddDialog = false
                        val url = newApiUrl.trim()
                        newApiUrl = ""
                        newApiName = ""
                        loading = true
                        loadMessage = "加载视频源..."
                        scope.launch(Dispatchers.IO) {
                            // 1. 保存到 Hawk
                            Hawk.put(HawkConfig.API_URL, url)
                            Hawk.put(HawkConfig.LIVE_API_URL, Hawk.get(HawkConfig.LIVE_API_URL, url))
                            // 2. 触发 loadConfig
                            val activity = context as? Activity
                            ApiConfig.get().loadConfig(
                                false,
                                object : ApiConfig.LoadConfigCallback {
                                    override fun success() {
                                        scope.launch(Dispatchers.Main) {
                                            loading = false
                                            loadMessage = ""
                                            viewModel.refresh()
                                            HomeRefreshBus.notifySourceChanged()  // 📢 通知首页刷新
                                        }
                                    }
                                    override fun error(msg: String?) {
                                        scope.launch(Dispatchers.Main) {
                                            loading = false
                                            loadMessage = ""
                                        }
                                    }
                                    override fun notice(msg: String?) {}
                                },
                                activity
                            )
                        }
                    }
                ) {
                    Text(if (loading) "加载中..." else "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newApiUrl = ""; newApiName = "" }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceItemRow(
    source: SourceBean,
    isCurrent: Boolean,
    onEnableClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Colors.SurfaceVariant)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 状态徽章
            val statusColor = if (isCurrent) Colors.Success else Colors.TextTertiary
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isCurrent) "启用" else "停止",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name.ifBlank { "(未命名源)" },
                    color = Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = source.api.ifBlank { source.key },
                    color = Colors.TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }
            FilterChip(
                selected = isCurrent,
                onClick = onEnableClick,
                label = { Text(if (isCurrent) "已启用" else "启用", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isCurrent) Colors.Accent else Colors.Surface,
                    labelColor = if (isCurrent) Colors.Background else Colors.TextSecondary
                )
            )
        }
    }
}
