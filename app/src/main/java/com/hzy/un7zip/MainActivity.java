package com.hzy.un7zip;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hzy.lib7z.IExtractCallback;
import com.hzy.lib7z.Z7Extractor;
import com.hzy.un7zip.event.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mText7zVersion.setText(Z7Extractor.getLzmaVersion());
        File outFile = getExternalFilesDir("extracted");
        if (outFile == null || !outFile.exists()) {
            outFile = getFilesDir();
        }
        mOutputPath = outFile.getPath();
        mTextOutputPath.setText(mOutputPath);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor actualisation = managedQuery(uri, projection, null, null, null);
            int actual_image_column_index = actualisation.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            actualisation.moveToFirst();
            mInputFilePath = actualisation.getString(actual_image_column_index);
            Toast.makeText(MainActivity.this,
                    "Choose File:" + mInputFilePath, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MainActivity.this, "Please Select 7z File First!",
                    Toast.LENGTH_SHORT).show();
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnzipEvent(MessageEvent event) {
        if (event.type == MessageEvent.DISMISS_MSG) {
            mProgressDialog.cancel();
        } else if (event.type == MessageEvent.SHOW_MSG) {
            mProgressDialog.setMessage(event.message);
        }
    }

    /**
     * real extract process
     */
    private void doExtractFile() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.show();
        new Thread() {
            @Override
            public void run() {
                Z7Extractor.extractFile(mInputFilePath, mOutputPath,
                        new IExtractCallback() {
                            @Override
                            public void onStart() {
                            }

                            @Override
                            public void onGetFileNum(int fileNum) {
                            }

                            @Override
                            public void onProgress(String name, long size) {
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.SHOW_MSG,
                                        "name: " + name + "\nsize: " + size));
                            }

                            @Override
                            public void onError(int errorCode, String message) {
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.DISMISS_MSG));
                            }

                            @Override
                            public void onSucceed() {
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.DISMISS_MSG));
                            }
                        });
            }
        }.start();
    }

    /**
     * extract some files from assets
     */
    @OnClick(R.id.button_extract_asset)
    public void onMButtonExtractAssetClicked() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.show();
        new Thread() {
            @Override
            public void run() {
                Z7Extractor.extractAsset(getAssets(), "TestAsset.7z", mOutputPath,
                        new IExtractCallback() {
                            @Override
                            public void onStart() {
                            }

                            @Override
                            public void onGetFileNum(int fileNum) {
                            }

                            @Override
                            public void onProgress(String name, long size) {
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.SHOW_MSG,
                                        "name: " + name + "\nsize: " + size));
                            }

                            @Override
                            public void onError(int errorCode, String message) {
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.DISMISS_MSG));
                            }

                            @Override
                            public void onSucceed() {
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.DISMISS_MSG));
                            }
                        });
            }
        }.start();
    }
}
