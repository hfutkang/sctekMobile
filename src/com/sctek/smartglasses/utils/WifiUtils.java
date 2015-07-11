package com.sctek.smartglasses.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;


public class WifiUtils {
	
	private static final String TAG = "WifiUtils";
	
	protected static final int WIFI_AP_STATE_UNKNOWN = -1;
	protected static final int WIFI_AP_STATE_DISABLING = 10;
	protected static final int WIFI_AP_STATE_DISABLED = 11;
	protected static final int WIFI_AP_STATE_ENABLING = 12;
	protected static final int WIFI_AP_STATE_ENABLED = 13;
	protected static final int WIFI_AP_STATE_FAILED = 14;

	public static void turnWifiApOn(Context mContext, WifiManager mWifiManager) {
		
		String defaultSsid = ((TelephonyManager)mContext
				.getSystemService(mContext.TELEPHONY_SERVICE)).getDeviceId().substring(0, 5);
		String ssid = PreferenceManager.
				getDefaultSharedPreferences(mContext).getString("ssid", defaultSsid);
		
		String pw = PreferenceManager.
				getDefaultSharedPreferences(mContext).getString("pw", "12345678");
		
		WifiConfiguration wcfg = new WifiConfiguration();
		wcfg.SSID = new String(ssid);
		wcfg.networkId = 1;
		wcfg.allowedAuthAlgorithms.clear();
		wcfg.allowedGroupCiphers.clear();
		wcfg.allowedKeyManagement.clear();
		wcfg.allowedPairwiseCiphers.clear();
		wcfg.allowedProtocols.clear();
		
			wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN, true);
	      wcfg.wepKeys[0] = "";    
	      wcfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);    
	      wcfg.wepTxKeyIndex = 0;
		
//		wcfg.preSharedKey = pw;     
//		wcfg.hiddenSSID = true;       
//		wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);       
//		wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);                             
//		wcfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);     
////		wcfg.allowedKeyManagement.set(4);
//		wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);                        
//		wcfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);      
//		wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);    
//		wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);    
		
		try {
			Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", wcfg.getClass());
			                                           
			Boolean rt = (Boolean)method.invoke(mWifiManager, wcfg);
			Log.d("setconfig", " " + rt);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			Log.d("setconfig", " no method");
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			Log.d("setconfig", " illegeal argument");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			Log.d("setconfig", " illegal access");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			Log.d("setconfig", " invocation failed");
			e.printStackTrace();
		}
		toggleWifi(mContext, mWifiManager);
	}
	
	public static void toggleWifi(Context mContext, WifiManager mWifiManager) {
        boolean wifiApIsOn = getWifiAPState(mWifiManager)==WIFI_AP_STATE_ENABLED || getWifiAPState(mWifiManager)==WIFI_AP_STATE_ENABLING;
//        new SetWifiAPTask(mContext, mWifiManager, !wifiApIsOn, false).execute();
        setWifiApEnabled(!wifiApIsOn, mWifiManager);
        
    }
	
	public static int getWifiAPState(WifiManager mWifiManager) {
			int state = WIFI_AP_STATE_UNKNOWN;
			try {
				Method method2 = mWifiManager.getClass().getMethod("getWifiApState");
				state = (Integer) method2.invoke(mWifiManager);
			} catch (Exception e) {}
			Log.d("WifiAP", "getWifiAPState.state " + state);
			return state;
    }
	
	public static int setWifiApEnabled(boolean enabled, WifiManager mWifiManager) {
		
		Log.d("WifiAP", "*** setWifiApEnabled CALLED **** " + enabled);
		if (enabled && mWifiManager.getConnectionInfo() !=null) {
			mWifiManager.setWifiEnabled(false);
			
			try {Thread.sleep(1500);} catch (Exception e) {}
		}
		
		int state = WIFI_AP_STATE_UNKNOWN;
		try {
			mWifiManager.setWifiEnabled(false);
			Method method1 = mWifiManager.getClass().getMethod("setWifiApEnabled",
			    WifiConfiguration.class, boolean.class);
			method1.invoke(mWifiManager, null, enabled); // true
			Method method2 = mWifiManager.getClass().getMethod("getWifiApState");
			state = (Integer) method2.invoke(mWifiManager);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		
		if (!enabled) {
			int loopMax = 10;
			while (loopMax>0 && (getWifiAPState(mWifiManager)==WIFI_AP_STATE_DISABLING
					|| getWifiAPState(mWifiManager)==WIFI_AP_STATE_ENABLED
					|| getWifiAPState(mWifiManager)==WIFI_AP_STATE_FAILED)) {
						try {Thread.sleep(500);loopMax--;} catch (Exception e) {}
			}
			mWifiManager.setWifiEnabled(true);
		} 
		else if (enabled) {
			int loopMax = 10;
			while (loopMax>0 && (getWifiAPState(mWifiManager)==WIFI_AP_STATE_ENABLING
					|| getWifiAPState(mWifiManager)==WIFI_AP_STATE_DISABLED
					|| getWifiAPState(mWifiManager)==WIFI_AP_STATE_FAILED)) {
						try {Thread.sleep(500);loopMax--;} catch (Exception e) {}
			}
		}
		return state;
	}
	
	
}
