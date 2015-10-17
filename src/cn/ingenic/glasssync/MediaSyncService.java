package cn.ingenic.glasssync;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sctek.smartglasses.ui.MainActivity;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.PhotosSyncRunnable;
import com.sctek.smartglasses.utils.VideoSyncRunnable;
import com.sctek.smartglasses.utils.WifiUtils;

import cn.ingenic.glasssync.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.audiofx.AudioEffect.OnControlStatusChangeListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MediaSyncService extends Service{
	
	private final static String TAG = "MediaSyncService";
	private final static int MAX_SYNC_THREADS = 2;
	private ExecutorService mExecutorService;
	
	public final static int VIDEO_TASK_START = 1;
	public final static int VIDEO_PROGRESS_UPDATE = 2;
	public final static int VIDEO_ONE_DONE = 3;
	public final static int VIDEO_TASK_DONE = 4;
	
	public final static int PHOTO_TASK_START = 5;
	public final static int PHOTO_ONE_DONE = 6;
	public final static int PHOTO_TASK_DONE = 7;
	
	private final static int VIDOE_NOTIFICATION_ID = 121;
	private final static int PHOTO_NOTIFICATION_ID = 122;
	
	private final static String PHOTO_NOTIFICATION_ACTION = "PHOTO_NOTIFICATION_ACTION";
	private final static String VIDEO_NOTIFICATION_ACTION = "VIDEO_NOTIFICATION_ACTION";
	
	private NotificationManager mNotificationManager;
	private Notification mVideoNotification;
	private Notification mPhotoNotification;
	
	private Handler mHandler =new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Log.e(TAG, "---------------msg-------------" + msg.what);
			super.handleMessage(msg);
			switch (msg.what) {
				case VIDEO_TASK_START:
					onVideoTaskStart(0, msg.arg1);
					break;
				case VIDEO_PROGRESS_UPDATE:
					onVideoUpdateProgress(msg.arg1, msg.arg2, (Long)msg.obj);
					break;
				case VIDEO_ONE_DONE:
					onVideoOneDone(msg.arg1, msg.arg2, (String)msg.obj);
					break;
				case VIDEO_TASK_DONE:
					onVideoTaskDone(msg.arg1, msg.arg2);
					break;
				case PHOTO_TASK_START:
					onPhotoTaskStart(msg.arg1);
					break;
				case PHOTO_ONE_DONE:
					onPhotoUpdateCount(msg.arg1, msg.arg2, (String)msg.obj);
					break;
				case PHOTO_TASK_DONE:
					onPhotoTaskDone(msg.arg1, msg.arg2);
					break;
			}
			
		}
	};

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mExecutorService = Executors.newFixedThreadPool(MAX_SYNC_THREADS);
		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		IntentFilter filter = new IntentFilter(PHOTO_NOTIFICATION_ACTION);
		filter.addAction(VIDEO_NOTIFICATION_ACTION);
		registerReceiver(mReceiver, filter);
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void startPhotoSync(ArrayList<MediaData> photos) {
		PhotosSyncRunnable mPhotosSyncRunnable = PhotosSyncRunnable.getInstance();
		if(mPhotosSyncRunnable.isRunning()) {
			Toast.makeText(this, R.string.download_task_running, Toast.LENGTH_SHORT).show();
			return;
		}
		
		initPhotoNotification();
		mPhotosSyncRunnable.setHandler(mHandler);
		mPhotosSyncRunnable.setData(photos);
		
		mExecutorService.submit(mPhotosSyncRunnable);
	}
	
	public void startVideoSync(ArrayList<MediaData> videos) {
		VideoSyncRunnable mVideoSyncRunnable = VideoSyncRunnable.getInstance();
		if(mVideoSyncRunnable.isRunning()) {
			Toast.makeText(this, R.string.download_task_running, Toast.LENGTH_SHORT).show();
			return;
		}
		initVideoNotification();
		mVideoSyncRunnable.setHandler(mHandler);
		mVideoSyncRunnable.setData(videos);
		
		mExecutorService.submit(mVideoSyncRunnable);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroy");
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	private ServiceBinder mBinder = new ServiceBinder();
	
	public class ServiceBinder extends Binder {
		public MediaSyncService getService() {
			return MediaSyncService.this;
		}
	}
	
	private void onVideoTaskStart(int count, int total) {
		Log.e(TAG, "onProgressUpdateCount" + count + "/" + total);
		String speedMsg = String.format("%dKb/s", 0);
		mVideoNotification.contentView.setTextViewText(R.id.speed_tv, speedMsg);
		String temp = (String)getResources().getText(R.string.syncing_videos);
		String msg = String.format(temp, count, total);
		mVideoNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mVideoNotification.tickerText = getResources().getText(R.string.start_sync_videos);
		mNotificationManager.notify(VIDOE_NOTIFICATION_ID, mVideoNotification);
	}
	
	private void onVideoUpdateProgress(long progress, long total, long speed) {
		Log.e(TAG, "onProgressUpdateCount:" + progress + "/" + total);
		String speedMsg = String.format("%dKb/s", (int)speed/1000);
		mVideoNotification.contentView.setTextViewText(R.id.speed_tv, speedMsg);
		mVideoNotification.contentView.setProgressBar(R.id.donwload_progress, (int)total, (int)progress, false);
		mNotificationManager.notify(VIDOE_NOTIFICATION_ID, mVideoNotification);
	}
	
	private void onVideoOneDone(int count, int totalCount, String name) {
		Log.e(TAG, "onProgressUpdateCount + " + name);
		String temp = (String)getResources().getText(R.string.syncing_videos);
		String msg = String.format(temp, count, totalCount);
		String speed = String.format("%dKb/s", 0);
		mVideoNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mVideoNotification.contentView.setTextViewText(R.id.speed_tv, speed);
		mVideoNotification.contentView.setProgressBar(R.id.donwload_progress, 100, 0, false);
		mNotificationManager.notify(VIDOE_NOTIFICATION_ID, mVideoNotification);
		
		String photoPath = Environment.getExternalStorageDirectory().toString()
				+ "/SmartGlasses/videos/" + name;
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(new File(photoPath)));
		sendBroadcast(mediaScanIntent);
	}
	
	private void onVideoTaskDone(int doneCount, int failCount) {
		String temp = (String)getResources().getText(R.string.sync_media_done);
		String msg = String.format(temp, doneCount, failCount);
		mVideoNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mVideoNotification.contentView.setProgressBar(R.id.donwload_progress, 1, 1, false);
		mVideoNotification.contentView.setTextViewText(R.id.speed_tv, "");
		mVideoNotification.tickerText = getResources().getText(R.string.syncing_videos_done);
		mVideoNotification.vibrate = new long[]{0,100,200,300};
		mNotificationManager.notify(VIDOE_NOTIFICATION_ID, mVideoNotification);
		
		if(!VideoSyncRunnable.getInstance().isRunning()&&MainActivity.getInstance() == null) {
			turnWifiApOff();
		}
	}
	
	private void onPhotoTaskStart(int total) {
		String temp = (String)getResources().getText(R.string.syncing_photos);
		String msg = String.format(temp, 0, total);
		mPhotoNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mPhotoNotification.tickerText = getResources().getText(R.string.start_sync_photos);
		mNotificationManager.notify(PHOTO_NOTIFICATION_ID, mPhotoNotification);
	}
	
	private void onPhotoUpdateCount(int count, int total, String name) {
		String temp = (String)getResources().getText(R.string.syncing_photos);
		String msg = String.format(temp, count, total);
		mPhotoNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mNotificationManager.notify(PHOTO_NOTIFICATION_ID, mPhotoNotification);
		
		String photoPath = Environment.getExternalStorageDirectory().toString()
				+ "/SmartGlasses/photos/" + name;
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(new File(photoPath)));
		sendBroadcast(mediaScanIntent);
	}
	
	private void onPhotoTaskDone(int doneCount, int failCount) {
		String temp = (String)getResources().getText(R.string.sync_media_done);
		String msg = String.format(temp, doneCount, failCount);
		mPhotoNotification.contentView.setTextViewText(R.id.download_lable_tv, msg);
		mPhotoNotification.contentView.setProgressBar(R.id.donwload_progress, 1, 1, false);
		mPhotoNotification.vibrate = new long[]{0,100,200,300};
		mPhotoNotification.tickerText = getResources().getText(R.string.syncing_photos_done);
		mNotificationManager.notify(PHOTO_NOTIFICATION_ID, mPhotoNotification);
		
		if(!PhotosSyncRunnable.getInstance().isRunning()&&MainActivity.getInstance() == null) {
			turnWifiApOff();
		}
	}
	
	private void initVideoNotification() {
		
		mVideoNotification = new Notification();
		
		RemoteViews videoView = new RemoteViews(getPackageName(), R.layout.notification_view);
		
		mVideoNotification.contentView = videoView;
		mVideoNotification.icon = R.drawable.ic_download;
		mVideoNotification.contentView.setProgressBar(R.id.donwload_progress, 100, 0, false);
		
		Intent videoIntent = new Intent(VIDEO_NOTIFICATION_ACTION);
		PendingIntent vidoePendingIntent = PendingIntent.getBroadcast(this, 0, videoIntent, PendingIntent.FLAG_ONE_SHOT);
		mVideoNotification.contentView.setOnClickPendingIntent(R.id.cancel_bt, vidoePendingIntent);
		
	}
	
private void initPhotoNotification() {
		
		mPhotoNotification = new Notification();
		
		RemoteViews photoView = new RemoteViews(getPackageName(), R.layout.notification_view);
		
		mPhotoNotification.contentView = photoView;
		mPhotoNotification.icon = R.drawable.ic_download;
		mPhotoNotification.contentView.setProgressBar(R.id.donwload_progress, 100, 100, true);
		mPhotoNotification.contentView.setTextViewText(R.id.speed_tv, "");
		
		Intent photoIntent = new Intent(PHOTO_NOTIFICATION_ACTION);
		PendingIntent photoPendingIntent = PendingIntent.getBroadcast(this, 0, photoIntent, PendingIntent.FLAG_ONE_SHOT);
		mPhotoNotification.contentView.setOnClickPendingIntent(R.id.cancel_bt, photoPendingIntent);
		
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onReceive:" + intent.getAction());
			if(VIDEO_NOTIFICATION_ACTION.equals(intent.getAction())) {
				VideoSyncRunnable.getInstance().cancel();
				mHandler.removeMessages(VIDEO_TASK_START);
				mHandler.removeMessages(VIDEO_ONE_DONE);
				mHandler.removeMessages(VIDEO_PROGRESS_UPDATE);
				mHandler.removeMessages(VIDEO_TASK_DONE);
				
				mNotificationManager.cancel(VIDOE_NOTIFICATION_ID);
			}
			else if(PHOTO_NOTIFICATION_ACTION.endsWith(intent.getAction())) {
				PhotosSyncRunnable.getInstance().cancel();
				mHandler.removeMessages(PHOTO_ONE_DONE);
				mHandler.removeMessages(PHOTO_TASK_START);
				mHandler.removeMessages(PHOTO_TASK_DONE);
				mNotificationManager.cancel(PHOTO_NOTIFICATION_ID);
			}
		}
	};
	
	private void turnWifiApOff() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				// TODO Auto-generated method stub
				WifiManager wifimanager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
				WifiUtils.setWifiApEnabled(false, wifimanager);
				return null;
			}
		}.execute();
	}

}
