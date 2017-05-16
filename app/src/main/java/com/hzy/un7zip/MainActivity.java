package com.hzy.un7zip;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hzy.lib7z.Un7Zip;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private String filePath;
    private String outPath;
    private TextView textPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "extracted";
        findViewById(R.id.button_choose_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });

        findViewById(R.id.button_extract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(filePath)) {
                    Toast.makeText(MainActivity.this, "Please select 7z file first!", Toast.LENGTH_SHORT).show();
                } else {
                    startExtractFile();
                }
            }
        });

        findViewById(R.id.button_extract_asset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExtractFileFromAssets();
            }
        });

        textPath = (TextView) findViewById(R.id.text_file_path);
    }

    private void startExtractFile() {
        new Thread() {
            @Override
            public void run() {
                Un7Zip.extract7z(filePath, outPath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "extracted to: " + outPath, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }.start();
    }


    private void startExtractFileFromAssets() {
        new Thread() {
            @Override
            public void run() {
                Un7Zip.extract7zFromAssets(MainActivity.this, "TestAsset.7z", outPath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "extracted to: " + outPath, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor actualimagecursor = managedQuery(uri, proj, null, null, null);
            int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            actualimagecursor.moveToFirst();
            filePath = actualimagecursor.getString(actual_image_column_index);
            Toast.makeText(MainActivity.this, "choose file:" + filePath, Toast.LENGTH_SHORT).show();
            textPath.setText(filePath);
        }
    }

}
