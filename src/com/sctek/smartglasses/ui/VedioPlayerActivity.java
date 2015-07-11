package com.sctek.smartglasses.ui;

import cn.ingenic.glasssync.R;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class VedioPlayerActivity extends Activity {
	
	private final static String TAG = "VedioPlayerActivity";
	
	protected static final int WIFI_AP_STATE_DISABLED = 11;
	protected static final int WIFI_AP_STATE_ENABLED = 13;
	
	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";
	
	public static final int START_LIVE = 15;
	public static final int STOP_LIVE = 16;
	
	private VideoView mVideoView;
	private Button mButton;
	private ProgressBar mProgressBar;
	private boolean playing = false;
	
	private int preApState;
	private View enableApView;
	private Button enableApBt;
	private SetWifiAPTask mWifiATask;
	private WaitGlassConnectTask mConnectTask;
	private WifiManager mWifiManager;
	private String glassIp;
	private HanLangCmdChannel mHanLangCmdChannel;
	private MediaController mMediaController;
	
	private String liveUrl;
	private boolean msgReceived = false;
	private ProgressDialog mProgressDialog;
	
	private static final int RESEDN_CONNET_WIFI_MSG = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vedio_player);
		Log.e(TAG, "onCreate");
		
		setTitle(R.string.video_live);
		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setHomeButtonEnabled(false);
		
		Vitamio.initialize(this, getResources().getIdentifier("libarm", "raw", getPackageName()));
		
		mVideoView = (VideoView)findViewById(R.id.video_player_vv);
//		mMediaController = (MediaController)findViewById(R.id.video_controller);
		mButton = (Button)findViewById(R.id.play_bt);
		enableApView = (View)findViewById(R.id.wifi_ap_hint_lo);
		enableApBt = (Button)findViewById(R.id.wifi_ap_on_bt);
		mProgressBar = (ProgressBar)findViewById(R.id.video_pb);
		
		mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		mHanLangCmdChannel = HanLangCmdChannel.getInstance(getApplicationContext());
		mHanLangCmdChannel.setHandler(mHanlder);
		
		preApState = WifiUtils.getWifiAPState(mWifiManager);
		mConnectTask = new WaitGlassConnectTask();
		mWifiATask = new SetWifiAPTask(true, false);
		mProgressDialog = new ProgressDialog(this);
		
		if(preApState == WIFI_AP_STATE_ENABLED)  {
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					sendApInfoToGlass();
				}
			}, 0);
		}
		
		liveUrl = "";
		
		initVideoPlayerView();
		
		IntentFilter filter = new IntentFilter(WIFI_AP_STATE_CHANGED_ACTION);
		registerReceiver(mApStateBroadcastReceiver,filter);
		
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(mVideoView.isPlaying()) {
			mVideoView.stopPlayback();
		}
		mVideoView.setVideoURI(null);
		mButton.setText(R.string.start_live);
		playing = false;
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mConnectTask.cancel(true);
		if(mVideoView.isPlaying())
			mVideoView.stopPlayback();
		super.onDestroy();
	}
	
	@SuppressLint("NewApi")
	private void initVideoPlayerView() {
		
		if(preApState == WIFI_AP_STATE_DISABLED) {
			enableApView.setVisibility(View.VISIBLE);
			mVideoView.setVisibility(View.GONE);
		}
		else {
			mButton.setVisibility(View.VISIBLE);
		}
		
		enableApBt.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mWifiATask.execute();
			}
		});
		
		mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(!playing) {
					
					if(!mHanLangCmdChannel.isConnected()) {
						Toast.makeText(VedioPlayerActivity.this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
						return;
					}
					
					Packet pk = mHanLangCmdChannel.createPacket();
					pk.putInt("type", START_LIVE);
					mHanLangCmdChannel.sendPacket(pk);
					
					mButton.setVisibility(View.GONE);
					mProgressBar.setVisibility(View.VISIBLE);
					
					liveUrl = String.format("rtsp://%s:8554/recorderLive", glassIp);
					Log.e(TAG, liveUrl);
					try {
					mVideoView.setVideoPath(liveUrl);
					mVideoView.requestFocus();
					} catch (Exception e){
						e.printStackTrace();
					}
				}
				else {
					
					if(!mHanLangCmdChannel.isConnected()) {
						Toast.makeText(VedioPlayerActivity.this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
						return;
					}
					
					Packet pk = mHanLangCmdChannel.createPacket();
					pk.putInt("type", STOP_LIVE);
					mHanLangCmdChannel.sendPacket(pk);
					
					mVideoView.stopPlayback();
					mButton.setText(R.string.start_live);
					playing = false;
				}
				
			}
		});
		
		mVideoView.setMediaController(new MediaController(VedioPlayerActivity.this));
		
		mVideoView.setOnInfoListener(new OnInfoListener() {
			
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				Log.e(TAG, "info:" + what + " extra:" + extra);
				switch(what) {
				case MediaPlayer.MEDIA_INFO_BUFFERING_START:
					break;
				case MediaPlayer.MEDIA_INFO_BUFFERING_END:
					break;
					default:
						break;
				}
				return true;
			}
		});
		
		mVideoView.setOnPreparedListener(new OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Log.e(TAG, "onPrepared");
				mProgressBar.setVisibility(View.GONE);
				playing = true;
				mButton.setText(R.string.stop_live);
				mp.setPlaybackSpeed(1.0f);
		       mp.start();
			}
		});
		
		mVideoView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(!mButton.isShown()) {
					mButton.setVisibility(View.VISIBLE);
				}
				else {
					mButton.setVisibility(View.GONE);
				}
				return false;
			}
		});
		
		mVideoView.setOnErrorListener(new OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				Log.e(TAG, "error:" + what);
				mButton.setText(R.string.start);
				playing = false;
				mVideoView.stopPlayback();
				mButton.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
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
					enableApView.setVisibility(View.GONE);
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
	
	public class SetWifiAPTask extends AsyncTask<Void, Void, Void> {
    	
		private boolean mMode;
		private ProgressDialog d = new ProgressDialog(VedioPlayerActivity.this);
		
		public SetWifiAPTask(boolean mode, boolean finish) {
		    mMode = mode;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			d.setTitle(R.string.turn_wifi_ap_on);
			d.setMessage(getResources().getText(R.string.turning_wifi_ap_on));
			d.show();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			Log.e(TAG, "onPostExecute");
			try {d.dismiss();} catch (IllegalArgumentException e) {};
			//updateStatusDisplay();
//			if (mFinish) mContext.finish();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				WifiUtils.turnWifiApOn(VedioPlayerActivity.this, mWifiManager);
			} catch(Exception e) {
				e.printStackTrace();
			}
		    return null;
		}
    }
	
	public class WaitGlassConnectTask extends AsyncTask<Void, Void, Void> {

		private ProgressDialog mProgressDialog;
		
		public WaitGlassConnectTask() {
			mProgressDialog = new ProgressDialog(VedioPlayerActivity.this);
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			mProgressDialog.setTitle(R.string.video_live);
			mProgressDialog.setMessage(getResources().getText(R.string.wait_device_connect));
			mProgressDialog.show();
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			mProgressDialog.dismiss();
			enableApView.setVisibility(View.GONE);
			mButton.setVisibility(View.VISIBLE);
			mVideoView.setVisibility(View.VISIBLE);
			super.onPostExecute(result);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			while(true) {
				glassIp = getConnectedGlassIP();
				if(isCancelled())
					return null;
				if(glassIp != null)
					break;
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		
	}
	
	public String getConnectedGlassIP() { 
		
		BufferedReader br = null;  
		String line;  
		String ip = null;
		try {  
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			while ((line = br.readLine()) != null) { 
				String[] splitted = line.split(" +");
				if (!"IP".equals(splitted[0])) {
					ip = splitted[0];
					break;
				}
			}
			br.close();
		} catch (Exception e) { 
			e.printStackTrace();  
		}  
		
		if(ip == null&& !msgReceived) {
			Packet packet = mHanLangCmdChannel.createPacket();
			packet.putInt("type", 1);
			
			String defaultSsid = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId().substring(0, 5);
			String ssid = PreferenceManager.
					getDefaultSharedPreferences(this).getString("ssid", defaultSsid);
			String pw = PreferenceManager.getDefaultSharedPreferences(this).getString("pw", "12345678");
			
			packet.putString("ssid", ssid);
			packet.putString("pw", pw);
			mHanLangCmdChannel.sendPacket(packet);
			
		}
		
		return ip;
	} 
	
private void sendApInfoToGlass() {
		
		if(mHanLangCmdChannel.isConnected()) {
			
			
			mProgressDialog.setMessage(getResources().getText(R.string.waiting_for_glass_connect));
			if(!mProgressDialog.isShowing())
				mProgressDialog.show();
			
			Packet packet = mHanLangCmdChannel.createPacket();
			packet.putInt("type", 1);
			
			String defaultSsid = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId().substring(0, 5);
			String ssid = PreferenceManager.
					getDefaultSharedPreferences(this).getString("ssid", defaultSsid);
			String pw = PreferenceManager.getDefaultSharedPreferences(this).getString("pw", "12345678");
			
			packet.putString("ssid", ssid);
			packet.putString("pw", pw);
			mHanLangCmdChannel.sendPacket(packet);
			mHanlder.sendEmptyMessageDelayed(RESEDN_CONNET_WIFI_MSG, 5000);
			
		}
		else {
			Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
private Handler mHanlder = new Handler() {
		
		private boolean connected = false;
		@Override
		public void handleMessage(Message msg) {
			
			if(msg.what == HanLangCmdChannel.RECEIVE_MSG_FROM_GLASS) {
				Packet data = (Packet)msg.obj;
				
				glassIp = data.getString("ip");
				
				if(glassIp != null && glassIp.length() != 0&&!connected) {
					mProgressDialog.dismiss();
					enableApView.setVisibility(View.GONE);
					mButton.setVisibility(View.VISIBLE);
					mVideoView.setVisibility(View.VISIBLE);
				}
			}
			else if(msg.what == RESEDN_CONNET_WIFI_MSG) {
				sendApInfoToGlass();
			}
				
		}
	};
}
