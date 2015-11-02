package com.sctek.smartglasses.utils;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.ingenic.glass.api.sync.SyncChannel.onChannelListener;

import cn.ingenic.glasssync.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

public class HanLangNotifyChannel {
	
	private final static String TAG = "HanLangNotifyChannel";

	private static final String NTF_CHANNEL_NAME = "ntfchannel";
	
	private final static int MSG_TYPE_LOW_POWER = 2;
	private final static int MSG_TYPE_PHONE = 1;
	private final static int REPORT_UPDATE_STATE = 21;
	
	public final static int UPDATE_TRY_CONNECT_WIFI = 0;
	public final static int UPDATE_CONNECTI_WIFI_TIMEOUT = 1;
	public final static int UPDATE_START_DOWNLOAD = 2;
	public final static int UPDATE_DOWNLOAD_ERROR = 3;
	public final static int UPDATE_INVALID_PACKAGE = 4;
	public final static int UPDATE_START = 5;
	public final static int UPDATE_SUCCESS = 6;
	public final static int UPDATE_FAILE = 7;
	
	private final static int updateMsgs[] = {R.string.update_try_connect_wifi, R.string.update_connect_wifi_timeout, R.string.update_start_download,
																										R.string.update_donwload_error, R.string.update_invalid_package, R.string.update_start,
																										R.string.update_success, R.string.update_fail, R.string.update_power_shortage, R.string.update_storage_shortage};
	
	private SyncChannel mChannel;
	
	private static HanLangNotifyChannel instance;
	
	private Context mContext;
	
	private HanLangNotifyChannel(Context context) {
		
		mChannel = SyncChannel.create(NTF_CHANNEL_NAME, context, mOnSyncListener);
		mContext = context;
	}
	
	public static HanLangNotifyChannel getInstance(Context context	) {
		if(instance == null)
			instance = new HanLangNotifyChannel(context);
		return instance;
	}
	
	private onChannelListener mOnSyncListener = new onChannelListener() {
		
		@Override
		public void onStateChanged(CONNECTION_STATE state) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onStateChanged:" + state.toString());
		}
		
		@Override
		public void onServiceConnected() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onSendCompleted(RESULT result, Packet packet) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onSendCompleted:" + result.toString());
		}
		
		@Override
		public void onReceive(RESULT result, Packet data) {
			// TODO Auto-generated method stub
			Log.e(TAG, "Channel onReceive");
			int type = data.getInt("type");
			int state = data.getInt("state");
			switch (type) {
			case MSG_TYPE_LOW_POWER:
				NotificationManager notificationManager =  
				(NotificationManager)(mContext.getSystemService(Context.NOTIFICATION_SERVICE));
				Notification.Builder builder= new Notification.Builder(mContext)
				.setContentTitle(mContext.getResources().getString(R.string.low_power))
				.setContentText(mContext.getResources().getString(R.string.low_power_msg))
				.setVibrate(new long[]{0,100,200,300})
				.setSmallIcon(R.drawable.hanlang_icon);
				Notification notification = builder.build();
				notificationManager.notify(3, notification);
				break;
			case REPORT_UPDATE_STATE:
				showUpdateNotification(state);
				break;
			}
		}
	};
	
	private void showUpdateNotification(int state) {
		NotificationManager notificationManager =  
				(NotificationManager)(mContext.getSystemService(Context.NOTIFICATION_SERVICE));
				Notification.Builder builder= new Notification.Builder(mContext);
				builder.setSmallIcon(R.drawable.hanlang_icon);
				builder.setContentTitle(mContext.getText(R.string.hanlang_update));
				builder.setContentText(mContext.getText(updateMsgs[state]));
				Notification notification = builder.getNotification();
				notificationManager.notify(4, notification);
	}
}
