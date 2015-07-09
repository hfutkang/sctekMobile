package cn.ingenic.glasssync.appmanager;

import cn.ingenic.glasssync.services.SyncData;

public interface Cache {
	void onGetAllAppInfos(SyncData[] dataArray);
	void onGetInstallAppInfos(SyncData[] dataArray);
	void onAllStart();
	void onInstallOneAllApp(SyncData data);//install one system app
	void onUnInstallOneAllApp(SyncData data);//unInstall one system app
	
	void onInstallStart();
	void onInstallApp(SyncData data);//install one app
	void onUnInstallApp(SyncData data);//unInstall one installed app
	void onChangeAllApp(SyncData data);//change one system app
	void onChangeInstallApp(SyncData data);//change one installed app
	void onConnectChanged(boolean connect);
	
}
