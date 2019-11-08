package com.hzy.un7zip;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.SnackbarUtils;
import com.hzy.lib7z.Z7Extractor;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    @BindView(R.id.text_7z_version)
    TextView mText7zVersion;
    @BindView(R.id.text_file_path)
    TextView mTextFilePath;
    @BindView(R.id.button_choose_file)
    Button mButtonChooseFile;
    @BindView(R.id.button_extract)
    Button mButtonExtract;
    @BindView(R.id.button_extract_asset)
    Button mButtonExtractAsset;
    @BindView(R.id.text_output_path)
    TextView mTextOutputPath;

    private String mOutputPath;
    private String mInputFilePath;
    private ProgressDialog mProgressDialog;
    private ExecutorService mExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
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
            Uri uri = data.getData();
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor actualisation = managedQuery(uri, projection, null, null, null);
            int actual_image_column_index = actualisation.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            actualisation.moveToFirst();
            mInputFilePath = actualisation.getString(actual_image_column_index);
            showMessage("Choose File:" + mInputFilePath);
            mTextFilePath.setText(mInputFilePath);
        }
    }

    /**
     * start to select some file
     */
    @OnClick(R.id.button_choose_file)
    public void onMButtonChooseFileClicked() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    /**
     * start to extract from selected file
     */
    @OnClick(R.id.button_extract)
    public void onMButtonExtractClicked() {
        if (TextUtils.isEmpty(mInputFilePath)) {
            showMessage("Please Select 7z File First!");
            return;
        }
        doExtractFile();
    }

    /**
     * request for some read storage permission for android 6.0+
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doExtractFile();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    /**
     * extract some files from assets
     */
    @OnClick(R.id.button_extract_asset)
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
