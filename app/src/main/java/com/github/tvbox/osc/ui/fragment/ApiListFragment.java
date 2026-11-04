package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
 * 配置列表 Fragment — 每个 Tab 的内容 (点播 or 直播)
 */
public class ApiListFragment extends Fragment {

    public static final int TYPE_VOD = 0;
    public static final int TYPE_LIVE = 1;

    private int type;
    private RecyclerView recyclerView;
    private View emptyView;
    private ApiSourceAdapter adapter;

    public static ApiListFragment newInstance(int type) {
        ApiListFragment f = new ApiListFragment();
        Bundle args = new Bundle();
        args.putInt("type", type);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) type = getArguments().getInt("type", TYPE_VOD);
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
            public void onActivate(int position, ApiSourceAdapter.SourceItem item) {
                activateSource(item.url);
            }

            @Override
            public void onDelete(int position, ApiSourceAdapter.SourceItem item) {
                // 防止删光
                List<ApiSourceAdapter.SourceItem> current = new ArrayList<>(adapter.getItems());
                long enabledCount = current.stream().filter(i -> i.enabled).count();
                if (item.enabled && enabledCount <= 1) {
                    Toast.makeText(getContext(), "至少保留一个配置", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> list = getHistory();
                list.remove(item.url);
                Hawk.put(type == TYPE_VOD ? HawkConfig.API_HISTORY : HawkConfig.LIVE_API_HISTORY, list);
                String currentUrl = getCurrentUrl();
                if (item.url.equals(currentUrl)) {
                    // 删的是当前启用的 — 自动启用下一个或置空
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

    private List<String> getHistory() {
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
        // 如果点播源变了, 同步直播源 + 刷新首页
        if (type == TYPE_VOD && !url.equals(oldUrl)) {
            Hawk.put(HawkConfig.LIVE_API_URL, url);
            if (!HistoryHelper.isApiLineHistory(url)) {
                HistoryHelper.clearApiLineList();
            }
            try {
                ApiConfig.get().setSourceBean(ApiConfig.get().getHomeSourceBean());
            } catch (Exception ignored) {}
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HOME_SOURCE_CHANGE));
        }
        loadData();
    }

    private String extractName(String url) {
        if (url == null) return "";
        // clan://localhost/xxx → xxx
        if (url.startsWith("clan://localhost/")) {
            String p = url.substring("clan://localhost/".length());
            int s = p.lastIndexOf('/');
            if (s >= 0) p = p.substring(s + 1);
            if (p.endsWith(".json")) p = p.substring(0, p.length() - 5);
            if (!p.isEmpty() && p.length() < 30) return p;
        }
        try {
            int l = url.lastIndexOf('/');
            if (l > 0) {
                String tail = url.substring(l + 1);
                int dot = tail.lastIndexOf('.');
                if (dot > 0) tail = tail.substring(0, dot);
                if (!tail.isEmpty() && tail.length() < 25) return tail;
            }
        } catch (Exception ignored) {}
        return url.length() > 28 ? url.substring(0, 28) + "…" : url;
    }
}
