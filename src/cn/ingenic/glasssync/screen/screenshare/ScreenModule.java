package cn.ingenic.glasssync.screen.screenshare;

import android.os.RemoteException;
import android.os.Environment;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.widget.ImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;


public class ScreenModule extends SyncModule {
    private static final String TAG = "MobileScreenModule";
    private static final Boolean DEBUG = false;
    private static final Boolean TIMER = false;

    private static final String SCREEN_NAME = "screen_module";
    private static final String SCREEN_SHARE = "screen_share";
    private static final String TRANSPORT_SCREEN_CMD = "screen_cmd";
    private static final String GET_SCREEN_CMD = "get_screen_cmd";    
    private static final String SEND_FRAME = "video_stream";
    private static final String END_FRAME = "end_stream";
    private static final String FRAME_WIDTH = "video_width";
    private static final String FRAME_HEIGHT = "video_height";
    private static final String FRAME_LENGTH = "first_frame_length";
    
    private static final int TRANSPORT_DATA_READY = 0;
    private static final int TRANSPORT_DATA_IN = 1;
    private static final int TRANSPORT_DATA_FINISH = 2;
    
    private static ScreenModule sInstance;
    private int frameNum = 0;
    private int mWidth = 320, mHeight=240;
    private long start = 0;
    private Context mContext;
    private Bitmap mBitmap;
    private Bitmap mBitmapRotation;
    private ImageView mImageView;
    private boolean mRotation = false;

    private ScreenModule(Context context) {
	super(SCREEN_NAME, context);
	mContext = context;
    }
    
    public static ScreenModule getInstance(Context c) {
	if (null == sInstance) {
	    sInstance = new ScreenModule(c);
	}
	return sInstance;
    }
    
    public void setImageView(ImageView imageView){
	mImageView = imageView;
	mRotation = false;
    }

    public void onConfigurationChanged(Configuration newConfig) {
    	if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
    	    mRotation = true;
    	}else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
    	    mRotation = false;
    	}
    }

    @Override
    protected void onCreate() {
    }
    
    @Override
    protected void onConnectionStateChanged(boolean connect) {
	Log.v(TAG, "onConnectionStateChanged connect = " + connect);
    }


    @Override
    protected void onRetrive(SyncData data) {
	if(DEBUG) Log.e(TAG, "---Mobile onRetrive");
	boolean isSend = false;
	boolean end = false;
	int choice = data.getInt(SCREEN_SHARE);
	if (DEBUG) Log.v(TAG, "choice = " + choice);
	switch (choice) {
	case TRANSPORT_DATA_READY:
	    if (DEBUG) Log.v(TAG, "TRANSPORT_DATA_READY");
	    isSend = data.getBoolean(GET_SCREEN_CMD, false);
	    if (isSend) {
		mWidth = data.getInt(FRAME_WIDTH);
		mHeight = data.getInt(FRAME_HEIGHT); 
		mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
		mBitmapRotation = Bitmap.createBitmap(mHeight, mWidth, Bitmap.Config.RGB_565);
		Log.e(TAG, "width:" + mWidth + " height:"+mHeight);
	    }
	    break;
	case TRANSPORT_DATA_IN:
	    if (DEBUG) Log.v(TAG, "TRANSPORT_DATA_IN frameNum = " + frameNum);
	    if (DEBUG) Log.v(TAG, "current time: " + System.currentTimeMillis());
	    int firstFrameLen = data.getInt(FRAME_LENGTH);
	    try {
		byte[] frame = data.getByteArray(SEND_FRAME);
		if (frameNum == 0) {
		    if (DEBUG) Log.v(TAG, "PPS SPS");
		    frameNum++;
		    byte[] frame1 = new byte[firstFrameLen]; 
		    System.arraycopy(frame, 0, frame1, 0, firstFrameLen);
		    nativeDecodeInit(frame1, frame1.length);

		    byte[] frame2 = new byte[frame.length - firstFrameLen]; 
		    System.arraycopy(frame, firstFrameLen, frame2, 0, frame.length - firstFrameLen);
		    if(mRotation){
			nativeShowFrameBitmap(mBitmapRotation, frame2, frame2.length,true);
			mImageView.setImageBitmap(mBitmapRotation);
		    }else{
			nativeShowFrameBitmap(mBitmap, frame2, frame2.length,false);
			mImageView.setImageBitmap(mBitmap);
		    }
		}else {
		    //need to divide frame
		    byte[] frame1 = new byte[firstFrameLen]; 
		    System.arraycopy(frame, 0, frame1, 0, firstFrameLen);
		    
		    if (DEBUG) frameNum++;
		    if (TIMER) 
			start = System.currentTimeMillis();

		    if(mRotation){
			nativeShowFrameBitmap(mBitmapRotation, frame1, frame1.length,true);
			mImageView.setImageBitmap(mBitmapRotation);
		    }else{
			nativeShowFrameBitmap(mBitmap, frame1, frame1.length,false);
			mImageView.setImageBitmap(mBitmap);
		    }
		    if (TIMER) {
			long elapsed = System.currentTimeMillis() - start;
			Log.v(TAG, "ShowVideoFrame need time " + elapsed + "ms");
		    }
		    
		    Thread.sleep(20);

		    byte[] frame2 = new byte[frame.length - firstFrameLen]; 
		    System.arraycopy(frame, firstFrameLen, frame2, 0, frame.length - firstFrameLen);
		    
		    if (TIMER) 
			start = System.currentTimeMillis();
		    
		    if(mRotation){
			nativeShowFrameBitmap(mBitmapRotation, frame2, frame2.length,true);
			mImageView.setImageBitmap(mBitmapRotation);
		    }else{
			nativeShowFrameBitmap(mBitmap, frame2, frame2.length,false);
			mImageView.setImageBitmap(mBitmap);
		    }
		    if (TIMER) {
			long elapsed1 = System.currentTimeMillis() - start;
			Log.v(TAG, "ShowVideoFrame need time " + elapsed1 + "ms");
		    }
		}
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    break;
	case TRANSPORT_DATA_FINISH:
	    end = data.getBoolean(END_FRAME, false);
	    if (DEBUG) Log.v(TAG, "TRANSPORT_DATA_FINISH end = " + end);
	    break;
	default:
	    break;
	}
	if(DEBUG) Log.v(TAG, "---Mobile onRetrive end");
    }

    static {
	System.loadLibrary("ffmpeg");
    }

    static native boolean nativeDecodeInit(byte[] frame, int len);
    static native boolean nativeShowFrameBitmap(Bitmap bitmap, byte[] frame, int len, boolean needRotation);


    public void sendRequestData(boolean bool) {
	SyncData data = new SyncData();
	if (DEBUG) Log.v(TAG, "sendRequestData");
	data.putInt(SCREEN_SHARE, 1);
	data.putBoolean(TRANSPORT_SCREEN_CMD, bool);
	if (DEBUG) Log.v(TAG, "bool = " + bool);
	try {
	    if (DEBUG) Log.v(TAG, "---send data " + bool);
	    sendCMD(data);
	} catch (SyncException e) {
	    Log.v(TAG, "---send cmd sync failed:" + e);
	}
    }
}
