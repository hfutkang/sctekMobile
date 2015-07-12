package com.sctek.smartglasses.fragments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import cn.ingenic.glasssync.MediaSyncService;
import cn.ingenic.glasssync.R;

import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.fragments.BaseFragment.GetRemoteMediaUrlTask;
import com.sctek.smartglasses.fragments.BaseFragment.SetWifiAPTask;
import com.sctek.smartglasses.ui.PhotoActivity;
import com.sctek.smartglasses.ui.VideoActivity;
import com.sctek.smartglasses.utils.CustomHttpClient;
import com.sctek.smartglasses.utils.GetRemoteVideoThumbWorks;
import com.sctek.smartglasses.utils.GlassImageDownloader;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.WifiUtils.WifiCipherType;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnKeyListener;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class RemoteVideoGridFragment extends BaseFragment {
	
	public static final int FRAGMENT_INDEX = 4;
	private static final String TAG = RemoteVideoGridFragment.class.getName();
	
	private static final int VEDIO_NOTIFICATION_ID = 1;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		
		mediaList = new ArrayList<MediaData>();
		preApState =  WifiUtils.getWifiAPState(mWifiManager);
		mWifiATask = new SetWifiAPTask(true, false);
		
		setHasOptionsMenu(true);
		getActivity().setTitle(R.string.remote_video);
		
		IntentFilter filter = new IntentFilter(WIFI_AP_STATE_CHANGED_ACTION);
		mContext.registerReceiver(mApStateBroadcastReceiver,filter);
		
		initProgressDialog();
		
		if(preApState == WIFI_AP_STATE_ENABLED) 
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(WifiUtils.needTurnWifiApOff(getActivity())) {
						mWifiATask.execute(true);
					}
					else {
						sendApInfoToGlass();
					}
				}
			}, 0);
		
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onStart");
		super.onStart();
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onResume");
		super.onResume();
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onPause");
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroy");
		mContext.unregisterReceiver(mApStateBroadcastReceiver);
		GetRemoteVideoThumbWorks.getInstance().stop();
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroyView");
		
		super.onDestroyView();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDetach");
		super.onDetach();
	}

	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.e(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.remote_video_fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.e(TAG, "onOptionsItemSelected");
		switch (item.getItemId()) {
			case R.id.download_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.download);
				deleteTv.setOnClickListener(onPhotoDownloadClickListener);
				return true;
			case R.id.remote_video_delete_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.delete);
				deleteTv.setOnClickListener(onRemoteVedioDeleteClickListener);
				return true;
			default:
				return true;
		}
	}
	
//	private void getImagePath() {
//		
//		imageUrls = new String[]{"http://192.168.5.253/pub/sct/tracker/VID_20150130_180836.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video/VID_20150219_000020.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video/VID_20150219_000231.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video/VID_20150219_000400.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video/VID_20150223_180915.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video1/VID_20150219_000020.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video1/VID_20150219_000231.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video1/VID_20150219_000400.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video1/VID_20150223_180915.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video2/VID_20150219_000020.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video2/VID_20150219_000231.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video2/VID_20150219_000400.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video2/VID_20150223_180915.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video3/VID_20150219_000231.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video3/VID_20150219_000400.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video3/VID_20150223_180915.mp4",
//				"http://192.168.5.253/pub/sct/tracker/Video3/VID_20150219_000020.mp4"};
//		
//		imagesName = new String[imageUrls.length];
//		for(int i = 0; i<imagesName.length; i++)
//			imagesName[i] = "remote image test";
//		
//		
//	
//	}
	
//	thumbWork.getRemoteVideoThumb(imageUrls[position], new GetRemoteVideoThumbListener() {
//		
//		@Override
//		public void onGetRemoteVideoThumbDone(Bitmap bitmap) {
//			// TODO Auto-generated method stub
//			Log.e(TAG, "5");
//			holder.imageView.setImageBitmap(bitmap);
//		}
//	});
	
	@SuppressLint("NewApi")
	public void onVideoDownloadTvClicked() {
		
//		new VideoDownloadTask().execute();
		getActivity().startService(new Intent(getActivity(), MediaSyncService.class));
		ArrayList<MediaData> data = (ArrayList<MediaData>)selectedMedias.clone();
		((VideoActivity)getActivity()).startVideoSync(data);
	}
	
	private OnClickListener onPhotoDownloadClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(selectedMedias.size() != 0)
				onVideoDownloadTvClicked();
			
			disCheckMedia();
			onCancelTvClicked();
		}
	};
	
	private BroadcastReceiver mApStateBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				int cstate = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1);
				Log.e(TAG, WIFI_AP_STATE_CHANGED_ACTION + ":" + cstate);
				if(cstate == WIFI_AP_STATE_ENABLED
						&& preApState != WIFI_AP_STATE_ENABLED) {
					
					BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
					if(!adapter.isEnabled()) {
						adapter.enable();
					}
					sendApInfoToGlass();
				}
				preApState = cstate;
			}
		}
	};
	
	private OnClickListener onRemoteVedioDeleteClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(selectedMedias.size() != 0) {
				new RemoteVedioDeleteTask().execute();
			}
			else {
				disCheckMedia();
			}
			onCancelTvClicked();
		}
	};
	
	private OnItemClickListener onVideoImageClickedListener = new OnItemClickListener() {

		@SuppressLint("NewApi")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
//			Uri uri = Uri.parse(imageUrls[position]);
//			Uri uri = Uri.parse("http://192.168.5.253/pub/sct/tracker/VID_20150130_180836.mp4");
			Intent intent = new Intent(Intent.ACTION_VIEW	);
//			intent.setData(uri);
			startActivity(intent);
		}
	};
	
private class VideoDownloadTask extends AsyncTask<String, Integer, Void> {
		
		private ProgressDialog progressDialog;
		private int totalcount;
		private int downloadcount;
		private GlassImageDownloader imageDownloader;
		
		private NotificationManager notificationManager;
		private Notification notification;
		
		@SuppressLint("NewApi")
		public VideoDownloadTask() {
			
			progressDialog = new ProgressDialog(getActivity());
			totalcount = selectedMedias.size();
			downloadcount = 0;
			
			imageDownloader = new GlassImageDownloader();
			
			notificationManager =  (NotificationManager)(getActivity().getSystemService(mContext.NOTIFICATION_SERVICE));
			notification = new Notification();
			
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			
			String msg = String.format("视频同步中(%d/%d)...", downloadcount, totalcount);
			
			notification.contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_view);
			notification.icon = R.drawable.glass;
			notification.contentView.setProgressBar(R.id.donwload_progress, 100, 100, true);
			notification.contentView.setTextViewText(R.id.download_lable_tv, msg);
			notificationManager.notify(VEDIO_NOTIFICATION_ID, notification);
			
			
			progressDialog.setMessage(msg);
			progressDialog.show();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			progressDialog.dismiss();
			
			String msg = String.format("同步完成(%d/%d)", downloadcount, totalcount);
			notification.contentView.setTextViewText(R.id.download_lable_tv, msg);
			notification.vibrate = new long[]{0,100,200,300}; 
			notificationManager.notify(VEDIO_NOTIFICATION_ID, notification);
			
			if(downloadcount != 0) {
				refreshGallery("vedios");
			}
			disCheckMedia();
//			selectedMedias.clear();
			super.onPostExecute(result);
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			String msg = String.format("视频同步中(%d/%d)...", downloadcount, totalcount);
			notification.contentView.setTextViewText(R.id.download_lable_tv, msg); 
			notificationManager.notify(VEDIO_NOTIFICATION_ID, notification);
			progressDialog.setMessage(msg);
		}

		@Override
		protected Void doInBackground(String... params) {
			// TODO Auto-generated method stub
			Log.e(TAG, "PhotoDownloadTask");
			for(int i = 0; i < selectedMedias.size(); i++) {
				MediaData data = selectedMedias.get(i);
				try {
					
					InputStream in = imageDownloader.getInputStream(data.url, 0);
					
					File dir = new File(VIDEO_DOWNLOAD_FOLDER);
					if(!dir.exists())
						dir.mkdir();
					
					File file = new File(VIDEO_DOWNLOAD_FOLDER, data.name);
					
					if(file.exists()) {
						downloadcount++;
						publishProgress();
						in.close();
						continue;
					}
					
					byte[] buffer = new byte[1024];
					int len = 0;
					
					FileOutputStream os = new FileOutputStream(file);
					while((len = in.read(buffer)) != -1) {
						os.write(buffer, 0, len);
					}
					downloadcount++;
					publishProgress();
					
					os.close();
					in.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		
	}

	private class RemoteVedioDeleteTask extends AsyncTask<Void, Boolean, Void> {
		
		private ProgressDialog mDeleteProgressDialog = new ProgressDialog(getActivity());
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			mDeleteProgressDialog.setMessage(getActivity().getResources().getText(R.string.deleting));
			mDeleteProgressDialog.show();
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			if(mDeleteProgressDialog.isShowing())
				mDeleteProgressDialog.cancel();
			super.onPostExecute(result);
		}
		
		@Override
		protected void onProgressUpdate(Boolean... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			if(!values[0]) {
				Toast.makeText(mContext, R.string.connect_error, Toast.LENGTH_LONG).show();
				disCheckMedia();
			}
			else
			{
				onMediaDeleted();
				Toast.makeText(mContext, R.string.delete_ok, Toast.LENGTH_LONG).show();
			}
		}
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String urlPref = String.format("http://%s/cgi-bin/deletefiles?", glassIp);
			StringBuffer urlBuffer = new StringBuffer(urlPref);
			urlBuffer.append("vedios");
			
			for(MediaData data : selectedMedias) {
				urlBuffer.append("&" + data.name);
			}
			Log.e(TAG, "delete url" + urlBuffer.toString());
			HttpClient httpClient = CustomHttpClient.getHttpClient();
			HttpGet httpGet = new HttpGet(urlBuffer.toString());
			if(GlassImageDownloader.deleteRequestExecute(httpClient, httpGet)) {
				publishProgress(true);
			}
			else
				publishProgress(false);
				
			return null;
		}
		
	}
	
	public void initProgressDialog() {
		
		mConnectProgressDialog = new ProgressDialog(getActivity());
		mConnectProgressDialog.setTitle(R.string.remote_video);
		mConnectProgressDialog.setCancelable(false);
		mConnectProgressDialog.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.cancel();
					getActivity().onBackPressed();
				}
				return false;
			}
		});
	}
	
}
