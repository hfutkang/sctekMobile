package com.sctek.smartglasses.ui;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.SyncApp;
import cn.ingenic.glasssync.screen.LiveDisplayActivity;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.activity.InitActivity;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import java.io.BufferedReader;
import java.io.FileReader;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.sctek.smartglasses.utils.HanLangCmdChannel;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.WifiUtils.WifiCipherType;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class GetGlassIpActivity extends Activity {
	
	private final static String TAG = "VedioPlayerActivity";
	
	protected static final int WIFI_AP_STATE_DISABLED = 11;
	protected static final int WIFI_AP_STATE_ENABLED = 13;
	
	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";
	
	private ProgressBar mProgressBar;
	
	private int preApState;
	private SetWifiAPTask mWifiATask;
	private WifiManager mWifiManager;
	private String glassIp;
	private HanLangCmdChannel mHanLangCmdChannel;
	
	private ProgressDialog mProgressDialog;
	
	private static final int RESEDN_CONNET_WIFI_MSG = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_get_glasses_ip);
		Log.e(TAG, "onCreate");
		SyncApp.getInstance().addActivity(this);
		setTitle(R.string.video_live);
		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setHomeButtonEnabled(false);
		
		mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		mHanLangCmdChannel = HanLangCmdChannel.getInstance(getApplicationContext());
		mHanLangCmdChannel.setHandler(mHanlder);
		
		preApState = WifiUtils.getWifiAPState(mWifiManager);
		mWifiATask = new SetWifiAPTask(true, false);
		
		initView();
		
		IntentFilter filter = new IntentFilter(WIFI_AP_STATE_CHANGED_ACTION);
		registerReceiver(mApStateBroadcastReceiver,filter);
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(preApState == WIFI_AP_STATE_ENABLED)  {
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(WifiUtils.needTurnWifiApOff(GetGlassIpActivity.this)) {
						mWifiATask.execute(true);
					}
					else {
						sendApInfoToGlass();
					}
				}
			}, 0);
		}
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mHanlder.removeMessages(RESEDN_CONNET_WIFI_MSG);
		unregisterReceiver(mApStateBroadcastReceiver);
		SyncApp.getInstance().removeActivity(this);
		super.onDestroy();
	}
	
	@SuppressLint("NewApi")
	private void initView() {
		
		if(preApState == WIFI_AP_STATE_DISABLED) {
			showTurnWifiApOnDialog();
		}
		
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(R.string.video_live);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_BACK) {
					finish();
					dialog.cancel();
				}
				return false;
			}
		});
		
	}
	
	private BroadcastReceiver mApStateBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				int cstate = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1);
				Log.e(TAG, WIFI_AP_STATE_CHANGED_ACTION + ":" + cstate);
				if(cstate == WIFI_AP_STATE_ENABLED && preApState != WIFI_AP_STATE_ENABLED) {
					BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
					if(!adapter.isEnabled()) {
						adapter.enable();
					}
					sendApInfoToGlass();
				}
				preApState = cstate;
			}
			
		}
	};
	
	public class SetWifiAPTask extends AsyncTask<Boolean, Void, Void> {
    	
		private boolean mMode;
		
		public SetWifiAPTask(boolean mode, boolean finish) {
		    mMode = mode;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog.setMessage(getResources().getText(R.string.turning_wifi_ap_on));
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			Log.e(TAG, "onPostExecute");
			//updateStatusDisplay();
//			if (mFinish) mContext.finish();
		}

		@Override
		protected Void doInBackground(Boolean... off) {
			try {
				if(off[0])
					WifiUtils.toggleWifi(GetGlassIpActivity.this, mWifiManager);
				WifiUtils.turnWifiApOn(GetGlassIpActivity.this, mWifiManager, WifiCipherType.WIFICIPHER_NOPASS);
			} catch(Exception e) {
				e.printStackTrace();
			}
		    return null;
		}
    }
	
	private void sendApInfoToGlass() {
		
		if(mHanLangCmdChannel.isConnected()) {
			
			
			mProgressDialog.setMessage(getResources().getText(R.string.waiting_for_glass_connect));
			if(!mProgressDialog.isShowing())
				mProgressDialog.show();
			
			Packet packet = mHanLangCmdChannel.createPacket();
			packet.putInt("type", 1);
			
			String ssid = WifiUtils.getDefaultApSsid(this);
			
			packet.putString("ssid", ssid);
			mHanLangCmdChannel.sendPacket(packet);
			mHanlder.sendEmptyMessageDelayed(RESEDN_CONNET_WIFI_MSG, 5000);
			
		}
		else {
			Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	public void showTurnWifiApOnDialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.turn_wifi_ap_on);
		builder.setMessage(R.string.wifi_ap_hint);
		builder.setPositiveButton(R.string.turn_wifi_ap_on_now, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				mWifiATask.execute(false);
				dialog.cancel();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
				finish();
			}
		});
		
		builder.setCancelable(false);
		builder.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_BACK) {
					finish();
					dialog.cancel();
				}
				return false;
			}
		});
		
		builder.create().show();
	}
	
	private Handler mHanlder = new Handler() {
		
		private boolean connected = false;
		@Override
		public void handleMessage(Message msg) {
			
			if(msg.what == HanLangCmdChannel.RECEIVE_MSG_FROM_GLASS) {
				Packet data = (Packet)msg.obj;
				
				glassIp = data.getString("ip");
				
				if(glassIp != null && glassIp.length() != 0&&!connected) {
					mProgressDialog.cancel();
					mHanlder.removeMessages(RESEDN_CONNET_WIFI_MSG);
					Intent intent = new Intent(GetGlassIpActivity.this, LiveDisplayActivity.class);
					intent.putExtra("ip", glassIp);
					startActivity(intent);
					finish();
				}
			}
			else if(msg.what == RESEDN_CONNET_WIFI_MSG) {
				sendApInfoToGlass();
			}
				
		}
	};
}
