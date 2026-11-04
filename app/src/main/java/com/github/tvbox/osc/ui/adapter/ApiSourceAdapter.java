package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置列表适配器 — 订阅管理页的列表 (匹配图1图2图3样式)
 * 激活项: 青绿色卡片 + 绿色 ✓ 勾选圈
 * 未激活项: 深灰卡片 + 灰色 ○ 单选圈
 * 右侧: 灰色 ✕ 删除按钮
 */
public class ApiSourceAdapter extends RecyclerView.Adapter<ApiSourceAdapter.VH> {

    public static class SourceItem {
        public String url;
        public String name;
        public String sourceTag; // "来自: 订阅: 1"
        public boolean enabled;

        public SourceItem(String url, String name, boolean enabled) {
            this.url = url;
            this.name = name;
            this.enabled = enabled;
        }
    }

    private List<SourceItem> items = new ArrayList<>();
    private OnSourceListener listener;

    public interface OnSourceListener {
        void onActivate(int position, SourceItem item);
        void onDelete(int position, SourceItem item);
    }

    public void setListener(OnSourceListener l) { this.listener = l; }

    public void setData(List<SourceItem> data) {
        this.items = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_api_source, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SourceItem item = items.get(pos);

        // 卡片背景 (激活=青底, 未激活=深灰)
        h.itemView.setSelected(item.enabled);

        // 左侧图标: 激活=绿色✓填充圈, 未激活=空心○圈
        if (item.enabled) {
            h.tvIcon.setText("✓");
            h.tvIcon.setTextColor(Color.WHITE);
            h.tvIcon.setBackgroundResource(R.drawable.bg_source_active_circle);
            h.tvName.setTextColor(Color.WHITE);
        } else {
            h.tvIcon.setText("");
            h.tvIcon.setTextColor(Color.GRAY);
            h.tvIcon.setBackgroundResource(R.drawable.bg_source_inactive_circle);
            h.tvName.setTextColor(Color.parseColor("#E0E0E0"));
        }

        // 名字: 优先自定义名字, 否则自动提取
        String displayName = item.name != null && !item.name.isEmpty()
                ? item.name : extractSimpleName(item.url);
        h.tvName.setText(displayName);

        // URL
        h.tvUrl.setText(item.url);

        // 来源标签 (图3里的 "来自: 订阅: 1")
        if (item.sourceTag != null && !item.sourceTag.isEmpty()) {
            h.tvSourceTag.setText(item.sourceTag);
            h.tvSourceTag.setVisibility(View.VISIBLE);
        } else {
            h.tvSourceTag.setVisibility(View.GONE);
        }

        // 点击卡片 = 激活
        h.itemView.setOnClickListener(v -> {
            if (listener != null && !item.enabled) {
                listener.onActivate(h.getAdapterPosition(), item);
            }
        });

        // X 按钮 = 删除
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(h.getAdapterPosition(), item);
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvUrl, tvSourceTag, btnDelete;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvUrl = itemView.findViewById(R.id.tvUrl);
            tvSourceTag = itemView.findViewById(R.id.tvSourceTag);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private static String extractSimpleName(String url) {
        if (url == null) return "";
        // clan://localhost/Download/xxx.json → xxx
        if (url.startsWith("clan://localhost/")) {
            String p = url.substring("clan://localhost/".length());
            int s = p.lastIndexOf('/');
            if (s >= 0) p = p.substring(s + 1);
            if (p.endsWith(".json")) p = p.substring(0, p.length() - 5);
            if (!p.isEmpty() && p.length() < 30) return p;
        }
        // https://.../xxx.txt → xxx
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
