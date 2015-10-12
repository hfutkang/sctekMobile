package cn.ingenic.glasssync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.sctek.smartglasses.ui.MainActivity;
import com.sctek.smartglasses.utils.CustomHttpClient;

import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class LocationReportService extends Service {
	
	private final static String TAG = "LocationReportService";
	private LocationManager mLocationManager;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		reportBSLocation();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void reportBSLocation() {
		
		new Thread(new Runnable() {
			
			private final static String REPORT_GPS_URL = "http://www.wear0309.com/location";
			
			@Override
			public void run() {
				
				int mmc;
				int mnc;
				int lac;
				int cid;
				String serial = PreferenceManager.getDefaultSharedPreferences(LocationReportService.this).getString("serial", "000000000");
				
				TelephonyManager mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
				String operator = mTelephonyManager.getNetworkOperator();
				
				CellLocation cellLocation = mTelephonyManager.getCellLocation();
				
				Log.e(TAG, "NetworkOperator:" + operator);
				mmc = Integer.parseInt(operator.substring(0, 3));
				mnc = Integer.parseInt(operator.substring(3));
				
				if(mnc == 2) {
					lac = ((CdmaCellLocation)cellLocation).getNetworkId();
					cid = ((CdmaCellLocation)cellLocation).getBaseStationId();
				}
				else {
					lac = ((GsmCellLocation)cellLocation).getLac();
					cid = ((GsmCellLocation)cellLocation).getCid();
				}
				
				Log.e(TAG, "mmc:" + mmc + " mnc:" + mnc + " lac:" + lac + " cid:" + cid); 
				
				HttpClient client = CustomHttpClient.getHttpClient();
				HttpPost httpPost = new HttpPost(REPORT_GPS_URL);
				
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("serial", serial));
				params.add(new BasicNameValuePair("mmc", String.valueOf(mmc)));
				params.add(new BasicNameValuePair("mnc", String.valueOf(mnc)));
				params.add(new BasicNameValuePair("lac", String.valueOf(lac)));
				params.add(new BasicNameValuePair("cid", String.valueOf(cid)));
				
				try {
					httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					client.execute(httpPost);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
}
