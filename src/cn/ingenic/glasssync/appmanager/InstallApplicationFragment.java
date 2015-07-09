package cn.ingenic.glasssync.appmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.appmanager.PhoneCommon.SimpleBase;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class InstallApplicationFragment extends SimpleFragment {

	private InsertListener mListener;
	private AppCache mAppCache;
	private View mView;

	class InsertListener implements AppCache.InstalledApplicationChangeListener {

		@Override
		public void onAddAllInstallApp() {
			if (DEBUG)
				Log.i(APP, "InstallListener onAddAllInstallApp .");
			stopDialog();
			Map<String, AppEntry> list = mAppCache.getInstallList();
			if (list.size() == 0) {
				setEmpty();
			} else {
				notifyChange(list);
			}

		}

		@Override
		public void onAddOneInstallApp() {
			refaush();
		}

		@Override
		public void onRemoveOneInstalledApp() {
			stopDialog();
			Toast.makeText(getActivity(),
					getActivity().getString(R.string.uninstall_success),
					Toast.LENGTH_LONG).show();
			refaush();

		}

		@Override
		public void onStart() {
			if (DEBUG)
				Log.i(APP, "InstallListener onStart  .");
			showDialog();
		}

		@Override
		public void onChangeOneApp() {
			refaush();
		}

		@Override
		public void onConnectChanged() {
			refaush();
			Toast.makeText(getActivity(),
					R.string.bluetooth_connect_wrong_message, Toast.LENGTH_LONG)
					.show();
			getActivity().finish();
		}

	}

	private void refaush() {
		Map<String, AppEntry> list = mAppCache.getInstallList();
		notifyChange(list);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = super.onCreateView(inflater, container, savedInstanceState);
		mAppCache = AppCache.getInstance();

		mListener = new InsertListener();
		mAppCache.registerInstalledApplicationChangeListener(mListener);
		if (DEBUG)
			Log.i(APP, "InstallApplicationFragment onCreateView  .");

		((ApplicationManagerActivity2) getActivity()).initData();

		return mView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG)
			Log.i(APP, "InstallApplicationFragment onDestroy  .");
		mAppCache.unRegisterInstalledApplicationChangeListener(mListener);
		mAppCache.destroy();
	}

}
