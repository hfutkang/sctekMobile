package cn.ingenic.glasssync;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.PhotosSyncRunnable;
import com.sctek.smartglasses.utils.VideoSyncRunnable;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

public class MediaSyncService extends Service{
	
	private final static String TAG = "MediaSyncService";
	private final static int MAX_SYNC_THREADS = 2;
	private ExecutorService mExecutorService;
	
	private final static int PHOTO_SYNC_ACTION = 1;
	private final static int VIDEO_SYNC_ACTION = 2;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mExecutorService = Executors.newFixedThreadPool(MAX_SYNC_THREADS);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void startPhotoSync(ArrayList<MediaData> photos) {
		PhotosSyncRunnable mPhotosSyncRunnable = new PhotosSyncRunnable(photos, this);
		mExecutorService.submit(mPhotosSyncRunnable);
	}
	
	public void startVideoSync(ArrayList<MediaData> videos) {
		VideoSyncRunnable mVideoSyncRunnable = new VideoSyncRunnable(videos, this);
		mExecutorService.submit(mVideoSyncRunnable);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
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

}
