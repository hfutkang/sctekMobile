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
	
	private HanLangCmdChannel mHanLangCmdChannel;
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
		
		mHanLangCmdChannel = HanLangCmdChannel.getInstance(getActivity().getApplicationContext());
		mHanLangCmdChannel.setHandler(mChannelHandler);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		initPrefereceView();
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
					mPowerPreference.setSummary("眼镜当前电量:" + level + "%");
					break;
				case GET_STORAGE_INFO:
					String total = data.getString("total");
					String available = data.getString("available");
					mStoragePreference.setSummary("眼镜当前存储:" + available + "/" + total); 
					break;
				}
			}
		}
	};
	
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
	
}
