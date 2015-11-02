package com.sctek.smartglasses.ui;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import cn.ingenic.glasssync.LocationReportService;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.SyncApp;
import cn.ingenic.glasssync.camera.PhotoModule;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.devicemanager.TimeSyncManager;
import cn.ingenic.glasssync.screen.LiveDisplayActivity;
import android.content.DialogInterface;
// import cn.ingenic.glasssync.ui.BindGlassActivity;

import cn.ingenic.glasssync.utils.ModuleUtils;

import com.fota.iport.MobAgentPolicy;
import com.fota.iport.config.VersionInfo;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
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
import com.sctek.smartglasses.utils.CustomHttpClient;
import com.sctek.smartglasses.utils.HanLangCmdChannel;
import com.sctek.smartglasses.utils.PhotosSyncRunnable;
import com.sctek.smartglasses.utils.VideoSyncRunnable;
import com.sctek.smartglasses.utils.WifiUtils;

import android.support.v4.app.FragmentActivity;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
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
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;

public class MainActivity extends FragmentActivity {
	
	private  String TAG = "MainActivity";
	
	public final static int GET_GLASS_INFO = 17;
	public final static int UPDATE_CONNECT_WIFI_MSG =20;
	
	private TextView photoTv;
	private TextView videoTv;
	private TextView settingTv;
	private TextView liveTv;
	private TextView unbindTv;
	private TextView aboutTv;
        private Button takePhotoBt;
        private Button takeVideoBt;
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
		photoTv = (TextView)findViewById(R.id.photo_tv);
		videoTv = (TextView)findViewById(R.id.video_tv);
		settingTv = (TextView)findViewById(R.id.setting_tv);
		liveTv = (TextView)findViewById(R.id.live_tv);
		unbindTv = (TextView)findViewById(R.id.unbind_tv);
		aboutTv = (TextView)findViewById(R.id.about_tv);
		takePhotoBt =(Button)findViewById(R.id.take_photo_bt);
		takeVideoBt =(Button)findViewById(R.id.take_video_bt);

		photoTv.setOnClickListener(mClickedListener);
		videoTv.setOnClickListener(mClickedListener);
		settingTv.setOnClickListener(mClickedListener);
		liveTv.setOnClickListener(mClickedListener);
		unbindTv.setOnClickListener(mClickedListener);
		aboutTv.setOnClickListener(mClickedListener);
		takePhotoBt.setOnClickListener(mClickedListener);
		takeVideoBt.setOnClickListener(mClickedListener);

		mSyncManager = DefaultSyncManager.getDefault();
		initImageLoader(getApplicationContext());
		
		HanLangCmdChannel.getInstance(getApplicationContext()).setHandler(handler);
		
		dialog = new ProgressDialog(MainActivity.this);
		
		GlassDetect mGlassDetect = (GlassDetect)GlassDetect.getInstance(getApplicationContext());
		mGlassDetect.setLockedAddress(mSyncManager.getLockedAddress());
		
		SharedPreferences pref = getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		
		boolean firstBind = getIntent().getBooleanExtra("first_bind", false);
		if(firstBind) {
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					startService(new Intent(MainActivity.this, LocationReportService.class));
				}
			}, 2000);
			mGlassDetect.set_audio_connect();
			syncContactToGlass(true);
			Editor editor = pref.edit();
	    	editor.putBoolean("last_headset_state", true);
	    	editor.commit();
		}
		else if(pref.getBoolean("last_headset_state", false)&&
				(mGlassDetect.getCurrentState() == BluetoothProfile.STATE_DISCONNECTED)) {
			mGlassDetect.set_audio_connect();
		}
		
		TimeSyncManager.getInstance().syncTime();		
		
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
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Locale local = getResources().getConfiguration().locale;
		if(!local.getLanguage().contains("zh")) {
			RelativeLayout layout = (RelativeLayout)findViewById(R.id.main_background);
    		layout.setBackgroundResource(R.drawable.app_background_en_low);
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
				.diskCache(new UnlimitedDiskCache(cacheFile))
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.writeDebugLogs() // Remove for release app
				.diskCacheExtraOptions(480, 320, null)
				.build();
		// Initialize ImageLoader with configuration.
		
		if(!ImageLoader.getInstance().isInited())
			ImageLoader.getInstance().init(config);
	}
	
	private OnClickListener mClickedListener = new OnClickListener() {
		
		@SuppressLint("NewApi")
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
				case R.id.photo_tv:
					startActivity(new Intent(MainActivity.this, PhotoActivity.class));
					break;
				case R.id.video_tv:
					startActivity(new Intent(MainActivity.this, VideoActivity.class));
					break;
				case R.id.setting_tv:
					startActivity(new Intent(MainActivity.this, SettingActivity.class));
					break;
				case R.id.live_tv:
					Intent intent = new Intent(MainActivity.this, GetGlassIpActivity.class);
					startActivity(intent);
					break;
				case R.id.unbind_tv:
					showUbindDialog();
					break;
				case R.id.about_tv:
					startActivity(new Intent(MainActivity.this, AboutActivity.class));
					break;
		   	        case R.id.take_photo_bt:
				    PhotoModule m=PhotoModule.getInstance(getApplicationContext());
				    m.send_take_photo();
				    takePhotoBt.setEnabled(false);
				    handler.postDelayed(takePhotoRunnable, 2000);
				    break;
				case R.id.take_video_bt:
				    PhotoModule.getInstance(getApplicationContext()).send_record();
				    takeVideoBt.setEnabled(false);
				    handler.postDelayed(takeVideoRunnable, 2000);
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
			case 3:
				showUpdateConfirmDialog();
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
	
        private Runnable takePhotoRunnable = new Runnable() {
		
		@Override
		public void run() {
		    takePhotoBt.setEnabled(true);
		      //takePhotoBt.setAlpha(255);
		}
	};

        private Runnable takeVideoRunnable = new Runnable() {
		
		@Override
		public void run() {
		    takeVideoBt.setEnabled(true);
		      //takeVideoBt.setAlpha(255);
		}
	};

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
	
	private void showUpdateConfirmDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.software_updates);
		builder.setMessage(R.string.updates_note);
		builder.setNegativeButton(R.string.update_later, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
		});
		
		builder.setPositiveButton(R.string.update_now, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
				showWifiConnectDialog();
			}
		});
		
		builder.create().show();
	}
	
	private void showWifiConnectDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = LayoutInflater.from(this).inflate(R.layout.config_wifi, null);
		
		final EditText ssidEt = (EditText)view.findViewById(R.id.ap_ssid_et);
		final EditText pwEt = (EditText)view.findViewById(R.id.ap_pw_et);
		
		builder.setView(view);
		builder.setTitle(R.string.config_wifi);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				String ssid = ssidEt.getText().toString();
				String pw = pwEt.getText().toString();
				
				HanLangCmdChannel channel = HanLangCmdChannel.getInstance(getApplicationContext());
				
				if(ssid.isEmpty()) {
					Toast.makeText(MainActivity.this, R.string.empty_ssid, Toast.LENGTH_SHORT).show();
					showWifiConnectDialog();
					return;
				}
				
				if(!channel.isConnected()) {
					Toast.makeText(MainActivity.this, R.string.update_bluetooth_error, Toast.LENGTH_SHORT).show();
					return;
				}
					
				VersionInfo vi = MobAgentPolicy.getVersionInfo();
				Packet pk = channel.createPacket();;
				pk.putInt("type", UPDATE_CONNECT_WIFI_MSG);
				pk.putString("ssid", ssid);
				pk.putString("pw", pw);
				pk.putString("url", vi.deltaUrl);
				pk.putString("deltaid", vi.deltaID);
				pk.putString("md5", vi.md5sum);
				pk.putInt("size", vi.fileSize);
				pk.putString("vname", vi.versionName);
				Log.e(TAG, "url:" + vi.deltaUrl + "deltaid:" + vi.deltaID + "md5:" + vi.md5sum + "size:" + vi.fileSize + "vname:" + vi.versionName);
				channel.sendPacket(pk);
			}
		});
		
		builder.setNegativeButton(R.string.cancel_update, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
		});
		
		builder.create().show();
	}
	
}
