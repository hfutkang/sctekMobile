package cn.ingenic.glasssync.smssend;

import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class SMSSendModule extends SyncModule {
    private static final String TAG = "SMSSendModule";
    private static final String LETAG = "GSSSM";

    private static final String GSSMD_CMD = "gssmd_cmd";
    private static final String GSSMD_SND = "gssmd_snd";
    private static final String GSSMD_RST = "gssmd_rst";

    private static final String GSSMD_IDTF = "gssmd_idtf";
    private static final String GSSMD_TEXT = "gssmd_text";
    private static final String GSSMD_PHNUM = "gssmd_phnum";

    private static final String GSSMD_RTVAL = "gssmd_rtval";

    Context mContext = null;
    private static SMSSendModule sInstance = null;

    private SMSSendModule(Context context){
	super(LETAG, context);
	mContext = context;
	Log.e(TAG, "SMSSendModule");
    }

    public static SMSSendModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new SMSSendModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	String cmd = data.getString(GSSMD_CMD);

	if (cmd.equals(GSSMD_SND)){
	    Log.e(TAG, "GSSMD_SND");
	    long idtf = data.getLong(GSSMD_IDTF);
	    String text = data.getString(GSSMD_TEXT);
	    String phnum = data.getString(GSSMD_PHNUM);
	    Log.e(TAG, "idtf:" + idtf + " text:" + text + " phnum:" + phnum);
	    SMSSendManager ssm = SMSSendManager.getInstance(mContext);
	    ssm.sendMessage(idtf, phnum, text);
	    
	    writeToDataBase(phnum, text);
	}
    }

    public void sendResult(long idtf, int rst){
	SyncData data = new SyncData();

	data.putString(GSSMD_CMD, GSSMD_RST);
	data.putLong(GSSMD_IDTF, idtf);
	data.putInt(GSSMD_RTVAL, rst);

	try {
	    Log.e(TAG, "sendResult:" + idtf + " " + rst);
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
    
    private void writeToDataBase(String phoneNumber, String smsContent)  
    {  
        ContentValues values = new ContentValues();  
        values.put("address", phoneNumber);  
        values.put("body", smsContent);  
        values.put("type", "2");  
        values.put("read", "1");
        mContext.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);  
    }  
}