package cn.ingenic.glasssync.screen.live;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.lang.ref.WeakReference;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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


    private static native final void native_init();
    private native final void native_setup(Object rtspclient_this);
    private native final void native_start(String url);
    private native final void native_close();
    private native final void native_release();
    private native final void native_set_surface(Surface surfaace);
    

    public RtspClient() {
	Looper looper;
	if ((looper = Looper.myLooper()) != null) {
	    mEventHandler = new EventHandler(this, looper);
	} else if ((looper = Looper.getMainLooper()) != null) {
	    mEventHandler = new EventHandler(this, looper);
	} else {
	    mEventHandler = null;
	}

	native_setup(new WeakReference<RtspClient>(this));
    }

    public void start(String url) {
	Log.e(TAG, "start");
	native_start(url);
    }

    public void close() {
	Log.e(TAG, "close");
	reset();
	native_close();
	release();
    }

    public void release() {
    	Log.e(TAG, "release");
    	native_release();
    }

    public void reset() {
	Log.e(TAG, "reset");
        mEventHandler.removeCallbacksAndMessages(null);
    }

    public void setSurface(SurfaceHolder holder) {
    	Log.e(TAG, "setSurface");
	native_set_surface(holder.getSurface());
    }

    public interface OnRtspClientListener {
	public void onData(RtspClient rc, int what, int extra);
	public void onVideoSizeChanged(int width, int height);
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
	
	private static final int NATIVE_MSG_NOTIFY_EVENT_ERROR = 0;
	private static final int NATIVE_MSG_NOTIFY_VIDEO_SIZE = 1;
	private static final int NATIVE_MSG_NOTIFY_SET_LISTENER_DONE = 2;
	private static final int NATIVE_MSG_NOTIFY_DECODER_DONE = 3;
	
	
	@Override
	public void handleMessage(Message msg) {
	    switch(msg.what) {
	    case NATIVE_MSG_NOTIFY_EVENT_ERROR:
		break;

	    case NATIVE_MSG_NOTIFY_VIDEO_SIZE:
		Log.e(TAG,"msg.arg1=" + msg.arg1 + " msg.arg2" + msg.arg2);
		if (mOnRtspClientListener != null) 
		    mOnRtspClientListener.onVideoSizeChanged(msg.arg1, msg.arg2);
		break;
	    case NATIVE_MSG_NOTIFY_DECODER_DONE:
		Log.e(TAG, "NATIVE_MSG_NOTIFY_DECODER_DONE");
		if (mOnRtspClientListener != null)
		    mOnRtspClientListener.onData(mRtspClient, msg.arg1, msg.arg2);
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