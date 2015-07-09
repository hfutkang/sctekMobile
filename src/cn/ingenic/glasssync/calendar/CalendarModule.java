package cn.ingenic.glasssync.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.util.Log;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.DefaultSyncManager.OnChannelCallBack;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.Transaction;

import java.util.UUID;

public class CalendarModule extends Module {

	public static final String TAG = "CalendarModule";
	public static final boolean V = true;

	public static final String CALENDAR = "calendar";
	private Context mContext;
	CalendarController mController = null;
	private boolean isRegister = false;
	public CalendarModule() {
		super(CALENDAR);
	}

	@Override
	protected Transaction createTransaction() {
		return new CalendarTransaction();
	}

	@Override
	protected void onCreate(Context context) {
		if (V) {
			Log.d(TAG, "CalendarModule created.");
		}

		mContext = context;
		mController = CalendarController.getInstance(context);
		
		mController.mContentResolver.registerContentObserver(CalendarAlerts.CONTENT_URI, true, mObserver);
		isRegister = true;
	}
	
	private ContentObserver mObserver = new ContentObserver(new Handler()){
		@Override
        public boolean deliverSelfNotifications() {
            return true;
        }
		@Override
        public void onChange(boolean selfChange) {
			Log.d(TAG,"CalendarAlerts table is onChange --> update to watch");
			super.onChange(true);
			CalendarModule.recallBackSyncMsg();
			CalendarModule.sendMsgToSync(5000);
		}
	};

	@Override
	protected void onInit() {
		boolean isEnable = DefaultSyncManager.getDefault().isFeatureEnabled(
				CALENDAR);

		sendMsgToSync(0);
	}

	@Override
	protected void onConnectivityStateChange(boolean connected) {
		super.onConnectivityStateChange(connected);
//		if (connected
//				&& (DefaultSyncManager.RIGHT_NOW_MODE == DefaultSyncManager
//						.getDefault().getCurrentMode())) {
//			sendMsgToSync(0);
//		} else {
//			recallBackSyncMsg();
//		}

	}

	@Override
	protected void onFeatureStateChange(String feature, boolean enabled) {
		if (enabled
				/*&& DefaultSyncManager.isConnect()
				&& (DefaultSyncManager.RIGHT_NOW_MODE == DefaultSyncManager
						.getDefault().getCurrentMode())*/) {
			sendMsgToSync(0);
			if(!isRegister){
				mController.mContentResolver.registerContentObserver(CalendarAlerts.CONTENT_URI, true, mObserver);
				isRegister = true;
			}
		} else {
			recallBackSyncMsg();
			if(isRegister){
				mController.mContentResolver.unregisterContentObserver(mObserver);
				isRegister = false;
			}
		}
		super.onFeatureStateChange(feature, enabled);
	}
	
	@Override
	protected void onModeChanged(int mode) {
		if (mode == DefaultSyncManager.RIGHT_NOW_MODE
				/*&& DefaultSyncManager.isConnect()*/) {
			sendMsgToSync(0);
		} 
	}

	public static void recallBackSyncMsg() {
		if (CalendarController.mHandler != null) {
			CalendarController.mHandler
					.removeMessages(CalendarController.MSG_UPDATE_ALL);
		}
	}

	public static void sendMsgToSync(long delayMillis) {
		if (CalendarController.mHandler != null) {
			Message mMessage = new Message();
			mMessage.what = CalendarController.MSG_UPDATE_ALL;
			CalendarController.mHandler.sendMessageDelayed(mMessage,
					delayMillis);
		}
	}

}
