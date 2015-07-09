package cn.ingenic.glasssync.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.R.layout;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.lang.Runnable;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.ui.view.MultiMediaPictureItem;
import cn.ingenic.glasssync.ui.view.MultiMediaVideoItem;

public class Fragment_Media extends Fragment implements OnClickListener{
    private final String TAG = "Fragment_Media";
    public final static boolean DEBUG=true;

    private final String SYNC_FINISH="sync_file_finish";

    private TextView mPictureMenu;
    private TextView mVideoMenu;

    private View mFragmentView;
    private MultiMediaPictureItem mPictureView;
    private MultiMediaVideoItem mVideoView;

    private boolean mStopThread = false;
    String[] picColu = new String[] { MediaStore.Images.Media._ID,
    				      MediaStore.Images.Media.DATA };
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
				 Bundle savedInstanceState) {
	if(DEBUG)Log.d(TAG, "onCreateView");
	if(mFragmentView==null){
	    mFragmentView=LayoutInflater.from(getActivity()).inflate(R.layout.layout_media, null);
	    RelativeLayout able = (RelativeLayout)mFragmentView.findViewById(R.id.able);
	    mPictureMenu = (TextView) mFragmentView.findViewById(R.id.picture_menu);
	    mVideoMenu = (TextView) mFragmentView.findViewById(R.id.video_menu);
	    mPictureMenu.setOnClickListener(this);
	    mVideoMenu.setOnClickListener(this);

	    mPictureView = new MultiMediaPictureItem(getActivity());
	    mPictureView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	    able.addView(mPictureView);

	    mVideoView = new MultiMediaVideoItem(getActivity());
	    mVideoView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	    able.addView(mVideoView);
	}

	ViewGroup parent = (ViewGroup) mFragmentView.getParent();
	if (parent != null) {
	    parent.removeView(mFragmentView);
	}

	mPictureMenu.setSelected(true);
	mVideoMenu.setSelected(false);
	showPictureView();

	IntentFilter filter = new IntentFilter();
	filter.addAction(SYNC_FINISH);
	getActivity().registerReceiver(mSyncFileReceiver, filter);

	return mFragmentView;
    }
    
    @Override 
	public void onDestroyView(){
	super.onDestroyView();   
        if(DEBUG)Log.d(TAG,"---onDestroyView");
	getActivity().unregisterReceiver(mSyncFileReceiver);
    }
    @Override 
	public void onDestroy(){
	super.onDestroy();   
	mPictureView.stopThread();
	mStopThread = true;
    }
    @Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
	if(isVisibleToUser == false){
	    if(mPictureView != null)
		mPictureView.setUserInVisible();

	    if(mVideoView != null)
		mVideoView.setUserInVisible();
	}	    
    }

    @Override
	public void onClick(View v) {
	  // TODO Auto-generated method stub
	switch (v.getId()) {
	case R.id.picture_menu:
	    if(DEBUG)Log.d(TAG, R.id.picture_menu + "R.id.picture_menu");
	    mPictureMenu.setSelected(true);
	    mVideoMenu.setSelected(false);
	    showPictureView();
	    break;
	case R.id.video_menu:
	    if(DEBUG)Log.d(TAG, R.id.video_menu + "R.id.video_menu");
	    mVideoMenu.setSelected(true);
	    mPictureMenu.setSelected(false);
	    showVideoView();
	    break;
	}
    }

    private void scanFile(String path) {
	final String Path = path;
    	new Thread(new Runnable() {
    		@Override
    		    public void run() {
    		    StringBuilder where = new StringBuilder();
    		    where.append(MediaStore.Images.Media.DATA + " like ?");
    		    String whereVal[] = { "%" + "IGlass/Thumbnails" + "%" };
    		    Cursor cs = getActivity().getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, picColu, where.toString(), whereVal, null);
    		    if (cs == null || cs.getCount() == 0) {
    			Log.e(TAG,"-------no thumbnails found in IGlass/.");
    			return;
    		    }
    		    int id_idx = cs
    			.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
    		    int path_idx = cs
    			.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    		    if (cs.moveToLast()) {
    			do {
    			    Long id = cs.getLong(id_idx);
    			    String path = cs.getString(path_idx);
    			    if(DEBUG)Log.e(TAG, "id:" + id + " path:" + path);
			    if(path.equals(Path)){
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("id", id);
				map.put("path", path);
				if (check_video(path)) {
				    mVideoView.addItem(map);
				}else{
				    mPictureView.addItem(map);
				}
				break;
			    }
				
    			} while (cs.moveToPrevious() && !mStopThread);
    		    }
    		    cs.close();
    		}
    	    }).start();

    }

    public static boolean check_video(String path) {
    	if (path.toLowerCase().endsWith("_mp4_thumb.jpg")
    	    || path.toLowerCase().endsWith("_3gp_thumb.jpg")
    	    || path.toLowerCase().endsWith("_mkv_thumb.jpg")
    	    || path.toLowerCase().endsWith("_avi_thumb.jpg")
    	    || path.toLowerCase().endsWith("_mov_thumb.jpg")
    	    || path.toLowerCase().endsWith("_wmv_thumb.jpg"))
    	    return true;
    	return false;
    }

    private void showPictureView(){
	mPictureView.setVisibility(View.VISIBLE);
	mVideoView.setVisibility(View.GONE);
    }

    private void showVideoView(){
	mPictureView.setVisibility(View.GONE);
	mVideoView.setVisibility(View.VISIBLE);
    }

    private final BroadcastReceiver mSyncFileReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		if (SYNC_FINISH.equals(intent.getAction())) {
		    scanFile(intent.getStringExtra("path"));
		}
	    }
	};
}
