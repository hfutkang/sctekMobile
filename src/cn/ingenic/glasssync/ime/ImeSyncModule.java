package cn.ingenic.glasssync.ime;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

public class ImeSyncModule extends SyncModule {

	final static int OP_needInput =1;  	//glass -> phone
	final static int OP_sendInput = 2;	//phone -> glass
	final static int OP_finishInput = 3;//glass -> phone
	final static int OP_closeIme = 4;	//phone -> glass
        final static int OP_sendDelete = 5;  //phone -> glass
        final static int OP_finishDelete = 6;  //glass -> phone
	
	boolean mInputUIshow= false;
	private static ImeSyncModule s;
	Context mContext;
	
	private ImeSyncModule( Context context) {
		super("IME", context);
		mContext = context;
	}
	public static ImeSyncModule getInstance(Context c){
		if(s == null)
			s = new ImeSyncModule(c);
		return s;
	}

	@Override
	protected void onCreate() {

	}
	
	@Override
	protected void onRetrive(SyncData data) {
		int op = data.getInt("op");
		switch(op){
		case OP_needInput: // state: need input
			int inputType = data.getInt("inputType");
			String textstr = data.getString("textString");
			doInput(inputType, textstr);
			break;
		case OP_finishInput:
			if (mInputUIshow)
			    doInput(-100, null);
			break;
		}
	}

        private void doInput(int type, String textstr){
		Intent intent = new Intent(mContext, InputUI.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("inputType", type);
		intent.putExtra("textString", textstr);
		mContext.startActivity(intent);
	}
	
	void sendInputing(String in,Message msg){
		SyncData.Config conf=new SyncData.Config();
		conf.mmCallback = msg;
		SyncData data = new SyncData();
		data.setConfig(conf);
		data.putInt("op", OP_sendInput);
		data.putString("input", in);
		sendToWatch(data);
	}
        void sendDelete(Message msg){
		SyncData.Config conf=new SyncData.Config();
		conf.mmCallback = msg;
		SyncData data = new SyncData();
		data.setConfig(conf);
		data.putInt("op", OP_sendDelete);
		sendToWatch(data);		
        }
	void sendCloseIme(){
		SyncData data = new SyncData();
		data.putInt("op", OP_closeIme);
		sendToWatch(data);
	}
	
	void sendDataToHoldConnect(){
		SyncData data = new SyncData();
		data.putInt("oo", 1);
		sendToWatch(data);
	}
	
	private void sendToWatch(SyncData data){
		try {
			this.send(data);
		} catch (SyncException e) {
			loge(e+"");
		}
	}
	void loge(String s){
		android.util.Log.e("ime","[ImeSyncModule]"+s);
	}
}
