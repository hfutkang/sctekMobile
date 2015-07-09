package cn.ingenic.glasssync.appmanager;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.appmanager.PhoneCommon.SimpleBase;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

public class MessageSender {
	private final boolean DEBUG = true;
	private final String APP = "ApplicationManager";

	private CallBackHandler mCallBackHandler;
	private Context mContext;
	private static Map<String, Integer> mMessageCallBackMap = null;
	private AppCache mAppCache;
	private SimpleFragment mSimpleFragment;

	private static MessageSender mMessageSender = null;

	public static MessageSender getInstance(Context context) {
		if (mMessageSender == null) {
			mMessageSender = new MessageSender(context);
		}
		return mMessageSender;
	}

	private MessageSender(Context context) {
		mContext = context;
		HandlerThread ht = new HandlerThread("app_manager_call_back");
		ht.start();
		mCallBackHandler = new CallBackHandler(ht.getLooper());
		mAppCache = AppCache.getInstance();
	}

	static {
		if (mMessageCallBackMap == null || mMessageCallBackMap.size() == 0) {
			mMessageCallBackMap = new HashMap<String, Integer>();
			mMessageCallBackMap.put(PhoneCommon.MessageKey.GET_APP_INFOS_KEY
					+ PhoneCommon.MessageValue.INSTALL, CallBack.INSTALL);
			mMessageCallBackMap.put(PhoneCommon.MessageKey.GET_APP_INFOS_KEY
					+ PhoneCommon.MessageValue.ALL, CallBack.ALL);
			mMessageCallBackMap.put(PhoneCommon.UNINSTALL_MESSAGE,
					CallBack.UNINSTALL);
			mMessageCallBackMap.put(PhoneCommon.MessageKey.GET_APP_INFOS_KEY
					+ CallBack.START_WITHOUT_MODE, CallBack.START_WITHOUT_MODE);
			mMessageCallBackMap.put(PhoneCommon.CONNECT_MESSAGE, CallBack.CONNECT_MESSAGE);
		}
	}
	

	private Activity mFirstInitActivity=null;
	public void getAppInfos(Activity activity) {
		mFirstInitActivity=activity;
		SyncData data = new SyncData();
		data.putString(PhoneCommon.AppDataKey.COMMON, PhoneCommon.MESSAGE);
		data.putInt(PhoneCommon.MessageKey.GET_APP_INFOS_KEY,
				PhoneCommon.SimpleBase.INSTALL);
		send(data, PhoneCommon.MessageKey.GET_APP_INFOS_KEY
				+ CallBack.START_WITHOUT_MODE);
		mAppCache.onInstallStart();
	}

	public final void getAppInfos(int mode) {
		if (DEBUG)
			Log.i(APP,
					"send GET APP INFOS message , mode is :"
							+ mode);
		SyncData data = new SyncData();
		data.putString(PhoneCommon.AppDataKey.COMMON, PhoneCommon.MESSAGE);
		data.putInt(PhoneCommon.MessageKey.GET_APP_INFOS_KEY, mode);
		send(data, PhoneCommon.MessageKey.GET_APP_INFOS_KEY + mode);
		if (mode == PhoneCommon.SimpleBase.ALL) {
			mAppCache.onAllStart();
		} else if (mode == PhoneCommon.SimpleBase.INSTALL) {
			mAppCache.onInstallStart();
		}
	}
	
	public void sendConnectMessage(){
		if (DEBUG)Log.i(APP," send connect message ");
		SyncData data = new SyncData();
		data.putString(PhoneCommon.AppDataKey.COMMON, PhoneCommon.CONNECT_MESSAGE);
		send(data,PhoneCommon.CONNECT_MESSAGE);
	}

	public void sendUnInstallMessage(String packageName,SimpleFragment sf) {
		SyncData data = new SyncData();
		data.putString(PhoneCommon.AppDataKey.COMMON,
				PhoneCommon.UNINSTALL_MESSAGE);
		data.putString(PhoneCommon.AppDataKey.PACKAGE_NAME, packageName);
		send(data, PhoneCommon.UNINSTALL_MESSAGE);
		this.mSimpleFragment=sf;
		sf.showUnInstallDialog();
	}

	private void send(SyncData data, String key) {
		if (DEBUG)
			Log.i(APP, "ApplicationManagerActivity2 send .");
		AppManagerModule module = AppManagerModule.getInstance(mContext);
		SyncData.Config config = new SyncData.Config();
		Message m = mCallBackHandler.obtainMessage();
		
		m.what = mMessageCallBackMap.get(key);
		config.mmCallback = m;
		data.setConfig(config);
		try {
			module.send(data);
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	private interface CallBack {
		public static final int INSTALL = SimpleBase.INSTALL;
		public static final int ALL = SimpleBase.ALL;
		public static final int UNINSTALL = 2;
		public static final int START_WITHOUT_MODE = 3;
		public static final int CONNECT_MESSAGE=4;
	}

	private class CallBackHandler extends Handler {
		public CallBackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case CallBack.INSTALL:
			case CallBack.ALL:
				if (DEBUG)
					Log.i(APP, "getAppInfos CALLBACK result :" + msg.arg1);
				if (msg.arg1 != SyncModule.SUCCESS) {
					Toast.makeText(mContext,
							R.string.bluetooth_connect_wrong_message,
							Toast.LENGTH_LONG).show();
				}
				break;
			case CallBack.UNINSTALL:
				if (DEBUG)
					Log.i(APP, "UnInstall CALLBACK result :" + msg.arg1);
				if (msg.arg1 != SyncModule.SUCCESS) {
					mSimpleFragment.stopDialog();
					Toast.makeText(mContext,
							R.string.uninstall_message_send_failed,
							Toast.LENGTH_LONG).show();
				}
				break;
			case CallBack.START_WITHOUT_MODE:
				if (DEBUG)
					Log.i(APP, "start without mode CALLBACK result :" + msg.arg1);
				if (msg.arg1 != SyncModule.SUCCESS) {
					Toast.makeText(mContext,
							R.string.bluetooth_connect_wrong_message,
							Toast.LENGTH_LONG).show();
					if(mFirstInitActivity!=null)mFirstInitActivity.finish();
				}
				break;
			case CallBack.CONNECT_MESSAGE:
				if (DEBUG)
					Log.i(APP, "Connect message CALLBACK result :" + msg.arg1);
				break;
			}

		}

	}

}
