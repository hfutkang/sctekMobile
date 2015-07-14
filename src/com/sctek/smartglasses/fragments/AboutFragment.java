package com.sctek.smartglasses.fragments;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.sctek.smartglasses.utils.HanLangCmdChannel;

import cn.ingenic.glasssync.R;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("NewApi")
public class AboutFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
	
	private final static String TAG = AboutFragment.class.getName();
	
	private final static int GET_POWER_LEVEL = 13;
	private final static int GET_STORAGE_INFO = 14;
	private final static int GET_UP_TIME = 16;
	private final static int GET_GLASS_INFO = 17;
	
	private final static int GET_POWER_TIMEOUT = 1;
	private final static int GET_STORAGE_TIMEOUT = 2;
	private final static int GET_UPTIME_TIMEOUT = 3;
	
	private HanLangCmdChannel mHanLangCmdChannel;
	private BluetoothAdapter mBluetoothAdapter;
	
	private Preference mModelPreference;
	private Preference mCpuPreference;
	private Preference mRamPrefrence;
	private Preference mVersionPreference;
	private Preference mSerialPreference;
	private Preference mPowerPreference;
	private Preference mStoragePreference;
	private Preference mUptimePreference;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.about_preference);
		
		getActivity().getActionBar().show();
		getActivity().setTitle(R.string.about_glass);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		mHanLangCmdChannel = HanLangCmdChannel.getInstance(getActivity().getApplicationContext());
		mHanLangCmdChannel.setHandler(mChannelHandler);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		initPrefereceView();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		mHanLangCmdChannel.setHandler(null);
		super.onDestroy();
	}
	private Handler mChannelHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == HanLangCmdChannel.RECEIVE_MSG_FROM_GLASS) {
				Packet data = (Packet)msg.obj;
				int type = data.getInt("type");
				switch (type) {
				case GET_POWER_LEVEL:
					int level = data.getInt("power");
					mPowerPreference.setSummary("悍狼当前电量:" + level + "%");
					removeMessages(GET_POWER_TIMEOUT);
					break;
				case GET_STORAGE_INFO:
					String total = data.getString("total");
					String available = data.getString("available");
					mStoragePreference.setSummary("悍狼当前存储:" + available + "/" + total); 
					removeMessages(GET_STORAGE_TIMEOUT);
					break;
				case GET_UP_TIME:
					long upTime = data.getLong("uptime");
					mUptimePreference.setSummary(parseUpTime(upTime));
					removeMessages(GET_UPTIME_TIMEOUT);
					break;
					
				}
			}
			else if(msg.what == GET_POWER_TIMEOUT) {
				mPowerPreference.setSummary(R.string.get_power_timeout);
			}
			else if(msg.what == GET_STORAGE_TIMEOUT) {
				mStoragePreference.setSummary(R.string.get_storage_timeout);
			}
			else if(msg.what == GET_UPTIME_TIMEOUT) {
				mUptimePreference.setSummary(R.string.get_uptime_timeout);
			}
		}
	};
	
	private void initPrefereceView() {
		
		mModelPreference = findPreference("model");
		mCpuPreference = findPreference("cpu");
		mRamPrefrence = findPreference("ram");
		mVersionPreference = findPreference("version");
		mSerialPreference = findPreference("serial");
		mPowerPreference = findPreference("power");
		mStoragePreference = findPreference("storage");
		mUptimePreference = findPreference("uptime");
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mModelPreference.setSummary(sharedPreferences.getString("model", "HanLang T10-C22"));
		mCpuPreference.setSummary(sharedPreferences.getString("cpu", "Ingenic Xburst V4.15"));
		mRamPrefrence.setSummary(sharedPreferences.getString("ram", "512M"));
		mVersionPreference.setSummary(sharedPreferences.getString("version", "4.3"));
		mSerialPreference.setSummary(sharedPreferences.getString("serial", "1234567ABCDEF"));
		
		getPower();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_POWER_TIMEOUT, 5000);
		
		getStorage();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_STORAGE_TIMEOUT, 5000);
		
		getUptime();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_UPTIME_TIMEOUT, 5000);
		
	}
	
	private String parseUpTime(long time) {
		
		long totalSecond = time/1000;
		int upSecond = (int) (totalSecond%60);
		int upMinute = (int) ((totalSecond/60)%60);
		int upHour = (int) ((totalSecond/60/60));
		
		String result = null;
		if(upHour > 0 ) {
			result = String.format("悍狼已运行%d小时%d分%d秒", upHour, upMinute, upSecond);
		}
		else if(upMinute > 0) {
			result = String.format("悍狼已运行%d分%d秒", upMinute, upSecond);
		}
		else {
			result = String.format("悍狼已运行%d秒", upSecond);
		}
		return result;
		
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		String key = preference.getKey();
		if(!mHanLangCmdChannel.isConnected()) {
			Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			return true;
		}
		if("power".equals(key)) {
			Packet pk = mHanLangCmdChannel.createPacket();
			pk.putInt("type", GET_POWER_LEVEL);
			mHanLangCmdChannel.sendPacket(pk);
		}
		else if("storage".equals(key)) {
			Packet pk = mHanLangCmdChannel.createPacket();
			pk.putInt("type", GET_STORAGE_INFO);
			mHanLangCmdChannel.sendPacket(pk);
		}
		
		return true;
	}
	
	private void getPower() {
		Packet pk = mHanLangCmdChannel.createPacket();
		pk.putInt("type", GET_POWER_LEVEL);
		mHanLangCmdChannel.sendPacket(pk);
	}
	
	private void getStorage() {
		Packet pk = mHanLangCmdChannel.createPacket();
		pk.putInt("type", GET_STORAGE_INFO);
		mHanLangCmdChannel.sendPacket(pk);
	}
	
	private void getUptime() {
		Packet pk = mHanLangCmdChannel.createPacket();
		pk.putInt("type", GET_UP_TIME);
		mHanLangCmdChannel.sendPacket(pk);
	}
	
}
