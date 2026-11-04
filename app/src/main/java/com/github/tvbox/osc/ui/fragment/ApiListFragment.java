package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.ApiSourceAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置列表 Fragment — 一个 Tab 的内容 (点播 or 直播)
 */
public class ApiListFragment extends Fragment {

    public static final int TYPE_VOD = 0;   // 点播
    public static final int TYPE_LIVE = 1;  // 直播

    private int type;
    private RecyclerView recyclerView;
    private View emptyView;
    private ApiSourceAdapter adapter;

    public static ApiListFragment newInstance(int type) {
        ApiListFragment f = new ApiListFragment();
        f.type = type;
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_api_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ApiSourceAdapter();
        adapter.setListener(new ApiSourceAdapter.OnSourceListener() {
            @Override
            public void onToggle(int position, ApiSourceAdapter.SourceItem item, boolean enabled) {
                if (enabled) {
                    activateSource(item.url);
                } else {
                    // 不允许全部禁用 — 如果只剩一个, 不允许关
                    Toast.makeText(getContext(), "至少启用一个配置", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position); // 还原
                }
            }

            @Override
            public void onDelete(int position, ApiSourceAdapter.SourceItem item) {
                List<String> list = getHistory();
                list.remove(item.url);
                Hawk.put(type == TYPE_VOD ? HawkConfig.API_HISTORY : HawkConfig.LIVE_API_HISTORY, list);
                // 如果删的是当前启用的, 启用下一个或置空
                String current = getCurrentUrl();
                if (item.url.equals(current)) {
                    String newCurrent = list.isEmpty() ? "" : list.get(0);
                    setCurrentUrl(newCurrent);
                    activateSource(newCurrent);
                }
                loadData();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    public void loadData() {
        List<String> history = getHistory();
        String current = getCurrentUrl();
        List<ApiSourceAdapter.SourceItem> items = new ArrayList<>();
        for (String url : history) {
            items.add(new ApiSourceAdapter.SourceItem(url, extractName(url), url.equals(current)));
        }
        // 当前启用的也加上 (即使不在 history 里)
        if (!current.isEmpty() && !history.contains(current)) {
            items.add(0, new ApiSourceAdapter.SourceItem(current, extractName(current), true));
        }
        adapter.setData(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String getHistory() {
        return Hawk.get(type == TYPE_VOD ? HawkConfig.API_HISTORY : HawkConfig.LIVE_API_HISTORY,
                new ArrayList<String>());
    }

    private String getCurrentUrl() {
        return Hawk.get(type == TYPE_VOD ? HawkConfig.API_URL : HawkConfig.LIVE_API_URL, "");
    }

    private void setCurrentUrl(String url) {
        Hawk.put(type == TYPE_VOD ? HawkConfig.API_URL : HawkConfig.LIVE_API_URL, url);
    }

    private void activateSource(String url) {
        if (url.isEmpty()) return;
        String oldUrl = getCurrentUrl();
        setCurrentUrl(url);
        // 如果点播源变了, 刷新首页
        if (type == TYPE_VOD && !url.equals(oldUrl)) {
            // 同步直播源
            Hawk.put(HawkConfig.LIVE_API_URL, url);
            try {
                ApiConfig.get().setSourceBean(ApiConfig.get().getHomeSourceBean());
            } catch (Exception ignored) {}
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HOME_SOURCE_CHANGE));
        }
        loadData();
    }

    /** 从 URL 提取可读名字 */
    private String extractName(String url) {
        // clan://localhost/Download/xxx.json → xxx
        if (url.startsWith("clan://localhost/")) {
            String path = url.substring("clan://localhost/".length());
            int slash = path.lastIndexOf('/');
            if (slash >= 0) path = path.substring(slash + 1);
            if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
            return path;
        }
        // https://xxx.com/path/config.json → config
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0) {
                String tail = url.substring(lastSlash + 1);
                if (tail.endsWith(".json")) tail = tail.substring(0, tail.length() - 5);
                if (tail.length() > 0 && tail.length() < 30) return tail;
            }
        } catch (Exception ignored) {}
        return url;
    }
}
