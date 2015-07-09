package com.sctek.smartglasses.ui;

import com.sctek.smartglasses.fragments.SettingFragment;
import android.support.v4.app.FragmentActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

public class SettingActivity extends FragmentActivity {

	private String TAG;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new Handler().post(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TAG = SettingFragment.class.getName();
				SettingFragment settingGF = (SettingFragment)getFragmentManager().findFragmentByTag(TAG);
				if(settingGF == null) {
					settingGF = new SettingFragment();
				}
				getFragmentManager().beginTransaction()
						.replace(android.R.id.content, settingGF, TAG).commit();
			}
		});
	}
}
