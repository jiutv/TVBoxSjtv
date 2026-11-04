package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.ComponentActivity;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.FileUtils;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 添加订阅对话框 (图3)
 * 可输入名字+链接, 或选本地文件
 */
public class AddSourceDialog extends BaseDialog {

    public static final int REQUEST_PICK_FILE = 10001;

    private EditText inputName;
    private EditText inputUrl;
    private OnSaveListener listener;

    public interface OnSaveListener {
        void onSave(String name, String url);
    }

    public AddSourceDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_add_source);
        setCanceledOnTouchOutside(true);

        inputName = findViewById(R.id.inputName);
        inputUrl = findViewById(R.id.inputUrl);
        TextView btnSave = findViewById(R.id.btnSave);
        TextView btnPickFile = findViewById(R.id.btnPickFile);

        btnSave.setOnClickListener(v -> save());

        btnPickFile.setOnClickListener(v -> {
            if (getContext() instanceof ComponentActivity) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/json");
                ((ComponentActivity) getContext()).startActivityForResult(intent, REQUEST_PICK_FILE);
            } else {
                Toast.makeText(getContext(), "无法打开文件选择器", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void save() {
        String name = inputName.getText().toString().trim();
        String url = inputUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "请输入链接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (listener != null) {
            listener.onSave(name, url);
        }
        dismiss();
    }

    public void setOnSaveListener(OnSaveListener l) {
        this.listener = l;
    }

    /** 从文件选择器拿到结果后回填 URL */
    public void onFilePicked(Uri uri) {
        String api = null;
        try {
            api = copyUriToLocalConfig(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (api != null && !api.isEmpty()) {
            inputUrl.setText("clan://localhost/" + api.replaceFirst("^" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/", ""));
            Toast.makeText(getContext(), "已选择本地配置", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "读取文件失败", Toast.LENGTH_SHORT).show();
        }
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
