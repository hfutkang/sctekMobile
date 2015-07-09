package cn.ingenic.glasssync.share;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.os.Environment;
import android.util.Log;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import cn.ingenic.glasssync.R;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXAppExtendObject;
import com.tencent.mm.sdk.openapi.WXEmojiObject;
import com.tencent.mm.sdk.openapi.WXImageObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXMusicObject;
import com.tencent.mm.sdk.openapi.WXTextObject;
import com.tencent.mm.sdk.openapi.WXVideoObject;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tencent.mm.sdk.openapi.SendAuth;

import cn.ingenic.glasssync.multimedia.MultiMediaModule;
import cn.ingenic.glasssync.wxapi.WXEntryActivity;

public class ShareModule extends SyncModule {
    private static final String TAG = "ShareModule";
    private boolean DEBUG = true;

    public static final String SHARE_TYPE_IMG = "img";
    public static final String SHARE_TYPE_VIDEO = "video";
    private Context mContext;
    private MultiMediaReceiver nReceiver;
    private IWXAPI api;
    private Boolean mToastVisble = false;
    private ProgressDialog myDialog;
    private String mFileName;
    private static ShareModule mShareModule;
    public static ShareModule getInstance(Context c){
	if(mShareModule == null)
	    mShareModule = new ShareModule(c);
	return mShareModule;
    }
    public ShareModule(Context context){
	super(TAG, context);
	mContext = context;
    }

    @Override
    protected void onCreate() {
	if(DEBUG) Log.e(TAG, "onCreate in");

	init_receiver(mContext);
    }

    private void init_receiver(Context c){
        nReceiver = new MultiMediaReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("cn.ingenic.glasssync.share.REQUEST_FILE_OK");
        c.registerReceiver(nReceiver,filter);
    }

    class MultiMediaReceiver extends BroadcastReceiver{
    	private String TAG = "MultiMediaReceiver";
	
        @Override
    	public void onReceive(Context context, Intent intent) {
    	    if(DEBUG) Log.e(TAG, "--onReceive " + intent.getAction());
    	    if (intent.getAction().equals("cn.ingenic.glasssync.share.REQUEST_FILE_OK")){
		if(mFileName == null)
		    return;

    		unloadToast();
    		String path = get_file_path(mFileName,MultiMediaModule.GSMMD_PIC);
    		mFileName = null;
    		shareToWeixin(path);
    	    }
        }
    }

    @Override
    protected void onRetrive(SyncData data) {
	if(DEBUG) Log.e(TAG, "---onRetrive");
	if(mFileName !=null){
	    Log.i(TAG, "---existing a share-file("+ mFileName+")now. try again later!");
	    return;
	}
	String name = data.getString(SHARE_TYPE_IMG);
	String path = check_file_exist(name,MultiMediaModule.GSMMD_PIC);
	if(path == null){
	      //open mulmedia sync
	    Intent i = new Intent("cn.ingenic.glasssync.mulmedia.REQUEST_MULMEDIA");
	    i.putExtra("file_name",name); 
	    i.putExtra("file_type",MultiMediaModule.GSMMD_PIC); 
	    mContext.sendBroadcast(i);
	    loadToast();
	    mFileName = name;

            new Thread(new Runnable() {
                @Override
                public void run() {
		    try {
			Thread.sleep(10000); //10s
		    } catch (Exception ex) {
		    }
			unloadToast();
                }
            }).start();

	    return;
	}
	shareToWeixin(path);
    }

    private void shareToWeixin(String path){
	  Intent intent = new Intent();
	  intent.setAction("android.intent.action.VIEW");
	  intent.setData(Uri.parse("sdksample://www"));
	  intent.putExtra("fromGlassSync",true); 
	intent.putExtra("path",path); 
	intent.putExtra("type",MultiMediaModule.GSMMD_PIC); 
	 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
	
	mContext.startActivity(intent);
    }
    private String get_file_path(String name, int type){
	String dirpath;
	if (type == MultiMediaModule.GSMMD_PIC){
	    dirpath = "/IGlass/Pictures/";
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    dirpath = "/IGlass/Video/";
	}else{
	    dirpath = "/IGlass/data/";
	}
	return Environment.getExternalStorageDirectory() + dirpath + name;
    }
    private String check_file_exist(String name, int type){
	String dirpath;
	if (type == MultiMediaModule.GSMMD_PIC){
	    dirpath = "/IGlass/Pictures/";
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    dirpath = "/IGlass/Video/";
	}else{
	    dirpath = "/IGlass/data/";
	}

	File f = new File(Environment.getExternalStorageDirectory() + dirpath + name);
	if(DEBUG) Log.e(TAG, "----File " + f.getPath());
	if (f.exists()){
	    return f.getPath();
	}
	if(DEBUG) Log.e(TAG, "-----------GSMMD_NOEXIST");
	return null;
    }

    private void loadToast(){
	if(DEBUG) Log.e(TAG, "-----------loadToast");
	myDialog = new ProgressDialog(mContext);
	myDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT); 
        myDialog.setTitle("Glass图片分享");
        myDialog.setMessage("同步中...");
        myDialog.setIndeterminate(true);
        myDialog.setCancelable(false);
        myDialog.show();
    }

    private void unloadToast(){
	if(myDialog !=null){
	    myDialog.dismiss();
	    myDialog = null;
	}
    }
}