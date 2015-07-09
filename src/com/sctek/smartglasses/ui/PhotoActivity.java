package com.sctek.smartglasses.ui;

import java.util.ArrayList;

import cn.ingenic.glasssync.MediaSyncService;

import com.sctek.smartglasses.fragments.NativePhotoGridFragment;
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

public class PhotoActivity extends FragmentActivity {

	private String TAG = "PhotoActivity";
	private MediaSyncService mMediaSyncService;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_photo);
		new Handler().post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TAG = NativePhotoGridFragment.class.getName();
				NativePhotoGridFragment PhotoGF = (NativePhotoGridFragment)getFragmentManager().findFragmentByTag(TAG);
				if(PhotoGF == null)
					PhotoGF = new NativePhotoGridFragment();
				
				Bundle pBundle = new Bundle();
				pBundle.putInt("index", NativePhotoGridFragment.FRAGMENT_INDEX);
				PhotoGF.setArguments(pBundle);
				
				getFragmentManager().beginTransaction().replace(android.R.id.content, 
						PhotoGF, TAG).commit();
			}
		});
		
		bindService(new Intent(this, MediaSyncService.class), mConnection, BIND_AUTO_CREATE);
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
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
	
	public void startPhotoSync(ArrayList<MediaData> data)  {
		Log.e(TAG, "startPhotoSync");
		mMediaSyncService.startPhotoSync(data);
	}
}
