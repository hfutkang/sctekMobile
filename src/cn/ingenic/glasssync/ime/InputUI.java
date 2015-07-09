package cn.ingenic.glasssync.ime;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import cn.ingenic.glasssync.R;

public class InputUI extends Activity {

	public static final String TAG="ime";
	private static final int MSG_sending=1;
	private static final int MSG_hold = 2;
        private static final int MSG_delete = 3;
	private static long Delay_Hold = cn.ingenic.glasssync.DefaultSyncManager.TIMEOUT-5000;
	EditText et1;
        Button mSend,mReConnect,mDelete;
	ImeSyncModule ime;
	Handler mHandler;
	ConnectionStateReceiver mCSReceiver;
	private boolean mIsFinish;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreat===in");
		ime = ImeSyncModule.getInstance(this);
		setContentView(R.layout.ime_input);
		et1 = (EditText)findViewById(R.id.et1);
		findViewById(R.id.tv1).setEnabled(false);;
		mReConnect = (Button)findViewById(R.id.ime_re);
		mReConnect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mHandler.sendEmptyMessage(MSG_hold);
			}
		});
		mReConnect.setVisibility(View.GONE);
		mSend = (Button)findViewById(R.id.ime_ok);
		mSend.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				sendInput();
			}
		});

		mDelete = (Button)findViewById(R.id.ime_del);
		mDelete.setOnClickListener(new OnClickListener(){
			public void onClick(View arg1){
			    sendDelete();
			}
		});
		mDelete.setVisibility(View.GONE);

		mHandler = new Handler(getMainLooper()){

			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
				case MSG_sending:
					mSend.setEnabled(true);
					if(msg.arg1==0){
					    et1.getText().clear();
					    //Log.d(TAG,"send success!");
					    finish();
					}else
						Log.d(TAG,"send failed! error code="+msg.arg1);
					break;
				case MSG_hold:
					ime.sendDataToHoldConnect();
					sendEmptyMessageDelayed(MSG_hold,Delay_Hold);
					break;
				case MSG_delete:
				    mDelete.setEnabled(true);
				    break;
				}
			}
			
		};
		mCSReceiver = new ConnectionStateReceiver();
		
	}
    @Override
	protected void onStart() {
		super.onStart();
		IntentFilter iif = new IntentFilter();
		iif.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);// bt on/off
		iif.addAction(ACTION);
		registerReceiver(mCSReceiver, iif);
	}
	@Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // for display even lock screen, & wake up screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        		|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        		|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        		|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        ime.mInputUIshow = true;
        Intent i = getIntent();
        int type = i.getIntExtra("inputType",1);
	String textstr = i.getStringExtra("textString");

    	//et1.getText().clear();

	if (textstr != null && !textstr.isEmpty()){
	    et1.setText(textstr);
	}

        if(type != -100){
        	changeType(type);
        }else{
	    //android.util.Log.e("ime","onResume() finish()");
	    finish();
	}
	mHandler.sendEmptyMessage(MSG_hold);
    }
    protected void onStop(){
    	super.onStop();
    	Log.d(TAG,"send close ime to Watch!");
    	if(mIsFinish){
    		mHandler.removeMessages(MSG_hold);
        	ime.sendCloseIme();
        	ime.mInputUIshow = false;
    	}
    	unregisterReceiver(mCSReceiver);
    	android.util.Log.e("ime","onStop() finish()");
    }
    protected void onDestory(){
    	super.onDestroy();
    	android.util.Log.e("ime","onDestory");
    }
    private void sendInput(){
    	String in = et1.getText().toString();
    	//if(!TextUtils.isEmpty(in)){
    		ime.sendInputing(in,mHandler.obtainMessage(MSG_sending));
    		mSend.setEnabled(false);
		//}else
    		//Toast.makeText(this,R.string.please_input,0).show();
    }
    
    private void sendDelete(){
	ime.sendDelete(mHandler.obtainMessage(MSG_delete));
	//mDelete.setEnabled(false);
    }

	private void changeType(int type){
//		String ss = " [unknown type] ";
//		switch (type) {
//		case 1:
//			ss = "text";
//			break;
//		case 2:
//			ss = "number";
//			break;
//		case 3:
//			ss = "phone";
//			break;
//		case 4:
//			ss = "date";
//			break;
//		}
		et1.setInputType(type);
//		et1.setHint("  " + type + " : " + ss);
	}
	private void ConnectChange(boolean state){
	    if(!state){
		android.util.Log.e("ime","ConnectChange() finish()");
		finish();
	    }
	}
	
	@Override
	public void finish() {
		super.finish();
		mIsFinish=true;
	    Log.d(TAG, "finish()");
	}

	private static final String ACTION="cn.ingenic.action.bluetooth_status";
	class ConnectionStateReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context c, Intent i) {
			if(i.getAction().equals(ACTION)){
				boolean state=i.getBooleanExtra("", false);
				android.util.Log.i("ime","connect ? "+state);
				ConnectChange(state);
			}else if(i.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				int state = i.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state != BluetoothAdapter.STATE_ON)
                	ConnectChange(false);
			}
		}
		
	}
}
