package cn.ingenic.glasssync.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.ui.Fragment_Setting;

public class GlassSyncWifiModule extends SyncModule {
    private static final String TAG = "GlassSyncWifiModule";
    private static final String LETAG = "GSWFMD";

    private static final String GSWFMD_SSID = "gswfmd_ssid";
    private static final String GSWFMD_PSD = "gswfmd_psd";
    private static final String GSWFMD_MGMT = "gswfmd_mgmt";
    private static final String GSWFMD_PROP = "gswfmd_prop";
    private static final String GSWFMD_GRP = "gswfmd_grp";
    private static final String GSWFMD_CONN = "gswfmd_conn";

    private static final String GSWFMD_CMD = "gswfmd_cmd";
    private static final String GSWFMD_RQWF = "gswfmd_rqwf";
    private static final String GSWFMD_SDWF = "gswfmd_sdwf";
    private static final String GSWFMD_RESULT = "gswfmd_result";
    private Context mContext;
    private Handler mHandler;
    private static GlassSyncWifiModule sInstance;
    private GlassSyncWifiModule(Context context,Handler handler){
	super(LETAG, context);
	mContext = context;
	mHandler = handler;
    }

    public static GlassSyncWifiModule getInstance(Context c,Handler handler) {
	if (null == sInstance)
	    sInstance = new GlassSyncWifiModule(c,handler);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onRetrive(SyncData data) {
    	String cmd = data.getString(GSWFMD_CMD);
    	boolean result = data.getBoolean(GSWFMD_RESULT,false);
    	if (cmd == null)
    	    return;
    	if (cmd.equals(GSWFMD_SDWF) && mHandler != null){
	    if(result == true){
		Message wifiSyncFinish = new Message();
		wifiSyncFinish.what = Fragment_Setting.MSG_SEND_FINISH;
		wifiSyncFinish.obj = Fragment_Setting.WIFI_KEY;
		mHandler.sendMessage(wifiSyncFinish);
	    }else if(result == false){
		Message wifiSyncFail = new Message();
		wifiSyncFail.what = Fragment_Setting.MSG_SEND_FAIL;
		wifiSyncFail.obj = Fragment_Setting.WIFI_KEY;
		mHandler.sendMessage(wifiSyncFail);
	    }
    	}
    }

    public void send_Wifi(String ssid, String pwd){
	if (ssid == null)
	    return;

	SyncData data = new SyncData();
	data.putString(GSWFMD_CMD, GSWFMD_SDWF);
	data.putString(GSWFMD_SSID, ssid);

	if (pwd != null)
	    data.putString(GSWFMD_PSD, pwd);
	try {
	    Log.e(TAG, "send wifi() name=" + ssid+"  pwd="+pwd);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
}