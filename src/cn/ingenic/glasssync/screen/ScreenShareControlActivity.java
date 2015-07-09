package cn.ingenic.glasssync.screen;

import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.util.Log;
import android.content.res.Configuration;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import cn.ingenic.glasssync.DefaultSyncManager;

import cn.ingenic.glasssync.screen.screenshare.ScreenModule;
import cn.ingenic.glasssync.screen.control.ScreenControlView;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class ScreenShareControlActivity extends Activity {
        private static final String TAG = "ScreenShareControlActivity";
        private ScreenModule mScreenModule;
        private RelativeLayout mContainer;
        private ScreenControlView mScreenControlView; 
        public static ImageView mImageView;
        public static int mScreenWidth, mScreenHeight;
        private TextView mSign,mLeft,mRight,mTop,mBottom;
        private TextView mFailedTransportView;
        private DefaultSyncManager mManager;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothDevice mDevice;
        private Handler mGlassReceive;
        private boolean mHandlerExit = false;
        private float fontSize = 0.0f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_share_controller);
		
		mContainer = (RelativeLayout)findViewById(R.id.container);
		mScreenControlView = new ScreenControlView(this);
		mScreenControlView.setTextViewVisibility(false);
		mContainer.addView(mScreenControlView);

		mScreenWidth = getWindowManager().getDefaultDisplay().getWidth();
		mScreenHeight = getWindowManager().getDefaultDisplay().getHeight();
		Log.v(TAG, "mScreenWidth:" + mScreenWidth + " mScreenHeight:" + mScreenHeight);
		if (mScreenWidth >= 1800) {
		        fontSize = 25.0f;
		}else{
		        fontSize = 14.0f;
		}

		mImageView = (ImageView)findViewById(R.id.screen_share_surface_view);
		mImageView.setVisibility(View.VISIBLE); 

		Log.v(TAG, "on create in");
		mScreenModule = ScreenModule.getInstance(this);
		mScreenModule.setImageView(mImageView);


		mFailedTransportView = (TextView)findViewById(R.id.screen_trans_failed);
		mFailedTransportView.setVisibility(View.INVISIBLE); 
		// mFailedTransportView.setText(R.string.screen_transport_failed); 
		// mFailedTransportView.setTextSize(fontSize); 

		controlView();
	}

        private void controlView(){
	        mSign = (TextView)findViewById(R.id.sign);
		mLeft = (TextView)findViewById(R.id.left);		  
		mRight = (TextView)findViewById(R.id.right);
		mTop = (TextView)findViewById(R.id.top);
		mBottom = (TextView)findViewById(R.id.bottom);
		AlphaAnimation alp = new AlphaAnimation(1.0f,0.0f);
		alp.setDuration(3000);
		mSign.setAnimation(alp);
		mLeft.setAnimation(alp);
		mRight.setAnimation(alp);
		mTop.setAnimation(alp);
		mBottom.setAnimation(alp);
		
		alp.setAnimationListener(new AnimationListener(){       
		        @Override
		        public void onAnimationStart(Animation animation) {
		        }

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override   
		        public void onAnimationEnd(Animation animation) {
			        mSign.setVisibility(View.INVISIBLE);
				mLeft.setVisibility(View.INVISIBLE);
				mRight.setVisibility(View.INVISIBLE);
				mTop.setVisibility(View.INVISIBLE);
				mBottom.setVisibility(View.INVISIBLE);
				if ((mManager == null) || (!mManager.isConnect())) {
				       mFailedTransportView.setVisibility(View.VISIBLE); 
				}

			}
		});
	}
    
    	@Override
	protected void onStop() {
		super.onStop();
		Log.v(TAG, "onStop");
		mHandlerExit = true;
		mScreenModule.sendRequestData(false);
	}

    public void onConfigurationChanged(Configuration newConfig) {
	Log.e(TAG, "onConfigurationChanged orientation="+newConfig.orientation);
	Log.e(TAG, "creenWidthDp="+newConfig.screenWidthDp+"/"+newConfig.screenHeightDp);

	mScreenModule.onConfigurationChanged(newConfig);
	mScreenControlView.onConfigurationChanged(newConfig);
	super.onConfigurationChanged(newConfig);  
    }  
    	@Override
	protected void onStart() {
		super.onStart();
		Log.v(TAG, "onStart");

		mHandlerExit = false;
		boolean btEnabled = checkBTEnabled();

		if (btEnabled) {
		        String bindAddress = mManager.getLockedAddress();
			mDevice = mBluetoothAdapter.getRemoteDevice(bindAddress);
			String bindName = mDevice.getName();
			
			if (bindName.equals("IGlass")) {
			        Log.v(TAG, "screen share unsupported for IGlass devices");
				mHandlerExit = true;
				mImageView.setVisibility(View.INVISIBLE); 
				mFailedTransportView.setVisibility(View.VISIBLE); 
			}else{
			        Log.v(TAG, "Bluetooth connected, ready for transport");
				mScreenModule.sendRequestData(true);
			}
		}

		mGlassReceive = new Handler();
		mGlassReceive.postDelayed(new GlassReceive(), 1000);
	}

    	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "onDestroy");
		mScreenModule.sendRequestData(false);
	}


        private boolean checkBTEnabled() {
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
	        if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())) {
		        Log.v(TAG, "BluetoothAdapter is null or unenable, screen share failed");
			return false;
		}
		
		mManager = DefaultSyncManager.getDefault();
		if (mManager != null) {
		        Log.v(TAG, "mManager.isConnect() = " + mManager.isConnect());
		}

		if ((mManager == null) || (!mManager.isConnect()))  {
		        Log.v(TAG, "unused bluetooth, screen share failed");
			return false;
		}
		return true;
	}

        class GlassReceive implements Runnable {
	        public void run() {
			if (mHandlerExit) return;

			boolean btStatus = mManager.isConnect();
			if (btStatus) {
			    if (mFailedTransportView.getVisibility() == View.VISIBLE) {
				        Log.v(TAG, "-------send transdata signal");
					mScreenModule.sendRequestData(true);
					mImageView.setVisibility(View.VISIBLE); 
					
					  //clear last view
					mImageView.setImageDrawable(null); 

					mFailedTransportView.setVisibility(View.INVISIBLE); 
					
				}
			}else{ //bluetooth unenabled
			    mImageView.setVisibility(View.INVISIBLE); 
			    mFailedTransportView.setVisibility(View.VISIBLE); 
			}
			mGlassReceive.postDelayed(new GlassReceive(), 1000);
		}
	}
}
