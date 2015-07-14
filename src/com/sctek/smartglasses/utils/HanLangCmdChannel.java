package com.sctek.smartglasses.utils;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class HanLangCmdChannel {
	
	private static final String TAG = "HanLangCmdChannel";
	
	private static final String CMD_CHANNEL_NAME = "cmdchannel";
	
	private SyncChannel mChannel;
	
	private static HanLangCmdChannel instance;
	
	private Handler mHandler;
	
	public static final int RECEIVE_MSG_FROM_GLASS = 101;
	
	public final static int GET_GLASS_INFO = 17;
	
	private Context mContext;

	private HanLangCmdChannel(Context context) {
		
		mChannel = SyncChannel.create(CMD_CHANNEL_NAME, context, mOnSyncListener);
		mContext = context;
	}
	
	public static HanLangCmdChannel getInstance(Context context	) {
		if(instance == null)
			instance = new HanLangCmdChannel(context);
		return instance;
	}
	
	public Packet createPacket() {
		return mChannel.createPacket();
	}
	
	public void sendPacket(Packet pk) {
		mChannel.sendPacket(pk);
	}
	
	public boolean isConnected() {
		return mChannel.isConnected();
	}
	
	public void sendInt (String key, int value) {
		Packet packet = mChannel.createPacket();
		packet.putInt(key, value);
		mChannel.sendPacket(packet);
	}
	
	public void sendBoolean (String key, boolean value) {
		Packet packet = mChannel.createPacket();
		packet.putBoolean(key, value);
		mChannel.sendPacket(packet);
	}
	
	public void sendString (String key, String value) {
		Packet packet = mChannel.createPacket();
		packet.putString(key, value);
		mChannel.sendPacket(packet);
	}
	
	public void sendFloat (String key, float value) {
		Packet packet = mChannel.createPacket();
		packet.putFloat(key, value);
		mChannel.sendPacket(packet);
	}
	
	public void setHandler (Handler handler) {
		mHandler = handler;
	}
	
	private MyOnSyncListener mOnSyncListener = new MyOnSyncListener();
	private class MyOnSyncListener implements SyncChannel.onChannelListener {
	
		@Override
		public void onReceive(RESULT arg0, Packet data) {
			// TODO Auto-generated method stub
			Log.e(TAG, "Channel onReceive");
			
			if(GET_GLASS_INFO == data.getInt("type")) {
				
				String model = data.getString("model");
				String cpu = data.getString("cpu");
				String version = data.getString("version");
				String serial = data.getString("serial");
				
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
				Editor editor = preferences.edit();
				editor.putString("model", model);
				editor.putString("cpu", cpu);
				editor.putString("version", version);
				editor.putString("serial", serial);
				editor.commit();
			}
			else if(mHandler != null) {
				Message msg =	mHandler.obtainMessage(RECEIVE_MSG_FROM_GLASS, data);
				msg.sendToTarget();
			}
			
			
		}
	
		@Override
		public void onSendCompleted(RESULT result, Packet arg1) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onSendCompleted:" + result.name());
		}
	
		@Override
		public void onStateChanged(CONNECTION_STATE arg0) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onStateChanged:" + arg0.name());
		}
		
	}
}
