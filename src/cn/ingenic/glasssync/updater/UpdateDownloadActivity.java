package cn.ingenic.glasssync.updater;

import java.io.File;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import cn.ingenic.glasssync.R;

/**
 * a dialog window , to notice user whether start to download the file of update.
 * @author kli & dfdun */
public class UpdateDownloadActivity extends Activity implements OnClickListener {
	public final static int MODE_UPDATE = 1;
	public final static int MODE_ROLLBACK = 2;
	public final static String EXTRAS_MODE = "mode";
	public final static String EXTRAS_VERSION = "version";
	
	UpdateManager mManager;
	UpdateInfo mInfo;
	MyLog klilog = new MyLog(UpdateDownloadActivity.class);
	static boolean mIsDownloading=false; //  whether there is a download task is running
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mManager = UpdateManager.getInstance(this);
	    setContentView(R.layout.activity_update_download);
	    
	    Bundle bundle = getIntent().getExtras();
	    if(bundle == null){
	    	return;
	    }
	    int mode = bundle.getInt(EXTRAS_MODE, 0);
	    mInfo = (UpdateInfo)bundle.getParcelable(EXTRAS_VERSION);
	    
	    if(mode == 0 || mInfo == null){
	    	return;
	    }
	    klilog.i(mInfo.toString());
	    setupMode(mode);
	    StringBuilder sb = new StringBuilder();
	    sb.append(getString(R.string.version)+mInfo.version_to);
	    sb.append("\n");
	    sb.append(getString(R.string.size, mInfo.size));
	    sb.append("\n");
	    sb.append(getString(R.string.description));
	    sb.append("\n");
	    sb.append(mInfo.description.replace("\\n", "\n"));
	    ((TextView)findViewById(R.id.description)).setText(sb);
	    findViewById(R.id.btn_later).setOnClickListener(this);
	    findViewById(R.id.btn_ok).setOnClickListener(this);
	}
	
	private void setupMode(int mode){
		
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.btn_later:
			finish();
			break;
		case R.id.btn_ok:
            boolean SD_enable = Environment.getExternalStorageState()
                    .equals(Environment.MEDIA_MOUNTED);
			if(networkEnable() && SD_enable){
				download(mInfo.url);
			}else{
			    if(SD_enable)
				Toast.makeText(this, R.string.network_not_avaliable, Toast.LENGTH_SHORT).show();
			    else
			        Toast.makeText(this, R.string.sdcard_not_avaliable, Toast.LENGTH_SHORT).show();
			}
			finish();
			break;
		}
	}
	
	private boolean networkEnable(){
		boolean anyDate = ((RadioButton)findViewById(R.id.any_data)).isChecked();
		boolean onlyWifi = ((RadioButton)findViewById(R.id.wifi_only)).isChecked();
		boolean res = false;
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null) {
			int nType = networkInfo.getType();
			if(anyDate){
				res = true;
			}else if(onlyWifi){
				res = nType == ConnectivityManager.TYPE_WIFI ? true : false;
			}
		}
		return res;
	}
	
	private void download(String url){
	   if(mIsDownloading){
		   Toast.makeText(this, R.string.downloading, Toast.LENGTH_LONG).show();
		   return;
	   }
	   mIsDownloading = true; // set downloading true.
	   Toast.makeText(this, R.string.downloading, Toast.LENGTH_LONG).show();
        DownloadManager dm=((DownloadManager)getSystemService("download"));
        Uri uri = Uri.parse(url);
        Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).mkdirs();
        Request dwreq = new DownloadManager.Request(uri);
        dwreq.setTitle(getString(R.string.download_title));
        dwreq.setDescription(getString(R.string.download_description));

        String filename = ""; // :TODO  now it is flash dir
        filename = "../"+UpdateUtils.FILENAME_SAVED;
        File f = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/"+filename);
        android.util.Log.i("dfdun", "file dir: "+f.getAbsolutePath());
        if (f.exists()) {
            f.delete();
        }
        dwreq.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                filename);
        if (android.os.Build.VERSION.SDK_INT >= 11)
        dwreq.setNotificationVisibility(Request.VISIBILITY_VISIBLE);//Since API 11
        
        long id = dm.enqueue(dwreq);
        UpdateUtils.putDownloadInfo(this, id, mInfo);
    }

}
