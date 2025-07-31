package com.hzy.un7zip;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.UriUtils;
import com.hzy.lib7z.Z7Extractor;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    TextView mText7zVersion;
    TextView mTextFilePath;
    Button mButtonChooseFile;
    Button mButtonExtract;
    Button mButtonExtractAsset;
    TextView mTextOutputPath;

    private String mOutputPath;
    private String mInputFilePath;
    private ProgressDialog mProgressDialog;
    private ExecutorService mExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mText7zVersion = findViewById(R.id.text_7z_version);
        mTextFilePath = findViewById(R.id.text_file_path);
        mButtonChooseFile = findViewById(R.id.button_choose_file);
        mButtonExtract = findViewById(R.id.button_extract);
        mButtonExtractAsset = findViewById(R.id.button_extract_asset);
        mTextOutputPath = findViewById(R.id.text_output_path);
        mButtonChooseFile.setOnClickListener(v -> onMButtonChooseFileClicked());
        mButtonExtract.setOnClickListener(v -> onMButtonExtractClicked());
        mButtonExtractAsset.setOnClickListener(v -> onMButtonExtractAssetClicked());
        mProgressDialog = new ProgressDialog(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mText7zVersion.setText(Z7Extractor.getLzmaVersion());
        File outFile = getExternalFilesDir("extracted");
        if (outFile == null || !outFile.exists()) {
            outFile = getFilesDir();
        }
        mOutputPath = outFile.getPath();
        mTextOutputPath.setText(mOutputPath);
    }

    @Override
    protected void onDestroy() {
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    private void showMessage(String msg) {
        SnackbarUtils.with(mTextOutputPath).setMessage(msg).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 103) {
                PermissionUtils.permission(PermissionConstants.STORAGE)
                        .callback(new PermissionUtils.SimpleCallback() {
                            @Override
                            public void onGranted() {
                                Uri uri = data.getData();
                                if (uri != null) {
                                    mInputFilePath = UriUtils.uri2File(uri).getPath();
                                    showMessage("Choose File:" + mInputFilePath);
                                    mTextFilePath.setText(mInputFilePath);
                                }
                            }

                            @Override
                            public void onDenied() {

                            }
                        }).request();
            }
        }
    }

    public void onMButtonChooseFileClicked() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 103);
    }

    public void onMButtonExtractClicked() {
        if (TextUtils.isEmpty(mInputFilePath)) {
            showMessage("Please Select 7z File First!");
            return;
        }
        PermissionUtils.permission(PermissionConstants.STORAGE)
                .callback(new PermissionUtils.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        doExtractFile();
                    }

                    @Override
                    public void onDenied() {

                    }
                }).request();
    }

    /**
     * real extract process
     */
    private void doExtractFile() {
        mProgressDialog.show();
        mExecutor.submit(() ->
                Z7Extractor.extractFile(mInputFilePath, mOutputPath, new UnzipCallback() {
                    @Override
                    public void onProgress(String name, long size) {
                        runOnUiThread(() -> mProgressDialog.setMessage("name: "
                                + name + "\nsize: " + size));
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        runOnUiThread(() -> {
                            showMessage(message);
                            mProgressDialog.dismiss();
                        });
                    }

                    @Override
                    public void onSucceed() {
                        runOnUiThread(() -> {
                            showMessage("Succeed!!");
                            mProgressDialog.dismiss();
                        });
                    }
                }));
    }

    public void onMButtonExtractAssetClicked() {
        mProgressDialog.show();
        mExecutor.submit(() ->
                Z7Extractor.extractAsset(getAssets(), "TestAsset.7z",
                        mOutputPath, new UnzipCallback() {
                            @Override
                            public void onProgress(String name, long size) {
                                runOnUiThread(() -> mProgressDialog.setMessage("name: "
                                        + name + "\nsize: " + size));
                            }

                            @Override
                            public void onError(int errorCode, String message) {
                                runOnUiThread(() -> {
                                    showMessage(message);
                                    mProgressDialog.dismiss();
                                });
                            }

                            @Override
                            public void onSucceed() {
                                runOnUiThread(() -> {
                                    showMessage("Succeed!!");
                                    mProgressDialog.dismiss();
                                });
                            }
                        }));
    }
}
