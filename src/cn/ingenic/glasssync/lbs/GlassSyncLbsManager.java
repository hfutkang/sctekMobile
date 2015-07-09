package cn.ingenic.glasssync.lbs;

import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.widget.Toast;
import android.os.Bundle;
import android.location.Criteria;
import android.location.Location;  
import android.location.LocationListener;  
import android.location.LocationManager;
import android.location.LocationProvider;

import cn.ingenic.glasssync.lbs.GlassSyncLbsModule;

public class GlassSyncLbsManager{
    private static final String TAG = "GlassSyncLbsManager";
    private final boolean DEBUG = true;
    private Context mContext = null;
    private static GlassSyncLbsManager sInstance = null;

    private final static String DEFAULT_LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;
    private LocationManager mLocationManager = null;

    private GlassSyncLbsManager(Context context){
	mContext = context;
    }

    public static GlassSyncLbsManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new GlassSyncLbsManager(c);
	return sInstance;
    }

    public Boolean startLocation(){
	if(mLocationManager != null)
	    return true;

	mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
	if(mLocationManager == null){
	    Toast.makeText(mContext,"get LocationManager failed",
			   Toast.LENGTH_LONG).show();
	    Log.e(TAG, "Fail to get LocationManager system service.");
	    return false;
	}
	GlassSyncLbsModule m = GlassSyncLbsModule.getInstance(mContext);
	m.send_open_testprovider();

	Criteria criteria = new Criteria();
	criteria.setAccuracy(Criteria.ACCURACY_FINE);
	criteria.setAltitudeRequired(false);
	criteria.setBearingRequired(false);
	criteria.setCostAllowed(true);
	criteria.setPowerRequirement(Criteria.POWER_LOW);
	String provider = mLocationManager.getBestProvider(criteria, true);
	if(DEBUG) Log.e(TAG,"--best provider="+provider);
	Location location = mLocationManager.getLastKnownLocation(provider);
	if (location != null) {
	    double lat = location.getLatitude();
	    double lng = location.getLongitude();
	    m.send_lbs(location);
	    if(DEBUG) Log.e(TAG,"纬度:" + lat + "\n经度:" + lng);
	}
	mLocationManager.requestLocationUpdates(provider, 2000, 10,mLocationListener);
	return true;
    }
    public void stopLocation(){
	if(mLocationManager != null){
	    GlassSyncLbsModule m = GlassSyncLbsModule.getInstance(mContext);
	    m.send_close_testprovider();
	    mLocationManager.removeUpdates(mLocationListener);
	    mLocationManager = null;
	}
    }

    private final LocationListener mLocationListener = new LocationListener() {
	    public void onLocationChanged(Location location) { //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
		if(DEBUG) Log.e(TAG, "onLocationChanged");
		GlassSyncLbsModule m = GlassSyncLbsModule.getInstance(mContext);
		m.send_lbs(location);
		double lat = location.getLatitude();
		double lng = location.getLongitude();

		if(DEBUG)
		Toast.makeText(mContext, "纬度:" + lat + "\n经度:" + lng,
			       Toast.LENGTH_LONG).show();
	    }
			
	    public void onProviderDisabled(String provider) {  
		// Provider被disable时触发此函数，比如GPS被关闭  
	    }
  
	    public void onProviderEnabled(String provider) {  
		//  Provider被enable时触发此函数，比如GPS被打开  
	    }  
  
	    public void onStatusChanged(String provider, int status, Bundle extras) {  
		// Provider的转态在可用、暂时不可用和无服务三个状态直接切换时触发此函数  
	    }  
	};
}