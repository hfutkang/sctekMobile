package cn.ingenic.glasssync.screen.live;

import android.util.Log;
import android.content.Context;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.screen.LiveDisplayActivity;

public class LiveModule extends SyncModule {
    private static final String TAG = "MobileLiveModule";
    private static final String MODULE_NAME = "live_module";
    private Boolean DEBUG = true;

    // key-value pairs
    private final String LIVE_MESSAGE = "live_message";
    private final String LIVE_ERROR = "live_err";    
    private final String LIVE_RTSP_URL = "live_rtsp_url";

    // glass message receiver
    private final int LIVE_MSG_START = 0;
    private final int LIVE_MSG_STOP = 1;
    private final int LIVE_MSG_GET_URL = 2;

    // mobile message receiver
    private final int LIVE_MSG_WIFI_CONNECTED = 1000;
    private final int LIVE_MSG_WIFI_UNCONNECTED = 1001;
    private final int LIVE_MSG_CAMERA_OPENED = 1002;
    private final int LIVE_MSG_CAMERA_NOT_OPENED = 1003;

    private static LiveModule sInstance;
    private static Boolean MessageHandle = false;
    private static final Object mHandleLock = new Object();
    private Context mContext;
    private String mUrl = null;

    private LiveModule(Context context) {
	super(MODULE_NAME, context);
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
	synchronized (mHandleLock) {
	    if(DEBUG) Log.d(TAG, "onCreate");
	    MessageHandle = true;
	}
    }
    

    @Override
    protected void onRetrive(SyncData data) {

	synchronized (mHandleLock) {
	    if(DEBUG) Log.d(TAG, "Mobile onRetrive MessageHandle = " + MessageHandle);
	    if (MessageHandle == false)
		return ;

	    int message = data.getInt(LIVE_MESSAGE);
	    switch (message) {
	    case LIVE_MSG_WIFI_CONNECTED:
		mUrl = data.getString(LIVE_RTSP_URL);
		if (DEBUG) Log.d(TAG, "LIVE_MSG_WIFI_CONNECTED mUrl = " + mUrl);
		if (LiveDisplayActivity.mPD != null) {
		    LiveDisplayActivity.mPD.setMessage(mContext.getString(R.string.live_loading));
		    LiveDisplayActivity.mPD.show();
		}
		if (mUrl != null)
		    LiveDisplayActivity.startRtspClient(mUrl);
		break;

	    case LIVE_MSG_WIFI_UNCONNECTED:
		if (DEBUG) Log.d(TAG, "LIVE_MSG_WIFI_UNCONNECTED");
		LiveDisplayActivity.showLiveErrorDialog(mContext.getString(R.string.live_wifi_disconnect));
		break;

	    case LIVE_MSG_CAMERA_OPENED:
		if (DEBUG) Log.d(TAG, "LIVE_MSG_CAMERA_OPENED");
		if (LiveDisplayActivity.mPD != null) {
		    LiveDisplayActivity.mPD.setMessage(mContext.getString(R.string.live_get_url));
		    LiveDisplayActivity.mPD.show();
		}
		sendGetURLMessage();
		break;

	    case LIVE_MSG_CAMERA_NOT_OPENED:
		if (DEBUG) Log.d(TAG, "LIVE_MSG_CAMERA_NOT_OPENED");
		String err = data.getString(LIVE_ERROR);
		if (err != null) {
		    LiveDisplayActivity.showLiveErrorDialog(err);
		}
		break;

	    default:
		Log.e(TAG, "Unknow message " + message);
		break;
	    }
	    if(DEBUG) Log.d(TAG, "Mobile onRetrive end");
	}
    }

    private void sendGetURLMessage() {

	synchronized (mHandleLock) {
	    if (DEBUG) Log.d(TAG, "sendGetURLMessage MessageHandle = " + MessageHandle);
	    if (MessageHandle == false)
		return ;

	    SyncData data = new SyncData();
	    data.putInt(LIVE_MESSAGE, LIVE_MSG_GET_URL);
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "send file sync failed:" + e);
	    }
	}
    }

    public void sendStartMessage() {
	synchronized (mHandleLock) {
	    if (DEBUG) Log.d(TAG, "sendStartMessage MessageHandle = " + MessageHandle);
	    if (MessageHandle == false)
		return ;

	    SyncData data = new SyncData();
	    data.putInt(LIVE_MESSAGE, LIVE_MSG_START);
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "send file sync failed:" + e);
	    }
	}
    }

    public void sendStopMessage() {
	synchronized (mHandleLock) {
	    if (DEBUG) Log.d(TAG, "sendStopMessage MessageHandle = " + MessageHandle);
	    if (MessageHandle == false)
		return ;

	    SyncData data = new SyncData();
	    data.putInt(LIVE_MESSAGE, LIVE_MSG_STOP);
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "send file sync failed:" + e);
	    }
	}
    }

    public void StartMessageHandle() {
	synchronized (mHandleLock) {
	    if (DEBUG) Log.d(TAG, "StartMessageHandle");
	    MessageHandle = true;
	}
    }

    public void StopMessageHandle() {
	synchronized (mHandleLock) {
	    if (DEBUG) Log.d(TAG, "StopMessageHandle");
	    MessageHandle = false;
	}
    }
}
