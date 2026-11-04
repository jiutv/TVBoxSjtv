package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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
 * 配置管理页 (图2)
 * TabLayout + ViewPager: 点播 / 直播
 * 右上角 + → 添加订阅对话框 (图3)
 */
public class ApiConfigActivity extends BaseActivity {

    private ViewPager viewPager;
    private ApiListFragment vodFragment;
    private ApiListFragment liveFragment;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_api_config;
    }

    @Override
    protected void init() {
        viewPager = findViewById(R.id.viewPager);
        TextView btnBack = findViewById(R.id.btnBack);
        TextView btnAdd = findViewById(R.id.btnAdd);
        TextView tabVod = findViewById(R.id.tabVod);
        TextView tabLive = findViewById(R.id.tabLive);

        vodFragment = ApiListFragment.newInstance(ApiListFragment.TYPE_VOD);
        liveFragment = ApiListFragment.newInstance(ApiListFragment.TYPE_LIVE);

        Fragment[] fragments = {vodFragment, liveFragment};

        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager(), 0) {
            @NonNull @Override public Fragment getItem(int position) { return fragments[position]; }
            @Override public int getCount() { return fragments.length; }
        });

        // 手动 Tab 切换 (不用 Material TabLayout, 项目没这个依赖)
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                updateTabStyle(tabVod, tabLive, position);
            }
        });
        tabVod.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        tabLive.setOnClickListener(v -> viewPager.setCurrentItem(1, true));

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showAddDialog());
    }

    private void updateTabStyle(TextView tabVod, TextView tabLive, int selected) {
        if (selected == 0) {
            tabVod.setTextColor(0xFFFFFFFF);
            tabVod.setTypeface(null, android.graphics.Typeface.BOLD);
            tabLive.setTextColor(0xFF888888);
            tabLive.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabLive.setTextColor(0xFFFFFFFF);
            tabLive.setTypeface(null, android.graphics.Typeface.BOLD);
            tabVod.setTextColor(0xFF888888);
            tabVod.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void showAddDialog() {
        final int currentTab = viewPager.getCurrentItem();
        AddSourceDialog dialog = new AddSourceDialog(this);
        dialog.setOnSaveListener((name, url) -> {
            List<String> list = Hawk.get(currentTab == 0 ? HawkConfig.API_HISTORY : HawkConfig.LIVE_API_HISTORY, new ArrayList<String>());
            if (!list.contains(url)) list.add(0, url); // 新的放最前
            if (currentTab == 0) {
                Hawk.put(HawkConfig.API_HISTORY, list);
                HistoryHelper.setApiHistory(url);
            } else {
                Hawk.put(HawkConfig.LIVE_API_HISTORY, list);
                HistoryHelper.setLiveApiHistory(url);
            }
            // 自动启用新添加的
            if (currentTab == 0) {
                Hawk.put(HawkConfig.API_URL, url);
                Hawk.put(HawkConfig.LIVE_API_URL, url);
            } else {
                Hawk.put(HawkConfig.LIVE_API_URL, url);
            }
            Toast.makeText(this, "已添加并启用", Toast.LENGTH_SHORT).show();
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
