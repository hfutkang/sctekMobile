package cn.ingenic.glasssync.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cn.ingenic.glasssync.DefaultSyncManager;
public class CalendarSyncReceiver extends BroadcastReceiver {
	CalendarController mController;
	private static final String TAG = "CalendarSyncReceiver";
	public final static String ACTION_DELETE_EVENT_FROM_WATCH = "com.android.calendar.action_delete_event_from_watch";


	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(TAG, "receiver action:" + action);
		if (action.equals(Intent.ACTION_PROVIDER_CHANGED)) {
//			CalendarModule.recallBackSyncMsg();
//			CalendarModule.sendMsgToSync(100);
		}
	}

}
