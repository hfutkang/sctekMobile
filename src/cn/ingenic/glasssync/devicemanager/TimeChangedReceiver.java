package cn.ingenic.glasssync.devicemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
public class TimeChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		//klilog.i("time changed received, action:"+intent.getAction()+"   DeviceModule.mEnabled="+DeviceModule.mEnabled);
		if(DeviceModule.mTimeSyncEnabled)TimeSyncManager.getInstance().syncTime();
	}

}
