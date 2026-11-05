package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.ui.dialog.AddSourceDialog;
import com.github.tvbox.osc.ui.fragment.ApiListFragment;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

/**
 * 订阅管理页 (图1/图2)
 * 顶栏: ← 返回  订阅管理  📺  📋  ＋
 * Tab: 点播订阅 / 直播订阅 (带下划线指示器)
 * 点 ＋ → AddSourceDialog (图3 添加订阅)
 */
public class ApiConfigActivity extends BaseActivity {

    private ViewPager viewPager;
    private ApiListFragment vodFragment;
    private ApiListFragment liveFragment;
    private TextView tabVod, tabLive;
    private View tabVodIndicator, tabLiveIndicator;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_api_config;
    }

    @Override
    protected void init() {
        viewPager = findViewById(R.id.viewPager);
        TextView btnBack = findViewById(R.id.btnBack);
        TextView btnAdd = findViewById(R.id.btnAdd);
        tabVod = findViewById(R.id.tabVod);
        tabLive = findViewById(R.id.tabLive);
        tabVodIndicator = findViewById(R.id.tabVodIndicator);
        tabLiveIndicator = findViewById(R.id.tabLiveIndicator);

        vodFragment = ApiListFragment.newInstance(ApiListFragment.TYPE_VOD);
        liveFragment = ApiListFragment.newInstance(ApiListFragment.TYPE_LIVE);

        Fragment[] fragments = {vodFragment, liveFragment};

        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager(), 0) {
            @NonNull @Override public Fragment getItem(int position) { return fragments[position]; }
            @Override public int getCount() { return fragments.length; }
        });

        // 下划线 Tab 切换
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                updateTabUI(position);
            }
        });
        tabVod.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        tabLive.setOnClickListener(v -> viewPager.setCurrentItem(1, true));

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showAddDialog());

        updateTabUI(0);
    }

    private void updateTabUI(int selected) {
        int activeColor = 0xFFD4A04C;   // 金色 (图1图2的下划线色)
        int inactiveColor = 0xFF888888;

        if (selected == 0) {
            tabVod.setTextColor(activeColor);
            tabVod.setTypeface(null, android.graphics.Typeface.BOLD);
            tabLive.setTextColor(inactiveColor);
            tabLive.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabVodIndicator.setBackgroundColor(activeColor);
            tabLiveIndicator.setBackgroundColor(0x00000000);
        } else {
            tabLive.setTextColor(activeColor);
            tabLive.setTypeface(null, android.graphics.Typeface.BOLD);
            tabVod.setTextColor(inactiveColor);
            tabVod.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabLiveIndicator.setBackgroundColor(activeColor);
            tabVodIndicator.setBackgroundColor(0x00000000);
        }
    }

    private void showAddDialog() {
        final int currentTab = viewPager.getCurrentItem();
        AddSourceDialog dialog = new AddSourceDialog(this);
        dialog.setOnSaveListener((name, url, enableNow) -> {
            List<String> list = Hawk.get(currentTab == 0 ? HawkConfig.API_HISTORY : HawkConfig.LIVE_API_HISTORY, new ArrayList<String>());
            if (!list.contains(url)) list.add(0, url);
            if (currentTab == 0) {
                Hawk.put(HawkConfig.API_HISTORY, list);
                if (enableNow) {
                    HistoryHelper.setApiHistory(url);
                    Hawk.put(HawkConfig.API_URL, url);
                    Hawk.put(HawkConfig.LIVE_API_URL, url);
                }
            } else {
                Hawk.put(HawkConfig.LIVE_API_HISTORY, list);
                if (enableNow) {
                    Hawk.put(HawkConfig.LIVE_API_URL, url);
                }
            }
            if (enableNow) Toast.makeText(this, "已添加并启用", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "已添加到列表", Toast.LENGTH_SHORT).show();
            vodFragment.loadData();
            liveFragment.loadData();
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == AddSourceDialog.REQUEST_PICK_FILE && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                String path = copyUriToLocal(uri);
                if (path != null) {
                    String absPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                    String clanUrl = "clan://localhost/" + path.replaceFirst("^" + absPath + "/", "");
                    int tab = viewPager.getCurrentItem();
                    List<String> list = Hawk.get(tab == 0 ? HawkConfig.API_HISTORY : HawkConfig.LIVE_API_HISTORY, new ArrayList<String>());
                    if (!list.contains(clanUrl)) list.add(0, clanUrl);
                    if (tab == 0) {
                        Hawk.put(HawkConfig.API_HISTORY, list);
                        Hawk.put(HawkConfig.API_URL, clanUrl);
                        Hawk.put(HawkConfig.LIVE_API_URL, clanUrl);
                    } else {
                        Hawk.put(HawkConfig.LIVE_API_HISTORY, list);
                        Hawk.put(HawkConfig.LIVE_API_URL, clanUrl);
                    }
                    Toast.makeText(this, "已添加本地配置", Toast.LENGTH_SHORT).show();
                    vodFragment.loadData();
                    liveFragment.loadData();
                }
            } catch (Exception e) {
                Toast.makeText(this, "读取文件失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String copyUriToLocal(Uri uri) {
        try {
            java.io.InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return null;
            java.io.File dir = new java.io.File(getCacheDir(), "config");
            if (!dir.exists()) dir.mkdirs();
            String name = "local_config_" + System.currentTimeMillis() + ".json";
            java.io.File outFile = new java.io.File(dir, name);
            java.io.FileOutputStream output = new java.io.FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = input.read(buf)) != -1) output.write(buf, 0, len);
            output.close();
            input.close();
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
