package com.hu.andun7z;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.ExFilePickerActivity;
import ru.bartwell.exfilepicker.ExFilePickerParcelObject;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	ProgressDialog dialog = null;
	EditText etFile = null;
	EditText etOut = null;
	Handler handler = null;
	Button btnSrc, btnDst, btnExecute;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dialog = new ProgressDialog(this);
		dialog.setCancelable(false);
		dialog.setTitle(R.string.dialog_title);
		dialog.setMessage(getText(R.string.dialog_content));
		
		etFile = (EditText)findViewById(R.id.editText1);
		etOut = (EditText)findViewById(R.id.editText2);
		btnSrc = (Button) findViewById(R.id.button_src_file);
		btnDst = (Button) findViewById(R.id.button_out_path);
		btnExecute = (Button) findViewById(R.id.button1);
		
		handler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				dialog.dismiss();
				return false;
			}
		});
		
		btnSrc.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, ExFilePickerActivity.class);
				intent.putExtra(ExFilePicker.SET_ONLY_ONE_ITEM, true);
				intent.putExtra(ExFilePicker.SET_FILTER_LISTED, new String[] { "7z"});
				intent.putExtra(ExFilePicker.DISABLE_NEW_FOLDER_BUTTON, true);
				startActivityForResult(intent, 0);
			}
		});
		
		btnDst.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, ExFilePickerActivity.class);
				intent.putExtra(ExFilePicker.SET_ONLY_ONE_ITEM, true);
				intent.putExtra(ExFilePicker.SET_CHOICE_TYPE, ExFilePicker.CHOICE_TYPE_DIRECTORIES);
				startActivityForResult(intent, 1);
			}
		});
		
		btnExecute.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.show();
				new Thread(){
					public void run() {
						AndUn7z.extract7z(etFile.getText().toString(), etOut.getText().toString());
						//AndUn7z.extractAssets(MainActivity.this, etFile.getText().toString(), etOut.getText().toString());
						handler.sendEmptyMessage(0);
					};
				}.start();
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(data == null){
			return;
		}
		ExFilePickerParcelObject object = (ExFilePickerParcelObject) data.getParcelableExtra(ExFilePickerParcelObject.class.getCanonicalName());
		if(object.count != 1){
			return;
		}
		String text = object.path + object.names.get(0);
		if(requestCode == 0){
			etFile.setText(text);
			etFile.setSelection(text.length());
		}else if(requestCode == 1){
			etOut.setText(text);
			etOut.setSelection(text.length());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
