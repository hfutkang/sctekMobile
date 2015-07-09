package com.sctek.smartglasses.ui;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.sctek.smartglasses.ui.MainActivity;

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

import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ListAdapter;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;

import cn.ingenic.glasssync.ui.MyScrollView;
	
public class BindGlassActivity extends Activity {
    private final String TAG = "BindGlassActivity";
    private final static int PAIRED = 0;
    private final static int FOUND = 1;
    public final static int BIND_TIMEOUT = 2;
    public final static int REQUEST_CONNECT = 3;
    public final static int REQUEST_PAIR = 4;
    public final static int REQUEST_SCAN_DEVICE = 5;

    private int BIND_TIMEOUT_DELAY = 25*1000;

    private static final boolean DEBUG = true;
    private DefaultSyncManager mManager;
    private TextView tv_name_info,tv_address_info;
    private BluetoothAdapter mAdapter;
    private LinearLayout mDevice_Info;
    private boolean mIsScan=false;
    private boolean mStartDiscovery = false;
    private static final String REMOTE_BT_MAC="coldwave";
    private ListView mListView;
    private ShowBtAdapter mBtAdapter;
    private List<BluetoothDevice> mList = new ArrayList<BluetoothDevice>();

    private ProgressDialog mDialog;
    private Context mContext;

    private MyScrollView mScrollView;

    private Handler mHandler = new Handler(){  
	    @Override  
	    public void handleMessage(Message msg) {  
		switch(msg.what){
		case REQUEST_SCAN_DEVICE:
		    scanDevice();
		    break;
		case BIND_TIMEOUT:
		    mHandler.removeMessages(BIND_TIMEOUT);
		    Toast.makeText(BindGlassActivity.this, R.string.bind_timeout,Toast.LENGTH_SHORT).show();
		    cancelDialog();
		    break;
		case REQUEST_CONNECT:
		     mStartDiscovery = false;
		     mAdapter.cancelDiscovery();
		     BluetoothDevice device = (BluetoothDevice)msg.obj;
		     if(device.getBondState() == BluetoothDevice.BOND_BONDED){
			showDialog(mContext.getString(R.string.bind_device));
			try {
				mManager.connect(device.getAddress());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Message msgB = mHandler.obtainMessage();
			msgB.what = BIND_TIMEOUT;
			mHandler.sendMessageDelayed(msgB,BIND_TIMEOUT_DELAY);
		     }else{
			showDialog(mContext.getString(R.string.bluetooth_pairing));
			try {			
				Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
				createBondMethod.invoke(device);
				// mPairingDevice = device;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		     }
			
		    break;
		}
	    }
	};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	// TODO Auto-generated method stub
	super.onCreate(savedInstanceState);
	mContext = this;
	setContentView(R.layout.other_bind_activity_cruise);
	mListView = (ListView)findViewById(R.id.paired_listView);
	
	mDialog = new ProgressDialog(this);  
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条的形式为圆形转动的进度条  
        mDialog.setCancelable(true);// 设置是否可以通过点击Back键取消  
        mDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条  

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

        LinearLayout globleLayout = (LinearLayout) findViewById(R.id.globleLayout);  
        mScrollView = (MyScrollView) globleLayout.findViewById(R.id.scrollView);  
	LinearLayout header = mScrollView.refreshInit(globleLayout,mHandler);
	mScrollView.refreshStart();
	initAdapter();
    }
    private void initAdapter(){
	mBtAdapter = new ShowBtAdapter(mList,BindGlassActivity.this,mHandler);
	mListView.setAdapter(mBtAdapter);

	scanDevice();
    }

    private void scanDevice() {
	if(mAdapter.isDiscovering()){
	    // if "phone system" settings has already startDiscovery to scan bluetooth device, 
	    // we need to cancelDiscovery;
	    if(DEBUG) Log.d(TAG,"BT isDiscovering...");
	    mAdapter.cancelDiscovery();
	    mScrollView.refreshEnd();
	    return;
	}
	mList.clear();
	mBtAdapter.notifyDataSetChanged();
	setListViewHeightBasedOnChildren(mListView);
	
	mStartDiscovery = true;
	mAdapter.startDiscovery();
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		if (DEBUG)
		    Log.d(TAG, "rcv " + intent.getAction());
		if(DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED.equals(intent.getAction())){
		    cancelDialog();
		    mHandler.removeMessages(BIND_TIMEOUT);
		}
		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
		    BluetoothDevice scanDevice = intent
			.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		    if(scanDevice == null || scanDevice.getName() == null)return;

		    if (DEBUG)Log.e(TAG, "name="+scanDevice.getName()+"address="+scanDevice.getAddress()+"--Build.BOARD="+Build.BOARD);

		    if(scanDevice.getName().equals("cruise")){
			    mIsScan=true;
			    if (!mList.contains(scanDevice)){
				    mList.add(scanDevice);
				    mBtAdapter.notifyDataSetChanged();
				    setListViewHeightBasedOnChildren(mListView);
			    }
		    }
		}else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent
									   .getAction())) {
		    if (DEBUG)
			Log.e(TAG, "ACTION_BOND_STATE_CHANGED");
		    BluetoothDevice device = intent
			.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (DEBUG)Log.d(TAG, device.getBondState() + "Other activity===bonded");
		    switch (device.getBondState()) {
		    case BluetoothDevice.BOND_BONDED:
			cancelDialog();
			// if(device.getAddress().equals(mPairingDevice.getAddress()))
			Message requestBindMsg = mHandler.obtainMessage();
			requestBindMsg.what = REQUEST_CONNECT;
			requestBindMsg.obj = device;
			mHandler.sendMessage(requestBindMsg);
		    
			  //	if (device.getName().equals("IGlass")) {
			  // tv_bindstate.setText(R.string.pair_success);
			// if (!mList.contains(device)){
			//     mList.add(device);
			//     mBtAdapter.notifyDataSetChanged();
			//     setListViewHeightBasedOnChildren(mListView);
			//     if (mFoundList.contains(device)){
			// 	mFoundList.remove(device);
			// 	mBtFoundAdapter.notifyDataSetChanged();
			// 	setListViewHeightBasedOnChildren(mFoundListView);				
			//     }
			// }
			  //}
			break;
		    case BluetoothDevice.BOND_BONDING:
			showDialog(mContext.getString(R.string.bluetooth_pairing));
			break;
		    case BluetoothDevice.BOND_NONE:
			cancelDialog();
			break;
		    }
		}else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE
			 .equals(intent.getAction())) {
		    int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
						   DefaultSyncManager.IDLE);
		    boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true : false;
		    if (DEBUG) Log.d(TAG, isConnect + "    isConnect");
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
			    mHandler.removeMessages(BIND_TIMEOUT);
			    Intent bind_intent = new Intent(BindGlassActivity.this,
							    MainActivity.class);
			    startActivity(bind_intent);
			    finish();
			}
		    }else{
			Toast.makeText(BindGlassActivity.this, R.string.disconnect,Toast.LENGTH_SHORT).show();
			cancelDialog();	
		    }
		}else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())){
		    if (DEBUG)Log.d(TAG, "Discovery finished ");
		    mScrollView.refreshEnd();
		    mStartDiscovery = false;			
		}
	    }
	};

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

    private void showDialog(String message){
	mDialog.setMessage(message);  
        mDialog.show();  
    }

    private void cancelDialog(){
        mDialog.cancel();  
    
    }    
}
