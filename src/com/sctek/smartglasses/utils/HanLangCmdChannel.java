package com.sctek.smartglasses.utils;

import java.util.zip.Inflater;

import com.fota.iport.ICheckVersionCallback;
import com.fota.iport.MobAgentPolicy;
import com.fota.iport.config.MobileParamInfo;
import com.fota.iport.config.VersionInfo;
import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.sctek.smartglasses.ui.MainActivity;

import cn.ingenic.glasssync.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class HanLangCmdChannel {
	
	private static final String TAG = "HanLangCmdChannel";
	
	private static final String CMD_CHANNEL_NAME = "cmdchannel";
	
	private static final String HANLANG_FOTA_TOKE = "fb5c379aeed5277fdf4b89c797af1bcd";
	
	private SyncChannel mChannel;
	
	private static HanLangCmdChannel instance;
	
	private Handler mHandler;
	
	public static final int RECEIVE_MSG_FROM_GLASS = 101;
	
	public final static int GET_GLASS_INFO = 17;
	
	public final static int UPDATE_CONNECT_WIFI_MSG =20;
	
	private Context mContext;

	private HanLangCmdChannel(Context context) {
		
		mChannel = SyncChannel.create(CMD_CHANNEL_NAME, context, mOnSyncListener);
		mContext = context;
		mHandler = new Handler();
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
		public void onServiceConnected() {
			Log.d(TAG, "onServiceConnected ");
		}	
		@Override
		public void onReceive(RESULT arg0, Packet data) {
			// TODO Auto-generated method stub
			Log.e(TAG, "Channel onReceive");
			
			if(GET_GLASS_INFO == data.getInt("type")) {
				
				String model = data.getString("model");
				String cpu = data.getString("cpu");
				String version = data.getString("version");
				String serial = data.getString("serial");
				int volume = data.getInt("volume");
				boolean round = data.getBoolean("round");
				String duration = data.getString("duration");
				
				Log.e(TAG, "volume:" + volume + " round:" + round + "duration:" + duration + "serial:" + serial); 
				
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
				Editor editor = preferences.edit();
				editor.putString("model", model);
				editor.putString("cpu", cpu);
				editor.putString("version", version);
				editor.putString("serial", serial);
				editor.putInt("volume", volume);
				editor.putBoolean("round_video", round);
				editor.putString("duration", duration);
				editor.commit();
				
				checkDeviceVersion(data);
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
	
	private void checkDeviceVersion(Packet pk) {
		MobAgentPolicy.initConfig(mContext.getApplicationContext());
    	Log.e(TAG, "checkDeviceVersion");
        //检测版本所需参数
        final MobileParamInfo mobileParamInfo = new MobileParamInfo();
//        mobileParamInfo.mid = pk.getString("serial");
        mobileParamInfo.mid = "1111111";
        mobileParamInfo.version = pk.getString("version");
        mobileParamInfo.oem = pk.getString("oem");
        mobileParamInfo.models = pk.getString("models");
        mobileParamInfo.token = HANLANG_FOTA_TOKE;
        mobileParamInfo.platform = pk.getString("platform");
        mobileParamInfo.deviceType = pk.getString("deviceType");

        if (!isValidToDownload(mobileParamInfo)) {
            return;
        }

        //检测版本
        MobAgentPolicy.checkVersion(mContext, mobileParamInfo, new ICheckVersionCallback() {
            @Override
            public void onCheckSuccess(int status) {
                Log.e(TAG, "==================status" + status);
                mHandler.sendEmptyMessage(3);
            }

            @Override
            public void onCheckFail(final int status, final String errorMsg) {
                Log.e(TAG, errorMsg);

            }

            @Override
            public void onInvalidDate() {
                Log.e(TAG, "Remote respond invalid message");
            }
        });
    }
	
	private boolean isValidToDownload(MobileParamInfo mobileParamInfo) {
        if (TextUtils.isEmpty(mobileParamInfo.mid)
                || TextUtils.isEmpty(mobileParamInfo.version)
                || TextUtils.isEmpty(mobileParamInfo.oem)
                || TextUtils.isEmpty(mobileParamInfo.models)
                || TextUtils.isEmpty(mobileParamInfo.token)
                || TextUtils.isEmpty(mobileParamInfo.platform)
                || TextUtils.isEmpty(mobileParamInfo.deviceType)
                ) {
            return false;
        }
        return true;
    }
	
}
