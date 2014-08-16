package com.hu.andun7z;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class MainActivity extends Activity {

	ProgressDialog dialog = null;
	EditText etFile = null;
	EditText etOut = null;
	Handler handler = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		dialog = new ProgressDialog(this);
		dialog.setCancelable(false);
		
		etFile = (EditText)findViewById(R.id.editText1);
		etOut = (EditText)findViewById(R.id.editText2);
		
		handler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				dialog.dismiss();
				return false;
			}
		});
		
		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.show();
				new Thread(){
					public void run() {
						AndUn7z.extract7z(etFile.getText().toString(), etOut.getText().toString());
						handler.sendEmptyMessage(0);
					};
				}.start();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
