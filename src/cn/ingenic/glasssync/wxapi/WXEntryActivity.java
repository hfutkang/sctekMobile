package cn.ingenic.glasssync.wxapi;

import java.io.File;

import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.ShowMessageFromWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXAppExtendObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXTextObject;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXImageObject;

//import net.sourceforge.simcpux.R;
//import net.sourceforge.simcpux.Util;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.share.Constants;
import cn.ingenic.glasssync.share.Util;
public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
	private static final String TAG = "WXEntryActivity";
	private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
	private static final int THUMB_SIZE = 150;	
      //private Button gotoBtn, regBtn, launchBtn, checkBtn;
	
	// IWXAPI �ǵ���app��΢��ͨ�ŵ�openapi�ӿ�
    private IWXAPI api;
    public static int GSMMD_PIC = 0x1;
    public static int GSMMD_VIDEO = 0x2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	Log.d(TAG,"---------onCreate in---");	    
	if(getIntent().getBooleanExtra("fromGlassSync",false) == false)
	    finish();

	api = WXAPIFactory.createWXAPI(this, Constants.APP_ID,false);
	Boolean flag = api.registerApp(Constants.APP_ID);
	api.handleIntent(getIntent(), this);
	  //Log.d(TAG,"----------0000000-------register result="+flag);
	  //setContentView(R.layout.weixin);
	sendToWX();
	finish();
    }
    private void sendToWX() {
	int fileType = getIntent().getIntExtra("type",0);
	if(fileType == GSMMD_PIC){
	    //send pic   

	    String path = getIntent().getStringExtra("path");
	    
	    File file = new File(path); //file must exist.
	    if (!file.exists()){ 
		Log.d(TAG,"----------pic not exist!----path="+path);
		return;
	    }

	    Log.d(TAG,"----------pic -path="+path);

	    WXImageObject imgObj = new WXImageObject();
	    imgObj.setImagePath(path);
	
	    WXMediaMessage msg = new WXMediaMessage();
	    msg.mediaObject = imgObj;
	
	    Bitmap bmp = BitmapFactory.decodeFile(path);
	    Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
	    bmp.recycle();
	    msg.thumbData = Util.bmpToByteArray(thumbBmp, true);
	
	    SendMessageToWX.Req req = new SendMessageToWX.Req();
	    req.transaction = buildTransaction("img");
	    req.message = msg;
	    req.scene = SendMessageToWX.Req.WXSceneTimeline;
	    api.sendReq(req);
	
	}else if(fileType == GSMMD_VIDEO){
	    //send video 
	    String path = getIntent().getStringExtra("path");
	}else{	
	    //send text
	    String text = getIntent().getStringExtra("text_data");
	    Log.d(TAG,"----------0000000--------text="+text);	    
	    WXTextObject textObj = new WXTextObject();
	    textObj.text = text;
	    WXMediaMessage msg = new WXMediaMessage();
	    msg.mediaObject = textObj;
	    msg.description = text;
	    
	    SendMessageToWX.Req req = new SendMessageToWX.Req();
	    req.transaction = buildTransaction("text"); 
	    req.message = msg;	
	    req.scene = SendMessageToWX.Req.WXSceneTimeline;//friends circle
	    api.sendReq(req);
	}
	Log.d(TAG,"----------^_^---");	    
    }

    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);
        
        // Í¨¹ýWXAPIFactory¹¤³§£¬»ñÈ¡IWXAPIµÄÊµÀý
    	api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);

    	regBtn = (Button) findViewById(R.id.reg_btn);
    	regBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// ½«¸Ãapp×¢²áµ½Î¢ÐÅ
			    api.registerApp(Constants.APP_ID);    	
			}
		});
    	
        gotoBtn = (Button) findViewById(R.id.goto_send_btn);
        gotoBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        startActivity(new Intent(WXEntryActivity.this, SendToWXActivity.class));
		        finish();
			}
		});
        
        launchBtn = (Button) findViewById(R.id.launch_wx_btn);
        launchBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Toast.makeText(WXEntryActivity.this, "launch result = " + api.openWXApp(), Toast.LENGTH_LONG).show();
			}
		});
        
        checkBtn = (Button) findViewById(R.id.check_timeline_supported_btn);
        checkBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int wxSdkVersion = api.getWXAppSupportAPI();
				if (wxSdkVersion >= TIMELINE_SUPPORTED_VERSION) {
					Toast.makeText(WXEntryActivity.this, "wxSdkVersion = " + Integer.toHexString(wxSdkVersion) + "\ntimeline supported", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(WXEntryActivity.this, "wxSdkVersion = " + Integer.toHexString(wxSdkVersion) + "\ntimeline not supported", Toast.LENGTH_LONG).show();
				}
			}
		});
        
        api.handleIntent(getIntent(), this);
    }

    */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		  Log.d(TAG,"----------onNewIntent--------");
		setIntent(intent);
        api.handleIntent(intent, this);
	}
	// ΢�ŷ������󵽵���Ӧ��ʱ����ص����÷���
	@Override
	public void onReq(BaseReq basereq) {
		  Log.d(TAG,"----------onReq--------"+basereq.getType());
		switch (basereq.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
		      //goToGetMsg();		
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
		      //goToShowMsg((ShowMessageFromWX.Req) req);
			break;
		default:
			break;
		}

	}

	// ����Ӧ�÷��͵�΢�ŵ�����������Ӧ����ص����÷���
	@Override
	public void onResp(BaseResp resp) {
		int result = 0;
		Log.d(TAG,"----------onResp--------"+resp.errCode);
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
		      result = R.string.errcode_success;
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
		      result = R.string.errcode_cancel;
			break;
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
		      result = R.string.errcode_deny;
			break;
		default:
		      result = R.string.errcode_unknown;
			break;
		}
		Log.d(TAG,"----------onResp--------result="+result);
		Toast.makeText(this, result, Toast.LENGTH_LONG).show();
	}
	
    private String buildTransaction(final String type) {
	return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    @Override
    public void onStart() {
        super.onStart();
	Log.d(TAG,"----------onStart");
    } 
   @Override
    public void onDestroy() {
        super.onDestroy();
	Log.d(TAG,"----------onDestroy");
    }

}
