package cn.ingenic.glasssync.screen.control;

import cn.ingenic.glasssync.R;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.util.Log;

public class ScreenControlView extends RelativeLayout {
	private GestureDetector mGestureDetector;
        private TextView mTextView;
        private boolean mRotation = false;
        public ScreenControlView(Context context) {
	    this(context, null);
	}
    
	public ScreenControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = (LayoutInflater) context
		    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.screen_controller, this, true);
		mGestureDetector = new GestureDetector(context, new MySimpleOnGestureListener());
		Typeface typeface = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/iphone.ttf");
		mTextView = (TextView) findViewById(R.id.info);
		mTextView.setTypeface(typeface);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}

        public void setTextViewVisibility(boolean bool) {
 	        mTextView.setVisibility(bool ? View.VISIBLE : View.GONE);       
	}

        public void onConfigurationChanged(Configuration newConfig) {
	    if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
		mRotation = true;
	    }else{
		mRotation = false;
	    }
	}
	private void sendSyncData(int gesture) {
	    if(mRotation == true){
		if(gesture == ScreenControlModule.GESTURE_SLIDE_LEFT)
		    gesture = ScreenControlModule.GESTURE_SLIDE_DOWN;
		else if(gesture == ScreenControlModule.GESTURE_SLIDE_RIGHT)
		    gesture = ScreenControlModule.GESTURE_SLIDE_UP;
		else if(gesture == ScreenControlModule.GESTURE_SLIDE_UP)
		    gesture = ScreenControlModule.GESTURE_SLIDE_LEFT;
		else if(gesture == ScreenControlModule.GESTURE_SLIDE_DOWN)
		    gesture = ScreenControlModule.GESTURE_SLIDE_RIGHT;
	    }	
	    ScreenControlModule.getInstance(getContext()).sendGestureData(gesture);
	}

        private class MySimpleOnGestureListener extends SimpleOnGestureListener {
		
		private static final int MIN_QUICK_SLIDE_VELOCITY_X = 600;
		private static final int MIN_QUICK_SLIDE_VELOCITY_Y = 600;
		private static final int MIN_QUICK_SLIDE_DISTANCE_X = 50;
		private static final int MIN_QUICK_SLIDE_DISTANCE_Y = 15;
		
		// 双击的第二下Touch down时触发
		@Override
		public boolean onDoubleTap(MotionEvent e) {
		        sendSyncData(ScreenControlModule.GESTURE_DOUBLE_TAP);
			return true;
		}

		// Touch了滑动一点距离后，up时触发
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
		        if(((e1.getX() - e2.getX()) > MIN_QUICK_SLIDE_DISTANCE_X
			    && Math.abs(e1.getY() - e2.getY()) < Math.abs(e1.getX() - e2.getX())) 
			   || (velocityX < -MIN_QUICK_SLIDE_VELOCITY_X
			       && Math.abs(velocityY) < MIN_QUICK_SLIDE_VELOCITY_X)) {
			        sendSyncData(ScreenControlModule.GESTURE_SLIDE_LEFT);
			}else if(((e2.getX() - e1.getX()) > MIN_QUICK_SLIDE_DISTANCE_X
				  && Math.abs(e1.getY() - e2.getY()) < Math.abs(e1.getX() - e2.getX()))
				 || (velocityX > MIN_QUICK_SLIDE_VELOCITY_X
				     && Math.abs(velocityY) < MIN_QUICK_SLIDE_VELOCITY_X)) {
			        sendSyncData(ScreenControlModule.GESTURE_SLIDE_RIGHT);
			}else if(((e1.getY() - e2.getY()) > MIN_QUICK_SLIDE_DISTANCE_Y
				  && Math.abs(e1.getX() - e2.getX()) < Math.abs(e1.getY() - e2.getY())) 
				 || (velocityY < -MIN_QUICK_SLIDE_VELOCITY_Y
				     && Math.abs(velocityX) < MIN_QUICK_SLIDE_VELOCITY_Y)){
			        sendSyncData(ScreenControlModule.GESTURE_SLIDE_UP);
			}else if(((e2.getY() - e1.getY()) > MIN_QUICK_SLIDE_DISTANCE_Y
				  && Math.abs(e1.getX() - e2.getX()) < Math.abs(e1.getY() - e2.getY())) 
				 || (velocityY > MIN_QUICK_SLIDE_VELOCITY_Y
				     && Math.abs(velocityX) < MIN_QUICK_SLIDE_VELOCITY_Y)){
			        sendSyncData(ScreenControlModule.GESTURE_SLIDE_DOWN);
			}
			
			return true;
		}

		// Touch了不移动一直Touch down时触发
		@Override
		public void onLongPress(MotionEvent e) {
		        sendSyncData(ScreenControlModule.GESTURE_LONG_PRESS);
		}

		// Touch了滑动时触发
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
		        //
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
		        sendSyncData(ScreenControlModule.GESTURE_SINGLE_TAP);	       	
			return true;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return true;
		}
	}
}
