package com.sctek.smartglasses.ui;

import java.io.File;
import java.util.UUID;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.SyncApp;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.devicemanager.TimeSyncManager;
import cn.ingenic.glasssync.screen.LiveDisplayActivity;
// import cn.ingenic.glasssync.ui.BindGlassActivity;


import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.sctek.smartglasses.fragments.NativePhotoGridFragment;
import com.sctek.smartglasses.fragments.NativeVideoGridFragment;
import com.sctek.smartglasses.fragments.PhotoViewPagerFragment;
import com.sctek.smartglasses.fragments.SettingFragment;

import android.support.v4.app.FragmentActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;

public class MainActivity extends FragmentActivity {
	
	private  String TAG = "MainActivity";
	
	private ImageButton photoIb;
	private ImageButton videoIb;
	private ImageButton settingIb;
	private ImageButton liveIb;
	private ImageButton unbindIb;
	private ImageButton aboutIb;
	
	private DefaultSyncManager mSyncManager;

	private static MainActivity mInstance = null;
	public static MainActivity getInstance() {
		return mInstance;
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.myactivity_main);
		getActionBar().hide();
		mInstance = this;
		SyncApp.getInstance().addActivity(this);
		photoIb = (ImageButton)findViewById(R.id.photo_ib);
		videoIb = (ImageButton)findViewById(R.id.video_ib);
		settingIb = (ImageButton)findViewById(R.id.setting_ib);
		liveIb = (ImageButton)findViewById(R.id.live_ib);
		unbindIb = (ImageButton)findViewById(R.id.unbind_ib);
		aboutIb = (ImageButton)findViewById(R.id.about_ib);
		
		photoIb.setOnClickListener(onImageButtonClickedListener);
		videoIb.setOnClickListener(onImageButtonClickedListener);
		settingIb.setOnClickListener(onImageButtonClickedListener);
		liveIb.setOnClickListener(onImageButtonClickedListener);
		unbindIb.setOnClickListener(onImageButtonClickedListener);
		aboutIb.setOnClickListener(onImageButtonClickedListener);
		
		mSyncManager = DefaultSyncManager.getDefault();
		initImageLoader(this);
		
		GlassDetect mGlassDetect = (GlassDetect)GlassDetect.getInstance(getApplicationContext());
		mGlassDetect.setLockedAddress(mSyncManager.getLockedAddress());
		SharedPreferences mSharedPreferences = getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		Editor editor = mSharedPreferences.edit();
		
		if(mGlassDetect.getCurrentState() == BluetoothProfile.STATE_DISCONNECTED) {
			editor.putBoolean("last_headset_state", false);
			editor.commit();
		}
		else if(mGlassDetect.getCurrentState() == BluetoothProfile.STATE_CONNECTED){
			editor.putBoolean("last_headset_state", true);
			editor.commit();
		}
		
		TimeSyncManager.getInstance().syncTime();
		
		syncContactToGlass();
	}

	private void syncContactToGlass(){
		ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(getApplicationContext());
		clm.sendSyncRequest(true,null);
		clm.setSyncEnable(true);
	}
	
	private long currentTime = System.currentTimeMillis();
	@SuppressLint("NewApi")
	@Override
	public void onBackPressed() {
	// TODO Auto-generated method stub
		int stackCount = getFragmentManager().getBackStackEntryCount();
		if(stackCount != 0) {
			if(stackCount == 1)
			getFragmentManager().popBackStack();
		} else {
			
			long tempTime = System.currentTimeMillis();
			long interTime = tempTime - currentTime;
			if(interTime > 2000) {
				Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
				currentTime = tempTime;
				return;
			}
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		ImageLoader.getInstance().clearDiskCache();
		ImageLoader.getInstance().clearMemoryCache();
		ImageLoader.getInstance().stop();
		super.onPause();
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
				.build();
		// Initialize ImageLoader with configuration.
		
		ImageLoader.getInstance().init(config);
	}
	
	private OnClickListener onImageButtonClickedListener = new OnClickListener() {
		
		@SuppressLint("NewApi")
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
				case R.id.photo_ib:
					startActivity(new Intent(MainActivity.this, PhotoActivity.class));
					break;
				case R.id.video_ib:
					startActivity(new Intent(MainActivity.this, VideoActivity.class));
					break;
				case R.id.setting_ib:
					startActivity(new Intent(MainActivity.this, SettingActivity.class));
					break;
				case R.id.live_ib:
					Intent intent = new Intent(MainActivity.this, GetGlassIpActivity.class);
					startActivity(intent);
					break;
				case R.id.unbind_ib:
					showUbindDialog();
					break;
				case R.id.about_ib:
					startActivity(new Intent(MainActivity.this, AboutActivity.class));
					break;
			default:
				break;
			}
		}
	};
	
	public void showUbindDialog() {
		
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.unbind);
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				unBond();
			}
		});
		
		builder.create().show();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG,"onDestroy in");
		mInstance = null;
		SyncApp.getInstance().exitAllActivity();
	}
	private void unBond() {
		GlassDetect glassDetect = (GlassDetect)GlassDetect.getInstance(getApplicationContext());
		glassDetect.set_audio_disconnect();
		new Thread(new Runnable() {
			@Override
			    public void run() {
				try {
			    mSyncManager.setLockedAddress("",true);
			    try {
				Thread.sleep(1000);
			    } catch (Exception e) {
			    }			    
			    mSyncManager.disconnect();
			    
//			    mBLDServer.removeBond();
			    Intent intent = new Intent(MainActivity.this,BindGlassActivity.class);	    
			    startActivity(intent);
			    finish();
			    } catch (Exception e) {
			    	e.printStackTrace();
			    }
			}
		    }).start();
	    }		
}
