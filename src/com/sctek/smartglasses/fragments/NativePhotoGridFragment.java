package com.sctek.smartglasses.fragments;

import java.io.File;
import java.util.ArrayList;

import cn.ingenic.glasssync.R;

import com.sctek.smartglasses.utils.MediaData;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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

public class NativePhotoGridFragment extends BaseFragment {
	
	public static final int FRAGMENT_INDEX = 1;
	private static final String TAG = NativePhotoGridFragment.class.getName();
	private boolean onCreate;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
		
		getActivity().getActionBar().show();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		onCreate = true;
		getImagePath();
		
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
		getActivity().setTitle(R.string.native_photo);
		
		if(!onCreate) {
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					getImagePath();
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
		inflater.inflate(R.menu.native_photo_fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.e(TAG, "onOptionsItemSelected");
		switch (item.getItemId()) {
			case R.id.share_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.share);
				deleteTv.setOnClickListener(onNativePhotoShareTvClickListener);
				return true;
			case R.id.glasses_item:
				showRemotePhotoFragment();
				return true;
			case R.id.native_photo_delete_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.delete);
				deleteTv.setOnClickListener(onNativePhotoDeleteTvClickListener);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void getImagePath() {
		
		mediaList = new ArrayList<MediaData>();
		
		ContentResolver cr = getActivity().getContentResolver();
		Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA}, 
				MediaStore.MediaColumns.DATA + " like ?", 
				new String[]{"%/SmartGlasses/photos%"}, null);
		
		while(cursor.moveToNext()) {
			
			Log.e(TAG, cursor.getString(2));
			MediaData md = new MediaData();
			md.setUrl("content://media/external/images/media/" + cursor.getInt(0));
			md.setName(cursor.getString(1));
			mediaList.add(md);
			
		}
		
		cursor.close();
	}
	
	private void showRemotePhotoFragment() {
		
		FragmentManager fragManager = getActivity().getFragmentManager();
		FragmentTransaction transcaction = fragManager.beginTransaction();
		String tag = RemotePhotoGridFragment.class.getName();
		RemotePhotoGridFragment remotePhotoFm = (RemotePhotoGridFragment)fragManager.findFragmentByTag(tag);
		if(remotePhotoFm == null) {
			remotePhotoFm = new RemotePhotoGridFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("index", RemotePhotoGridFragment.FRAGMENT_INDEX);
			remotePhotoFm.setArguments(bundle);
		}
		
		transcaction.replace(android.R.id.content, remotePhotoFm, tag);
		transcaction.addToBackStack(null);
		transcaction.commit();
	}
	
	private void onNativePhotoShareTvClicked() {
		
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
		shareIntent.setType("image/jpeg");
		ArrayList<Uri> photoUris = new ArrayList<Uri>();
		
		for(MediaData dd: selectedMedias) {
			photoUris.add(Uri.parse(dd.url));
		}
		
		shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoUris);
		startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
		disCheckMedia();
		
	}
	
	private OnClickListener onNativePhotoDeleteTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(selectedMedias.size() != 0)
				onNativeMediaDeleteTvClicked("photos");
			else
				disCheckMedia();
			onCancelTvClicked();
		}
	};
	
	private OnClickListener onNativePhotoShareTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(selectedMedias.size() != 0)
				onNativePhotoShareTvClicked();
			else
				disCheckMedia();
			onCancelTvClicked();
		}
	};
	
}
