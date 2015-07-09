package cn.ingenic.glasssync.screen.control;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class ScreenControlModule extends SyncModule {
	private static final String TAG = "ScreenControlModule";
	public static final Boolean VDBG = true;

	private static final String SCREENCONTROL_NAME = "scnctrl_module";
	private static final String GESTURE_CMD = "gesture_cmd";

	public static final int GESTURE_SINGLE_TAP = 0;
	public static final int GESTURE_DOUBLE_TAP = 1;
	public static final int GESTURE_LONG_PRESS = 2;
	public static final int GESTURE_SLIDE_UP = 3;
	public static final int GESTURE_SLIDE_DOWN = 4;
	public static final int GESTURE_SLIDE_LEFT = 5;
	public static final int GESTURE_SLIDE_RIGHT = 6;
	
	private static ScreenControlModule sInstance;

	private ScreenControlModule(Context context) {
		super(SCREENCONTROL_NAME, context);
	}

	public static ScreenControlModule getInstance(Context c) {
		if (null == sInstance) {
			sInstance = new ScreenControlModule(c);
		}
		return sInstance;
	}

	@Override
	protected void onCreate() {
	}

	private String getGestureStr(int gesture) {
		switch (gesture) {
		case GESTURE_SINGLE_TAP:
			return "tap";
		case GESTURE_DOUBLE_TAP:
			return "double";
		case GESTURE_LONG_PRESS:
			return "long";
		case GESTURE_SLIDE_UP:
			return "up";
		case GESTURE_SLIDE_DOWN:
			return "down";
		case GESTURE_SLIDE_LEFT:
			return "left";
		case GESTURE_SLIDE_RIGHT:
			return "right";
		default:
			break;
		}
		return null;
	}
	
	public void sendGestureData(int gesture) {
		SyncData data = new SyncData();
		data.putInt(GESTURE_CMD, gesture);
		try {
		        if (VDBG)
			    Log.e(TAG, "---send data " + getGestureStr(gesture));
			sendCMD(data);
		} catch (SyncException e) {
			Log.e(TAG, "---send file sync failed:" + e);
		}
	}
}
