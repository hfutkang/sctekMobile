package com.sctek.smartglasses.language;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;
import com.sctek.smartglasses.fragments.SettingFragment;
public class LanguageModule extends SyncModule {
	private final String TAG = "LanguageModule";
	public static String SMS_NAME = "lang_module";
	private final String LANGUAGE_TYPE = "languageType";
        private final String SYNC_RESULT = "result";
        private final int COMPLETE = 1;
        private final int DEFALUT_LANGUAGE = 2;
	private static LanguageModule mInstance;
        private Handler mHandler;
        private Context mContext;

	public static LanguageModule getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new LanguageModule(context);
		}
		return mInstance;
	}

	private LanguageModule(Context context) {
		super(SMS_NAME, context);
		mContext  = context;
	}

        @Override
	protected void onCreate() {

	}

      public void sendSyncRequest(int languageType,Handler handler) {
		Log.d(TAG, "---sendSyncRequest :: languageType = "+ languageType);
		mHandler = handler;
		SyncData data = new SyncData();
		data.putInt(LANGUAGE_TYPE, languageType);
		try {
			send(data);
		} catch (SyncException e) {
			Log.e(TAG, "---send sync failed:" + e);
		}
	}

        @Override
	protected void onRetrive(SyncData data) {
	    int type = data.getInt("type");
	    boolean result = data.getBoolean("result",false);
	    Log.d(TAG,"onRetrive::type =" + type + " result = "+result);
	    switch(type){
	    case COMPLETE :
		Message msg = mHandler.obtainMessage();
		msg.what = SettingFragment.LANGUAGE_SETTING;
		msg.obj = result;
		mHandler.sendMessage(msg);	
		break;
	    case DEFALUT_LANGUAGE:
		SharedPreferences pref = PreferenceManager
		    .getDefaultSharedPreferences(mContext);
		Editor editor = pref.edit();
		if (result)
		    editor.putString("language", mContext.getString(R.string.language_zh));
		else 
		    editor.putString("language", mContext.getString(R.string.language_us));
		editor.commit();
		break;
	    }
	    
	}

}

