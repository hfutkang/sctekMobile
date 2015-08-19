package com.sctek.smartglasses.utils;

import com.ingenic.glass.api.sync.SyncChannel.Packet;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

public class WifiApStateBroadcastReceiver extends BroadcastReceiver{

	private static final String TAG = "WifiApStateBroadcastReceiver";
	
	public static final String WIFI_AP_STATE_CHANGED_ACTION =
	        "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";
	
	protected static final int WIFI_AP_STATE_DISABLED = 11;
	protected static final int WIFI_AP_STATE_ENABLED = 13;
	
	public final static int TURN_WIFI_OFF = 18;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			int cstate = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1);
			int pstate = intent.getIntExtra(EXTRA_PREVIOUS_WIFI_AP_STATE, -1);
			Log.e(TAG, WIFI_AP_STATE_CHANGED_ACTION + ", current state:" + cstate + ",previous state:" + pstate);
			
			if(cstate == WIFI_AP_STATE_DISABLED) {
				HanLangCmdChannel channel = HanLangCmdChannel.getInstance(context);
				Packet packet = channel.createPacket();
				packet.putInt("type", TURN_WIFI_OFF);
				channel.sendPacket(packet);
			}
		}
	}

}
