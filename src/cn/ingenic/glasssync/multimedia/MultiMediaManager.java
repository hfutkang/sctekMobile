package cn.ingenic.glasssync.multimedia;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.os.Message;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import cn.ingenic.glasssync.multimedia.MultiMediaModule;

public class MultiMediaManager {
    private static final String TAG = "MultiMediaManager";
    private static final Boolean DEBUG = true;
    private Context mContext;
    private static MultiMediaManager sInstance;
    private MultiMediaReceiver nReceiver;

    private MultiMediaManager(Context context){
	Log.e(TAG, "MultiMediaManager");
	mContext = context;

	init_receiver(context);
	MultiMediaModule m = MultiMediaModule.getInstance(mContext);
    }

    public static MultiMediaManager getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new MultiMediaManager(c);
	return sInstance;
    }

    private void init_receiver(Context c){
        nReceiver = new MultiMediaReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("cn.ingenic.glasssync.mulmedia.REQUEST_MULMEDIA");
        c.registerReceiver(nReceiver,filter);
    }

    class MultiMediaReceiver extends BroadcastReceiver{
	private String TAG = "MultiMediaReceiver";
	
        @Override
	public void onReceive(Context context, Intent intent) {
	    //Log.e(TAG, "onReceive " + intent.getAction());
	    if (intent.getAction().equals("cn.ingenic.glasssync.mulmedia.REQUEST_MULMEDIA")){
		if(DEBUG) Log.e(TAG, "REQUEST_MULMEDIA");
		MultiMediaModule m = MultiMediaModule.getInstance(mContext);
		String fileName = intent.getStringExtra("file_name");
		int fileType = intent.getIntExtra("file_type",MultiMediaModule.GSMMD_NONE);
		if(DEBUG) Log.e(TAG, "---fileName="+fileName+" fileType="+fileType);
		if(fileName != null){
		    m.mul_request_single_file(fileName,fileType);
		}else{
		    m.mul_request();
		}
	    }
        }
    }

    private Handler mHandler = new Handler() {  
	    public void handleMessage(Message msg) {  
		switch (msg.what) {  
		default:  
		    break;  
		}  
	    }  
	}; 
}