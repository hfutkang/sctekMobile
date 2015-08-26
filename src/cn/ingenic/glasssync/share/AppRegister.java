package cn.ingenic.glasssync.share;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class AppRegister extends BroadcastReceiver {
    private static final String TAG = "AppRegister";
    private boolean DEBUG = true;
	@Override
	public void onReceive(Context context, Intent intent) {
		final IWXAPI api = WXAPIFactory.createWXAPI(context, null);
		if(DEBUG) Log.e(TAG, "---------------onreceive registerapp-");
		api.registerApp(Constants.APP_ID);
	}
}
