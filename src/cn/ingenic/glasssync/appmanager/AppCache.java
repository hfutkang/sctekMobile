package cn.ingenic.glasssync.appmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import cn.ingenic.glasssync.services.SyncData;

public class AppCache implements Cache {

	public static final boolean DEBUG = true;
	public static final String APP = "ApplicationManager";
	private static Map<String,AppEntry> mAllList;
	private static Map<String,AppEntry> mInstallList;
	private ArrayList<AllApplicationChangeListener> mAllListenerList = new ArrayList<AllApplicationChangeListener>();
	private ArrayList<InstalledApplicationChangeListener> mInstanedListenerList = new ArrayList<InstalledApplicationChangeListener>();
//	private ArrayList<ApplicationChangeListener> mListenerList = new ArrayList<ApplicationChangeListener>();
	private static AppCache mAppCache;

	public static AppCache getInstance() {
		if (mAppCache == null) {
			mAppCache = new AppCache();
		}
		return mAppCache;
	}

	private AppCache() {
		initList();
	}

	public Map<String,AppEntry> getAllList() {
		return mAllList;
	}

	public Map<String,AppEntry> getInstallList() {
		return mInstallList;
	}

	public interface AllApplicationChangeListener {
		public void onAddAllApp();

		public void onAddOneSystemApp();

		public void onRemoveOneSystemApp();
		public void onChange();

		public void onStart();
		public void onConnectChanged();
	}

	public interface InstalledApplicationChangeListener {
		public void onAddAllInstallApp();

		public void onAddOneInstallApp();

		public void onRemoveOneInstalledApp();

		public void onStart();

		public void onChangeOneApp();
		public void onConnectChanged();
	}
	
	public void registerAllApplicationChangeListener(AllApplicationChangeListener listener){
		if(!mAllListenerList.contains(listener))mAllListenerList.add(listener);
	}
	
	public void unRegisterAllApplicationChangeListener(AllApplicationChangeListener listener){
		if(mAllListenerList.contains(listener))mAllListenerList.remove(listener);
	}
	
	public void registerInstalledApplicationChangeListener(InstalledApplicationChangeListener listener){
		if(!mInstanedListenerList.contains(listener))mInstanedListenerList.add(listener);
	}
	
	public void unRegisterInstalledApplicationChangeListener(InstalledApplicationChangeListener listener){
		if(mInstanedListenerList.contains(listener))mInstanedListenerList.remove(listener);
	}

	@Override
	public void onGetAllAppInfos(SyncData[] dataArray) {
		if (mAllList.size() != 0)
			mAllList.clear();
		for (SyncData one : dataArray) {
			AppEntry ae=getOneEntry(one);
			mAllList.put(ae.getPackageName(), ae);
		}
		for (AllApplicationChangeListener listener : mAllListenerList) {
			listener.onAddAllApp();
		}
	}

	private AppEntry getOneEntry(SyncData data) {
		String packageName = data
				.getString(PhoneCommon.AppEntryKey.PACKAGE_NAME);
		String appSize = data
				.getString(PhoneCommon.AppEntryKey.APPLICATION_SIZE);
		String appName = data
				.getString(PhoneCommon.AppEntryKey.APPLICATION_NAME);
		byte[] icon = data.getByteArray(PhoneCommon.AppEntryKey.DRAWABLE);
		boolean isSystem = data.getBoolean(PhoneCommon.AppEntryKey.IS_SYSTEM,
				true);
		int enableSetting=data.getInt(PhoneCommon.AppEntryKey.SYSTEM_SETTING_ENABLE);
		return new AppEntry(appName, appSize, packageName, icon, isSystem,enableSetting);
	}

	@Override
	public void onGetInstallAppInfos(SyncData[] dataArray) {
		if (mInstallList.size() != 0)
			mInstallList.clear();
		for (SyncData data : dataArray) {
			AppEntry ae=getOneEntry(data);
			mInstallList.put(ae.getPackageName(), ae);
		}
		Log.e("ApplicationManager", "in onGetInstallAppInfos array length is :"
				+ dataArray.length);
		Log.e("ApplicationManager",
				"in onGetInstallAppInfos mListenerList size is :"
						+ mInstanedListenerList.size() + " hash:" + this);

		for (InstalledApplicationChangeListener listener : mInstanedListenerList) {
			listener.onAddAllInstallApp();
		}
	}

	public void destroy() {
		if (DEBUG)
			Log.i(APP, "destroy .");
		clearList();
	}

	private void initList() {
		mAllList = new HashMap<String,AppEntry>();
		mInstallList = new HashMap<String,AppEntry>();
	}

	private void clearList() {
		if (mAllList != null) {
			mAllList.clear();
		}
		if (mInstallList != null) {
			mInstallList.clear();
		}

	}

	@Override
	public void onAllStart() {
		for (AllApplicationChangeListener listener : mAllListenerList) {
				listener.onStart();
		}

	}

	@Override
	public void onInstallStart() {
		for (InstalledApplicationChangeListener listener : mInstanedListenerList) {
				listener.onStart();
		}

	}

	@Override
	public void onInstallApp(SyncData data) {
		AppEntry ae=getOneEntry(data);
		mInstallList.put(ae.getPackageName(), ae);
		for (InstalledApplicationChangeListener listener : mInstanedListenerList) {
			listener.onAddOneInstallApp();
		}
		
	}

	@Override
	public void onUnInstallApp(SyncData data) {
		AppEntry ae=getOneEntry(data);
		String pkgName = ae.getPackageName();
		Log.e("ApplicationManager","onUnInstallApp packageName is :"+pkgName);
		mInstallList.remove(pkgName);
		for (InstalledApplicationChangeListener listener : mInstanedListenerList) {
			listener.onRemoveOneInstalledApp();
		}
	}


	@Override
	public void onConnectChanged(boolean connect) {
		if(!connect){
			mAllList.clear();
			mInstallList.clear();
			for (InstalledApplicationChangeListener listener : mInstanedListenerList) {
				listener.onConnectChanged();
			}
			for (AllApplicationChangeListener listener : mAllListenerList) {
				listener.onConnectChanged();
		    }
		}
	}

	@Override
	public void onInstallOneAllApp(SyncData data) {
		AppEntry ae=getOneEntry(data);
		String pkgName = ae.getPackageName();
		Log.i("ApplicationManager","onInstallOneAllApp pkgName is :"+pkgName);
		mAllList.put(pkgName, ae);
		for (AllApplicationChangeListener listener : mAllListenerList) {
			listener.onAddOneSystemApp();
	    }
		
	}

	@Override
	public void onUnInstallOneAllApp(SyncData data) {
		AppEntry ae=getOneEntry(data);
		String pkgName = ae.getPackageName();
		Log.i("ApplicationManager","onUnInstallOneAllApp pkgName is :"+pkgName);
		mAllList.remove(pkgName);
		for (AllApplicationChangeListener listener : mAllListenerList) {
			listener.onRemoveOneSystemApp();
	    }
	}

	@Override
	public void onChangeAllApp(SyncData data) {
		AppEntry ae=getOneEntry(data);
		mAllList.remove(ae.getPackageName());
		mAllList.put(ae.getPackageName(), ae);
		for (AllApplicationChangeListener listener : mAllListenerList) {
			listener.onChange();
	    }
	}

	@Override
	public void onChangeInstallApp(SyncData data) {
		AppEntry ae=getOneEntry(data);
		mInstallList.remove(ae.getPackageName());
		mInstallList.put(ae.getPackageName(), ae);
		for (InstalledApplicationChangeListener listener : mInstanedListenerList) {
			listener.onChangeOneApp();
		}
		
	}

}
