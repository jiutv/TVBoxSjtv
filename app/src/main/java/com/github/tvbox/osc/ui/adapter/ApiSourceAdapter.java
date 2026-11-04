package com.github.tvbox.osc.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理列表适配器 (图2里的配置列表)
 */
public class ApiSourceAdapter extends RecyclerView.Adapter<ApiSourceAdapter.VH> {

    public static class SourceItem {
        public String url;
        public String name;
        public boolean enabled; // 当前启用的

        public SourceItem(String url, String name, boolean enabled) {
            this.url = url;
            this.name = name;
            this.enabled = enabled;
        }

        /** 显示的文本: 有名字优先名字, 否则 URL */
        public String getDisplayText() {
            return (name != null && !name.isEmpty()) ? name : url;
        }
    }

    private List<SourceItem> items = new ArrayList<>();
    private OnSourceListener listener;

    public interface OnSourceListener {
        void onToggle(int position, SourceItem item, boolean enabled);
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
        h.tvUrl.setText(item.getDisplayText());
        h.switchEnabled.setOnCheckedChangeListener(null); // 先清掉
        h.switchEnabled.setChecked(item.enabled);
        h.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onToggle(h.getAdapterPosition(), item, checked);
        });

        // 长按删除
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDelete(h.getAdapterPosition(), item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvUrl;
        Switch switchEnabled;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvUrl = itemView.findViewById(R.id.tvUrl);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
        }
    }
}
