package com.sctek.smartglasses.ui;

import com.sctek.smartglasses.fragments.NativePhotoGridFragment;
import com.sctek.smartglasses.fragments.NativeVideoGridFragment;
import com.sctek.smartglasses.fragments.PhotoViewPagerFragment;
import com.sctek.smartglasses.fragments.SettingFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.ui.BindGlassActivity;

public class MySideNavigationCallback implements ISideNavigationCallback {

	private String TAG;
	private Context mContext;
	private DefaultSyncManager mSyncManager;
	
	public MySideNavigationCallback(Context context) {
		mContext = context;
		mSyncManager = DefaultSyncManager.getDefault();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onSideNavigationItemClick(int itemId) {
		// TODO Auto-generated method stub
		
		switch (itemId) {
		case R.id.photo_item:
			TAG = NativePhotoGridFragment.class.getName();
			NativePhotoGridFragment PhotoGF = (NativePhotoGridFragment)((FragmentActivity) mContext)
					.getFragmentManager().findFragmentByTag(TAG);
			if(PhotoGF == null) {
				PhotoGF = new NativePhotoGridFragment();
				Bundle pBundle = new Bundle();
				pBundle.putInt("index", NativePhotoGridFragment.FRAGMENT_INDEX);
				PhotoGF.setArguments(pBundle);
			}
			
			((FragmentActivity)mContext).getFragmentManager()
					.beginTransaction().replace(android.R.id.content, PhotoGF, TAG).commit();
			break;
		case R.id.video_item:
			TAG = NativeVideoGridFragment.class.getName();
			NativeVideoGridFragment VideoGF = (NativeVideoGridFragment)((FragmentActivity) mContext)
					.getFragmentManager().findFragmentByTag(TAG);
			
			if(VideoGF == null) {
				
				VideoGF = new NativeVideoGridFragment();
				
				Bundle vBundle = new Bundle();
				vBundle.putInt("index", NativeVideoGridFragment.FRAGMENT_INDEX);
				VideoGF.setArguments(vBundle);
			}
			
			((FragmentActivity)mContext).getFragmentManager().beginTransaction()
					.replace(android.R.id.content, VideoGF, TAG).commit();
			break;
		case R.id.setting_item:
			
			TAG = SettingFragment.class.getName();
			SettingFragment settingGF = (SettingFragment)((FragmentActivity) mContext)
					.getFragmentManager().findFragmentByTag(TAG);
			
			if(settingGF == null) {
				settingGF = new SettingFragment();
			}
			
			((FragmentActivity)mContext).getFragmentManager().beginTransaction()
					.replace(android.R.id.content, settingGF, TAG).addToBackStack(null).commit();
			
			break;
		case R.id.unbind:
			showUbindDialog();
			break;
		case R.id.about_item:
			break;
		case R.id.video_live:
			Intent intent = new Intent(mContext, VedioPlayerActivity.class);
			mContext.startActivity(intent);
			break;
		default:
			break;
		}

	}
	
	 private void unBond() {
			new Thread(new Runnable() {
				@Override
				    public void run() {
					try {
				    mSyncManager.setLockedAddress("",true);
				    try {
					Thread.sleep(1000);
				    } catch (Exception e) {
				    }

//				    GlassDetect glassDetect = (GlassDetect)GlassDetect.getInstance(getActivity().getApplicationContext());
//				    glassDetect.set_audio_disconnect(GlassDetect.AUDIO_STRATEGY_DISCONNECT);
				    
				    mSyncManager.disconnect();
				    
				    //mBLDServer.removeBond();
				    Intent intent = new Intent(mContext,BindGlassActivity.class);	    
				    mContext.startActivity(intent);
				    ((Activity) mContext).finish();
				    } catch (Exception e) {
				    	e.printStackTrace();
				    }
				}
			    }).start();
		    }		
	 
	 public void showUbindDialog() {
			
			AlertDialog.Builder builder = new Builder(mContext);
			builder.setTitle(R.string.unbind);
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			});
			builder.setPositiveButton(R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					unBond();
				}
			});
			
			builder.create().show();
		}

}
