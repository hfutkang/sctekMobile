package cn.ingenic.glasssync.devicemanager;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.util.Log;

public class WifiManagerApi {
    
    private final boolean DEBUG = false;
    
    private final String TAG = "WifiManager";

    public static int WIFI_AP_STATE_ENABLED = WifiManager.WIFI_AP_STATE_ENABLED;

    private Context mContext;

    private WifiManager mWifiManager;
    

    public  WifiManagerApi(Context context){
	if(DEBUG) Log.e(TAG, "WifiManagerApi");
	mContext = context;
	mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public int getWifiApState(){
	int wifiApState = mWifiManager.getWifiApState();
	return wifiApState;
    }
    
    public WifiConfiguration  getWifiApConfiguration(){
	
	return	mWifiManager.getWifiApConfiguration();
    }
   


}