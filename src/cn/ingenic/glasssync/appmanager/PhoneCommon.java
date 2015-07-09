package cn.ingenic.glasssync.appmanager;

import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

public class PhoneCommon {
	public static final boolean DEBUG=true;
	public static final String APP="ApplicationManager";
	
	public static final String RECEIVE_ALL_DATAS="receiver_all_app_datas";
	public static final String RECEIVE_INSTALL_DATAS="receiver_install_app_datas";
	public static final String MESSAGE="message";
	public static final String UNINSTALL_MESSAGE="uninstall_message";
	public static final String ADD_APPLICATION_MESSAGE="add_application_message";
	public static final String REMOVE_APPLICATION_MESSAGE="remove_application_message";
	public static final String CHANGE_APPLICATION_MESSAGE="change_application_message";
	public static final String CONNECT_MESSAGE="connect_message";
	
	public static interface SimpleBase{
		public static final int INSTALL=0;
		public static final int ALL=1;
	}
     
	public static interface Application{
		public static final int ALL_APPLICATION_CACHE_COMMON=SimpleBase.ALL;
		public static final int INSTALL_APPLICATION_CACHE_COMMIN=SimpleBase.INSTALL;
	}
	
	public static interface AppEntryKey{
		public static final String DRAWABLE="drawable";
		public static final String APPLICATION_NAME="application_name";
		public static final String APPLICATION_SIZE="application_size";
		public static final String PACKAGE_NAME="package_name";
		public static final String IS_SYSTEM="is_system";
		public static final String SYSTEM_SETTING_ENABLE="system_setting_enable";
	}
	
	public static interface AppDataKey{
		public static final String ALL_APPLICATION="all_application_datas";
		public static final String COMMON="common";
		public static final String PACKAGE_NAME="package_name";
	}
	
	public static interface MessageKey{
		public static final String GET_APP_INFOS_KEY="mode";
	}
	
	public static interface MessageValue{
		public static final int INSTALL=SimpleBase.INSTALL;
		public static final int ALL=SimpleBase.ALL;
	}
	
	public static void parse(Cache cache,SyncData data,String common){
		if(DEBUG)Log.i(APP,"PhoneCommon parse common is :"+common);
		if(common.equals(RECEIVE_ALL_DATAS)){
			SyncData[] all=data.getDataArray(AppDataKey.ALL_APPLICATION);
			cache.onGetAllAppInfos(all);
		}else if(common.equals(RECEIVE_INSTALL_DATAS)){
			SyncData[] install=data.getDataArray(AppDataKey.ALL_APPLICATION);
			cache.onGetInstallAppInfos(install);
		}else if(common.equals(ADD_APPLICATION_MESSAGE)){
			SyncData[] add=data.getDataArray(AppDataKey.ALL_APPLICATION);
			SyncData aOne=add[0];
			boolean isSystem=aOne.getBoolean(AppEntryKey.IS_SYSTEM, true);
			if(!isSystem){
				cache.onInstallApp(aOne);
			}
			cache.onInstallOneAllApp(aOne);
			
			
		}else if(common.equals(REMOVE_APPLICATION_MESSAGE)){
			SyncData[] remove=data.getDataArray(AppDataKey.ALL_APPLICATION);
			SyncData rOne=remove[0];
			boolean isSystem=rOne.getBoolean(AppEntryKey.IS_SYSTEM, true);
			if(!isSystem){
				cache.onUnInstallApp(rOne);
			}
			cache.onUnInstallOneAllApp(rOne);
			
			
		}else if(common.equals(CHANGE_APPLICATION_MESSAGE)){
			SyncData[] change=data.getDataArray(AppDataKey.ALL_APPLICATION);
			SyncData cOne=change[0];
			boolean isSystem=cOne.getBoolean(AppEntryKey.IS_SYSTEM, true);
			if(!isSystem){
				cache.onChangeInstallApp(cOne);
			}
			cache.onChangeAllApp(cOne);
			
		}
	}
}
