package cn.ingenic.glasssync.appmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;

public class AppManagerModule extends SyncModule {
	
	private static final String APP_MANAGER_NAME="application_manager";
	private static AppManagerModule mAppManagerModule=null;
	private Context mContext;
	private AppCache mCache;
	
	public static AppManagerModule getInstance(Context context){
		if(mAppManagerModule==null){
			mAppManagerModule=new AppManagerModule(APP_MANAGER_NAME,context);
		}
		return mAppManagerModule;
	}

	private AppManagerModule(String name, Context context) {
		super(name, context);
		this.mContext=context;
		if(PhoneCommon.DEBUG)Log.i(PhoneCommon.APP,"AppManagerModule contrauctor  .");
	}

	@Override
	protected void onCreate() {
		mCache=AppCache.getInstance();
		if(PhoneCommon.DEBUG)Log.i(PhoneCommon.APP,"AppManagerModule pnCreate .");
	}
	
	

	@Override
	protected void onClear(String address) {
		super.onClear(address);
	}

	@Override
	protected void onConnectionStateChanged(boolean connect) {
		super.onConnectionStateChanged(connect);
		mCache.onConnectChanged(connect);
	}

	@Override
	protected void onRetrive(SyncData data) {
		super.onRetrive(data);
		if(PhoneCommon.DEBUG)Log.i(PhoneCommon.APP,"AppManagerModule onRetrive .");
		String common=data.getString(PhoneCommon.AppDataKey.COMMON);
		PhoneCommon.parse(mCache, data, common);
	}
	
	

}
