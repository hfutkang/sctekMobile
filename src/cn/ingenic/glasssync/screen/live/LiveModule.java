package cn.ingenic.glasssync.screen.live;

import android.os.RemoteException;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.widget.ImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.Dialog;

import android.view.View;  
import android.app.Application; 

import java.util.ArrayList;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

import cn.ingenic.glasssync.utils.MyDialog;
import cn.ingenic.glasssync.screen.live.RtspClient;
import cn.ingenic.glasssync.screen.LiveDisplayActivity;

public class LiveModule extends SyncModule {
    private static final String TAG = "MobileLiveModule";
    public static final Boolean DEBUG = false;

    public static final String LIVE_NAME = "live_module";
    public static final String LIVE_SHARE = "live_share";
    public static final String LIVE_TRANSPORT_CMD = "live_transport_cmd";
    public static final String LIVE_QUIT_CMD = "live_quit_cmd";
    public static final String LIVE_RTSP_URL = "live_rtsp_url";

    public static final String LIVE_WIFI_CONNECTED = "live_wifi_connected";
    public static final String LIVE_WIFI_UNCONNECTED = "live_wifi_unconnected";

    public static final String LIVE_CAMERA_OPENED = "live_camera_opened";
    public static final String LIVE_CAMERA_NOT_OPENED = "live_camera_not_opened";
    
    
    public static final int TRANSPORT_WIFI_CONNECTED = 0;
    public static final int TRANSPORT_WIFI_UNCONNECTED = 1;
    public static final int TRANSPORT_CAMERA_OPENED = 2;
    public static final int TRANSPORT_CAMERA_NOT_OPENED = 3;
	
    
    private static LiveModule sInstance;
    private Context mContext;
    private String mUrl = null;
    private String mNeededIP = null;
    private StringBuilder mIP = null;

    private final int MSG_RETRY = 0;
    private Handler mHandler = new Handler(){
	@Override
	public void handleMessage(Message msg){
	    switch (msg.what) {
	    case MSG_RETRY:
		    sendRequestData(msg.arg1 == 1);
		    break;
	    }
	}
    };

    private LiveModule(Context context) {
	super(LIVE_NAME, context);
	mContext = context;
    }
    
    public static LiveModule getInstance(Context c) {
	if (null == sInstance) {
	    sInstance = new LiveModule(c);
	}
	return sInstance;
    }
    
    @Override
    protected void onCreate() {
    }
    

    @Override
    protected void onRetrive(SyncData data) {
	if(DEBUG) Log.e(TAG, "---Mobile onRetrive");

	int choice = data.getInt(LIVE_SHARE);
	if (DEBUG) Log.e(TAG, "choice = " + choice);
	switch (choice) {
	case TRANSPORT_WIFI_CONNECTED:
	    mUrl = data.getString(LIVE_RTSP_URL);
	    Log.e(TAG, "TRANSPORT_WIFI_CONNECTED mUrl = " + mUrl);
	    break;
	case TRANSPORT_WIFI_UNCONNECTED:
	    if (DEBUG) Log.e(TAG, "RANSPORT_WIFI_UNCONNECTED");
	    break;
	case TRANSPORT_CAMERA_OPENED:
	    if (DEBUG) Log.e(TAG, "RANSPORT_CAMERA_OPENED");
	    LiveDisplayActivity.mRTSPOpened = true;
	    if (isHasNeedIP()) {
		LiveDisplayActivity.mPD.setMessage(mContext.getString(R.string.live_dialog_loading));
		LiveDisplayActivity.mPD.show();
		LiveDisplayActivity.mRtspClient.start(mUrl);
	    }else{
		LiveDisplayActivity.mWifiDeviceConnected = false;
	    }
	    break;
	case TRANSPORT_CAMERA_NOT_OPENED:
	    if (DEBUG) Log.e(TAG, "TRANSPORT_CAMERA_NOT_OPENED");
	    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_RETRY, 1, 0), 2000);
	    break;
	default:
	    break;
	}
	if(DEBUG) Log.e(TAG, "---Mobile onRetrive end");
    }

    private boolean isHasNeedIP() {
	ArrayList<String> connectedIP = LiveDisplayActivity.mConnectedIP;
	if (mUrl != null) {
	    for (String ip : connectedIP) {
		if(mUrl.indexOf(ip) != -1) {
		    mNeededIP = ip;  
		    return true;
		}
	    }
	}
	return false;
    }

    public void sendRequestData(boolean bool) {
	SyncData data = new SyncData();
	if (DEBUG) Log.e(TAG, "sendRequestData");
	data.putInt(LIVE_SHARE, 1);
	data.putBoolean(LIVE_TRANSPORT_CMD, bool);
	if (DEBUG) Log.e(TAG, "bool = " + bool);
	try {
	    if (DEBUG) Log.e(TAG, "---send data " + bool);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send file sync failed:" + e);
	}
    }

    public void sendQuitMessage() {
	SyncData data = new SyncData();
	Log.e(TAG, "sendQuitMessage");
	data.putInt(LIVE_SHARE, 1);
	data.putBoolean(LIVE_QUIT_CMD, true);
	try {
	    Log.e(TAG, "---send quit message");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send file sync failed:" + e);
	}
    }

}
