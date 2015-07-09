package cn.ingenic.glasssync.notify;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import android.app.Notification;

public class GlassSyncNotifyModule extends SyncModule {
    private static final String TAG = "GlassSyncNotifyModule";
    private static final String LETAG = "GSNFMD";

    public static final String EXTRA_TITLE = "android.title";
    public static final String EXTRA_TEXT = "android.text";

    private Context mContext;
    private static GlassSyncNotifyModule sInstance;

    private GlassSyncNotifyModule(Context context){
	super(LETAG, context);
	mContext = context;
    }

    public static GlassSyncNotifyModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncNotifyModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    public void sendNotify(Notification notify) {
	SyncData data = new SyncData();

	data.putString(EXTRA_TITLE, notify.extras.getCharSequence(EXTRA_TITLE).toString());
	data.putString(EXTRA_TEXT, notify.extras.getCharSequence(EXTRA_TEXT).toString());

	try {
	    Log.e(TAG, "send notify() " + notify);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
}