package cn.ingenic.glasssync.updater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.IRemoteBinder;
import cn.ingenic.glasssync.Module;
/** 
 * check update ,etc . 
 * @author kli , dfdun  */
public class UpdateManager {
	private MyLog klilog = new MyLog(this.getClass());
	public static final int SYNC_SUCCESS = 1;
	public static final int SYNC_FAIL = 2;

	public static final int CHECK_FAILED = 1;
	public static final int CHECK_NO_UPDATE = 2;
	public static final int CHECK_HAS_UPDATE = 3;
	public static final int CHECK_NO_ROLLBACK = 4;
	public static final int CHECK_HAS_ROLLBACK = 5;
	
	private static UpdateManager sInstance = null;
	private Context mContext;
	private List<UpdateInfo> mUpdateList;
	private List<String> mVersionList;
    private String mCurrentVersion , mDeviceName;
	private Comparator mComparator = new Comparator() {
		@Override
		public int compare(Object l, Object r) {
			// if l > r , swap them
			return mVersionList.indexOf(l) - mVersionList.indexOf(r);
		}
	};

    
	private UpdateManager(Context context){
		mContext = context;
	}
	
	public static UpdateManager getInstance(Context context){
		if(sInstance == null){
			sInstance = new UpdateManager(context);
		}
		return sInstance;
	}
	
	public void sync(Message callback){
		new SyncDataTask(callback).execute("");
	}

	/** get available version list ,base on current version*/
	public List<String> getVersionListBaseCurrent(){
		List<String> list = new ArrayList<String>();
		String currentVersion = mCurrentVersion;
		list.add(currentVersion);
		for(UpdateInfo info:mUpdateList){
			if(currentVersion.equals(info.version_from)){
				list.add(info.version_to);
			}
		}
		Collections.sort(list, mComparator);
		return list;
	}
	
	public UpdateInfo getUpdateInfoTo(String version){
		String currentVersion = mCurrentVersion;
		UpdateInfo update_info = null;
		for(UpdateInfo info:mUpdateList){
			if(info.version_from.equals(currentVersion) &&
					info.version_to.equals(version)){
				update_info = info;
				break;
			}
		}
		return update_info;
	}
	
	public String getCurrentVersion(){
	    // not use cache info, get latest info every time
        mCurrentVersion = getFromeBluetooth("currentVersion");
        return mCurrentVersion;
	}
	
	public String getModel(){
	    // not use cache info, get latest info every time
        mDeviceName=getFromeBluetooth("model");
	    return mDeviceName;
	}
	
    private String getFromeBluetooth(String key) {
        klilog.i("getFromBT, key="+key);
        DefaultSyncManager manager = DefaultSyncManager.getDefault();
        Module m = manager.getModule(UpdaterModule.UPDATER);
        IRemoteBinder binder = m
                .getRemoteService(IUpdaterRemoteService.DESPRITOR);
        IUpdaterRemoteService s = UpdaterRemoteServiceImpl
                .asRemoteInterface(binder);
        if(s == null)
            return "Unknown";
        String result=s.get(key);
        klilog.i("getFromBT, result is "+result);
        if (result != null)
            return result;
        else
            return "unknown";
    }

	private class SyncDataTask extends AsyncTask {
		
		private Message callback;

		public SyncDataTask(Message msg){
			callback = msg;
		}
		
		@Override
		protected Object doInBackground(Object... arg0) {
			String url = getUpdateUrl();
			if(url == null){
				return SYNC_FAIL;
			}else if(url == ""){
				mUpdateList = new ArrayList<UpdateInfo>();
				return SYNC_SUCCESS;
			}else{
				return SyncData(url);
			}
		}

		@Override
		protected void onPostExecute(Object result) {
			callback.arg1 = (Integer) result;
			callback.sendToTarget();
		}

		private String getUpdateUrl(){
			String url = null;
			HttpGet getMethod = new HttpGet(UpdateUtils.URL_PRODUCTS_UPDATE);
			HttpClient httpClient = new DefaultHttpClient();
			List<ProductInfo> productList = new ArrayList<ProductInfo>();
			try {
				HttpResponse response = httpClient.execute(getMethod);
				String result = EntityUtils.toString(response.getEntity(),
						UpdateUtils.ENCODE);
				if (200 == response.getStatusLine().getStatusCode()) {
					productList = ProductInfoHelper.getInstance().getProductList(result);
                    String this_model = mDeviceName.toLowerCase();
					for(ProductInfo info : productList){
						String model = info.model.toLowerCase(Locale.getDefault()).trim();
						if(this_model.equals(model)){
							url = info.url;
							return url;
						}
					}
					klilog.w("Products not in products list! : "+this_model);
					return "";
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null; 
		}
		
		private int SyncData(String syncUrl) {
			int res = SYNC_SUCCESS;
			HttpGet getMethod = new HttpGet(syncUrl);
			HttpClient httpClient = new DefaultHttpClient();
			try {
				HttpResponse response = httpClient.execute(getMethod);
				String result = EntityUtils.toString(response.getEntity(),
						UpdateUtils.ENCODE);
				int code = response.getStatusLine().getStatusCode();
				if (200 == code) { // get code of httpresponse
					UpdateInfoHelper helper = new UpdateInfoHelper(result);
					mVersionList = helper.getVersionList();
					mUpdateList = helper.getUpdateList();
				}else{
					klilog.e("Http response error, code:" + code);
					res = SYNC_FAIL;
				}
			} catch (ClientProtocolException e) {
				res = SYNC_FAIL;
				e.printStackTrace();
			} catch (IOException e) {
				res = SYNC_FAIL;
				e.printStackTrace();
			}
			return res;
		}
	}
}
