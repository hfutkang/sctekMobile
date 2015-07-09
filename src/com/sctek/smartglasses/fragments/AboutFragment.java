package com.sctek.smartglasses.fragments;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;

import cn.ingenic.glasssync.R;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("NewApi")
public class AboutFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
	
	private final static String TAG = AboutFragment.class.getName();
	
	private final static int GET_POWER_LEVEL = 13;
	private final static int GET_STORAGE_INFO = 14;
	
	private SyncChannel mChannel;
	private BluetoothAdapter mBluetoothAdapter;
	
	private Preference mVersionPreference;
	private Preference mSerialPreference;
	private Preference mPowerPreference;
	private Preference mStoragePreference;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.about_preference);
		
		getActivity().getActionBar().show();
		getActivity().setTitle(R.string.about_glass);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		mChannel = SyncChannel.create("00e04c68229b0", getActivity(), mOnSyncListener);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		initPrefereceView();
	}
	
	private MyOnSyncListener mOnSyncListener = new MyOnSyncListener();
	private class MyOnSyncListener implements SyncChannel.onChannelListener {

		@Override
		public void onReceive(RESULT arg0, Packet data) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onReceive");
			int type = data.getInt("type");
			switch (type) {
			case GET_POWER_LEVEL:
				int level = data.getInt("power");
				Message msg1 = handler.obtainMessage(GET_POWER_LEVEL, level, 0);
				msg1.sendToTarget();
				break;
			case GET_STORAGE_INFO:
				String total = data.getString("total");
				String available = data.getString("available");
				Message msg2 = handler.obtainMessage(GET_STORAGE_INFO);
				
				Bundle bundle = new Bundle();
				bundle.putString("total", total);
				bundle.putString("available", available);
				msg2.setData(bundle);
				
				msg2.sendToTarget();
				break;
			}
		}

		@Override
		public void onSendCompleted(RESULT arg0, Packet arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStateChanged(CONNECTION_STATE arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void initPrefereceView() {
		
		mVersionPreference = findPreference("version");
		mSerialPreference = findPreference("serial_number");
		mPowerPreference = findPreference("power");
		mStoragePreference = findPreference("storage");
		
		mPowerPreference.setOnPreferenceClickListener(this);
		mStoragePreference.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		String key = preference.getKey();
		if(!mChannel.isConnected()) {
			Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			return true;
		}
		if("power".equals(key)) {
			Packet pk = mChannel.createPacket();
			pk.putInt("type", GET_POWER_LEVEL);
			mChannel.sendPacket(pk);
		}
		else if("storage".equals(key)) {
			Packet pk = mChannel.createPacket();
			pk.putInt("type", GET_STORAGE_INFO);
			mChannel.sendPacket(pk);
		}
		
		return true;
	}
	
	Handler handler = new Handler() {
		
		public void dispatchMessage(android.os.Message msg) {
			
			switch (msg.what) {
			
			case GET_POWER_LEVEL:
				int level = msg.arg1;
				mPowerPreference.setSummary("眼镜当前电量:" + level + "%");
				break;
				
			case GET_STORAGE_INFO:
				Bundle bundle = msg.getData();
				String total = bundle.getString("total");
				String available = bundle.getString("available");
				mStoragePreference.setSummary("眼镜当前存储:" + available + "/" + total); 
				break;
			}
		}
	};
}
