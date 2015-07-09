package cn.ingenic.glasssync.appmanager;

import java.util.Map;

import cn.ingenic.glasssync.R;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class AllApplicationFragment extends SimpleFragment{


	private AllListener mListener;
	private AppCache mAppCache;


	  class AllListener implements AppCache.AllApplicationChangeListener{

		@Override
		public void onAddAllApp() {
			if(DEBUG)Log.i(APP,"in AllListener onAddAppInfos  .");
			stopDialog();
			Map<String,AppEntry> list=mAppCache.getAllList();
			if(list.size()==0){
				setEmpty();
			}else{
				notifyChange(list);
			}
		}

		@Override
		public void onAddOneSystemApp() {
			refaush();
		}

		@Override
		public void onRemoveOneSystemApp() {
			refaush();
		}

		@Override
		public void onStart() {
			if(DEBUG)Log.i(APP,"in AllListener onStart  .");
			showDialog();
		}

		@Override
		public void onConnectChanged() {
			refaush();
			
			Toast.makeText(getActivity(),
					R.string.bluetooth_connect_wrong_message,
					Toast.LENGTH_LONG).show();
			getActivity().finish();
		}

		@Override
		public void onChange() {
			refaush();
		}
		
	}
	private void refaush(){
		Map<String,AppEntry> list=mAppCache.getAllList();
		notifyChange(list);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view=super.onCreateView(inflater, container, savedInstanceState);
		mListener=new AllListener();
		mAppCache=AppCache.getInstance();
		mAppCache.registerAllApplicationChangeListener(mListener);
		if(DEBUG)Log.i(APP,"AllApplicationFragment onCreateView  .");

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(DEBUG)Log.i(APP,"AllApplicationFragment onDestroy  .");
		mAppCache.unRegisterAllApplicationChangeListener(mListener);
		mAppCache.destroy();
	}
	
	

}
