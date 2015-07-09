package cn.ingenic.glasssync.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.app.Notification;
import cn.ingenic.glasssync.notify.GlassSyncNotifyModule;
import android.app.PendingIntent;
import android.app.ActivityManager;
import java.util.List;
import android.app.ActivityManager.RecentTaskInfo;
import android.os.PowerManager;
import android.os.Bundle;
import android.app.FragmentManager;

public class GlassSyncNotifyService extends NotificationListenerService {
    private String TAG = this.getClass().getSimpleName();
    GlassSyncNotifyModule mGsnm;

    @Override
    public void onCreate() {
	Log.e(TAG, "onCreate");
        super.onCreate();
	mGsnm = GlassSyncNotifyModule.getInstance(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Log.i(TAG,"**********  onNotificationPosted");
	//Log.i(TAG,"ID :" + sbn.getId() + "ttext:" + sbn.getNotification().tickerText + " pName:" + sbn.getPackageName() + " tag:" + sbn.getTag());
	// Log.e(TAG, "" + sbn.getNotification());
	// Log.e(TAG, "" + sbn.getNotification().contentView.getPackage());

	 Log.e(TAG, "android.title " + sbn.getNotification().extras.getCharSequence("android.title"));
	// Log.e(TAG, "android.title.big " + sbn.getNotification().extras.getCharSequence("android.title.big"));
	 Log.e(TAG, "android.text " + sbn.getNotification().extras.getCharSequence("android.text"));
	// Log.e(TAG, "android.subText " + sbn.getNotification().extras.getCharSequence("android.subText"));
	// Log.e(TAG, "android.infoText " + sbn.getNotification().extras.getCharSequence("android.infoText"));
	// Log.e(TAG, "android.summaryText " + sbn.getNotification().extras.getCharSequence("android.summaryText"));

	 if (sbn.getPackageName().equals("com.tencent.mm")){
	 }else{
	     return;
	 }

	Notification ntfct = sbn.getNotification();

	mGsnm.sendNotify(ntfct);
	this.cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());

	//Log.e(TAG, "contentIntent " + ntfct.contentIntent);

        PendingIntent.OnFinished finish = new PendingIntent.OnFinished() {
		public void onSendFinished(PendingIntent pi, Intent intent,
					   int resultCode, String resultData, Bundle resultExtras) {
		    //Log.e(TAG, "d@@@@@@@@@@############");
		    // Intent i= new Intent(Intent.ACTION_MAIN); 
		    // i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
		    // i.addCategory(Intent.CATEGORY_HOME); 
		    // startActivity(i);
		}
	    };

	try {
	    ntfct.contentIntent.send(0, null, null);
	}catch (PendingIntent.CanceledException e) {
	}

	PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
	if (pm.isScreenOn()){
	    try {
		Thread.sleep(500);
	    } catch (InterruptedException e) {
	    }

	    ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
	    List<ActivityManager.RecentTaskInfo> run = am.getRecentTasks(2, 1);
	    //Log.e(TAG, "getRecentTasks size:" + run.size());
	    ActivityManager.RecentTaskInfo mRt = run.get(0);
	    //Log.e(TAG, "description " + mRt.description + " " + mRt.origActivity + " " + mRt.id);
	    mRt = run.get(1);
	    //Log.e(TAG, "description " + mRt.description + " " + mRt.origActivity + " " + mRt.id);

	    if (mRt.id > 0){
		am.moveTaskToFront(mRt.id, 0);
	    }else{
		Intent i= new Intent(Intent.ACTION_MAIN); 
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
		i.addCategory(Intent.CATEGORY_HOME); 
		startActivity(i);
	    }
	}
	//ntfct.contentIntent.cancel();

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}