package cn.ingenic.glasssync.devicemanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Module;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.calllog.CallLogManager;
/**
 * for device sync, to get watch's name & version info
 * @author kli*/
public class DeviceModule extends Module {

    public static final String TAG = "DEVICE";
    public static final boolean V = true;

    static final String MODULE = "DEVICE";
    
    public static final String FEATURE_CALLLOG = "calllog";
    public static final String FEATURE_WEATHER = "weather";
    public static final String FEATURE_BATTERY = "battery";
    public static final String FEATURE_TIME = "time";
    public static final String FEATURE_UNBIND = "unbind";
    public static boolean mTimeSyncEnabled;
    private static DeviceModule sInstance;
    
    private Context mContext;
    private ConnectionManager mConnectionManager;
	private CallLogManager mCallLogManager;
	private BatteryInfoManager mBatteryManager;
	private DeviceTransaction mTransaction;
    
    public ConnectionManager getConnectionManager() {
		return mConnectionManager;
	}

	public CallLogManager getCallLogManager() {
		return mCallLogManager;
	}

    private DeviceModule() {
        super(MODULE, new String[]{FEATURE_CALLLOG, FEATURE_WEATHER, FEATURE_BATTERY, FEATURE_TIME, FEATURE_UNBIND});
    }
    
    public static DeviceModule getInstance(){
    	if(sInstance == null){
    		sInstance = new DeviceModule();
    	}
    	return sInstance;
    }

    protected void onCreate(Context context) {
        if (V) {
            Log.d(TAG, "DeviceModule created.");
        }
        mContext = context;

        //init device transaction
        mTransaction = new DeviceTransaction();
        
        //init connection manager
        mConnectionManager = ConnectionManager.getInstance(context);
        
        //init calllog manager
        mCallLogManager = CallLogManager.getInstance(context);
        
        //init battery manager
        mBatteryManager = BatteryInfoManager.init(mContext);
        
        
//        if (!DefaultSyncManager.isWatch()) {
            registService(IDeviceRemoteService.DESPRITOR,
                    new DeviceRemoteServiceImpl(context));
//        } else {
//            registRemoteService(IDeviceRemoteService.DESPRITOR,
//                    new RemoteBinderImpl(MODULE,
//                    		IDeviceRemoteService.DESPRITOR));
//        }
    }
    
    @Override
	protected Transaction createTransaction() {
		return mTransaction;
	}
    
    public DeviceTransaction getTransation(){
    	return mTransaction;
    }

	@Override
	protected void onConnectivityStateChange(boolean connected) {
		super.onConnectivityStateChange(connected);
		klilog.i("connection changed:" + connected);
		android.util.Log.i("dfdun","connection changed:" + connected);
		Intent intent = new Intent();
        intent.setAction(Commands.ACTION_BLUETOOTH_STATUS);
        intent.putExtra("data", connected);
        mContext.sendBroadcast(intent);
		if (connected) {
				DefaultSyncManager manager = DefaultSyncManager.getDefault();
                // sync time
                if (manager.isFeatureEnabled(FEATURE_TIME)) {
                    TimeSyncManager.getInstance().syncTime();
                }
                // // sync call log
                // if (manager.isFeatureEnabled(FEATURE_CALLLOG)) {
                //     klilog.i("call log sync");
                //     mCallLogManager.sync();
                // }
                // sync battery info
                if (manager.isFeatureEnabled(FEATURE_BATTERY)){
                    mBatteryManager.sendBatteryInfo();
                }else{
                    BatteryInfoManager.setFeature(mContext,false);
                }
		} else {
			mCallLogManager.onDisconnected();
		}
	}

	@Override
	protected void onFeatureStateChange(String feature, boolean enabled) {
			if (FEATURE_BATTERY.equals(feature)) {
				BatteryInfoManager.setFeature(mContext, enabled);
			} else if (FEATURE_TIME.equals(feature)) {
			    mTimeSyncEnabled = enabled;
				if (mTimeSyncEnabled) {
                    klilog.i("time sync--22");
                    String time = System.currentTimeMillis() + "";
                    String timezoneId=java.util.TimeZone.getDefault().getID();
                    ConnectionManager.getInstance()
                            .device2Device(Commands.CMD_GET_TIME, time+","+timezoneId);
				}
			} else if (FEATURE_CALLLOG.equals(feature)) {
				if (enabled) {
					mCallLogManager.sync();
				}
			}
	}

	@Override
	protected void onClear(String address) {
		klilog.i("onClear.  address:"+address);
		mCallLogManager.reset();
	}
	
	@Override
	protected void onModeChanged(int mode) {
			mCallLogManager.sync();
	}

}
