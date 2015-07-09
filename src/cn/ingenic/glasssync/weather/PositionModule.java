package cn.ingenic.glasssync.weather;

import java.io.File;
import java.io.FileNotFoundException;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.wifi.WifiManager;
import android.view.KeyEvent;
import android.widget.Toast;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.R;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.location.BDLocationListener;
import android.net.ConnectivityManager;
import android.content.Context;
public class PositionModule extends SyncModule {
    private static final String TAG = "PositionModule";
    public static final Boolean VDBG = true;

    private static final String POSITION_CMD = "weather_cmd";
    private static final String POSITION_REQUEST = "request";
    private static final String POSITION_RESPONSE = "response";       
    public static final int  GET_POSITION_COMPLETE = 1;
    private final int REQUSET_LOCATION=2;
    private final int CENCEL_LOCATION=3;
    private Context mContext;
    private static PositionModule sInstance;  
    private LocationClient mLocationClient;
    private MyLocationListener mMyLocationListener;
    private BDLocation mLocation;
    private PositionModule(Context context){
	super(TAG, context);
	mContext = context;
	mLocationClient = new LocationClient(context);
	mMyLocationListener = new MyLocationListener();
	mLocationClient.registerLocationListener(mMyLocationListener);
	InitLocation();
	mLocationClient.start();
    }

    public static PositionModule getInstance(Context c) {
	if (null == sInstance){
	    sInstance = new PositionModule(c);
	}
	return sInstance;
    }
     @Override
    protected void onCreate() {
    }
    private Handler mHandler = new Handler(){  
	    @Override  
		public void handleMessage(Message msg) {  
	        switch (msg.what) {  
		case REQUSET_LOCATION:
		    mLocationClient.requestLocation();
		    break;
		case CENCEL_LOCATION:
		    release();
		    break;
		default:  
		    Log.e(TAG, "Not Get Message from Client!");  
	            break;  
		}  
	    }         
	};  
    public void sendResponse(String[] positionStr) {
	if(VDBG) Log.e(TAG, "---sendResponse"+"positionStr="+positionStr);
	SyncData data = new SyncData();
	data.putStringArray(POSITION_CMD, positionStr);	
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send file sync failed:" + e);
	}
    }

    @Override
	protected void onRetrive(SyncData data) {
	String cmd = data.getString(POSITION_CMD);
	if(VDBG) Log.d(TAG, "retrive data cmd:" + cmd);
	if(checkNetworkState())
	    mHandler.sendEmptyMessage(REQUSET_LOCATION);
	else {
	    Toast.makeText(mContext,mContext.getResources().getString(R.string.weather_tip2),
			   Toast.LENGTH_LONG).show();
	    sendResponse(null);
	}
    }

    protected void onFileSendComplete(String fileName, boolean success) {
	if(VDBG) Log.e(TAG, "---onFileSendComplete:" + fileName+" success="+success);
    }
    private void InitLocation(){
	LocationClientOption option = new LocationClientOption();
	option.setLocationMode(LocationMode.Hight_Accuracy);
	option.setCoorType("bd09ll");
	option.setIsNeedAddress(true);// 返回的定位结果包含地址信息
	option.setOpenGps(true);
	mLocationClient.setLocOption(option);
    }
    public class MyLocationListener implements BDLocationListener {
	@Override
	    public void onReceiveLocation(BDLocation location) {
	    //Receive Location 
	    Log.d(TAG,"onReceiveLocation");
	    if(location.getLocType() == BDLocation.TypeCriteriaException
		|| location.getLocType() == BDLocation.TypeNetWorkException
		|| location.getLocType() > 161) {
		getErrorMsg(location.getLocType());
	    } else{  
		Log.d(TAG,"--location latitude="+location.getLatitude()+" longitude="+location.getLongitude());
		String[] strArray = new String[8];
		strArray[0] = location.getAddrStr();
		strArray[1] = location.getCity();
		strArray[2] = location.getDistrict();
		strArray[3] = String.valueOf(location.getLatitude());
		strArray[4] = String.valueOf(location.getLocType());
		strArray[5] = String.valueOf(location.getLongitude());
		strArray[6] = location.getProvince();
		strArray[7] = location.getTime();
		sendResponse(strArray);	
	    }	    
	}
    }

    private void release() {
	mLocationClient.stop();
	mLocationClient.unRegisterLocationListener(mMyLocationListener);
    }
    private void getErrorMsg(int code) {
	if (code == BDLocation.TypeCriteriaException)
	    Log.e(TAG,"location error code :扫描整合定位依据失败");
	if (code == BDLocation.TypeNetWorkException)
	    Log.e(TAG,"location error code :网络异常，没有成功向服务器发起请求。");
	if (code > BDLocation.TypeNetWorkLocation
	    && code <= BDLocation.TypeServerError)
	    Log.e(TAG,"location error code :服务端定位失败");
	if (code > 500)
	    Log.e(TAG,"location error code :key异常");
	Toast.makeText(mContext,mContext.getResources().getString(R.string.weather_tip2),
			       Toast.LENGTH_LONG).show();
	sendResponse(null); 
    }


    private boolean checkNetworkState() {
	boolean flag = false;
	//得到网络连接信息
	ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	//去进行判断网络是否连接
	if (manager.getActiveNetworkInfo() != null) {
	    flag = manager.getActiveNetworkInfo().isAvailable();
	}
	return flag;
    }
}
