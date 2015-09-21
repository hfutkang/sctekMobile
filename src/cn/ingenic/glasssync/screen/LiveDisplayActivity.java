package cn.ingenic.glasssync.screen;

import android.app.Activity;
import android.app.ActivityManager;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import cn.ingenic.glasssync.R;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.os.Build;
import android.view.KeyEvent;
import android.os.Handler;
import android.os.Message;
import android.app.Dialog;
import android.app.Application; 
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.content.ComponentName;
import android.content.pm.ConfigurationInfo;


import android.text.format.Formatter;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import android.net.DhcpInfo;
import cn.ingenic.glasssync.devicemanager.WifiManagerApi;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import java.net.UnknownHostException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import cn.ingenic.glasssync.DefaultSyncManager;

import cn.ingenic.glasssync.utils.MyDialog;
import cn.ingenic.glasssync.screen.live.LiveModule;
import cn.ingenic.glasssync.screen.live.RtspClient;



public class LiveDisplayActivity extends Activity implements RtspClient.OnRtspClientListener {
    private static final String TAG = "LiveDisplayActivity";
    private static final boolean DEBUG = true;
    private static final String CHECK_PHONE_IP_ADDRESS = "192.168.43";
    private static final int UNCONNECTED_DIALOG_DISMISS = 2;
    private final int DIALOG_DISMISS = 1;

    private LiveModule mLiveModule;
    public static RtspClient mRtspClient;
    private String mMediaUrl = null;
    private SurfaceView mSurfaceView = null;
    public static Dialog mDialog, mQuitDialog, mUnConnectedDialog, mStreamDown, mStreamDisconnect;
    public static ProgressDialog mPD = null;
    public static boolean mBluetoothConnected = true;
    public static boolean mWifiDeviceConnected = true;
    public static boolean mRTSPOpened = false;
    private String mIPAddress = null;
    public static ArrayList<String> mConnectedIP;
    private WifiConfiguration mWifiConfiguration;
    private WifiInfo mWifiInfo;
    private DefaultSyncManager mManager;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mDialogDismiss = false;
    private long mStart = 0;
    private static Activity sActivity = null;

    private WifiManagerApi mWifiManager;

    private static boolean sHasError = false;

    private Handler mHandler = new Handler(){
    @Override
    public void handleMessage(Message msg){
	super.handleMessage(msg);
	if (DEBUG) Log.e(TAG, "mHandler handleMessage");
	if (msg.what == DIALOG_DISMISS) {
	    mPD.dismiss();
	}

	if (msg.what == UNCONNECTED_DIALOG_DISMISS) {
	    mPD.dismiss();
    	    initUnConnectedDialog();
	}
    }};

    private Handler mConnectedHandler = new Handler(){
    @Override
    public void handleMessage(Message msg){
    	super.handleMessage(msg);
    	if (msg.what == UNCONNECTED_DIALOG_DISMISS) {
	    if (DEBUG) Log.e(TAG, "mConnectedHandler handleMessage");
    	    initUnConnectedDialog();
    	}
    }};


    Runnable runnable1 = new Runnable(){
    @Override
    public void run() {
	Log.e(TAG, "runnable1");
	if (!mBluetoothConnected) {
	    long end = System.currentTimeMillis() - mStart;
	    if ((end > 20000000)) {
		Log.e(TAG, "1111111");
		Message message = Message.obtain();
		message.what = UNCONNECTED_DIALOG_DISMISS;
		mHandler.sendMessage(message);
	    }else{
		mHandler.postDelayed(this, 2000);   
	    }
	}else{
	    mHandler.postDelayed(this, 1000);   
	}
    }};

    Runnable runnable2 = new Runnable(){
    @Override
    public void run() {
	if (!mRTSPOpened) {
	    mConnectedHandler.postDelayed(this, 100);   
	}else{
	    if (!mWifiDeviceConnected) {
		Message message = Message.obtain();
		message.what = UNCONNECTED_DIALOG_DISMISS;
		mConnectedHandler.sendMessage(message);
	    }else{
		mConnectedHandler.postDelayed(this, 100);   
	    }
	}
    }};
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_display);
	Log.e(TAG, "onCreate");
	sActivity = this;
	sHasError = false;
	initView();
    }


    // public static boolean detectOpenGLES20(Context context) {  
    // 	ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);  
    // 	ConfigurationInfo info = am.getDeviceConfigurationInfo();  
    // 	return (info.reqGlEsVersion >= 0x20000);  
    // }  

    private void initView() {
	mWifiManager = new WifiManagerApi(this);

	Log.e(TAG, "InitView++");
	mDialog = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_dialog_title), getApplication().getResources().getString(R.string.live_dialog_ok),getApplication().getResources().getString(R.string.live_dialog_cancle),new MyDialog.LeaveMeetingDialogListener() {
            @Override
	    public void onClick(View view) {
		switch (view.getId()) {
		case R.id.dialog_tv_ok:
		     mDialog.cancel();
		     finish();
		     break;
		case R.id.dialog_tv_cancel_two:
		     mDialog.cancel();
		     Intent intent =  new Intent(Settings.ACTION_WIRELESS_SETTINGS);  
		     startActivity(intent);
		     finish();
		     break;		     
		}
	    }});
	mDialog.setCanceledOnTouchOutside(false);
	mDialog.setCancelable(false);
   
	mRtspClient = new RtspClient(this);
	mRtspClient.setListener(this);
	mLiveModule = LiveModule.getInstance(this);

	mPD = processDialog();
	mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
	mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback(){
            @Override
	    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.e(TAG, "surfaceChanged");
	    }
		
	    @Override
	    public void surfaceCreated(SurfaceHolder arg0) {
		Log.e(TAG, "surfaceCreated");
		if (mRtspClient != null)
		    mRtspClient.setSurface(arg0);
	    }
		
	    @Override
	    public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
	    }
	});
    }

    private void initQuitDialog() {
	mQuitDialog = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_not_connected_dialog_title), getApplication().getResources().getString(R.string.live_quit_dialog_cancle),new MyDialog.LeaveMeetingDialogListener() {
	@Override
        public void onClick(View view) {
	    switch (view.getId()) {
	    case R.id.dialog_tv_cancel_one:
		mQuitDialog.cancel();
		finish();
		break;
		
	    }
	}});
	mQuitDialog.setCanceledOnTouchOutside(false);
	mQuitDialog.setCancelable(false);
	mQuitDialog.show();
    }

    private void initUnConnectedDialog() {
    	 mUnConnectedDialog = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_not_connected_dialog_title), getApplication().getResources().getString(R.string.live_quit_dialog_cancle),new MyDialog.LeaveMeetingDialogListener() {
    	@Override
        public void onClick(View view) {
    	    switch (view.getId()) {
    	    case R.id.dialog_tv_cancel_one:
    		mUnConnectedDialog.cancel();
    		finish();
    		break;
		
    	    }
    	}});
        mUnConnectedDialog.setCanceledOnTouchOutside(false);
    	mUnConnectedDialog.setCancelable(false);
    	mUnConnectedDialog.show();
    }


    private boolean checkBTEnabled() {
	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
	if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())) {
	    Log.e(TAG, "BluetoothAdapter is null or unenable, screen share failed");
	    return false;
	}
	
	mManager = DefaultSyncManager.getDefault();
	if (mManager != null) {
	    Log.e(TAG, "mManager.isConnect() = " + mManager.isConnect());
	}
	
	if ((mManager == null) || (!mManager.isConnect()))  {
	    Log.e(TAG, "unused bluetooth, screen share failed");
	    return false;
	}
	return true;
    }

    private ArrayList<String> getConnectedHotIP() {
	ArrayList<String> connectedIP = new ArrayList<String>();
	if (DEBUG) Log.e(TAG, "getConnectedHotIP");
	try {
	    BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
	    String line;
	    while ((line = br.readLine()) != null) {
		String[] splitted = line.split(" +");
		if (splitted != null && splitted.length >= 4) {
		    String ip = splitted[0];
		    if (Character.isDigit(ip.charAt(0))) {
			connectedIP.add(ip);
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return connectedIP;
    }

    private boolean isHasDeviceConnectedAP() {
	if (DEBUG) Log.e(TAG, "isHasDeviceConnectedAP");
	for (String ip : mConnectedIP) {
	    return true;
	}
	return false;
    }

    private String selectNeededIP() {
	ArrayList<String> connectedIP = mConnectedIP;
	for (String ip : connectedIP) {
	    return ip;
	}
	return null;
    }


    private ProgressDialog processDialog(){
	if (DEBUG) Log.e(TAG, "processDialog");
	ProgressDialog pd = new ProgressDialog(LiveDisplayActivity.this); 
	pd.setCancelable(false); 
	pd.setMessage(LiveDisplayActivity.this.getString(R.string.waiting_for_glass_open_camera)); 
	pd.setOnKeyListener(new DialogInterface.OnKeyListener(){
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mPD != null)
					    mPD.dismiss();
					finish();
				}
				return false;
			}
		});
	return pd;
    }

    private void startRtspClient(String url){
	if (DEBUG) Log.e(TAG, "startRtspClient url = " + url);
	initializeRtspClient(url);
    }

    private void initializeRtspClient(String url) {
	if (DEBUG) Log.e(TAG, "initializeRtspClient");
	if (mRtspClient == null) {
	    mRtspClient = new RtspClient(this);
	    mRtspClient.setListener(this);
	}
	mRtspClient.start(url);
    }


    private void closeRtspClient(){
    	if (DEBUG) Log.e(TAG, "closeRtspClient");
        if (mRtspClient != null) {
	    mRtspClient.close();
            mRtspClient = null;
        }
    }

    @Override
    public void onStreamDisconnect() {
	mStreamDisconnect = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_network_disconnect), getApplication().getResources().getString(R.string.live_dialog_cancle),new MyDialog.LeaveMeetingDialogListener() {
		@Override
		public void onClick(View view) {
		    switch (view.getId()) {
		    case R.id.dialog_tv_cancel_one:
			mStreamDisconnect.cancel();
			finish();
			break;
		    }
		}});
	mStreamDisconnect.setCanceledOnTouchOutside(false);
	mStreamDisconnect.setCancelable(false);
	mStreamDisconnect.show();
    }

    @Override
    public void onStreamDown() {
	mStreamDown = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_stream_disconnect_dialog_title), getApplication().getResources().getString(R.string.live_quit_dialog_cancle),new MyDialog.LeaveMeetingDialogListener() {
	@Override
        public void onClick(View view) {
	    switch (view.getId()) {
	    case R.id.dialog_tv_cancel_one:
		mStreamDown.cancel();
		finish();
		break;
		
	    }
	}});
	mStreamDown.setCanceledOnTouchOutside(false);
	mStreamDown.setCancelable(false);
	mStreamDown.show();
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
	if (DEBUG) Log.e(TAG, "onVideoSizeChanged width=" + width + " height" + height);
	LayoutParams lp = mSurfaceView.getLayoutParams();
	lp.width = width;
	lp.height = height;
	mSurfaceView.setLayoutParams(lp);
	mSurfaceView.requestLayout();
    }
    
    private boolean checkWifiAPState() {
	if (mWifiManager != null) {
	    if (mWifiManager.getWifiApState() != WifiManagerApi.WIFI_AP_STATE_ENABLED) {
		mDialog.show();
		return false;
	    }else{
		mWifiConfiguration = mWifiManager.getWifiApConfiguration();
		if (mWifiConfiguration == null) {
		    Log.e(TAG, "mWifiConfiguration null");
		}else{
		    mConnectedIP = getConnectedHotIP();
		}

		boolean isWifiDeviceConnected = isHasDeviceConnectedAP();
		if (!isWifiDeviceConnected) {
		    initQuitDialog();
		    Log.e(TAG, "no device connected to phone");
		    return false;
		}
		return true;
	    }		     
	}	
	return false;
    }

    @Override
    public void onStart() {
	if (DEBUG) Log.e(TAG, "onStart");
	super.onStart();

	mWifiDeviceConnected = true;

	if (sHasError)
	    return;

	if (!checkWifiAPState())
	    return;

	if (!checkBTEnabled()) {
	    mBluetoothConnected = false;
	    mPD.setMessage(getString(R.string.live_dialog_loading)); 
	    mPD.show(); 
	    mHandler.postDelayed(runnable1, 1000);
	    
	    //add by hky 20150707
	    String url = "";
	    if("cruise".equalsIgnoreCase(Build.BOARD))
	    	url = "rtsp://" + getIntent().getStringExtra("ip") + ":8554/recorderLive";
	    else
	    	url = "rtsp://" + selectNeededIP() + ":8554/recorderLive";
	    Log.e(TAG, "live url :" + url);
	    startRtspClient(url);
	    Log.e(TAG, "bluetooth failed url = " + url);
	}else{
	    if (DEBUG) Log.e(TAG, "bluetooth connected");
	    mBluetoothConnected = true;
	    //mHandler.postDelayed(runnable1, 100);
	    mConnectedHandler.postDelayed(runnable2, 100);
	    mPD.show();
	    if (mLiveModule != null)
		mLiveModule.sendRequestData(true);
	}

	mStart = System.currentTimeMillis();
	mDialogDismiss = false;
    }

    @Override
    public void onStop() {
	Log.e(TAG, "onStop");
	closeRtspClient();
	mDialogDismiss = false;

	mHandler.removeCallbacks(runnable1);	
        mConnectedHandler.removeCallbacks(runnable2);	
	mWifiDeviceConnected = true;
	
	if (mLiveModule != null)
	    mLiveModule.sendQuitMessage();

	if (mPD != null)
	    mPD.dismiss();

	super.onStop();
	finish();
    }

    @Override
    protected void onDestroy() {
	if (DEBUG) Log.e(TAG, "onDestroy");
	super.onDestroy();
	mPD = null;
	sActivity = null;
	sHasError = false;
    }

    public static void showCameraErrorDialog(String err) {
	sHasError = true;
        final MyDialog dialog = new MyDialog(sActivity, R.style.MyDialog, err, 
			      sActivity.getString(R.string.live_quit_dialog_cancle), null);
	dialog.setLeaveMeetingDialogListener(new MyDialog.LeaveMeetingDialogListener() {
	     @Override
	     public void onClick(View view) {
		 switch (view.getId()) {
		 case R.id.dialog_tv_cancel_one:
		     dialog.cancel();
		     sActivity.finish();
		     break;		
		 }
	     }
	});
	dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
	     @Override
	     public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		 if (keyCode == KeyEvent.KEYCODE_BACK) {
		     dialog.cancel();
		     sActivity.finish();
		 }
		 return false;
	     }
	});
	dialog.setCanceledOnTouchOutside(false);
	dialog.setCancelable(false);
	dialog.show();
    }    
}
