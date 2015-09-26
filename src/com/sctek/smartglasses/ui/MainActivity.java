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


import cn.ingenic.glasssync.utils.ModuleUtils;

import com.ingenic.glass.api.sync.SyncChannel.Packet;
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
import com.sctek.smartglasses.utils.HanLangCmdChannel;
import com.sctek.smartglasses.utils.PhotosSyncRunnable;
import com.sctek.smartglasses.utils.VideoSyncRunnable;
import com.sctek.smartglasses.utils.WifiUtils;

import android.support.v4.app.FragmentActivity;
import android.text.style.SuperscriptSpan;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;

public class MainActivity extends FragmentActivity {
	
	private  String TAG = "MainActivity";
	
	public final static int GET_GLASS_INFO = 17;
	
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
		initImageLoader(getApplicationContext());
		
		dialog = new ProgressDialog(MainActivity.this);
		
		GlassDetect mGlassDetect = (GlassDetect)GlassDetect.getInstance(getApplicationContext());
		mGlassDetect.setLockedAddress(mSyncManager.getLockedAddress());
		
		SharedPreferences pref = getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		
		boolean firstBind = getIntent().getBooleanExtra("first_bind", false);
		if(firstBind) {
			mGlassDetect.set_audio_connect();
			Editor editor = pref.edit();
	    	editor.putBoolean("last_headset_state", true);
	    	editor.commit();
		}
		else if(pref.getBoolean("last_headset_state", false)&&
				(mGlassDetect.getCurrentState() == BluetoothProfile.STATE_DISCONNECTED)) {
			mGlassDetect.set_audio_connect();
		}
		
		TimeSyncManager.getInstance().syncTime();
		
		syncContactToGlass(true);
		
		getGlassInfo();
	}

	private void syncContactToGlass(Boolean value){
		ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(getApplicationContext());
		clm.sendSyncRequest(value,null);
		clm.setSyncEnable(value);
	}
	
	private void getGlassInfo() {
		HanLangCmdChannel channel = HanLangCmdChannel.getInstance(getApplicationContext());
		Packet pk = channel.createPacket();
		pk.putInt("type", GET_GLASS_INFO);
		channel.sendPacket(pk);
		
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
			turnApOff();
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
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
				.diskCacheExtraOptions(480, 320, null)
				.build();
		// Initialize ImageLoader with configuration.
		
		if(!ImageLoader.getInstance().isInited())
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
				try {
				GlassDetect glassDetect = (GlassDetect)GlassDetect.getInstance(getApplicationContext());
				glassDetect.set_audio_disconnect();

//				syncContactToGlass(false);
				disableLocalData();
				unBond();
				} catch (Exception e) {
					e.printStackTrace();
				}
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
		ImageLoader.getInstance().clearDiskCache();
		ImageLoader.getInstance().clearMemoryCache();
		ImageLoader.getInstance().destroy();
	}
	
	ProgressDialog dialog;
	Handler handler = new Handler() {
		
		public void handleMessage(android.os.Message msg) {
			switch(msg.what) {
			case 1:
				dialog.setMessage(getResources().getText(R.string.unbonding));
				dialog.show();
				break;
			case 2:
				if(dialog.isShowing())
					dialog.cancel();
				break;
			}
		}
	};
	
	private void unBond() {
		new Thread(new Runnable() {
			@Override
			    public void run() {
				handler.sendEmptyMessage(1);
				try {
			    mSyncManager.setLockedAddress("",true);
			    try {
				Thread.sleep(1000);
			    } catch (Exception e) {
			    }			    
			    mSyncManager.disconnect();
			    handler.sendEmptyMessage(2);
			    Intent intent = new Intent(MainActivity.this,BindHanlangActivity.class);	    
			    startActivity(intent);
			    finish();
			    } catch (Exception e) {
			    	e.printStackTrace();
			    }
			}
		    }).start();
	    }		
	
	private void disableLocalData(){	
		SharedPreferences sp = getSharedPreferences(SyncApp.SHARED_FILE_NAME
									  ,MODE_PRIVATE);
		Editor editor = sp.edit();    
		editor.clear();  
		editor.commit();
		
		SharedPreferences defaultSp = PreferenceManager.getDefaultSharedPreferences(this);
		Editor defaultEditor = defaultSp.edit();
		defaultEditor.clear();
		defaultEditor.commit();
    }    
	
	private void turnApOff() {
		PhotosSyncRunnable photosSyncRunnable = PhotosSyncRunnable.getInstance();
		VideoSyncRunnable videoSyncRunnable = VideoSyncRunnable.getInstance();
		WifiManager wifimanager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if(!photosSyncRunnable.isRunning()&&!videoSyncRunnable.isRunning()&&
				WifiUtils.getWifiAPState(wifimanager) == 13) {
			showTurnApOffDialog();
		}
		else {
			quit();
		}
	}
	
	private void showTurnApOffDialog() {
		
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.turn_wifi_ap_off);
		builder.setMessage(R.string.wifi_ap_hint_off);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// TODO Auto-generated method stub
						WifiManager wifimanager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
						WifiUtils.setWifiApEnabled(false, wifimanager);
						return null;
					}
				}.execute();
				
				dialog.cancel();
				quit();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
				quit();
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				quit();
			}
		});
		dialog.show();
	}
	
	private void quit() {
		super.onBackPressed();
	}
	
}
