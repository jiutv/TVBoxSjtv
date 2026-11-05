package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.ComponentActivity;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 添加订阅对话框 (图2样式)
 * 白色圆角卡片 + Tab (添加订阅 / 本地导入) + "使用此订阅"勾选 + 取消/添加按钮
 */
public class AddSourceDialog extends BaseDialog {

    public static final int REQUEST_PICK_FILE = 10001;

    private EditText inputName;
    private EditText inputUrl;
    private CheckBox cbUse;
    private OnSaveListener listener;

    public interface OnSaveListener {
        void onSave(String name, String url, boolean enableNow);
    }

    public AddSourceDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_add_source);
        setCanceledOnTouchOutside(true);

        inputName = findViewById(R.id.inputName);
        inputUrl = findViewById(R.id.inputUrl);
        cbUse = findViewById(R.id.cbUse);
        TextView btnAdd = findViewById(R.id.btnAdd);
        TextView btnCancel = findViewById(R.id.btnCancel);
        TextView tabOnline = findViewById(R.id.tabOnline);
        TextView tabLocal = findViewById(R.id.tabLocal);

        // 默认选中"使用此订阅"
        cbUse.setChecked(true);

        btnAdd.setOnClickListener(v -> save());
        btnCancel.setOnClickListener(v -> dismiss());

        // Tab 切换: 本地导入 = 调文件选择器
        tabOnline.setOnClickListener(v -> {
            tabOnline.setTextColor(0xFF333333);
            tabOnline.setTypeface(null, android.graphics.Typeface.BOLD);
            tabLocal.setTextColor(0xFF999999);
            tabLocal.setTypeface(null, android.graphics.Typeface.NORMAL);
        });
        tabLocal.setOnClickListener(v -> {
            tabLocal.setTextColor(0xFF333333);
            tabLocal.setTypeface(null, android.graphics.Typeface.BOLD);
            tabOnline.setTextColor(0xFF999999);
            tabOnline.setTypeface(null, android.graphics.Typeface.NORMAL);
            pickLocalFile();
        });
    }

    private void pickLocalFile() {
        if (getContext() instanceof ComponentActivity) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json,*/*");
            ((ComponentActivity) getContext()).startActivityForResult(intent, REQUEST_PICK_FILE);
        } else {
            Toast.makeText(getContext(), "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    private void save() {
        String name = inputName.getText().toString().trim();
        String url = inputUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "请输入地址", Toast.LENGTH_SHORT).show();
            return;
        }
        // 如果名字空, 自动从 URL 提取
        if (TextUtils.isEmpty(name)) {
            name = extractNameFromUrl(url);
        }
        if (listener != null) {
            listener.onSave(name, url, cbUse.isChecked());
        }
        dismiss();
    }

    public void setOnSaveListener(OnSaveListener l) {
        this.listener = l;
    }

    /** 外部拿到文件后回填 URL + 自动提取名字 */
    public void onFilePicked(Uri uri) {
        String localPath = null;
        try {
            localPath = copyUriToLocalConfig(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (localPath != null && !localPath.isEmpty()) {
            String clanUrl = "clan://localhost/" + localPath.replaceFirst("^" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "");
            inputUrl.setText(clanUrl);
            // 自动填名字
            String autoName = extractNameFromUrl(clanUrl);
            if (TextUtils.isEmpty(inputName.getText().toString())) {
                inputName.setText(autoName);
            }
            Toast.makeText(getContext(), "已选择本地配置", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "读取文件失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static String extractNameFromUrl(String url) {
        if (url == null) return "";
        try {
            // clan://localhost/Download/xxx.json → xxx
            if (url.startsWith("clan://localhost/")) {
                String p = url.substring("clan://localhost/".length());
                int s = p.lastIndexOf('/');
                if (s >= 0) p = p.substring(s + 1);
                if (p.endsWith(".json")) p = p.substring(0, p.length() - 5);
                if (!p.isEmpty() && p.length() < 25) return p;
            }
            // https://.../xxx.txt → xxx
            int l = url.lastIndexOf('/');
            if (l > 0) {
                String tail = url.substring(l + 1);
                int dot = tail.lastIndexOf('.');
                if (dot > 0) tail = tail.substring(0, dot);
                if (!tail.isEmpty() && tail.length() < 25) return tail;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String copyUriToLocalConfig(Uri uri) {
        try {
            InputStream input = getContext().getContentResolver().openInputStream(uri);
            if (input == null) return null;
            File dir = new File(FileUtils.getExternalCachePath(), "config");
            if (!dir.exists()) dir.mkdirs();
            String name = "local_config_" + System.currentTimeMillis() + ".json";
            File outFile = new File(dir, name);
            FileOutputStream output = new FileOutputStream(outFile);
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
