package cn.ingenic.glasssync.screen;

import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.ViewGroup.LayoutParams;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Dialog;
import android.app.Application; 
import android.app.ProgressDialog;
import android.app.Activity;
import android.app.ActivityManager;
// import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
// import android.content.pm.ConfigurationInfo;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.utils.MyDialog;
import cn.ingenic.glasssync.screen.live.LiveModule;
import cn.ingenic.glasssync.screen.live.RtspClient;

public class LiveDisplayActivity extends Activity implements RtspClient.OnRtspClientListener {
    private static final String TAG = "LiveDisplayActivity";
    private final boolean DEBUG = true;

    private final int BT_MSG_CONNECTED = 0;
    private final int BT_MSG_UNCONNECTED = 1;

    private LiveModule mLiveModule;
    public static RtspClient mRtspClient;
    private SurfaceView mSurfaceView = null;
    private Dialog mUnConnectedDialog, mStreamDown, mStreamDisconnect;
    public static ProgressDialog mPD = null;
    private DefaultSyncManager mManager;
    private BluetoothAdapter mBluetoothAdapter;
    private long mStart = 0;
    private static Activity sActivity = null;
    private static boolean sHasError = false;

    private Handler mHandler = new Handler() {
	    @Override
	    public void handleMessage(Message msg){
		super.handleMessage(msg);
		if (DEBUG) Log.e(TAG, "[ mHandler ] handle Message " + msg.what);
		if (msg.what == BT_MSG_CONNECTED) {
		    mPD.setMessage(getString(R.string.waiting_for_glass_open_camera)); 
		    mPD.show(); 
		    if (mLiveModule != null)
			mLiveModule.sendStartMessage();
		}

		if (msg.what == BT_MSG_UNCONNECTED) {
		    mPD.dismiss();
		    initUnConnectedDialog();
		}
	    }};

    Runnable BtRunnable = new Runnable() {
	    @Override
	    public void run() {
		if (DEBUG) Log.e(TAG, "BtRunnable");
		if (!checkBTEnabled()) {
		    long end = System.currentTimeMillis() - mStart;
		    if (DEBUG) Log.e(TAG, "[ Bt disabled ] end " + end + " mStart " + mStart);
		    if ((end > 60000)) {
			Log.e(TAG, "Wait enable BT failed in 20 sec");
			Message message = Message.obtain();
			message.what = BT_MSG_UNCONNECTED;
			mHandler.sendMessage(message);
		    } else {
			mHandler.postDelayed(this, 2000);  
		    }
		} else {
		    Log.e(TAG, "Bt enabled in 20 sec");
		    Message message = Message.obtain();
		    message.what = BT_MSG_CONNECTED;
		    mHandler.sendMessage(message);
		}
	    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	setContentView(R.layout.activity_display);
	Log.e(TAG, "onCreate");
	sActivity = this;
	sHasError = false;
	PowerManager pm = (PowerManager) this
		    .getSystemService(this.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
							  PowerManager.ACQUIRE_CAUSES_WAKEUP
							  | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
		wl.acquire();
		wl.release();

	initView();
    }


    // public static boolean detectOpenGLES20(Context context) {  
    // 	ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);  
    // 	ConfigurationInfo info = am.getDeviceConfigurationInfo();  
    // 	return (info.reqGlEsVersion >= 0x20000);  
    // }  

    private void initView() {
	Log.e(TAG, "InitView++");
	mRtspClient = new RtspClient(this);
	mRtspClient.setListener(this);
	mLiveModule = LiveModule.getInstance(this);

	mPD = processDialog();
	mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
	mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback(){
		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		    Log.e(TAG, "surfaceChanged");
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
		    Log.e(TAG, "surfaceCreated");
		    if (mRtspClient != null)
			mRtspClient.setSurface(arg0);
		}
		
		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
		    // TODO Auto-generated method stub
		}
	    });
    }

    private void initUnConnectedDialog() {
	mUnConnectedDialog = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_bt_disconnect), getApplication().getResources().getString(R.string.live_cancle),new MyDialog.LeaveMeetingDialogListener() {
		@Override
		public void onClick(View view) {
		    switch (view.getId()) {
		    case R.id.dialog_tv_cancel_one:
			mUnConnectedDialog.cancel();
			finish();
			break;
		
		    }
		}});
        mUnConnectedDialog.setCanceledOnTouchOutside(false);
    	mUnConnectedDialog.setCancelable(false);
   	mUnConnectedDialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		    if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mUnConnectedDialog != null)
			    mUnConnectedDialog.dismiss();
			finish();
		    }
		    return false;
		}
	    });
    	mUnConnectedDialog.show();
    }

    private ProgressDialog processDialog(){
	if (DEBUG) Log.e(TAG, "processDialog");
	ProgressDialog pd = new ProgressDialog(this); 
	pd.setCancelable(false); 
	pd.setOnKeyListener(new DialogInterface.OnKeyListener() {
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		    if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPD != null)
			    mPD.dismiss();
			finish();
		    }
		    return false;
		}
	    });
	return pd;
    }

    private boolean checkBTEnabled() {
	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
	if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())) {
	    Log.e(TAG, "BluetoothAdapter is null or unenable, screen share failed");
	    return false;
	}
	
	mManager = DefaultSyncManager.getDefault();
	if (mManager != null) {
	    Log.e(TAG, "mManager.isConnect() = " + mManager.isConnect());
	}
	
	if ((mManager == null) || (!mManager.isConnect()))  {
	    Log.e(TAG, "unused bluetooth, screen share failed");
	    return false;
	}
	if (DEBUG) Log.e(TAG, "Bluetooth connected");

	return true;
    }

    public static void startRtspClient(String url) {
	Log.e(TAG, "startRtspClient url = " + url);
	if (mRtspClient != null)
	    mRtspClient.start(url);
    }


    public static void closeRtspClient(){
    	Log.e(TAG, "closeRtspClient");
        if (mRtspClient != null)
	    mRtspClient.close();
    }

    @Override
    public void onStart() {
	if (DEBUG) Log.e(TAG, "onStart");
	super.onStart();

	if (sHasError)
	    return;

	if (!checkBTEnabled()) {
	    mPD.setMessage(getString(R.string.live_open_bt)); 
	    mPD.show(); 
	    mStart = System.currentTimeMillis();
	    mHandler.postDelayed(BtRunnable, 1000);
	} else {
	    mPD.setMessage(getString(R.string.waiting_for_glass_open_camera)); 
	    mPD.show();
	    if (mLiveModule != null)
		mLiveModule.sendStartMessage();
	}
    }

    @Override
    public void onStop() {
	Log.e(TAG, "onStop");
	closeRtspClient();

	mHandler.removeCallbacks(BtRunnable);	
	
	if (mLiveModule != null)
	    mLiveModule.sendStopMessage();

	if (mPD != null)
	    mPD.dismiss();

	super.onStop();
	finish();
    }

    @Override
    protected void onDestroy() {
	if (DEBUG) Log.e(TAG, "onDestroy");
	super.onDestroy();
	mPD = null;
	sActivity = null;
	sHasError = false;
    }

    // RTSPClient Listener function
    @Override
    public void onVideoSizeChanged(int width, int height) {
	if (DEBUG) Log.e(TAG, "[ onVideoSizeChanged ] width = " + width + " height = " + height);
	LayoutParams lp = mSurfaceView.getLayoutParams();
	lp.width = width;
	lp.height = height;
	mSurfaceView.setLayoutParams(lp);
	mSurfaceView.requestLayout();
    }
    
    @Override
    public void onStreamDown() {
	mStreamDown = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_stream_down), getApplication().getResources().getString(R.string.live_cancle),new MyDialog.LeaveMeetingDialogListener() {
		@Override
		public void onClick(View view) {
		    switch (view.getId()) {
		    case R.id.dialog_tv_cancel_one:
			mStreamDown.cancel();
			finish();
			break;
		
		    }
		}});
	mStreamDown.setCanceledOnTouchOutside(false);
	mStreamDown.setCancelable(false);
	mStreamDown.show();
    }

    @Override
    public void onStreamDisconnect() {
	mStreamDisconnect = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.live_network_disconnect), getApplication().getResources().getString(R.string.live_cancle),new MyDialog.LeaveMeetingDialogListener() {
		@Override
		public void onClick(View view) {
		    switch (view.getId()) {
		    case R.id.dialog_tv_cancel_one:
			mStreamDisconnect.cancel();
			finish();
			break;
		
		    }
		}});
	mStreamDisconnect.setCanceledOnTouchOutside(false);
	mStreamDisconnect.setCancelable(false);
	mStreamDisconnect.show();
    }

    // LiveModule Listener function
    public static void showLiveErrorDialog(String err) {
	sHasError = true;
        final MyDialog dialog = new MyDialog(sActivity, R.style.MyDialog, err, 
				       sActivity.getString(R.string.live_cancle), null);
	dialog.setLeaveMeetingDialogListener(new MyDialog.LeaveMeetingDialogListener() {
		@Override
		public void onClick(View view) {
		    switch (view.getId()) {
		    case R.id.dialog_tv_cancel_one:
			dialog.cancel();
			sActivity.finish();
			break;		
		    }
		}
	    });
	dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		    if (keyCode == KeyEvent.KEYCODE_BACK) {
			dialog.cancel();
			sActivity.finish();
		    }
		    return false;
		}
	    });
	dialog.setCanceledOnTouchOutside(false);
	dialog.setCancelable(false);
	dialog.show();
    }    
}
