package cn.ingenic.glasssync.weather;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import cn.ingenic.glasssync.R;
public class HTTPGetModule extends SyncModule {
    private static final String TAG = "HTTPGetModule";

    private static final String HTTPGET_CMD = "httpget_cmd"; //The length of not more than 15

    private static HTTPGetModule sInstance;
    private static final int NOTIFY_ERROR = 1;
    private static final int NOTIFY_SUCCESS = 2;
    
    private Context mContext;
    
    private Handler mHandler = new WeatherHandler();
    protected class WeatherHandler extends Handler {
	@Override  
	    public void handleMessage(Message msg) {  
	    switch (msg.what) {
	    case NOTIFY_ERROR:
		Toast.makeText(mContext,mContext.getResources().getString(R.string.weather_tip2),
			       Toast.LENGTH_LONG).show();
		break;
	    default:  		    
		break;  
	    }  
	}
    }
    private HTTPGetModule(Context context){
	super(TAG, context);
	mContext = context;
    }

    public static HTTPGetModule getInstance(Context c) {
	if (null == sInstance){
	    sInstance = new HTTPGetModule(c);
	}
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    public void sendHTTPResponse(String response) {
	if(PositionModule.VDBG) Log.e(TAG, "---sendHTTPResponse");
	SyncData data = new SyncData();
	data.putString(HTTPGET_CMD, response);
	
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "---send file sync failed:" + e);
	}
    }

    @Override
	protected void onRetrive(SyncData data) {
	if(PositionModule.VDBG) Log.e(TAG, "---onRetrive");
	String url = data.getString(HTTPGET_CMD);
	requestInternet(url);
    }

    private void requestInternet(final String url){
	if(PositionModule.VDBG) Log.e(TAG, "--Internet request: " + url);
	new Thread(){
	    @Override
		public void run() {
		super.run();
		HttpUriRequest req = new HttpGet(url);
		String result = null;
		HttpClient mHttpClient = new DefaultHttpClient();
		try {
		    HttpResponse response = mHttpClient.execute(req);
		    if(response.getStatusLine().getStatusCode() == 200){
			result = EntityUtils.toString(response.getEntity(), "UTF-8");
			sendHTTPResponse(result);	
		    }else{
			Message m = mHandler.obtainMessage(NOTIFY_ERROR);
			mHandler.sendMessageDelayed(m,0);
		    }
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		} catch (ParseException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		    Message m = mHandler.obtainMessage(NOTIFY_ERROR);
		    mHandler.sendMessageDelayed(m,0);
		}
	    }	
	}.start();
    }

    protected void onFileSendComplete(String fileName, boolean success) {
	if(PositionModule.VDBG) Log.e(TAG, "---onFileSendComplete:" + fileName+" success="+success);
    }
}
