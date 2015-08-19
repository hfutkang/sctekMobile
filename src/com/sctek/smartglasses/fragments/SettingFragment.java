package com.sctek.smartglasses.fragments;

import java.util.HashMap;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.SyncApp;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.devicemanager.TimeSyncManager;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.sctek.smartglasses.ui.VolumeSeekBarPreference;
import com.sctek.smartglasses.utils.HanLangCmdChannel;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.WifiUtils.WifiCipherType;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("NewApi")
public class SettingFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener
							,Preference.OnPreferenceClickListener {
	
	private final static String TAG = SettingFragment.class.getName();
	
	public final static int CONNET_WIFI_MSG = 1;
	public final static int SET_PHOTO_PIXEL = 2;
	public final static int SET_VEDIO_PIXEL = 3;
	public final static int SET_VEDIO_DURATION = 4;
	public final static int SWITCH_GLASSES = 5;
	public final static int SWITCH_ANTI_SHAKE = 16;
	public final static int SWITCH_TIME_STAMP = 17;
	public final static int SET_VOLUME = 8;
	public final static int SET_SSID = 9;
	public final static int SET_PW = 10;
	public final static int SET_WIFI_AP = 11;
	public final static int SWITCH_ROUND_VIDEO = 12;
	
	public final static int MSG_SEND_FINISH = 1;
	public final static int CONTACT_READABLE = 103;
	
	public final static int SETTING_DELAY_TIME = 3000;
	
	public final int PHONE_AUDIO_CONNECT = 6;
	public final int PHONE_AUDIO_DISCONNECT = 7;
	
	private boolean contactReadable = false;
	private boolean syncContactToGlass = false;
	
	private static final String[] lables = {"pixel", "pixel", "pixel", "duration", "sw", "sw", "sw", "volume", "ssid", "pw", "NULL", "sw" };
	private static final String[] keys = {"NULL", "photo_pixel", "vedio_pixel", "duration", 
		"default_switch", "anti_shake", "timestamp"};
	
	private ListPreference mVedioDurationPreference;
//	private VolumeSeekBarPreference mVolumeSeekBarPreference;
	private Preference mWifiPreference;
	private SwitchPreference mBluetoothPhonePreference;
	private SwitchPreference mRoundVideoPreference;
	private SwitchPreference mSyncContactPreference;
	
	private SharedPreferences mHeadsetPreferences;
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private boolean setBack = false;
	
	private GlassDetect mGlassDetect;
	
	private ProgressDialog mProgressDialog;
	
	private HanLangCmdChannel mHanLangCmdChannel;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.setting_preference);
		
		getActivity().getActionBar().show();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		getActivity().setTitle(R.string.setting);
		
		mHeadsetPreferences = getActivity().getApplicationContext().
				getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		mGlassDetect = (GlassDetect)GlassDetect.getInstance(getActivity());
		String addr = DefaultSyncManager.getDefault().getLockedAddress();
		mGlassDetect.setLockedAddress(addr);
		mProgressDialog = new ProgressDialog(getActivity());
		mHanLangCmdChannel = HanLangCmdChannel.getInstance(getActivity().getApplicationContext());
		mHanLangCmdChannel.setHandler(handler);
//		mGlassDetect.setCallBack(handler);
		
		IntentFilter filter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		getActivity().registerReceiver(mBroadcastReceiver, filter);
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		checkContactReadable();
		
		initPrefereceView();
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = super.onCreateView(inflater, container, savedInstanceState);
		return view;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		getActivity().unregisterReceiver(mBroadcastReceiver);
		handler.removeCallbacksAndMessages(null);
		mHanLangCmdChannel.setHandler(null);
		super.onDestroy();
	}
	
	private void initPrefereceView() {
		
		mVedioDurationPreference = (ListPreference)findPreference("duration");
//		mVolumeSeekBarPreference = (VolumeSeekBarPreference)findPreference("volume");
		mWifiPreference = (Preference)findPreference("wifi");
		mBluetoothPhonePreference = (SwitchPreference)findPreference("phone_on");
		mRoundVideoPreference = (SwitchPreference)findPreference("round_video");
		mSyncContactPreference = (SwitchPreference)findPreference("sync_contact");
		
		SharedPreferences mSharedPreferences = getActivity().getApplicationContext().
				getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		mBluetoothPhonePreference.setChecked(mSharedPreferences.getBoolean("last_headset_state", false));
		
		try {
			mVedioDurationPreference.setOnPreferenceChangeListener(this);
//			mVolumeSeekBarPreference.setOnPreferenceChangeListener(this);
			mBluetoothPhonePreference.setOnPreferenceChangeListener(this);
			mRoundVideoPreference.setOnPreferenceChangeListener(this);
			mSyncContactPreference.setOnPreferenceChangeListener(this);
			mWifiPreference.setOnPreferenceClickListener(this);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void onPreferenceChanged(Preference preference, Object value) {
		
		String key = preference.getKey();
		Packet pk = mHanLangCmdChannel.createPacket();
		Log.e(TAG, "" + key);
		if("duration".equals(key)) {
			
			String duration = (String)value;
			pk.putInt("type", SET_VEDIO_DURATION);
			pk.putString("duration", duration);
			
		}
		else if("volume".equals(key)) {
			int volume = (Integer)value;
			pk.putInt("type", SET_VOLUME);
			pk.putInt("volume", volume);
		}
		else if("phone_on".equals(key)) {
			boolean on = (Boolean)value;
			if(on) {
				Log.e(TAG, "set phone on");
				mProgressDialog.setMessage(getActivity().getResources().getText(R.string.turning_bluetoothheadset_on));
				mProgressDialog.show();
				mGlassDetect.set_audio_connect();
			}
			else {
				Log.e(TAG, "set phone off");
				mProgressDialog.setMessage(getActivity().getResources().getText(R.string.turning_bluetoothheadset_off));
				mProgressDialog.show();
				mGlassDetect.set_audio_disconnect();
			}
			return;
		}
		else if("round_video".equals(key)) {
			boolean sw = (Boolean)value;
			pk.putInt("type", SWITCH_ROUND_VIDEO);
			pk.putBoolean("sw", sw);
		}
		else if("sync_contact".equals(key)) {
			syncContactToGlass = (Boolean)value;
			if(syncContactToGlass) {
				if(contactReadable) {
					syncContactToGlass(syncContactToGlass);
				}
				else {
					checkContactReadable();
					Toast.makeText(getActivity(), R.string.no_read_contact_permission, Toast.LENGTH_SHORT).show();
				}
			}
			else {
				syncContactToGlass(syncContactToGlass);
			}
			return ;
		}
		
		mHanLangCmdChannel.sendPacket(pk);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		
		if(setBack) {
			Log.e(TAG, "set back true");
			setBack = false;
			handler.removeCallbacks(disableSetBackRunnable);
			return true;
		}
		Log.e(TAG, "set back false");
		String key = preference.getKey();
		if(mHanLangCmdChannel.isConnected()) {
			onPreferenceChanged(preference, newValue);
			return false;
		}
		else {
			Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onPreferenceClick");
		String key = preference.getKey();
		Log.e(TAG, key);
		if("wifi".equals(key)) {
			showWifiSettingDialog();
		}
		return true;
	}
	
	private class SettingRunnable implements Runnable{
		
		private String key;
		private boolean bValue;
		private String sValue;
		private Preference mPref;
		private boolean isBoolean;
		
		public SettingRunnable(String key, boolean value, Preference pref) {
			
			this.key = key;
			bValue = value;
			isBoolean = true;
			mPref = pref;
		}
		
		public SettingRunnable(String key, String value, Preference pref) {
			this.key = key;
			sValue = value;
			isBoolean = false;
			mPref = pref;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.e(TAG, "run");
			setBack = true;
			
			if(isBoolean) 
				((SwitchPreference)mPref).setChecked(bValue);
			else
				((ListPreference)mPref).setValue(sValue);
			
		}
		
	}
	
	private Runnable disableSetBackRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.e(TAG, "disableSetBackRunnable");
			setBack = false;
		}
	};
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			
			if(msg.what == HanLangCmdChannel.RECEIVE_MSG_FROM_GLASS) {
				
				Packet data = (Packet)msg.obj;
				int type = data.getInt("type");
				String sValue = null;
				boolean bValue = false;
				
				switch (type) {
				
					case SET_VEDIO_DURATION:
						setBack = true;
						sValue = data.getString(lables[type -1]);
						mVedioDurationPreference.setValue(sValue);
						break;
//					case SET_VOLUME:
//						int volume = data.getInt(lables[type -1]);
//						mVolumeSeekBarPreference.setValue(volume);
//						break;
					case SWITCH_ROUND_VIDEO:
						setBack = true;
						bValue = data.getBoolean(lables[type -1]);
						mRoundVideoPreference.setChecked(bValue);
						break;
					case SET_WIFI_AP:
						String ssid = data.getString("ssid");
						String pw = data.getString("pw");
						
						SharedPreferences pref = PreferenceManager
								.getDefaultSharedPreferences(getActivity());
						Editor editor = pref.edit();
						editor.putString("ssid", ssid);
						editor.putString("pw", pw);
						editor.commit();
						
						WifiManager wm = (WifiManager)getActivity().getSystemService(getActivity().WIFI_SERVICE);
						if(WifiUtils.getWifiAPState(wm) == 13) {
							WifiUtils.toggleWifi(getActivity(), wm);
							WifiUtils.turnWifiApOn(getActivity(), wm, WifiCipherType.WIFICIPHER_NOPASS);
						}
						break;
						default:
							break;
				}
			}
			else if(msg.what == PHONE_AUDIO_CONNECT) {
				setBack = true;
				Log.e(TAG, "phone_on1");
				mBluetoothPhonePreference.setChecked(true);
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				
				Editor editorOn = mHeadsetPreferences.edit();
				editorOn.putBoolean("last_headset_state", true);
				editorOn.commit();
			}
			else if(msg.what == PHONE_AUDIO_DISCONNECT) {
				setBack = true;
				Log.e(TAG, "phone_off1");
				mBluetoothPhonePreference.setChecked(false);
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				
				Editor editorOff = mHeadsetPreferences.edit();
				editorOff.putBoolean("last_headset_state", false);
				editorOff.commit();
			}
			else if(msg.what == MSG_SEND_FINISH) {
					setBack = true;
					mSyncContactPreference.setChecked(syncContactToGlass);
			}
			else if(msg.what == CONTACT_READABLE) {
				setBack = true;
				mSyncContactPreference.setChecked((Boolean)msg.obj);
			}
			handler.postDelayed(disableSetBackRunnable, 200);
		}
	};
	
	private void showWifiSettingDialog() {
		
		final String prSsid = WifiUtils.getDefaultApSsid(getActivity());
		
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.wifi_setting, null);
		final EditText ssidEt = (EditText)view.findViewById(R.id.ap_ssid_et);
		
		ssidEt.setText(prSsid);
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.wifi_ap_setting);
		builder.setView(view);
		
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		
		builder.setPositiveButton(R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
				
				
				String ssid = ssidEt.getText().toString();
				
				if(ssid.isEmpty()) {
					Toast.makeText(getActivity(), R.string.empty_ssid, Toast.LENGTH_SHORT).show();
					return;
				}
				
				if(ssid.equals(prSsid))
					return;
				
				Packet pk = mHanLangCmdChannel.createPacket();
				pk.putInt("type", SET_WIFI_AP);
				pk.putString("ssid", ssid);
				mHanLangCmdChannel.sendPacket(pk);
			}
		});
		
		builder.create().show();
	}
	
	private BroadcastReceiver mBroadcastReceiver =  new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.e(TAG, intent.getAction());
			if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
				int state=intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
				Log.e(TAG, "state:" + state);
				if(state == BluetoothProfile.STATE_CONNECTED) {
					handler.sendEmptyMessage(PHONE_AUDIO_CONNECT);
				}
				if(state == BluetoothProfile.STATE_DISCONNECTED) {
					handler.sendEmptyMessage(PHONE_AUDIO_DISCONNECT);
				}
			}
		}
	};
	
	private void syncContactToGlass(boolean on){
		ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(getActivity().getApplicationContext());
		clm.sendSyncRequest(on,handler);
		clm.setSyncEnable(on);
	}
	
	private void checkContactReadable() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Cursor cursor = null;
				cursor = getActivity().getApplicationContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null,null,null,null);
				if (cursor != null && cursor.getCount() != 0) {
					contactReadable = true;
				}
				else {
					contactReadable = false;
				}
				
				handler.obtainMessage(CONTACT_READABLE, contactReadable).sendToTarget();
				cursor.close();
			}
		}).start();
	}
}
