package cn.ingenic.glasssync.lbs;

import android.content.Context;
import android.util.Log;

import android.location.Location;  
import android.location.LocationListener;  
import android.location.LocationManager;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class GlassSyncLbsModule extends SyncModule {
    private static final String TAG = "GlassSyncLbsModule";
    private static final String LETAG = "GSLBSMD";
    private static final boolean DEBUG = true;

    private static final String CMD_TYPE = "cmd_type";
    private static final int TYPE_OPEN = 1;
    private static final int TYPE_CLOSE = 2;
    private static final int TYPE_DATA = 3;

    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String BEARING = "bearing";

    private Context mContext = null;
    private static GlassSyncLbsModule sInstance = null;

    private GlassSyncLbsModule(Context context){
		super(LETAG, context);
		mContext = context;
    }

    public static GlassSyncLbsModule getInstance(Context c) {
		if (null == sInstance)
			sInstance = new GlassSyncLbsModule(c);
		return sInstance;
    }

    @Override
		protected void onCreate() {
    }

    public void send_open_testprovider(){
	SyncData data = new SyncData();
	data.putInt(CMD_TYPE, TYPE_OPEN);
	
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void send_close_testprovider(){
	SyncData data = new SyncData();
	data.putInt(CMD_TYPE, TYPE_CLOSE);
	
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void send_lbs(Location location){
	SyncData data = new SyncData();

	double lat = location.getLatitude();
	double lon = location.getLongitude();
	float bearing = location.getBearing();

	data.putInt(CMD_TYPE, TYPE_DATA);
	data.putDouble(LATITUDE, lat);
	data.putDouble(LONGITUDE, lon);
	data.putFloat(BEARING, bearing);

	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
}