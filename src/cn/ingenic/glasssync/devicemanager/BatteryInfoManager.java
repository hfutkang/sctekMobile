package cn.ingenic.glasssync.devicemanager;

import org.json.JSONException;
import org.json.JSONObject;

import cn.ingenic.glasssync.DefaultSyncManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

public class BatteryInfoManager {
    // from  SystemUI/res/drawable/stat_sys_battery.xml
    private static final int BatteryLevel[]={4,15,35,49,60,75,90,100};
	private static BatteryInfoManager sInstance;
	private JSONObject mStoreStatus;
	private static boolean mBatterySync=true;
	
	private BatteryInfoManager(Context context){
        //register battery listener
//		if(!DefaultSyncManager.isWatch()){
	    context.registerReceiver(new BatteryReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//		}
	}
	
	public static BatteryInfoManager init(Context context){
		if(sInstance == null){
			sInstance = new BatteryInfoManager(context);
		}
		return sInstance;
	}
	
	public static BatteryInfoManager getInstance(){
		return sInstance;
	}
	
    public static void setFeature(Context c, boolean enable) {
        mBatterySync = enable;
        if(!enable){
//            JSONObject batteryInfo = new JSONObject();
//            try {
//            batteryInfo.put("level", 111);
//            batteryInfo.put("scale", 100);
//            batteryInfo.put("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
//            ConnectionManager.device2Device(Commands.CMD_GET_BATTERY, batteryInfo.toString());
//            }catch(JSONException e){
//                klilog.e("battery info JSONException:"+e);
//            }
        } else {
            init(c).sendBatteryInfo();
        }
    }
	
	private class BatteryReceiver extends BroadcastReceiver{
        int lastSendLevel = 111, lastSendPlugged = -100;
		@Override
		public void onReceive(Context context, Intent intent) {
			if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                if (!mBatterySync)  //if not enable battery info Sync. just return
                    return;
				Bundle datas =  intent.getExtras();
				JSONObject batteryInfo = new JSONObject();
				int level=datas.getInt("level", 0),plugged=datas.getInt("plugged", 0);
				try {
					batteryInfo.put("level", level);
					batteryInfo.put("scale", datas.getInt("scale", 100));
					batteryInfo.put("plugged", plugged);
					batteryInfo.put("status", datas.getInt("status", BatteryManager.BATTERY_STATUS_UNKNOWN));
//					klilog.i("BatteryInfoManager] battery info (Phone):"+batteryInfo);
                    if (mStoreStatus != null)
                        if (batteryInfo.toString().equals(mStoreStatus.toString())) {
                            return;// if status is not change , do nothing
                        }
					mStoreStatus = batteryInfo;
                    if (LevelToIndex(lastSendLevel) != LevelToIndex(level)
                            || lastSendPlugged != plugged) {
                        if (0 == sendBatteryInfo()) {
                            lastSendLevel = level;
                            lastSendPlugged = plugged;
                        }
                        klilog.i("BatteryInfoManager] battery info need send.");
                    }else{
                        klilog.i("BatteryInfoManager] battery info need NO send.");
                    }
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
    private int LevelToIndex(int level) {
        for (int i = 0; i < 8; i++) {
            if (level <= BatteryLevel[i]) {
                return i;
            }
        }
        return -1;
    }
	
	public int sendBatteryInfo(){
        klilog.i("BatteryInfoManager] send to watch:" + mStoreStatus+",[SyncBattery]="+mBatterySync);
        if (!mBatterySync) {
            return -1;
        }
		if(mStoreStatus != null){
			ConnectionManager.getInstance().device2Device(Commands.CMD_GET_BATTERY, mStoreStatus.toString());
		}else{
			klilog.e("send battery info error: not status");
			return -2;
		}
		return 0;
	}
}
