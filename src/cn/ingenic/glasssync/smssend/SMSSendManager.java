package cn.ingenic.glasssync.smssend;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.telephony.SmsManager;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.IntentFilter;

public class SMSSendManager {
    private static final String TAG = "SMSSendManager";
    private static final String INTENTEXTRA = "intentextra";
    
    private ArrayList <waitMsg> mWaitMsg = new ArrayList<waitMsg>();

    Context mContext = null;
    private static SMSSendManager sInstance = null;
    private SentActionReceiver nReceiver;
    private Handler mMessageSentHandler;

    private SMSSendManager(Context context){
	mContext = context;

	mMessageSentHandler = new Handler();
	mMessageSentHandler.postDelayed(new MessageSentProcess(), 50);

	nReceiver = new SentActionReceiver();
	IntentFilter filter = new IntentFilter();
        filter.addAction("SENT_SMS_ACTION");
	mContext.registerReceiver(nReceiver, filter);

	Log.e(TAG, "SMSSendManager");

	SMSSendModule ssm = SMSSendModule.getInstance(context);
    }

    public static SMSSendManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new SMSSendManager(c);
	return sInstance;
    }

    public void addWaitMsg(long idtf, String phnum, String content){
	if (idtf == 0 || phnum == null || content == null)
	    return;

	waitMsg wm = new waitMsg();

	wm.idtf = idtf;
	wm.phnum = phnum;
	wm.content = content;

	mWaitMsg.add(wm);
    }

    class MessageSentProcess implements Runnable {
	public void run() {
	    if (mWaitMsg.isEmpty()){
		mMessageSentHandler.postDelayed(new MessageSentProcess(), 300);
		return;
	    }

	    waitMsg wm = mWaitMsg.get(0);
	    sendMessage(wm.idtf, wm.phnum, wm.content);
	    mWaitMsg.remove(wm);
	    mMessageSentHandler.postDelayed(new MessageSentProcess(), 50);
	}
    }

    public void sendMessage(long idtf, String phnum, String content){
	SmsManager smsManager = SmsManager.getDefault();

	Intent sentIntent = new Intent("SENT_SMS_ACTION");
	sentIntent.putExtra(INTENTEXTRA, idtf);
	PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0, sentIntent, 0);
	ArrayList<String> divideContents = smsManager.divideMessage(content);
	int ContentCount = divideContents.size();
	ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(ContentCount);

	for(int i = 0;i<ContentCount;i++){  
	    sentIntents.add(sentPI);
    }  
	smsManager.sendMultipartTextMessage(phnum, null, divideContents, sentIntents, null);
    }

    class SentActionReceiver extends BroadcastReceiver{
        @Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.getAction().equals("SENT_SMS_ACTION")){
		Log.e(TAG, "onReceive " + intent.getAction());
		long idtf = intent.getLongExtra(INTENTEXTRA, 0l);
		if (idtf <= 0)
		    return;

		int rst = 1;
		switch (getResultCode()) {
		case Activity.RESULT_OK:
		    Log.e(TAG, "send sms succ");
		    rst = 0;
		    break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
		case SmsManager.RESULT_ERROR_RADIO_OFF:
		case SmsManager.RESULT_ERROR_NULL_PDU:
		    Log.e(TAG, "send sms fail");
		    rst = 1;
		    break;
		}

		SMSSendModule ssm = SMSSendModule.getInstance(mContext);
		ssm.sendResult(idtf, rst);
	    }	    
        }
    }

    private class waitMsg{
	long idtf;
	String phnum;
	String content;
    }
}