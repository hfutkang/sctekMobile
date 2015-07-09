package cn.ingenic.glasssync.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import cn.ingenic.glasssync.SyncApp;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.SystemModule;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.lbs.GlassSyncLbsManager;
import cn.ingenic.glasssync.screen.ScreenShareControlActivity;
import cn.ingenic.glasssync.sms.SmsModule;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;
import android.content.Context;
import cn.ingenic.glasssync.data.FeatureConfigCmd;
import cn.ingenic.glasssync.data.Projo;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.net.wifi.WifiManager;
import android.app.Activity;
import cn.ingenic.glasssync.wifi.GlassSyncWifiModule;
public class Fragment_Setting extends Fragment implements OnClickListener {
    private boolean DEBUG = true;
    private final String TAG = "Fragment_Setting";

    private View fragmentView;
    private SharedPreferences mSharedPreferences;
    private Editor mEditor;

    public static final String MMS_KEY = SmsModule.SMS_NAME, CONTACT_KEY = ContactsLiteModule.MODULE_NAME,TIME_KEY = DeviceModule.FEATURE_TIME,WIFI_KEY="WIFI_SYNC";
      //msg.what
    private static final int MSG_NOTIFY = 0;
    public static final int MSG_SEND_FINISH = 1;
    public static final int MSG_SEND_FAIL = 2;
    private final int MSG_SEND_MESSAGE_TIMEOUT = 3;
    private final int MSG_SEND_CONTACT_TIMEOUT = 4;
    private final int MSG_WIFI_SYNC_TIMEOUT = 5;
    private long SYNCDATA_DELAYTIMES = 10*1000;

    private Context mContext;
    private GlassDetect mGlassDetect;
    private BluetoothDevice mBluetoothDevice;

    private ElementView mTimeView;
    private ElementView mContactView;
    private ElementView mSmsView;
    private ElementView mPhoneView;
    private ElementView mWifiView;
    private ElementView mGpsView;

    private int mTimeCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
    private int mContactCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
    private int mSmsCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
    private int mPhoneCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
    private int mWifiCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
    private int mGpsCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
    private boolean mContactDBReadability = false;
    private boolean mSmsDBReadability = false;
    private boolean mHeadsetRequestFromClick = false;
    private Receiver nReceiver;

    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mContext = getActivity();
	    mGlassDetect = (GlassDetect)GlassDetect.getInstance(mContext);

	    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
	    DefaultSyncManager defaultSyncManager = DefaultSyncManager.getDefault();
	    String sAddress = defaultSyncManager.getLockedAddress();
	    mBluetoothDevice = btAdapter.getRemoteDevice(sAddress);

	    LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    fragmentView = inflater.inflate(R.layout.fragment_item_setting,null);
	    findViewAddListener();

	    mGlassDetect.setLockedAddress(sAddress);
	    mGlassDetect.set_audio_connect();

	    nReceiver = new Receiver();
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
	    filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
	    mContext.registerReceiver(nReceiver,filter);
	    
	    mSharedPreferences = getActivity().getSharedPreferences(SyncApp.SHARED_FILE_NAME,getActivity().MODE_PRIVATE);
	    mEditor = mSharedPreferences.edit();

	    preCheckDBReadability("contact");
	    preCheckDBReadability("sms");

	    initData();
	}

    Handler mHandler = new Handler() {
	    
	    @Override
		public void handleMessage(Message msg) {
		super.handleMessage(msg);
		if(DEBUG) dumpWhat(msg.what);
		
		switch (msg.what) {
		case MSG_NOTIFY:
		    Arg arg = (Arg) msg.obj;
		    String key = arg.key;
		    boolean value = arg.value;
		    if (DEBUG)Log.e(TAG, "MSG_NOTIFY " + key + " " + value);

		    if(key.equals(CONTACT_KEY)){
			ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(getActivity().getApplicationContext());
			clm.sendSyncRequest(value,mHandler);
			clm.setSyncEnable(value);
			if(value ==true){
			    Message msgC = new Message();
			    msgC.what = MSG_SEND_CONTACT_TIMEOUT;
			    msgC.obj = key;
			    mHandler.sendMessageDelayed(msgC,SYNCDATA_DELAYTIMES);
			}
		    }else if (key.equals(MMS_KEY)) {
			SmsModule sm = (SmsModule) SmsModule.getInstance(getActivity().getApplicationContext());
			sm.sendSyncRequest(value,mHandler);
			sm.setSyncEnable(value);
			if(value ==true){
			    Message msgS = new Message();
			    msgS.what = MSG_SEND_MESSAGE_TIMEOUT;
			    msgS.obj = key;
			    mHandler.sendMessageDelayed(msgS,SYNCDATA_DELAYTIMES);
			}
		    }else if(key.equals(WIFI_KEY)){
			if(value ==true){
			    Message msgS = new Message();
			    msgS.what = MSG_WIFI_SYNC_TIMEOUT;
			    msgS.obj = key;
			    mHandler.sendMessageDelayed(msgS,SYNCDATA_DELAYTIMES);
			}
		    }else if (key.equals(TIME_KEY)) {
			DefaultSyncManager manager = DefaultSyncManager
			    .getDefault();
			Config config = new Config(SystemModule.SYSTEM);
			Map<String, Boolean> map = new HashMap<String, Boolean>();
			map.put(key, value);
			Projo projo = new FeatureConfigCmd();
			projo.put(FeatureConfigCmd.FeatureConfigColumn.feature_map,
				  map);
			ArrayList<Projo> datas = new ArrayList<Projo>();
			datas.add(projo);
			manager.request(config, datas);
			manager.featureStateChange(key, value);
		    }
		    break;
		case MSG_SEND_FINISH:
		    syncDataFinished((String)msg.obj);
		    break;
		case MSG_SEND_MESSAGE_TIMEOUT:
		case MSG_SEND_CONTACT_TIMEOUT:
		case MSG_WIFI_SYNC_TIMEOUT:
		case MSG_SEND_FAIL:
		    syncDataFailed((String)msg.obj);
		    break;
		}
	    }

	};

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
				 Bundle savedInstanceState) {		
	ViewGroup parent = (ViewGroup) fragmentView.getParent();
	if (parent != null) {
	    parent.removeView(fragmentView);
	}
	return fragmentView;
    }

    private void findViewAddListener() {
	mTimeView = (ElementView)fragmentView.findViewById(R.id.time);
	mSmsView = (ElementView)fragmentView.findViewById(R.id.message);
	mContactView = (ElementView)fragmentView.findViewById(R.id.contact);
	mPhoneView = (ElementView)fragmentView.findViewById(R.id.phone);
	mWifiView = (ElementView)fragmentView.findViewById(R.id.wifi);
	mGpsView = (ElementView)fragmentView.findViewById(R.id.gps);

	mTimeView.setTitle(getResources().getString(R.string.time));
	mSmsView.setTitle(getResources().getString(R.string.message));
	mContactView.setTitle(getResources().getString(R.string.contact));
	mPhoneView.setTitle(getResources().getString(R.string.phone));
	mWifiView.setTitle(getResources().getString(R.string.wifi));
	mGpsView.setTitle(getResources().getString(R.string.gps));

	mTimeView.setOnClickListener(this);
	mSmsView.setOnClickListener(this);
	mContactView.setOnClickListener(this);
	mPhoneView.setOnClickListener(this);
	mWifiView.setOnClickListener(this);
	mGpsView.setOnClickListener(this);
      }
    @Override
	public void onClick(View view){
	switch (view.getId()) {
	case R.id.time:
	      //notice: no checking state
	    if(mTimeCheckStatus == ElementView.COLUMN_STATUS_UNCHECK){
		mTimeCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
		syncData(TIME_KEY,true);
		saveData("time_checked",true);	    
	    }else{
		  //checked -> uncheck
		mTimeCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
		syncData(TIME_KEY,false);
		saveData("time_checked",false);
	    }
	    mTimeView.setShow(mTimeCheckStatus);
	    break;
	case R.id.contact:
	    if(mContactCheckStatus == ElementView.COLUMN_STATUS_CHECKING)
		break;
	    
	    if(mContactCheckStatus == ElementView.COLUMN_STATUS_UNCHECK){
		if(mContactDBReadability == true){
		    mContactCheckStatus = ElementView.COLUMN_STATUS_CHECKING;
		    syncData(CONTACT_KEY,true);	    
		}else{
		    Toast.makeText(mContext.getApplicationContext(), R.string.contact_send_fail,
				   Toast.LENGTH_SHORT).show();					
		}
	    }else{
		  //checked -> uncheck
		mContactCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
		syncData(CONTACT_KEY,false);
		saveData("contact_checked",false);
	    }
	    mContactView.setShow(mContactCheckStatus);
	    break;
	case R.id.message:
	    if(mSmsCheckStatus == ElementView.COLUMN_STATUS_CHECKING)
		break;
	    
	    if(mSmsCheckStatus == ElementView.COLUMN_STATUS_UNCHECK){
		if(mSmsDBReadability == true){
		    mSmsCheckStatus = ElementView.COLUMN_STATUS_CHECKING;
		    syncData(MMS_KEY,true);	    
		}else{
		    Toast.makeText(mContext.getApplicationContext(), R.string.sms_send_fail,
				   Toast.LENGTH_SHORT).show();					
		}
	    }else{
		  //checked -> uncheck
		mSmsCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
		syncData(MMS_KEY,false);
		saveData("sms_checked",false);
	    }
	    mSmsView.setShow(mSmsCheckStatus);
	    break;
	case R.id.phone:
	    if(mPhoneCheckStatus == ElementView.COLUMN_STATUS_CHECKING)
		break;
	    
	    if(mPhoneCheckStatus == ElementView.COLUMN_STATUS_UNCHECK){
		mPhoneCheckStatus = ElementView.COLUMN_STATUS_CHECKING;
		mGlassDetect.set_audio_connect();
		mHeadsetRequestFromClick = true;
	    }else{
		  //checked -> uncheck
		mPhoneCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
		mGlassDetect.set_audio_disconnect();
		mHeadsetRequestFromClick = true;
	    }
	    
	    mPhoneView.setShow(mPhoneCheckStatus);
	    break;
	case R.id.wifi:
	    if(mWifiCheckStatus == ElementView.COLUMN_STATUS_CHECKING)
			break;
		GlassSyncWifiModule.getInstance(this.getActivity(), mHandler);
	    if(mWifiCheckStatus == ElementView.COLUMN_STATUS_UNCHECK){
		boolean wifiOn = checkWifiState();
		if(wifiOn){
		    Intent intent = new Intent(mContext,WifiScanActivity.class);
		    startActivityForResult(intent,0);
		    mWifiCheckStatus = ElementView.COLUMN_STATUS_CHECKING;
		}else{
		    Toast.makeText(mContext.getApplicationContext(), R.string.wifi_off,
				   Toast.LENGTH_SHORT).show();
		}
	    }else{
		  //checked -> uncheck
		mWifiCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
		saveData("wifi_checked",false);
	    }
	    mWifiView.setShow(mWifiCheckStatus);
	    break;
	case R.id.gps:
	    syncLocation();
	    break;
	default:
	    break;
	}
    }
       
    private static class Arg {
	final String key;
	final boolean value;
	
	Arg(String key, boolean value) {
	    this.key = key;
	    this.value = value;
	}
    }
    
    public boolean checkWifiState(){
	WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
	if(wifiManager != null && wifiManager.getWifiState() != wifiManager.WIFI_STATE_ENABLED){
	    return false;
	}else if(wifiManager.getWifiState() == wifiManager.WIFI_STATE_ENABLED){
	    return true;
	}
	return false;
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);			
	if (resultCode == Activity.RESULT_OK) {
	    if(data == null){
		return;
	    }
	    String wifiName = data.getExtras().getString("wifiName");
	    String wifiPassword = data.getExtras().getString("wifiPassword");
	    GlassSyncWifiModule.getInstance(Fragment_Setting.this.getActivity(),mHandler).send_Wifi(wifiName,wifiPassword);
	    syncData(WIFI_KEY,true);
	}else if(resultCode == Activity.RESULT_CANCELED){
	    mWifiCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
	    mWifiView.setShow(mWifiCheckStatus );
	    saveData("wifi_checked",false);	
	}
    }
    private  void saveData(String key,boolean value){
	mEditor.putBoolean(key, value);
	mEditor.commit();
    }
    private void syncLocation(){
	if(mGpsCheckStatus == ElementView.COLUMN_STATUS_UNCHECK){
	    boolean ret = GlassSyncLbsManager.getInstance(mContext).startLocation();
	    if(ret){
		mGpsCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
		saveData("gps_checked",true);
	    }
	}else{
	    GlassSyncLbsManager.getInstance(mContext).stopLocation();
	    mGpsCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
	    saveData("gps_checked",false);
	}
	mGpsView.setShow(mGpsCheckStatus);
    }
    private void syncData(String key,boolean checked){
	if (DEBUG) Log.d(TAG,"key="+key+"  checked="+checked);
	Message msg = mHandler.obtainMessage(MSG_NOTIFY);
	msg.obj = new Arg(key, checked);
	msg.sendToTarget();
    }

    private void syncDataFailed(String key){
	if(mSmsCheckStatus == ElementView.COLUMN_STATUS_CHECKING  && key.equals(MMS_KEY)){
	    mHandler.removeMessages(MSG_SEND_MESSAGE_TIMEOUT);

	    mSmsCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
	    mSmsView.setShow(mSmsCheckStatus);

	    saveData("sms_checked",false);

	    Toast.makeText(mContext.getApplicationContext(), R.string.sms_send_fail,
			   Toast.LENGTH_SHORT).show();
	}else if(mContactCheckStatus == ElementView.COLUMN_STATUS_CHECKING && key.equals(CONTACT_KEY)){
	    mHandler.removeMessages(MSG_SEND_CONTACT_TIMEOUT);

	    mContactCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
	    mContactView.setShow(mContactCheckStatus);

	    saveData("contact_checked",false);

	    Toast.makeText(mContext.getApplicationContext(), R.string.contact_send_fail,
			   Toast.LENGTH_SHORT).show();				
	}else if(mWifiCheckStatus == ElementView.COLUMN_STATUS_CHECKING && key.equals(WIFI_KEY)){
	    mHandler.removeMessages(MSG_WIFI_SYNC_TIMEOUT);

	    mWifiCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
	    mWifiView.setShow(mWifiCheckStatus);

	    saveData("wifi_checked",false);

	    Toast.makeText(mContext.getApplicationContext(), R.string.wifi_sync_fail,
			   Toast.LENGTH_SHORT).show();				
	}
    }

    private void syncDataFinished(String key){
	if(mSmsCheckStatus == ElementView.COLUMN_STATUS_CHECKING && key.equals(MMS_KEY)){
	    mHandler.removeMessages(MSG_SEND_MESSAGE_TIMEOUT);

	    mSmsCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
	    mSmsView.setShow(mSmsCheckStatus);

	    saveData("sms_checked",true);

	}else if(mContactCheckStatus == ElementView.COLUMN_STATUS_CHECKING && key.equals(CONTACT_KEY)){
	    mHandler.removeMessages(MSG_SEND_CONTACT_TIMEOUT);

	    mContactCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
	    mContactView.setShow(mContactCheckStatus);

	    saveData("contact_checked",true);

	}else if(mWifiCheckStatus == ElementView.COLUMN_STATUS_CHECKING && key.equals(WIFI_KEY)){
	    mHandler.removeMessages(MSG_WIFI_SYNC_TIMEOUT);
	    mWifiCheckStatus  = ElementView.COLUMN_STATUS_CHECKED;
	    mWifiView.setShow(mWifiCheckStatus);
	    saveData("wifi_checked",true);

	}
    }
    private void initData() {
	if (mSharedPreferences.getBoolean("contact_checked", false)) {
	    mContactCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
	    mContactView.setShow(mContactCheckStatus);
	}
	if (mSharedPreferences.getBoolean("sms_checked", false)) {
	    mSmsCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
	    mSmsView.setShow(mSmsCheckStatus);

	}
	if (mSharedPreferences.getBoolean("wifi_checked", false)) {
	    mWifiCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
	    mWifiView.setShow(mWifiCheckStatus);
	}
	if (mSharedPreferences.getBoolean("gps_checked", false)) {
	    syncLocation();
	}

	{//sync time default
	    syncData(TIME_KEY,true);
	    saveData("time_checked",true);	    
	    mTimeView.setShow(mTimeCheckStatus);
	}
	  //sync headset default
	mPhoneView.setShow(mPhoneCheckStatus);
	if(mGlassDetect.getCurrentState() == BluetoothProfile.STATE_DISCONNECTED){
	    mGlassDetect.set_audio_connect();
	}
   }

    @Override
	public void onDestroy() {
	  // TODO Auto-generated method stub
	if (DEBUG)Log.e(TAG,"---onDestroy in");
	mHandler.removeMessages(MSG_SEND_CONTACT_TIMEOUT);
	mHandler.removeMessages(MSG_SEND_MESSAGE_TIMEOUT);
	mContext.unregisterReceiver(nReceiver);
	super.onDestroy();

	}
    
    private void updatePhoneAudioUI(int status){
	if(DEBUG)Log.d(TAG,"updatePhoneAudioUI status = "+status);
	if (status == BluetoothProfile.STATE_CONNECTED) {	
	    mPhoneCheckStatus = ElementView.COLUMN_STATUS_CHECKED;
	}else if(status == BluetoothProfile.STATE_DISCONNECTED){	
	    mPhoneCheckStatus = ElementView.COLUMN_STATUS_UNCHECK;
	}

	mPhoneView.setShow(mPhoneCheckStatus);
    }

    private void preCheckDBReadability(String type){	
        final String id = type;
	new Thread(new Runnable() {
		@Override
    		    public void run() {
		    boolean returnValue = false;
		    Cursor cursor = null;
		    Message msgContact = new Message();
		    if(id.equals("contact")){
			cursor = getActivity().getApplicationContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null,null,null,null);
			if (cursor != null && cursor.getCount() != 0)
			    mContactDBReadability = true;
		    }else if(id.equals("sms")){
			cursor = getActivity().getApplicationContext().getContentResolver().query(Uri.parse("content://sms"),null,null,null,null);
			if (cursor != null && cursor.getCount() != 0)
			    mSmsDBReadability = true;
		    }
		  
		  cursor.close();
		}}).start();
	
    }

    class Receiver extends BroadcastReceiver{
        @Override
	    public void onReceive(Context context, Intent intent) {
	    if (DEBUG)Log.d(TAG, "onReceive " + intent.getAction());
	    if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
		int state=intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
		if(mHeadsetRequestFromClick == false){
		    Boolean state_on = mPhoneCheckStatus == ElementView.COLUMN_STATUS_CHECKED ? true:false;
		    if (DEBUG)Log.d(TAG, "last_headset_state " + state_on);
		    saveData("last_headset_state",state_on);
		}
		mHeadsetRequestFromClick = false;
		updatePhoneAudioUI(state);
	     }
        }
    }
    private void dumpWhat(int what){
	switch(what){
	case 0:
	    Log.d(TAG,"what is :"+"MSG_NOTIFY");
	    break;
	case 1:
	    Log.d(TAG,"what is :"+"MSG_SEND_FINISH");
	    break;
	case 2:
	    Log.d(TAG,"what is :"+"MSG_SEND_FAIL");
	    break;
	case 3:
	    Log.d(TAG,"what is :"+"MSG_SEND_MESSAGE_TIMEOUT");
	    break;
	case 4:
	    Log.d(TAG,"what is :"+"MSG_SEND_CONTACT_TIMEOUT");
	    break;
	case 5:
	    Log.d(TAG,"what is :"+"PHONE_AUDIO_CONNECT_TIMEOUT");
	    break;
	case 6:
	    Log.d(TAG,"what is :"+"PHONE_AUDIO_CONNECT");
	    break;
	case 7:
	    Log.d(TAG,"what is :"+"PHONE_AUDIO_DISCONNECT");
	    break;
	default:
	    Log.d(TAG,"what is :"+"nothing!");
	    break;
	}
    }
}
