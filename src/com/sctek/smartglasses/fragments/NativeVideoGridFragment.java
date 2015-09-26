package com.sctek.smartglasses.fragments;

import java.util.ArrayList;

import cn.ingenic.glasssync.R;

import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.WifiUtils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

//= {"http://h.hiphotos.baidu.com/image/w%3D310/sign=6c58b6e7b1119313c743f9b155380c10/a6efce1b9d16fdfa904abecbb78f8c5494ee7bf4.jpg",
//		"http://d.hiphotos.baidu.com/image/pic/item/d0c8a786c9177f3e3455af2873cf3bc79f3d56b5.jpg",
//		"http://b.hiphotos.baidu.com/image/pic/item/71cf3bc79f3df8dc885cdcb4ce11728b471028b5.jpg",
//		"http://c.hiphotos.baidu.com/image/pic/item/ae51f3deb48f8c54bec4960a39292df5e0fe7ffb.jpg",
//		"http://e.hiphotos.baidu.com/image/pic/item/dc54564e9258d1098f42fa97d258ccbf6c814d75.jpg",
//		"http://h.hiphotos.baidu.com/image/pic/item/bd3eb13533fa828be182cec6fe1f4134970a5a75.jpg",
//		"http://b.hiphotos.baidu.com/image/pic/item/ca1349540923dd546f64f44dd209b3de9c8248fb.jpg",
//		"http://h.hiphotos.baidu.com/image/pic/item/960a304e251f95ca9ee2c193ca177f3e670952fb.jpg",
//		"http://c.hiphotos.baidu.com/image/pic/item/43a7d933c895d1432fcfb70670f082025aaf075d.jpg",
//		"http://f.hiphotos.baidu.com/image/pic/item/faedab64034f78f07ab2c9d67a310a55b3191c5d.jpg",
//		"http://f.hiphotos.baidu.com/image/pic/item/f9198618367adab4ef69486688d4b31c8701e45e.jpg",
//		"http://e.hiphotos.baidu.com/image/pic/item/b21c8701a18b87d68bfbf83a040828381f30fd5e.jpg",
//		"http://h.hiphotos.baidu.com/image/pic/item/d1160924ab18972ba32d53efe5cd7b899e510afb.jpg",
//		"http://f.hiphotos.baidu.com/image/pic/item/e7cd7b899e510fb3720e24b2da33c895d1430cfb.jpg",
//		"http://photo.poco.cn/lastphoto-htx-id-4072622-p-0.xhtml?spread_id=FXuU",
//		"http://a.hiphotos.baidu.com/image/pic/item/9922720e0cf3d7caa3976c53f11fbe096b63a9b6.jpg",
//		"http://h.hiphotos.baidu.com/image/pic/item/024f78f0f736afc3f61b6c40b019ebc4b74512fb.jpg",
//		"http://f.hiphotos.baidu.com/image/pic/item/b219ebc4b74543a9170ca9e91d178a82b90114fb.jpg"};

public class NativeVideoGridFragment extends BaseFragment {
	
	public static final int FRAGMENT_INDEX = 2;
	private static final String TAG = NativeVideoGridFragment.class.getName();
	private boolean onCreate;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate");
		
		getActivity().getActionBar().show();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		onCreate = true;
		getVideoPath();
		
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onStart");
		super.onStart();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onResume");
		getActivity().setTitle(R.string.native_video);
		
		if(!onCreate) {
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					getVideoPath();
					mImageAdapter.notifyDataSetChanged();
				}
			});
			
		}
		
		onCreate = false;
		
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
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroyView");
		selectedMedias.clear();
		super.onDestroyView();
	}
	
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDetach");
		super.onDetach();
	}

	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.e(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.native_video_fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.e(TAG, "onOptionsItemSelected");
		switch (item.getItemId()) {
			case R.id.share_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.GONE);
				for(CheckBox cb : shareCheckBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.share);
				deleteTv.setOnClickListener(onNativeVideoShareTvClickListener);
				return true;
			case R.id.glasses_item:
				showRemoteVideoFragment();
				return true;
			case R.id.native_video_delete_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.delete);
				deleteTv.setOnClickListener(onNativeVideoDeleteTvClickListener);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@SuppressLint("NewApi")
	private void getVideoPath() {
		
		mediaList = new ArrayList<MediaData>();
		
		ContentResolver cr = getActivity().getContentResolver();
		Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME}, 
				MediaStore.MediaColumns.DATA + " like ?", 
				new String[]{"%/SmartGlasses/videos%"}, null);
		
		while(cursor.moveToNext()) {
			
			MediaData md = new MediaData();
			md.setUrl("content://media/external/video/media/" + cursor.getInt(0));
			md.setName(cursor.getString(1));
			mediaList.add(md);
			
		}
		
		cursor.close();
	}
	
	@SuppressLint("NewApi")
	private void showRemoteVideoFragment() {
		
		FragmentManager fragManager = getActivity().getFragmentManager();
		FragmentTransaction transcaction = fragManager.beginTransaction();
		String tag = RemoteVideoGridFragment.class.getName();
		RemoteVideoGridFragment remoteVideoFm = (RemoteVideoGridFragment)fragManager.findFragmentByTag(tag);
		if(remoteVideoFm == null) {
			remoteVideoFm = new RemoteVideoGridFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("index", RemoteVideoGridFragment.FRAGMENT_INDEX);
			bundle.putParcelableArrayList("videos", mediaList);
			remoteVideoFm.setArguments(bundle);
		}
		
		transcaction.replace(android.R.id.content, remoteVideoFm, tag);
		transcaction.addToBackStack(null);
		transcaction.commit();
	}
	
	private void onNativeVideoShareTvClicked() {
		
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.setType("video/mp4");
		
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(shareVideo.url));
		Intent selectIntent = Intent.createChooser(shareIntent, getResources().getText(R.string.share));
		startActivity(selectIntent);
		
		selectedCb.setChecked(false);
		
	}
	
	private void showTurnApOffWhenShareVideoDialog() {
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.turn_wifi_ap_off);
		builder.setMessage(R.string.wifi_ap_hint_off);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						// TODO Auto-generated method stub
						WifiManager wifimanager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
						WifiUtils.setWifiApEnabled(false, wifimanager);
						return null;
					}
				}.execute();
				
				onNativeVideoShareTvClicked();
				
				dialog.cancel();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
				onNativeVideoShareTvClicked();
				
				dialog.cancel();
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				onNativeVideoShareTvClicked();
				
			}
		});
		dialog.show();
	}
	
	private OnClickListener onNativeVideoDeleteTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(selectedMedias.size() != 0)
				showDeleteConfirmDialog();
			else
			 disCheckMedia();
		}
	};
	
	private OnClickListener onNativeVideoShareTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(shareVideo != null) {
				if(WifiUtils.getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED) 
					showTurnApOffWhenShareVideoDialog();
				else 
					onNativeVideoShareTvClicked();
			}
			onShareCancelTvClicked();
		}
	};
	
	private void onShareCancelTvClicked() {
		
		deleteView.setVisibility(View.GONE);
		for(CheckBox cb : shareCheckBoxs) {
			cb.setVisibility(View.GONE);
		}
		
	}
	
	private void showDeleteConfirmDialog() {
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.delete);
		builder.setMessage(R.string.delete_message);
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				onNativeMediaDeleteTvClicked("videos");
				
				onCancelTvClicked();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		
		builder.create().show();
		
	}
	
}
