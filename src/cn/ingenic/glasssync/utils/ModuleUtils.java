package cn.ingenic.glasssync.utils;

import cn.ingenic.glasssync.R;
import android.util.Log;
import android.content.Context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.FeatureConfigCmd;

import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.SystemModule;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;
import cn.ingenic.glasssync.sms.SmsModule;

public class ModuleUtils {
    public static void disableSettings(Context context){
	disableTimeSync();
	disableSMSSync(context);
	disableContactSync(context);
    }

    private static void disableTimeSync(){
	DefaultSyncManager manager = DefaultSyncManager.getDefault();

	Config config = new Config(SystemModule.SYSTEM);
	Map<String, Boolean> map = new HashMap<String, Boolean>();
	map.put(DeviceModule.FEATURE_TIME, false);
	Projo projo = new FeatureConfigCmd();
	projo.put(FeatureConfigCmd.FeatureConfigColumn.feature_map, map);
	ArrayList<Projo> datas = new ArrayList<Projo>();
	datas.add(projo);
	manager.request(config, datas);
	manager.featureStateChange(DeviceModule.FEATURE_TIME, false);
    }

    private static void disableSMSSync(Context context){
	SmsModule sm = (SmsModule) SmsModule.getInstance(context);
	sm.sendSyncRequest(false,null);
	sm.setSyncEnable(false);
    }	

    private static void disableContactSync(Context context){
	ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(context);
	clm.sendSyncRequest(false,null);
	clm.setSyncEnable(false);
    }

}
