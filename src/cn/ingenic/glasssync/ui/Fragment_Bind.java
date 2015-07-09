package cn.ingenic.glasssync.ui;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.utils.ModuleUtils;
import java.io.OutputStream;
import cn.ingenic.glasssync.ui.BindGlassActivity;

import cn.ingenic.glasssync.SyncApp;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Fragment_Bind extends Fragment implements OnClickListener {
	private final String TAG="Fragment_Bind";
	private static final boolean DEBUG = true;
	private String mAddress;
	private DefaultSyncManager mManager;
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mBLDServer;
	private TextView tv_sacnMessage, bind_glass, tv_bindAddress, tv_exit,
			tv_bindMac;
	private List<TextView> mList = new ArrayList<TextView>();
	private LinearLayout mLayout_bind, mLayout_unbind;
	private View fragmentView;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (fragmentView == null) {
			fragmentView = inflater.inflate(R.layout.fragment_item_bind, null);
		}
		// 缓存的rootView需要判断是否已经被加过parent，
		// 如果有parent需要从parent删除，要不然会发生这个rootview已经有parent的错误。
		ViewGroup parent = (ViewGroup) fragmentView.getParent();
		if (parent != null) {
			parent.removeView(fragmentView);
		}
		if(DEBUG) Log.d(TAG, "onCreat---------come in----------");
		addViewAndListener();
		mManager = DefaultSyncManager.getDefault();
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mLayout_bind.setVisibility(View.VISIBLE);
		mLayout_unbind.setVisibility(View.GONE);
		
		if(mManager.getLockedAddress().equals("")){
		    if(DEBUG) Log.d(TAG, "find a invalid remote mac addr!");
		    Intent intent = new Intent(getActivity(),BindGlassActivity.class);	    
		    startActivity(intent);
		    getActivity().finish();
		}else{
		    mAddress = mManager.getLockedAddress();
		    mBLDServer = mAdapter.getRemoteDevice(mAddress);
		    tv_bindAddress.setText(mBLDServer.getName());
		    tv_bindMac.setText(mBLDServer.getAddress());
		}
		return fragmentView;
	}

	private void addViewAndListener() {
		tv_sacnMessage = (TextView) fragmentView
				.findViewById(R.id.tv_sacnMessage);
		tv_bindAddress = (TextView) fragmentView
				.findViewById(R.id.tv_bindAddress);
		mLayout_bind = (LinearLayout) fragmentView
				.findViewById(R.id.layout_bind);
		mLayout_unbind = (LinearLayout) fragmentView
				.findViewById(R.id.layout_unbind);
		tv_bindMac = (TextView) fragmentView.findViewById(R.id.tv_bindMac);
		//bind_glass = (TextView) fragmentView.findViewById(R.id.bind_glass);
		tv_exit = (TextView) fragmentView.findViewById(R.id.tv_exit);
		tv_exit.setOnClickListener(this);
		//mList.add(bind_glass);
		//mList.add(tv_bindAddress);
		//mList.add(tv_exit);
		//mList.add(tv_sacnMessage);
		//Typeface(mList);
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(DEBUG) Log.e(TAG,"---onResume");

		if (mAddress != null && !mAddress.equals("")) {
			mLayout_bind.setVisibility(View.VISIBLE);
			mLayout_unbind.setVisibility(View.GONE);
		}
	}

        @Override 
	    public void onDestroyView(){
	    super.onDestroyView();   
	    if(DEBUG) Log.e(TAG,"---onDestroyView");
	}

	public void Typeface(List<TextView> list) {
		Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(),
				"fonts/iphone.ttf");
		for (TextView view : list) {
			view.setTypeface(typeface);
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.tv_exit) {
			tv_exit.setText(getActivity().getResources()
					.getString(R.string.disconnecting));
			GlassDetect glassDetect = (GlassDetect)GlassDetect.getInstance(getActivity().getApplicationContext());
			glassDetect.set_audio_disconnect();

			ModuleUtils.disableSettings(getActivity().getApplicationContext());
			disableLocalData();
			unBond();
		}

	}
    
    private void unBond() {
	new Thread(new Runnable() {
		@Override
		    public void run() {
		    mManager.setLockedAddress("",true);
		    if(DEBUG) Log.d(LogTag.APP, "unbind setlockeraddress ok");
		    try {
			Thread.sleep(1000);
		    } catch (Exception e) {
			Log.e(TAG,"---Exception="+e);
		    }

		    mManager.disconnect();
		    
		    //mBLDServer.removeBond();
		    Fragment_MainActivity activity = (Fragment_MainActivity)getActivity();
		    if(activity != null){
			activity.getHandler().sendEmptyMessageDelayed(Fragment_MainActivity.MESSAGE_UNBIND, 0);
		    }
		}
    	    }).start();
    }		

    private void disableLocalData(){	
	SharedPreferences sp = getActivity().getSharedPreferences(SyncApp.SHARED_FILE_NAME
								  ,getActivity().MODE_PRIVATE);
        Editor editor = sp.edit();    
	
	editor.clear();  
	editor.commit();
    }    
}
