package com.sctek.smartglasses.ui;

import java.io.File;
import java.util.ArrayList;

import cn.ingenic.glasssync.MediaSyncService;
import cn.ingenic.glasssync.SyncApp;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.sctek.smartglasses.fragments.NativePhotoGridFragment;
import com.sctek.smartglasses.utils.MediaData;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

public class PhotoActivity extends FragmentActivity {

	private String TAG = "PhotoActivity";
	private MediaSyncService mMediaSyncService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
//		SyncApp.getInstance().addActivity(this);
		initImageLoader(getApplicationContext());
		new Handler().post(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TAG = NativePhotoGridFragment.class.getName();
				FragmentManager fgManager = getFragmentManager();
				
				NativePhotoGridFragment PhotoGF = (NativePhotoGridFragment)fgManager.findFragmentByTag(TAG);
				if(PhotoGF == null) {
					Log.e(TAG, "fragment is null");
					PhotoGF = new NativePhotoGridFragment();
				
					Bundle pBundle = new Bundle();
					pBundle.putInt("index", NativePhotoGridFragment.FRAGMENT_INDEX);
					PhotoGF.setArguments(pBundle);
					
					fgManager.beginTransaction().replace(android.R.id.content, 
							PhotoGF, TAG).commit();
				}
			}
		});
		
		bindService(new Intent(this, MediaSyncService.class), mConnection, BIND_AUTO_CREATE);
		
	}
	
	@Override
	protected void onResume() {
		Log.e(TAG, "onResume");
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
		Log.e(TAG, "onDestory");
		unbindService(mConnection);
		super.onDestroy();
//		SyncApp.getInstance().removeActivity(this);
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
	
	public static void initImageLoader(Context context) {
		// This configuration tuning is custom. You can tune every option, you may tune some of them,
		// or you can create default configuration by
		//  ImageLoaderConfiguration.createDefault(this);
		// method.
		String cacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/.glasses_image_cache";
		File cacheFile = StorageUtils.getOwnCacheDirectory(context, cacheDir);
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
				.threadPriority(Thread.NORM_PRIORITY - 2)
				.threadPoolSize(3)
				.denyCacheImageMultipleSizesInMemory()
				.diskCacheFileNameGenerator(new Md5FileNameGenerator())
				.diskCacheSize(50 * 1024 * 1024) // 50 Mb
				.diskCache(new UnlimitedDiscCache(cacheFile))
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.writeDebugLogs() // Remove for release app
				.diskCacheExtraOptions(480, 320, null)
				.build();
		// Initialize ImageLoader with configuration.
		
		ImageLoader.getInstance().init(config);
	}
}
