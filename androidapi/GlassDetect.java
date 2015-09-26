package cn.ingenic.glasssync.devicemanager;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.List;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import cn.ingenic.glasssync.DefaultSyncManager;

public class GlassDetect {
    private static final String TAG = "GlassDetect";
    private static boolean DEBUG = true;
    private Context mContext;
    private GlassDetectReceiver nReceiver;
    private static GlassDetect sInstance;
    private String mAddress;
    private Handler mCallBackHandler = null;
    private BluetoothAdapter mBTAdapter;
    private BluetoothHeadset mBluetoothHeadset = null;

    public static final String PHONE_AUDIO_SYNC = "BLUETOOTHHEADSET";

    private GlassDetect(Context context){
	if(DEBUG) Log.e(TAG, "GlassDetect");
	mContext = context;

	mBTAdapter = BluetoothAdapter.getDefaultAdapter();
	if (mBTAdapter == null){
	    Log.e(TAG, "getDefaultAdapter fail");
	    return;
	}
	mBTAdapter.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET);

        nReceiver = new GlassDetectReceiver();
        IntentFilter filter = new IntentFilter();
	filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
	        context.registerReceiver(nReceiver,filter);
    }

    protected void finalize(){
	mContext.unregisterReceiver(nReceiver);
    }

    public void set_audio_connect(){
	if(mBluetoothHeadset == null || mAddress == null ) {
	    Log.e(TAG,"have not get BluetoothHeadset service now!");
	    return;
	}

	BluetoothDevice connectedBD= getConnectedDevice();
	if(connectedBD != null){
	    Log.e(TAG,"connect failed:have a connected device ("+connectedBD.getAddress()+") now.");
	    return;
	}
	BluetoothDevice btd = mBTAdapter.getRemoteDevice(mAddress);
	if(DEBUG) Log.e(TAG, "set_audio_connect btd="+btd+"--mAddress="+mAddress);
	if (btd != null){
	    if (DEBUG)Log.e(TAG, " Priority="+mBluetoothHeadset.getPriority(btd));
	    if (mBluetoothHeadset.getPriority(btd) < BluetoothProfile.PRIORITY_ON)
		mBluetoothHeadset.setPriority(btd, BluetoothProfile.PRIORITY_ON);
	    mBluetoothHeadset.connect(btd);
	}

    }

    public void set_audio_disconnect(){
	  //mAudioStrategy = audioStrategy;
	if(mBluetoothHeadset == null) {
	    Log.e(TAG,"have not get BluetoothHeadset service now!");
	    return;
	}

	BluetoothDevice connectedBD = getConnectedDevice();
	if(connectedBD == null){
	    Log.e(TAG,"disconnect failed:have not connected device now.mAddress="+mAddress);
	    return;
	}
	//Compile in Eclipse shielding
	if(mBluetoothHeadset.getPriority(connectedBD) > BluetoothProfile.PRIORITY_OFF)
	    mBluetoothHeadset.setPriority(connectedBD, BluetoothProfile.PRIORITY_OFF);
	mBluetoothHeadset.disconnect(connectedBD);
    }

    private BluetoothDevice getConnectedDevice(){
	List<BluetoothDevice> lcon =  mBluetoothHeadset.getConnectedDevices();
	if (DEBUG)Log.e(TAG, "List<BluetoothDevice> lcon = "+lcon);
	for (BluetoothDevice lbt : lcon){
	    if (lbt.getAddress().equals(mAddress)){
		return lbt;
	    }
	}
	return null;
    }

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
	    public void onServiceConnected(int profile, BluetoothProfile proxy) {
		if (DEBUG)Log.e(TAG, "onServiceConnected");
		if (profile == BluetoothProfile.HEADSET) {
		    mBluetoothHeadset = (BluetoothHeadset) proxy;
		}
	    }
	    public void onServiceDisconnected(int profile) {
		if (DEBUG)Log.e(TAG, "onServiceDisconnected");
		if (profile == BluetoothProfile.HEADSET) {
		    mBluetoothHeadset = null;
		}
	    }
	};

    class GlassDetectReceiver extends BroadcastReceiver{
	private String TAG = "GlassDetectReceiver";
	
        @Override
	    public void onReceive(Context context, Intent intent) {
	    if (DEBUG)Log.e(TAG, "onReceive " + intent.getAction());
	    if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
		int connectState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_CONNECTED);
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		Log.e("wdu", "connectState = " + connectState + "connectState ="+manager.isConnect());
		if (connectState == BluetoothProfile.STATE_CONNECTED && !manager.isConnect()){       	
		    set_audio_disconnect();
		}

	     }
        }
    }

    public static GlassDetect getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassDetect(c);
	return sInstance;
    }
 
   public void setCallBack(Handler handler) {
	mCallBackHandler = handler;
    }

    public void setLockedAddress(String address) {
	mAddress = address;
    }

    public int getCurrentState() {
	int state = BluetoothProfile.STATE_DISCONNECTED;
	if(mBluetoothHeadset == null) {
	    Log.e(TAG,"have not get BluetoothHeadset service now!");
	    return state;
	}
	BluetoothDevice connectedBD= getConnectedDevice();
	if(connectedBD != null){
	    return mBluetoothHeadset.getConnectionState(connectedBD);
	}
	return state;
    }

}