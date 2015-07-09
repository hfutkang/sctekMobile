package cn.ingenic.glasssync.sms;

import android.content.Context;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidTableManager;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
public class SmsModule extends SyncModule {
        private static final String TAG = "SmsModule";
	private static SmsModule mInstance=null;
	public static String SMS_NAME="sms_module";
	private Context mContext;
	private static SmsMidManager mSmsMidManager;
        private boolean mSyncEnabled = false;
        private static final String SYNC_REQUEST = "sync_request"; //The length of not more than 15
        private CallBackHandler mCallBackHandler;
        private	Handler mHandler;
    private int MSG_SEND_FINISH = 1;
    private int MSG_SEND_FAIL = 2;
	public static SmsModule getInstance(Context context){
		if(mInstance==null){
			mInstance=new SmsModule(SMS_NAME,context);
			mSmsMidManager=new SmsMidManager(context,mInstance);
		}
		return mInstance;
	}
	

	public SmsModule(String name, Context context) {
		super(name, context, true);
		
		mContext=context;
		mSyncEnabled = false;
		HandlerThread ht = new HandlerThread("app_manager_call_back");
		ht.start();
		mCallBackHandler = new CallBackHandler(ht.getLooper());
	}
        
        @Override
	    public void setSyncEnable(boolean enabled){
	    mSyncEnabled = enabled;
	}

        @Override
	    public boolean getSyncEnable(){
	    return mSyncEnabled;
	}

        public void sendSyncRequest(boolean enabled,Handler handler) {
	    Log.d(TAG, "---sendSyncRequest");
	    SyncData data = new SyncData();
	    data.putString("command", "sync_req");
	    data.putBoolean(SYNC_REQUEST, enabled);
	    mHandler=handler;
	    SyncData.Config config = new SyncData.Config();
	    Message m = mCallBackHandler.obtainMessage();		
	    m.what = 1;
	    config.mmCallback = m;
	    data.setConfig(config);
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "---send sync failed:" + e);
	    }
	}

	@Override
	protected void onCreate() {
	
	}

	@Override
	public MidTableManager getMidTableManager() {
		
		return mSmsMidManager;
	}
	private class CallBackHandler extends Handler {
		public CallBackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(mHandler == null) return;
			switch (msg.what) {
			case 1 :
			    Log.d(TAG,"success"+msg.arg1);
			    if(msg.arg1==0){
				Message message = mHandler.obtainMessage();
				message.obj = SMS_NAME;
			        message.what = MSG_SEND_FINISH;
			        message.sendToTarget();
				Log.d(TAG, "msg.sendToTarget():"+"msg.obj="+SMS_NAME);
			    }else{
				Message message = mHandler.obtainMessage();
				message.obj = SMS_NAME;
			        message.what = MSG_SEND_FAIL;
			        message.sendToTarget();
				Log.d(TAG, "msg.sendToTarget():"+"msg.obj="+SMS_NAME);
			    }
			  
			}

		}

	}


}
