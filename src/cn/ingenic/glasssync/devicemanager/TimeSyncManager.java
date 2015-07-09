package cn.ingenic.glasssync.devicemanager;

import android.content.Context;

public class TimeSyncManager {
	private static TimeSyncManager sInstance;
	
	private TimeSyncManager(){
	}
	
	public static TimeSyncManager getInstance(){
		if(sInstance == null){
			sInstance = new TimeSyncManager();
		}
		return sInstance;
	}
	
	public void syncTime(){
		klilog.i("time sync--11");
        String time = System.currentTimeMillis() + "";
        String timezoneId=java.util.TimeZone.getDefault().getID();
        ConnectionManager.getInstance().device2Device(Commands.CMD_GET_TIME, time+","+timezoneId);
	}
}
