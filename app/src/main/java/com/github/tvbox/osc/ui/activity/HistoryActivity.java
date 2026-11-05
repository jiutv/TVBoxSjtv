package com.github.tvbox.osc.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.ui.dialog.ConfirmClearDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {
    private ImageView tvDelete;
    private ImageView tvClear;
    private TextView tvDelTip;
    private TvRecyclerView mGridView;
    public static HistoryAdapter historyAdapter;
    public static CollectAdapter collectAdapter;
    private boolean delMode = false;

    // 手机版 Tab 切换相关
    private boolean isPhoneMode;
    private TextView tabHistory;
    private TextView tabCollect;
    private View tabIndicator;
    private boolean showCollect = false; // false=历史, true=收藏

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_history;
    }

    @Override
    protected boolean shouldRefreshAutoSize() {
        return true;
    }

    @Override
    protected void init() {
        isPhoneMode = getResources().getBoolean(R.bool.is_phone);
        initView();
        initData();
    }

    private void toggleDelMode() {
        HawkConfig.hotVodDelete = !HawkConfig.hotVodDelete;
        if (showCollect && collectAdapter != null) collectAdapter.notifyDataSetChanged();
        else if (!showCollect && historyAdapter != null) historyAdapter.notifyDataSetChanged();
        delMode = !delMode;
        tvDelTip.setVisibility(delMode ? View.VISIBLE : View.GONE);
    }

    private void switchTab(boolean toCollect) {
        showCollect = toCollect;
        if (isPhoneMode) {
            if (showCollect) {
                tabCollect.setTextColor(0xFFFFFFFF);
                tabCollect.setTypeface(null, android.graphics.Typeface.BOLD);
                tabHistory.setTextColor(0x99FFFFFF);
                tabHistory.setTypeface(null, android.graphics.Typeface.NORMAL);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabIndicator.getLayoutParams();
                lp.leftMargin = tabHistory.getWidth(); // 约一半位置
                lp.width = tabCollect.getWidth();
                tabIndicator.setLayoutParams(lp);
            } else {
                tabHistory.setTextColor(0xFFFFFFFF);
                tabHistory.setTypeface(null, android.graphics.Typeface.BOLD);
                tabCollect.setTextColor(0x99FFFFFF);
                tabCollect.setTypeface(null, android.graphics.Typeface.NORMAL);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabIndicator.getLayoutParams();
                lp.leftMargin = 0;
                lp.width = tabHistory.getWidth();
                tabIndicator.setLayoutParams(lp);
            }
        }
        initData();
    }

    private void initView() {
        EventBus.getDefault().register(this);
        tvDelete = findViewById(R.id.tvDelete);
        tvClear = findViewById(R.id.tvClear);
        tvDelTip = findViewById(R.id.tvDelTip);
        mGridView = findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);

        int spanCount = isPhoneMode ? 3 : (isBaseOnWidth() ? 5 : 6);
        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, spanCount));

        historyAdapter = new HistoryAdapter();
        collectAdapter = new CollectAdapter();

        mGridView.setAdapter(isPhoneMode && showCollect ? collectAdapter : historyAdapter);

        if (isPhoneMode) {
            tabHistory = findViewById(R.id.tabHistory);
            tabCollect = findViewById(R.id.tabCollect);
            tabIndicator = findViewById(R.id.tabIndicator);
            if (tabHistory != null) {
                tabHistory.setOnClickListener(v -> switchTab(false));
                tabCollect.setOnClickListener(v -> switchTab(true));
            }
        }

        tvDelete.setOnClickListener(v -> toggleDelMode());
        tvClear.setOnClickListener(v -> {
            if (isPhoneMode && showCollect) {
                ConfirmClearDialog dialog = new ConfirmClearDialog(mContext, "Collect");
                dialog.show();
            } else {
                ConfirmClearDialog dialog = new ConfirmClearDialog(mContext, "History");
                dialog.show();
            }
        });

        mGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            public boolean onInBorderKeyEvent(int direction, View focused) {
                if (direction == View.FOCUS_UP) {
                    tvDelete.setFocusable(true);
                    tvClear.setFocusable(true);
                    tvDelete.requestFocus();
                }
                return false;
            }
        });

        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {}
        });

        // 历史列表点击
        historyAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodInfo vodInfo = historyAdapter.getData().get(position);
            if (vodInfo == null) return;
            if (delMode) {
                historyAdapter.remove(position);
                RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
            } else {
                openDetail(vodInfo.id, vodInfo.sourceKey, vodInfo.name, vodInfo.pic);
            }
        });

        historyAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodInfo vodInfo = historyAdapter.getData().get(position);
            if (vodInfo != null) {
                Bundle bundle = new Bundle();
                bundle.putString("title", vodInfo.name);
                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                    jumpActivity(FastSearchActivity.class, bundle);
                } else {
                    jumpActivity(SearchActivity.class, bundle);
                }
            }
            return true;
        });

        // 收藏列表点击
        collectAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodCollect vodCollect = collectAdapter.getData().get(position);
            if (vodCollect == null) return;
            if (delMode) {
                collectAdapter.remove(position);
                RoomDataManger.deleteVodCollect(vodCollect.getId());
            } else {
                openDetail(vodCollect.vodId, vodCollect.sourceKey, vodCollect.name, vodCollect.pic);
            }
        });

        collectAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodCollect vodCollect = collectAdapter.getData().get(position);
            if (vodCollect != null) {
                Bundle bundle = new Bundle();
                bundle.putString("title", vodCollect.name);
                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                    jumpActivity(FastSearchActivity.class, bundle);
                } else {
                    jumpActivity(SearchActivity.class, bundle);
                }
            }
            return true;
        });
    }

    private void openDetail(String id, String sourceKey, String name, String pic) {
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        bundle.putString("sourceKey", sourceKey);
        SourceBean sourceBean = ApiConfig.get().getSource(sourceKey);
        if (sourceBean != null) {
            bundle.putString("title", name);
            bundle.putString("picture", pic);
            jumpActivity(DetailActivity.class, bundle);
        } else {
            bundle.putString("title", name);
            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                jumpActivity(FastSearchActivity.class, bundle);
            } else {
                jumpActivity(SearchActivity.class, bundle);
            }
        }
    }

    private void initData() {
        if (isPhoneMode && showCollect) {
            // 收藏数据
            List<VodCollect> list = RoomDataManger.getAllVodCollect();
            collectAdapter.setNewData(list);
            mGridView.setAdapter(collectAdapter);
        } else {
            // 历史数据
            List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(100);
            List<VodInfo> vodInfoList = new ArrayList<>();
            for (VodInfo vodInfo : allVodRecord) {
                if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty())
                    vodInfo.note = "上次看到" + vodInfo.playNote;
                vodInfoList.add(vodInfo);
            }
            historyAdapter.setNewData(vodInfoList);
            mGridView.setAdapter(historyAdapter);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            initData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (delMode) {
            toggleDelMode();
            return;
        }
        super.onBackPressed();
    }
}
