package com.sctek.smartglasses.ui;

import cn.ingenic.glasssync.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;

public class BaseActivity extends ActionBarActivity {
	
	private static final String TAG = "BaseActivity";
	private SideNavigationView mSideNavigationView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		mSideNavigationView = new SideNavigationView(this);
		mSideNavigationView.setMenuItems(R.menu.side_navigation_menu);
		mSideNavigationView.setMenuClickCallback(new SideNavigationCallback());
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT
				, LayoutParams.MATCH_PARENT);
		addContentView(mSideNavigationView, params);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case android.R.id.home:
			mSideNavigationView.toggleMenu();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	public class SideNavigationCallback implements ISideNavigationCallback {

		@Override
		public void onSideNavigationItemClick(int itemId) {
			// TODO Auto-generated method stub
			switch (itemId) {
			case R.id.photo_item:
				Intent intent = new Intent(BaseActivity.this, MainActivity.class);
				startActivity(intent);
				finish();
				break;
			case R.id.video_item:
				Log.e(TAG, "video");
				break;
			case R.id.setting_item:
				Log.e(TAG, "setting");
				break;
			case R.id.about_item:
				Log.e(TAG, "about");
				break;
			default:
				break;
			}
		}
	}

}
