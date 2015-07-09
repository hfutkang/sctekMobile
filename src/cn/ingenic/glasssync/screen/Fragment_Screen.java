package cn.ingenic.glasssync.screen;

import cn.ingenic.glasssync.R;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.os.Message;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.util.Log;
import cn.ingenic.glasssync.screen.screenshare.ScreenModule;
import cn.ingenic.glasssync.screen.control.ScreenControlView;
import android.view.View.OnClickListener;
import android.content.Intent;

public class Fragment_Screen extends Fragment implements OnClickListener {
    private static final String TAG = "ScreenShareControlActivity";
    private static final int SEND_DATA_FINISH = 1;
    private Timer mTimer;
    private ScreenModule mScreenModule;
    private RelativeLayout mContainer;
    private ScreenControlView mScreenControlView; 
    public static ImageView mImageView;
    private SurfaceHolder mHolder;
    public static Handler mHandler;
    private View fragmentView;
    private TextView mButton, mLiveButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
				     Bundle savedInstanceState) {
	    
	    if (fragmentView == null) {
		fragmentView = inflater.inflate(R.layout.screen_share_controller_button, null);
	    }	  
	    ViewGroup parent = (ViewGroup) fragmentView.getParent();
	    if (parent != null) {
		parent.removeView(fragmentView);
	    }	

	    mButton = (TextView)fragmentView.findViewById(R.id.screenButton1);
	    mButton.setOnClickListener(this);
	    mLiveButton = (TextView)fragmentView.findViewById(R.id.liveButton1);
	    mLiveButton.setOnClickListener(this);
	    return fragmentView;
	}

    	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

        @Override
	public void onClick(View v) {
	switch (v.getId()) {
	case R.id.screenButton1:
	    //tv_binding.setText(R.string.scan_device);
	    Intent intent=new Intent(getActivity(),ScreenShareControlActivity.class);
	    startActivity(intent);	
	    return;
	case R.id.liveButton1:
	    Log.e(TAG, "liveButton1");
	    Intent liveIntent =new Intent(getActivity(),LiveDisplayActivity.class);
	    startActivity(liveIntent);	
	    return;
	default: break;
	}
    }
}
