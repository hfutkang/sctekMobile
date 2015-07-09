package com.sctek.smartglasses.fragments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import cn.ingenic.glasssync.R;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sctek.smartglasses.ui.MySideNavigationCallback;
import com.sctek.smartglasses.ui.SideNavigationView;
import com.sctek.smartglasses.utils.CustomHttpClient;
import com.sctek.smartglasses.utils.GetRemoteVideoThumbWorks;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.MultiMediaScanner;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.XmlContentHandler;
import com.sctek.smartglasses.utils.GetRemoteVideoThumbWorks.GetRemoteVideoThumbListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class BaseFragment extends Fragment {
	
	public static final String TAG = BaseFragment.class.getName();
	public static final String URL_PREFIX = "http://192.168.5.122/";
	
	public static final String PHOTO_DOWNLOAD_FOLDER = 
			Environment.getExternalStorageDirectory().toString()	+ "/SmartGlasses/photos/";
	public static final String VIDEO_DOWNLOAD_FOLDER = 
			Environment.getExternalStorageDirectory().toString()	+ "/SmartGlasses/vedios";
	
	public static final String EXTERNEL_DIRCTORY_PATH = 
			Environment.getExternalStorageDirectory() + "/SmartGlasses/photos/";
	
	protected static final int WIFI_AP_STATE_DISABLED = 11;
	protected static final int WIFI_AP_STATE_ENABLED = 13;
	
	public static final String WIFI_AP_STATE_CHANGED_ACTION =
	        "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";
    
	public ArrayList<MediaData> mediaList;
	
	public ArrayList<MediaData> selectedMedias;
	
	public ArrayList<CheckBox> checkBoxs;
	
	public HashMap<Long, String> downloadIdImageMap;
	
	private DisplayImageOptions options;
	
	private SideNavigationView mSideNavigationView;
	
	public View deleteView;
	public View selectAllView;
	
	public boolean showImageCheckBox;
	public boolean wifi_msg_received = false;
	
	public ImageAdapter mImageAdapter;
	
	public TextView deleteTv;
	public TextView cancelTv;
	protected CheckBox selectAllCb;
	
	private int childIndex;
	
	public WifiManager mWifiManager;
	public SyncChannel mChannel;
	
	public Context mContext;
	public int preApState;
	public SetWifiAPTask mWifiATask;
	private GetRemoteMediaUrlTask mMediaUrlTask;
	
	public ProgressDialog mConnectProgressDialog ;
	public ProgressDialog mDeleteProgressDialog ;
	public String glassIp;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
		setHasOptionsMenu(true);
		
		mContext = (Context)getActivity().getApplicationContext();
		options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.ic_stub)
		.resetViewBeforeLoading(true)
		.cacheOnDisk(true)
		.imageScaleType(ImageScaleType.EXACTLY)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.considerExifParams(true)
		.displayer(new FadeInBitmapDisplayer(300))
		.build();
		
		childIndex = getArguments().getInt("index");
		selectedMedias = new ArrayList<MediaData>();
		showImageCheckBox = false;
		mImageAdapter = new ImageAdapter();
		downloadIdImageMap = new HashMap<Long, String>();
		checkBoxs = new ArrayList<CheckBox>();
		mMediaUrlTask = new GetRemoteMediaUrlTask();
		mDeleteProgressDialog = new ProgressDialog(getActivity());
		
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mChannel = SyncChannel.create("00e04c68229b0", mContext, mOnSyncListener);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_image_grid, container, false);
		
		selectAllView = view.findViewById(R.id.select_all_lo);
		deleteView = view.findViewById(R.id.delete_bt_lo);
		deleteTv = (TextView) view .findViewById(R.id.delete_tv);
		cancelTv = (TextView) view.findViewById(R.id.cancel_tv);
		selectAllCb = (CheckBox)view.findViewById(R.id.select_all_cb);
		
		cancelTv.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onCancelTvClicked();
				disCheckMedia();

			}
		});
		
		selectAllCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked) {
					for(MediaData md : mediaList) {
						if(!selectedMedias.contains(md))
							selectedMedias.add(md);
					}
					for(CheckBox cb : checkBoxs) {
						cb.setChecked(true);
					}
				}
				else {
					for(CheckBox cb : checkBoxs)
						cb.setChecked(false);
					selectedMedias.clear();
				}
			}
		});
		
		GridView grid = (GridView) view.findViewById(R.id.grid);
		grid.setAdapter(mImageAdapter);
		
		switch (childIndex) {
			case NativePhotoGridFragment.FRAGMENT_INDEX:
				grid.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, false));
				grid.setOnItemClickListener(onPhotoImageClickedListener);
				break;
			case RemotePhotoGridFragment.FRAGMENT_INDEX:
				grid.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, false));
				grid.setOnItemClickListener(onPhotoImageClickedListener);
				if(WIFI_AP_STATE_ENABLED != WifiUtils.getWifiAPState(mWifiManager))
					showTurnWifiApOnDialog();
				break;
			case NativeVideoGridFragment.FRAGMENT_INDEX:
				grid.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, false));
				grid.setOnItemClickListener(onVideoImageClickedListener);
				break;
			case RemoteVideoGridFragment.FRAGMENT_INDEX:
				grid.setOnItemClickListener(onVideoImageClickedListener);
				if(WIFI_AP_STATE_ENABLED != WifiUtils.getWifiAPState(mWifiManager))
					showTurnWifiApOnDialog();
				break;
		}
		
		return view;
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
		mMediaUrlTask.cancel(true);
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroyView");
		super.onDestroyView();
	}
	
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDetach");
		super.onDetach();
	}
	
	public class ImageAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private GetRemoteVideoThumbWorks thumbWork;

		ImageAdapter() {
			inflater = LayoutInflater.from(mContext);
			thumbWork = GetRemoteVideoThumbWorks.getInstance();
		}

		@Override
		public int getCount() {
			return mediaList.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.e(TAG, "getView");
			final ViewHolder holder;
			final int mPositoin = position;
			View view = convertView;
			if (view == null) {
				
				view = inflater.inflate(R.layout.image_grid_item, parent, false);
				view.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
				
				holder = new ViewHolder();
				assert view != null;
				holder.imageView = (ImageView) view.findViewById(R.id.image);
				holder.progressBar = (ProgressBar) view.findViewById(R.id.progress);
				holder.imageName = (TextView)view.findViewById(R.id.image_name_tv);
				holder.imageCb = (CheckBox)view.findViewById(R.id.image_select_cb);
				
				view.setTag(holder);
				
				checkBoxs.add(holder.imageCb);
				
			} else {
				holder = (ViewHolder) view.getTag();
			}
			
			holder.imageCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				private int imageIndex = mPositoin;
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					try {
						if(isChecked) {
							if(!selectedMedias.contains(mediaList.get(imageIndex)))
								selectedMedias.add(mediaList.get(imageIndex));
						}
						else
							selectedMedias.remove(mediaList.get(imageIndex));
					} catch (IndexOutOfBoundsException expected){
						
					}
				}
			});
			
			if(selectedMedias.contains(mediaList.get(mPositoin))) {
				holder.imageCb.setChecked(true);
			} 
			else {
				holder.imageCb.setChecked(false);
			}
			
			if(childIndex != RemoteVideoGridFragment.FRAGMENT_INDEX) {
				ImageLoader.getInstance()
						.displayImage(mediaList.get(position).url, holder.imageView, options, new SimpleImageLoadingListener() {
							@Override
							public void onLoadingStarted(String imageUri, View view) {
								holder.progressBar.setProgress(0);
								holder.progressBar.setVisibility(View.VISIBLE);
								holder.imageName.setVisibility(View.GONE);
							}
	
							@Override
							public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
								try {
									holder.progressBar.setVisibility(View.GONE);
									holder.imageName.setVisibility(View.VISIBLE);
									holder.imageName.setText(mediaList.get(mPositoin).name);
								} catch (IndexOutOfBoundsException e) {
								}
							}
	
							@Override
							public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
								try {
									holder.progressBar.setVisibility(View.GONE);
									holder.imageName.setVisibility(View.VISIBLE);
									holder.imageName.setText(mediaList.get(mPositoin).name);
								} catch (IndexOutOfBoundsException e) {
								}
							}
						}, new ImageLoadingProgressListener() {
							@Override
							public void onProgressUpdate(String imageUri, View view, int current, int total) {
								holder.progressBar.setProgress(Math.round(100.0f * current / total));
							}
						});
			}
			else {
				holder.imageName.setText(mediaList.get(mPositoin).name);
				holder.progressBar.setVisibility(View.GONE);
				holder.imageView.setImageResource(R.drawable.ic_stub);
				
				thumbWork.getRemoteVideoThumb(mediaList.get(position).url, new GetRemoteVideoThumbListener() {
					
					@Override
					public void onGetRemoteVideoThumbDone(Bitmap bitmap) {
						// TODO Auto-generated method stub
						Log.e(TAG, "5");
						holder.imageView.setImageBitmap(bitmap);
					}
				});
			}

			return view;
		}
	}

	static class ViewHolder {
		ImageView imageView;
		ProgressBar progressBar;
		TextView imageName;
		CheckBox imageCb;
	}
	
	private OnItemClickListener onPhotoImageClickedListener = new OnItemClickListener() {

		@SuppressLint("NewApi")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			FragmentManager fragManager = getActivity().getFragmentManager();
			FragmentTransaction transcaction = fragManager.beginTransaction();
			String tag = PhotoViewPagerFragment.class.getName();
			PhotoViewPagerFragment photoFm = (PhotoViewPagerFragment)fragManager.findFragmentByTag(tag);
			if(photoFm == null)
				photoFm = new PhotoViewPagerFragment();
			
			Bundle bundle = new Bundle();
			bundle.putInt("position", position);
			bundle.putParcelableArrayList("data", mediaList);
			photoFm.setArguments(bundle);
			
			transcaction.replace(android.R.id.content, photoFm, tag);
			transcaction.addToBackStack(null);
			transcaction.commit();
		}
		
	};
	
	private OnItemClickListener onVideoImageClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			Uri uri = Uri.parse(mediaList.get(position).url);
//			Uri uri = Uri.parse("http://192.168.5.253/pub/sct/tracker/VID_20150130_180836.mp4");
			Intent intent = new Intent(Intent.ACTION_VIEW	);
			intent.setData(uri);
			startActivity(intent);
		}
	};
	
	public class SetWifiAPTask extends AsyncTask<Void, Void, Void> {
    	
		private boolean mMode;
		private boolean mFinish;
		
		public SetWifiAPTask(boolean mode, boolean finish) {
		    mMode = mode;
		    mFinish = finish;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mConnectProgressDialog.setMessage(getResources().getText(R.string.turning_wifi_ap_on));
			mConnectProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			//updateStatusDisplay();
//			if (mFinish) mContext.finish();
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.e(TAG, "1234");
			try {
				WifiUtils.turnWifiApOn(getActivity(), mWifiManager);
			} catch(Exception e) {
				e.printStackTrace();
			}
		    return null;
		}
    }
	
	public void onCancelTvClicked() {
		
		deleteView.setVisibility(View.GONE);
		selectAllView.setVisibility(View.GONE);
		for(CheckBox cb : checkBoxs) {
			cb.setVisibility(View.GONE);
		}
		
	}
	
	public void disCheckMedia() {
		selectAllCb.setChecked(false);
		selectedMedias.clear();
	}
	
	
	public void onNativeMediaDeleteTvClicked(String type) {
		
		String imagesPath[] = getMediaPath(type);
		
		for(String path : imagesPath) {
			File file = new File(path);
			if(file.exists())
				file.delete();
		}
		
		MultiMediaScanner scanner = new MultiMediaScanner(mContext, imagesPath, null, mHandler);
		scanner.connect();
		
		onMediaDeleted();
		
	}
	
	public Handler mHandler = new Handler() {
		
    	@Override
    	public void dispatchMessage(Message msg) {
    		// TODO Auto-generated method stub
    		super.dispatchMessage(msg);
    		switch (msg.what) {
    		case 0:
    			String msg0 = String.format("正在删除(%d/%d)...", msg.arg1, msg.arg2);
    			mDeleteProgressDialog.setMessage(msg0);
    			mDeleteProgressDialog.show();
    	    	break;
    		case 1:
    			String msg1 = String.format("正在删除(%d/%d)...", msg.arg1, msg.arg2);
    			mDeleteProgressDialog.setMessage(msg1);
    	    	break;
    		case 2:
    			if(mDeleteProgressDialog.isShowing())
    				mDeleteProgressDialog.cancel();
    			break;
    		case 3:
    			mConnectProgressDialog.setMessage(getResources().getText(R.string.get_file_list));
    			break;
    		case 4:
    			if(mConnectProgressDialog.isShowing())
    				mConnectProgressDialog.cancel();
    			break;
    		}
    	}
    };
	
	public void onMediaDeleted() {
		ArrayList<MediaData> tmp = new ArrayList<MediaData>(selectedMedias);
		
		disCheckMedia();
		for(MediaData md : tmp) {
			Log.e(TAG, md.name);
			int i = mediaList.indexOf(md);
			if(i != -1)
				mediaList.remove(i);
		}
		tmp.clear();
		mImageAdapter.notifyDataSetChanged();
	}
	
	public void onRemotePhotoDeleteTvClicked() {
		
		DownloadManager mDownloadManager = (DownloadManager)mContext
				.getSystemService(mContext.DOWNLOAD_SERVICE);
//		DownloadManager.Request request = new DownloadManager.Request(uri)
	}
	
	private String[] getMediaPath(String type) {
		
		String paths[] = new String[selectedMedias.size()];
		String dirPath = Environment.getExternalStorageDirectory().toString()
				+ "/SmartGlasses/" + type + "/";
		
		for(int i = 0; i < selectedMedias.size(); i++) {
			
			MediaData data = selectedMedias.get(i);
			paths[i] = dirPath + data.name;
		}
		return paths;
	}
	
	private String[] getImagesId(ArrayList<String> imagesUrl) {
		String ids[] = new String[imagesUrl.size()];
		int i = 0;
		for(String url : imagesUrl) {
			int idIndex = url.lastIndexOf("/");
			ids[i++] = url.substring(idIndex + 1);
			Log.e(TAG, ids[i-1]);
		}
		return ids;
	}
	
	class GetRemoteMediaUrlTask extends AsyncTask<String, Integer, String> {
		
		public GetRemoteMediaUrlTask() {
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mHandler.sendEmptyMessage(3);
		}
		
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}
		@Override
		protected String doInBackground(String... type) {
			// TODO Auto-generated method stub
			if(!getMediaUrl(type[0], type[1])) {
				publishProgress(2);
			}
			else if(!isCancelled())
				publishProgress(1);
			return "";
		}
		
		@Override
		protected void onProgressUpdate(Integer ...values) {
			// TODO Auto-generated method stub
			mHandler.sendEmptyMessage(4);
			switch(values[0]) {
			case 1:
				mImageAdapter.notifyDataSetChanged();
				break;
			case 2:
				Toast.makeText(getActivity(), R.string.connect_error, Toast.LENGTH_LONG).show();
				break;
			}
			super.onProgressUpdate(values);
		}
		
	}
    
	private MyOnSyncListener mOnSyncListener = new MyOnSyncListener();
	private class MyOnSyncListener implements SyncChannel.onChannelListener {
	
		private boolean connected = false;
		@Override
		public void onReceive(RESULT result, Packet data) {
			// TODO Auto-generated method stub
			Log.e(TAG, "Channel onReceive");
			
			glassIp = data.getString("ip");
			if(glassIp != null && glassIp.length() != 0&&!connected)	 {
				
				connected = true;
				
				if(childIndex == RemotePhotoGridFragment.FRAGMENT_INDEX)
					mMediaUrlTask.execute(new String[]{glassIp, "photos"});
				else if(childIndex == RemoteVideoGridFragment.FRAGMENT_INDEX)
					mMediaUrlTask.execute(new String[]{glassIp, "vedios"});
				
			}
		}
	
		@Override
		public void onSendCompleted(RESULT arg0, Packet arg1) {
			// TODO Auto-generated method stub
			
			Log.e(TAG, "onSendCompleted");
		}
	
		@Override
		public void onStateChanged(CONNECTION_STATE arg0) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onStateChanged:" + arg0.toString());
		}
		
	}
	
	public String getConnectedGlassIP() { 
		
		BufferedReader br = null;  
		String line;  
		String ip = null;
		try {  
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			while ((line = br.readLine()) != null) { 
				String[] splitted = line.split(" +");
				if (!"IP".equals(splitted[0])) {
					ip = splitted[0];
					break;
				}
			}
			br.close();
		} catch (Exception e) { 
			e.printStackTrace();  
		}  
		
		if(ip == null && !wifi_msg_received) {
			Packet packet = mChannel.createPacket();
			packet.putInt("type", 1);
			
			String defaultSsid = ((TelephonyManager)mContext
					.getSystemService(mContext.TELEPHONY_SERVICE)).getDeviceId();
			String ssid = PreferenceManager.
					getDefaultSharedPreferences(mContext).getString("ssid", defaultSsid);
			String pw = PreferenceManager.getDefaultSharedPreferences(mContext).getString("pw", "12345678");
			
			packet.putString("ssid", ssid);
			packet.putString("pw", pw);
			mChannel.sendPacket(packet);
		}
		return ip;
	} 
	
	private boolean getMediaUrl(final String ip, String type) {
		
		String uri = String.format("http://" + ip + "/cgi-bin/listfiles?%s", type);
		final HttpClient httpClient = CustomHttpClient.getHttpClient();
		final HttpGet httpGet = new HttpGet(uri);
		boolean ok = false;
		try {
			ok = httpRequestExecute(httpClient, httpGet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(ok) {
			mediaList = getMediaData(ip);
		}
		if(mediaList == null)
			return false;
		return true;
	}
	
	public void refreshGallery(String type) {
//		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//		mediaScanIntent.setData(Uri.fromFile(file));
//		getActivity().getApplicationContext().sendBroadcast(mediaScanIntent);
		String path[] = getMediaPath(type);
		MultiMediaScanner scanner = new MultiMediaScanner(mContext, path, null, null);
		scanner.connect();
	}
	
	private ArrayList<MediaData> getMediaData(String ip) {
		try {
			File xmlFile = new File(getActivity().getCacheDir(), "medianame.xml");
			XmlContentHandler xmlHandler = new XmlContentHandler(ip);
			SAXParserFactory factory = SAXParserFactory.newInstance();    
			SAXParser parser = factory.newSAXParser();    
			XMLReader xmlReader = parser.getXMLReader(); 
			xmlReader.setContentHandler(xmlHandler);
			xmlReader.parse(new InputSource(new FileInputStream(xmlFile)));
			
			return xmlHandler.getMedias();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean httpRequestExecute(HttpClient httpclient, HttpGet httpget) {
		
		InputStream in = null;
		int retry = 3;
		while (retry-- != 0) {
			Log.e(TAG, "123");
			try{	
				HttpResponse response = httpclient.execute(httpget);
				in = response.getEntity().getContent();
				
				byte[] buffer = new byte[4096];
				
				File cacheDir = getActivity().getCacheDir();
				File xmlFile = new File(cacheDir, "medianame.xml");
				FileOutputStream fo = new FileOutputStream(xmlFile, false);
				
				int n = 0;
				while((n = in.read(buffer)) != -1){
					fo.write(buffer, 0, n);
				}
				
				fo.close();
				in.close();
				
				return true;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public void sendApInfoToGlass() {
		
		if(mChannel.isConnected()) {
			
			mConnectProgressDialog.setMessage(getResources().getText(R.string.wait_device_connect));
			if(!mConnectProgressDialog.isShowing())
				mConnectProgressDialog.show();
			
			Packet packet = mChannel.createPacket();
			packet.putInt("type", 1);
			
			String defaultSsid = ((TelephonyManager)getActivity()
					.getSystemService(mContext.TELEPHONY_SERVICE)).getDeviceId();
			String ssid = PreferenceManager.
					getDefaultSharedPreferences(mContext).getString("ssid", defaultSsid);
			String pw = PreferenceManager.getDefaultSharedPreferences(mContext).getString("pw", "12345678");
			
			packet.putString("ssid", ssid);
			packet.putString("pw", pw);
			mChannel.sendPacket(packet);
		}
		else {
			Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			getActivity().onBackPressed();
		}
	}
	
	public void showTurnWifiApOnDialog() {
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.turn_wifi_ap_on);
		builder.setMessage(R.string.wifi_ap_hint);
		builder.setPositiveButton(R.string.turn_wifi_ap_on_now, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				mWifiATask.execute();
				dialog.cancel();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
				getActivity().onBackPressed();
			}
		});
		
		builder.setCancelable(false);
		builder.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_BACK) {
					getActivity().onBackPressed();
					dialog.cancel();
				}
				return false;
			}
		});
		
		builder.create().show();
	}
}
