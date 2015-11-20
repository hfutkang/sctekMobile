package com.sctek.smartglasses.ui;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.sctek.smartglasses.ui.MainActivity;
import com.sctek.smartglasses.utils.CustomHttpClient;
import com.sctek.smartglasses.zxing.CaptureActivity;

import android.R.layout;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ListAdapter;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.ui.MyScrollView;
	
public class BindHanlangActivity extends Activity {
    private final String TAG = "BindGlassActivity";
    private final static int PAIRED = 0;
    private final static int FOUND = 1;
    public final static int BIND_TIMEOUT = 2;
    public final static int REQUEST_CONNECT = 3;
    public final static int REQUEST_PAIR = 4;
    public final static int REQUEST_SCAN_DEVICE = 5;
    
    public final static int REQUEST_CANCEL_SCAN_DEVICE = 6;
    public final static int BT_BOND_FAILED = 7;
    public final static int BIND_FAIL = 8;

    private int BIND_TIMEOUT_DELAY = 20*1000;
    private final static int CANCEL_SCAN_DELAY_TIME = 15*1000;

    private static final boolean DEBUG = true;
    private DefaultSyncManager mManager;
    private BluetoothAdapter mAdapter;
    private boolean mStartDiscovery = false;
    private static final String REMOTE_BT_MAC="WEAR";
    private static final String HANLANG_BT_NAME="WEAR";
    private ShowBtAdapter mBtAdapter;
    private List<BluetoothDevice> mList = new ArrayList<BluetoothDevice>();
    private Context mContext;
    
    private Button mBindHanLangBt;
    private Button mScanQrCodeBt;
    private TextView mBindHintTv;
    private CircularProgress mBindProgressBar;
    private BluetoothDevice mHanlangDevice;
    
    private String deviceName;

    private Handler mHandler = new Handler(){  
	    @Override  
	    public void handleMessage(Message msg) {  
	    	// mHandler.removeMessages(BIND_TIMEOUT);
		switch(msg.what){
		// case BIND_TIMEOUT:
		// 	 bindHanLangFail(R.string.operation_timeout);
		//     break;
		case REQUEST_CONNECT:
		     mAdapter.cancelDiscovery();
		     BluetoothDevice device = (BluetoothDevice)msg.obj;
		     Log.d(TAG,"--REQUEST_CONNECT bond state="+device.getBondState());
		     if(device.getBondState() == BluetoothDevice.BOND_BONDED){
			try {
				mBindHintTv.setText(R.string.binding_hanlang);
				mManager.connect(device.getAddress());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Message msgB = mHandler.obtainMessage();
			msgB.what = BIND_FAIL;
			mHandler.sendMessageDelayed(msgB,BIND_TIMEOUT_DELAY);
			
		     }else{
		    	 mBindHintTv.setText(R.string.bluetooth_paring);
			try {			
				Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
				createBondMethod.invoke(device);
				// mPairingDevice = device;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Message msgB = mHandler.obtainMessage();
			msgB.what = BT_BOND_FAILED;
			mHandler.sendMessageDelayed(msgB,BIND_TIMEOUT_DELAY);
		     }
		    break;
		case REQUEST_CANCEL_SCAN_DEVICE:
			mAdapter.cancelDiscovery();
			bindHanLangFail(R.string.scan_device_timeout);
			break;
		case BT_BOND_FAILED:
			bindHanLangFail(R.string.pair_fail);
			restartBluetooth();
			break;
		case BIND_FAIL:
			bindHanLangFail(R.string.bind_fail);
			break;
		}
	    }
	};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	// TODO Auto-generated method stub
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	setContentView(R.layout.activity_bind_hanlang);
	mContext = this;

	mBindHanLangBt = (Button)findViewById(R.id.bind_hanlang_bt);
	mScanQrCodeBt = (Button)findViewById(R.id.scan_qrcode_bt);
	mBindHintTv = (TextView)findViewById(R.id.bind_hanlang_hint_tv);
	mBindProgressBar = (CircularProgress)findViewById(R.id.bind_hanlang_pb);
	
	mManager = DefaultSyncManager.getDefault();
	mAdapter = BluetoothAdapter.getDefaultAdapter();

	IntentFilter filter = new IntentFilter();
	filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
	filter.addAction(BluetoothDevice.ACTION_FOUND);
	filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
	filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	filter.addAction(DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED);
	registerReceiver(mBluetoothReceiver, filter);
	
    }
    
    public void onBindHanLangButtonClicked(View view) {
    	
    	if(view != null)
    		deviceName = null;
    	
    	mAdapter.startDiscovery();
    	
    	mHandler.sendEmptyMessageDelayed(REQUEST_CANCEL_SCAN_DEVICE, CANCEL_SCAN_DELAY_TIME);
    	
    	mBindHanLangBt.setEnabled(false);
    	mScanQrCodeBt.setEnabled(false);
    	
    	mBindHintTv.setText(R.string.serching_hanlang);
    	mBindProgressBar.setIndeterminate(true);
    	mBindProgressBar.startAnimation();
    	
    }
    
    public void onBindScanQrCodeClicked(View view) {
    	try {
    	Intent intent = new Intent(this, CaptureActivity.class);
    	startActivityForResult(intent, 1);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		    Log.i(TAG, "rcv " + intent.getAction());
		if(DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED.equals(intent.getAction())){
			//mHandler.removeMessages(BIND_TIMEOUT);
			mHandler.removeMessages(BIND_FAIL);
		}
		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
		    BluetoothDevice scanDevice = intent
			.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		    if(scanDevice == null || scanDevice.getName() == null) return;

		    if (DEBUG)Log.d(TAG, "name="+scanDevice.getName()+"address="+scanDevice.getAddress()+"--Build.BOARD="+Build.BOARD);

		    String name = scanDevice.getName();
		    
		    if(deviceName == null && name.toUpperCase().startsWith(HANLANG_BT_NAME)){
		    	mHandler.removeMessages(REQUEST_CANCEL_SCAN_DEVICE);
		    	Message requestpairMsg = mHandler.obtainMessage();
		    	requestpairMsg.what = REQUEST_CONNECT;
		    	requestpairMsg.obj = scanDevice;
			mHandler.sendMessage(requestpairMsg);
		    }
		    else if(deviceName != null && name.toUpperCase().endsWith(deviceName)) {
		    	mHandler.removeMessages(REQUEST_CANCEL_SCAN_DEVICE);
		    	Message requestpairMsg = mHandler.obtainMessage();
		    	requestpairMsg.what = REQUEST_CONNECT;
		    	requestpairMsg.obj = scanDevice;
			mHandler.sendMessage(requestpairMsg);
		    }
		}else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent
									   .getAction())) {
		    if (DEBUG)
			Log.e(TAG, "ACTION_BOND_STATE_CHANGED");
		    BluetoothDevice device = intent
			.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (DEBUG)Log.d(TAG, device.getBondState() + "Other activity===bonded");
		    String name = device.getName();
		    if((deviceName == null && name.toUpperCase().startsWith(HANLANG_BT_NAME)) ||
		    		(deviceName != null && name.toUpperCase().equals(deviceName))) {
			    switch (device.getBondState()) {
			    case BluetoothDevice.BOND_BONDED:
				// if(device.getAddress().equals(mPairingDevice.getAddress()))
			    mHandler.removeMessages(REQUEST_CANCEL_SCAN_DEVICE);
			    mHandler.removeMessages(BT_BOND_FAILED);
				Message requestBindMsg = mHandler.obtainMessage();
				requestBindMsg.what = REQUEST_CONNECT;
				requestBindMsg.obj = device;
				mHandler.sendMessage(requestBindMsg);
				break;
			    case BluetoothDevice.BOND_NONE:
				mHandler.sendEmptyMessage(BT_BOND_FAILED);
				break;
		    }
		    }
		}else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE
			 .equals(intent.getAction())) {

		    int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
						   DefaultSyncManager.IDLE);
		    boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true : false;
		    Log.e(TAG, DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE + ":" + isConnect);
		    if (DEBUG) Log.e(TAG, isConnect + "    isConnect");
		    mHandler.removeMessages(BIND_FAIL);
		    if (isConnect) {
			String addr = mManager.getLockedAddress();
			if(addr.equals("")){
			      //local has disconnect last,but remote not get notification
			      //notify again
			    Log.d(TAG, "local has disconnect,but remote not get notificaton.notify again!");
			    mManager.disconnect();
			}else{
			    mManager.setLockedAddress(addr);
			      //unregisterReceiver(mBluetoothReceiver);
			    //mHandler.removeMessages(BIND_TIMEOUT);
			    Intent bind_intent = new Intent(BindHanlangActivity.this,
							    MainActivity.class);
			    bind_intent.putExtra("first_bind", true);
			    startActivity(bind_intent);
			    finish();
			}
		    }else{
			mHandler.sendEmptyMessage(BIND_FAIL);
		    }
		}else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())){		
		}
	    }};

    // @Override
    // 	public void onClick(View v) {
    // 	// TODO Auto-generated method stub
    // 	switch (v.getId()) {
    // 	case R.id.other:
    // 	    Intent intent=new Intent(BindGlassActivity.this,QRCodeActivity.class);
    // 	    startActivity(intent);
    // 	    finish();
    // 	    return;
    // 	default: break;
    // 	}
    // }

    @Override
	protected void onStop() {
	super.onStop();			
	    Log.d(TAG, "onStop in");
    }

    @Override
	protected void onDestroy() {
	super.onDestroy();			
	if(DEBUG){
	    Log.d(TAG, "onDestroy in");
	}
	unregisterReceiver(mBluetoothReceiver);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	Log.e(TAG, "onActivityResult");
    	Log.e(TAG, "scan result:" + data.getStringExtra("name"));
    	if(resultCode == RESULT_OK) {
    		String resultString = data.getStringExtra("name");
    		deviceName = getNameFromQrResult(resultString);
    		
    		
    		if(deviceName == null) {
    			Toast.makeText(this, R.string.not_hanlang_glass, Toast.LENGTH_SHORT).show();
    			return;
    		}
    		
    		onBindHanLangButtonClicked(null);
    		
//    		mBindHanLangBt.setEnabled(false);
//        	mScanQrCodeBt.setEnabled(false);
//        	
//        	mBindProgressBar.setIndeterminate(true);
//        	mBindProgressBar.startAnimation();
//        	
//    		BluetoothDevice device = mAdapter.getRemoteDevice(mac);
//    		Message requestpairMsg = mHandler.obtainMessage();
//	    	requestpairMsg.what = REQUEST_CONNECT;
//	    	requestpairMsg.obj = device;
//			mHandler.sendMessage(requestpairMsg);
			
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }

    public void setListViewHeightBasedOnChildren(ListView listView) {
      
	ListAdapter listAdapter = listView.getAdapter();    
	if (listAdapter == null) {    
	    return;  
	}
    
	ViewGroup.LayoutParams params = listView.getLayoutParams();  
  
	if(listAdapter.getCount()==0){
	    params.height = 0;     
	    listView.setLayoutParams(params);  
	}else{
	    View listItem = listAdapter.getView(0, null, listView);
	    if(listItem == null)return;
	    listItem.measure(0, 0);
	    int height = listItem.getMeasuredHeight();
	    params.height = (height + listView.getDividerHeight())*listAdapter.getCount();     
	    listView.setLayoutParams(params);     
	}
    }
    
    private void bindHanLangFail(int resId) {
    	
    	mBindHintTv.setText(resId);
    	
    	mBindHanLangBt.setEnabled(true);
    	mScanQrCodeBt.setEnabled(true);
    	
    	mBindProgressBar.stopAnimation();
    	mBindProgressBar.setIndeterminate(false);
    }
    
    private String getNameFromQrResult(String result) {
    	Log.e(TAG, "result:" + result);
    	if(result == null)
    		return null;
    	
    	result.toUpperCase();
    	String strings[] = result.split("#");
    	
    	if(strings.length != 2)
    		return null;
    	
    	if(strings[1].startsWith("WEAR")) {
    		Pattern patter = Pattern.compile("^WEAR_[A-Z]{2}[0-9]*$");
    		Matcher matcher = patter.matcher(strings[1]);
    		if(matcher.matches())
    			return strings[1];
    	}
    	
    	return null;
    }
    private void restartBluetooth(){
	    Log.d(TAG,"restartBluetooth");
    }
}
