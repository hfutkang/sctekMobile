package cn.ingenic.glasssync.share;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.os.Environment;
import android.util.Log;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.sinaapi.WBAuthAndShareActivity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import cn.ingenic.glasssync.R;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXAppExtendObject;
import com.tencent.mm.sdk.openapi.WXEmojiObject;
import com.tencent.mm.sdk.openapi.WXImageObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXMusicObject;
import com.tencent.mm.sdk.openapi.WXTextObject;
import com.tencent.mm.sdk.openapi.WXVideoObject;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tencent.mm.sdk.openapi.SendAuth;

import cn.ingenic.glasssync.multimedia.MultiMediaModule;
import cn.ingenic.glasssync.wxapi.WXEntryActivity;

public class VoiceShareModule extends SyncModule {
	private static final String TAG = "VoiceShareModule";
	private boolean DEBUG = true;

	private static final String MODULE_NAME = "vshare_module";
	private final String SHARE_TYPE_VENDOR = "vendor";
	private final String SHARE_TYPE_CONTENT = "content";

	private final String WEIBO_VENDOR_SINA = "SINA";
	private final String WEIBO_VENDOR_WECHAT = "WECHAT";

	private Context mContext;

	private IWXAPI mWXAPI;

	private static VoiceShareModule sInstance;

	public static VoiceShareModule getInstance(Context c) {
		if (sInstance == null)
			sInstance = new VoiceShareModule(c);
		return sInstance;
	}

	public VoiceShareModule(Context context) {
		super(MODULE_NAME, context);
		mContext = context;
	}

	@Override
	protected void onCreate() {
		if (DEBUG)
			Log.e(TAG, "onCreate in");
	}

	@Override
	protected void onRetrive(SyncData data) {
		if (DEBUG)
			Log.e(TAG, "---onRetrive");

		String vendor = data.getString(SHARE_TYPE_VENDOR);
		String content = data.getString(SHARE_TYPE_CONTENT);
		if (DEBUG)
			Log.d(TAG, "vendor:" + vendor + " content:" + content);
		if (WEIBO_VENDOR_SINA.equals(vendor))
			shareToSina(content);
		else if (WEIBO_VENDOR_WECHAT.equals(vendor))
			shareToWeixin(content);
	}

	private void shareToSina(String content) {
		if (DEBUG)
			Log.d(TAG, "shareToSina " + content);
		Intent intent = new Intent(mContext, WBAuthAndShareActivity.class);
		intent.putExtra("fromGlassSync", true);
		intent.putExtra(WBAuthAndShareActivity.KEY_SHARE_FROM_GLASSSYNC, true);
		intent.putExtra(WBAuthAndShareActivity.KEY_SHARE_CONTENT_TEXT, content);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	private void shareToWeixin(String content) {
		if (DEBUG)
			Log.d(TAG, "shareToWeixin " + content);
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW");
		intent.setData(Uri.parse("sdksample://www"));
		intent.putExtra("fromGlassSync", true);
		intent.putExtra("text_data", content);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		mContext.startActivity(intent);
	}
}