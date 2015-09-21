package cn.ingenic.glasssync.screen.live;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.content.Context;
import android.view.View;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.screen.LiveDisplayActivity;

public class RtspClient {

    static {
	System.loadLibrary("live_jni");
	native_init();
    }

    private static final String TAG = "RtspClient";
    private static final boolean DEBUG = true;
    
    private int mNativeContext = 0;
    private OnRtspClientListener mOnRtspClientListener = null;
    private EventHandler mEventHandler = null;
    private Context mContext = null;

    private static native final void native_init();
    private native final void native_setup(Object rtspclient_this);
    private native final void native_start(String url);
    private native final void native_close();
    private native final void native_release();
    private native final void native_set_surface(Surface surfaace);
    

    public RtspClient(Context context) {
	Looper looper;
	if ((looper = Looper.myLooper()) != null) {
	    mEventHandler = new EventHandler(this, looper);
	} else if ((looper = Looper.getMainLooper()) != null) {
	    mEventHandler = new EventHandler(this, looper);
	} else {
	    mEventHandler = null;
	}

	mContext = context;
	native_setup(new WeakReference<RtspClient>(this));
    }

    public void start(String url) {
	if (DEBUG) Log.e(TAG, "start");
	native_start(url);
    }

    public void close() {
	if (DEBUG) Log.e(TAG, "close");
	reset();
	native_close();
	release();
    }

    public void release() {
    	if (DEBUG) Log.e(TAG, "release");
    	native_release();
	mContext = null;
    }

    public void reset() {
	if (DEBUG) Log.e(TAG, "reset");
        mEventHandler.removeCallbacksAndMessages(null);
    }

    public void setSurface(SurfaceHolder holder) {
    	if (DEBUG) Log.e(TAG, "setSurface");
	native_set_surface(holder.getSurface());
    }

    public interface OnRtspClientListener {
	public void onVideoSizeChanged(int width, int height);
	public void onStreamDown();
	public void onStreamDisconnect();
    }
    
    public void setListener(OnRtspClientListener listener) {
	mOnRtspClientListener = listener;
    }

    private class EventHandler extends Handler {
	private RtspClient mRtspClient = null;

	public EventHandler(RtspClient rc, Looper looper) {
	    super(looper);
	    mRtspClient = rc;
	}
	
	private final int NATIVE_MSG_NOTIFY_FRAME_STATE = 0;
	private final int NATIVE_MSG_NOTIFY_STREAM_DOWN = 1;
	private final int NATIVE_MSG_NOTIFY_VIDEO_SIZE = 2;
	
	@Override
	public void handleMessage(Message msg) {
	    switch(msg.what) {
	    case NATIVE_MSG_NOTIFY_VIDEO_SIZE:
		if (DEBUG) Log.e(TAG, "NATIVE_MSG_NOTIFY_VIDEO_SIZE : " + msg.arg1 + "x" + msg.arg2);
		if (mOnRtspClientListener != null) 
		    mOnRtspClientListener.onVideoSizeChanged(msg.arg1, msg.arg2);
		break;

	    case NATIVE_MSG_NOTIFY_STREAM_DOWN:
		if (DEBUG) Log.e(TAG, "NATIVE_MSG_NOTIFY_STREAM_DOWN");
		if (mOnRtspClientListener != null)
		    mOnRtspClientListener.onStreamDown();
		break;

	    case NATIVE_MSG_NOTIFY_FRAME_STATE:
		if (DEBUG) Log.e(TAG, "NATIVE_MSG_NOTIFY_FRAME_STATE : " + msg.arg1);
		if (msg.arg1 == 0) { // FRAME_MISS
		    if (LiveDisplayActivity.mPD != null) {
			LiveDisplayActivity.mPD.setMessage(mContext.getString(R.string.live_wait_network_data));
			LiveDisplayActivity.mPD.show();
		    }
		} else if (msg.arg1 == 1) { // FRAME_GOT
		    if (LiveDisplayActivity.mPD != null)
			LiveDisplayActivity.mPD.dismiss();
		} else if (msg.arg1 == 2) { // FRAME_DISCONNECT
		    if (mOnRtspClientListener != null)
			mOnRtspClientListener.onStreamDisconnect();
		}
		break;

	    default:
		Log.e(TAG, "Unknown message type " + msg.what);
		break;
	    }
	}
    }
    
    private static void postEventFromNative(Object rtspclient_ref,
                                            int what, int arg1, int arg2, Object obj) {
    	Log.e(TAG, "postEventFromNative");
    	RtspClient rc = (RtspClient)((WeakReference)rtspclient_ref).get();
    	if (rc == null) {
    	    return;
    	}
	
    	if (rc.mEventHandler != null) {
    	    Message m = rc.mEventHandler.obtainMessage(what, arg1, arg2, obj);
    	    rc.mEventHandler.sendMessage(m);
    	}
    }
     
}