package com.sctek.smartglasses.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import cn.ingenic.glasssync.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.RemoteViews;

public class VideoSyncRunnable implements Runnable {
	
	private final static String TAG = "VideoSyncRunnable";
	public static final String VIDEO_DOWNLOAD_FOLDER = 
			Environment.getExternalStorageDirectory().toString()	+ "/SmartGlasses/vedios/";
	
	private ArrayList<MediaData> mVideos;
	private Context mContext;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private GlassImageDownloader mDownloader;
	private int doneCount = 0;
	private int totalCount = 0;
	
	private boolean canceled = false;
	
	public VideoSyncRunnable(ArrayList<MediaData> videos, Context context) {
		mVideos = videos;
		mContext = context;
		
		mNotificationManager = (NotificationManager)mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
		mNotification = new Notification();
		mDownloader = new GlassImageDownloader();
		totalCount = videos.size();
		
		mNotification.contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_view);
		mNotification.icon = R.drawable.hanlang_icon;
		mNotification.contentView.setProgressBar(R.id.donwload_progress, 100, 100, true);
		
		long timeLable = System.currentTimeMillis();
		Log.e(TAG, "" + timeLable);
		Intent intent = new Intent("" + timeLable);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		mNotification.contentView.setOnClickPendingIntent(R.id.cancel_bt, pendingIntent);
		
		IntentFilter filter = new IntentFilter("" + timeLable);
		mContext.registerReceiver(mReceiver, filter);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		onProgressUpdate(0);
		for(MediaData data : mVideos) {
			
			long startPostion = 0;
			HttpURLConnection conn = null;
			File file = new File(VIDEO_DOWNLOAD_FOLDER, data.name);
			if(file.exists()) {
				conn = mDownloader.createConnection(data.url, 0);
				long videoLeng = conn.getContentLength();
				if(file.length() < videoLeng) {
					startPostion = file.length();
					conn.disconnect();
				}
				else {
					doneCount++;
					onProgressUpdate(doneCount);
					continue;
				}
			}
			
			File dir = new File(VIDEO_DOWNLOAD_FOLDER);
			if(!dir.exists())
				dir.mkdirs();
			
			try {
				InputStream in = mDownloader.getInputStream(data.url, startPostion);
				
				FileOutputStream os = new FileOutputStream(file, true);
				byte[] buffer = new byte[4096];
				int len = 0;
				while((len = in.read(buffer)) != -1 && !canceled) {
					Log.e(TAG, "" + len);
					os.write(buffer, 0, len);
				}
				
				if(canceled)
					return;
				
				os.close();
				in.close();
				doneCount++;
				onProgressUpdate(doneCount, data.name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String msg = String.format("同步完成(%d/%d)...", doneCount, totalCount);
		mNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mNotification.vibrate = new long[]{0,100,200,300};
		mNotificationManager.notify(1, mNotification); 
		
	}
	
	private void onProgressUpdate(int progress, String name) {

		String msg = String.format("视频同步中(%d/%d)...", progress, totalCount);
		mNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mNotificationManager.notify(1, mNotification);
		
		String photoPath = Environment.getExternalStorageDirectory().toString()
				+ "/SmartGlasses/vedios/" + name;
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(new File(photoPath)));
		mContext.getApplicationContext().sendBroadcast(mediaScanIntent);
	}
	
	private void onProgressUpdate(int progress) {

		String msg = String.format("downloading(%d/%d)...", progress, totalCount);
		mNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mNotificationManager.notify(1, mNotification);
		
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onReceive:" + intent.getAction());
			canceled = true;
			mNotificationManager.cancel(1);
		}
	};
	
}
