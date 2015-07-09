package com.sctek.smartglasses.ui;

import java.util.ArrayList;

import cn.ingenic.glasssync.MediaSyncService;
import cn.ingenic.glasssync.SyncApp;
import com.sctek.smartglasses.fragments.NativeVideoGridFragment;
import com.sctek.smartglasses.utils.MediaData;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

public class VideoActivity extends FragmentActivity {

	private String TAG;
	private MediaSyncService mMediaSyncService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SyncApp.getInstance().addActivity(this);
		new Handler().post(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TAG = NativeVideoGridFragment.class.getName();
				NativeVideoGridFragment VideoGF = (NativeVideoGridFragment)getFragmentManager().findFragmentByTag(TAG);
				if(VideoGF == null) {
					VideoGF = new NativeVideoGridFragment();
					
				}
				Bundle vBundle = new Bundle();
				vBundle.putInt("index", NativeVideoGridFragment.FRAGMENT_INDEX);
				VideoGF.setArguments(vBundle);
				getFragmentManager().beginTransaction()
						.replace(android.R.id.content, VideoGF, TAG).commit();
			}
		});
		
		bindService(new Intent(this, MediaSyncService.class), mConnection, BIND_AUTO_CREATE);
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		int stackCount = getFragmentManager().getBackStackEntryCount();
		if(stackCount != 0) {
			getFragmentManager().popBackStack();
		}
		else 
			super.onBackPressed();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unbindService(mConnection);
		SyncApp.getInstance().removeActivity(this);
		super.onDestroy();
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onServiceConnected");
			mMediaSyncService = ((MediaSyncService.ServiceBinder)service).getService();
		}
	};
	
	public void startVideoSync(ArrayList<MediaData> data)  {
		Log.e(TAG, "startPhotoSync");
		mMediaSyncService.startVideoSync(data);
	}

}
