package cn.ingenic.glasssync.devicemanager;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.WindowManager;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.data.Projo;

/** @author dfdun<br>
 * watch receive the data , and make some action depended on the data received 
 * */
public class DeviceTransaction extends Transaction {
	
	private Vibrator mVibrator;
    private boolean mShouldVibrat;
    private Ringtone mRingtone;
    private AlertDialog mDialog;
    private int mVolume,mModel; // original volume,ring model

    @Override
	public void onStart(ArrayList<Projo> datas) {
		super.onStart(datas);
		for(Projo data : datas){
				handleCommandOnPhone(data);
		}
	}
	
	private void handleCommandOnPhone(Projo projo){
		int cmd = (Integer)projo.get(DeviceColumn.command);
		String data = (String)projo.get(DeviceColumn.data);
		handleCommandOnPhone(cmd, data);
	}
	
	protected void handleCommandOnPhone(int cmd, String data){
		String res = "";
		klilog.i("phone - handle command: "+cmd+", data = "+data);
		switch(cmd){
		case Commands.CMD_INTERNET_REQUEST:
			requestInternet(cmd, data);
			break;
			
		case Commands.CMD_LOCK_SCREEN:
			res = lockScreen();
			ConnectionManager.getInstance().device2Device(cmd, res);
			break;

		// case Commands.CMD_CLEAR_CALL_LOG:
		// 	DeviceModule.getInstance().getCallLogManager().watchClearFinished(Integer.valueOf(data));
		// 	break;
			
		// case Commands.CMD_SYNC_CALL_LOG:
		// 	int syncRes = Integer.valueOf(data);
		// 	DeviceModule.getInstance().getCallLogManager().watchSyncFinished(syncRes);
		// 	break;
			
		// case Commands.CMD_SYNC_CALL_LOG_REQUEST:
		// 	DeviceModule.getInstance().getCallLogManager().syncIgnoreMode();
		// 	break;
			
		// case Commands.CMD_SYNC_WATCH_ON_CLEAR:
		// 	DeviceModule.getInstance().getCallLogManager().reset();
		// 	DeviceModule.getInstance().getCallLogManager().sync();
		// 	break;
			
		case Commands.CMD_REQUEST_BATTERY:
			BatteryInfoManager.getInstance().sendBatteryInfo();
			break;
		case Commands.CMD_RING_AND_VIBRAT:
            if ("stop".equals(data)) {
                stopRingAndVibrat();
                if(mDialog!=null&&mDialog.isShowing()){
                    mDialog.hide();
                }
            } else {
                RingAndVibrat();
            }
		    break;
		case Commands.CMD_GLASS_UNBIND:
		    ConnectionManager.getInstance().device2Apps(mContext, cmd, data);
		    break;
		}
	}
	
	private void RingAndVibrat(){
	    if(mShouldVibrat){//if vibrat is running return.
	        return;
	    }
        mShouldVibrat=true;
	    if(mVibrator==null)
	        mVibrator= (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
	    new Thread(){
            public void run(){
                while (mShouldVibrat) {
                    mVibrator.vibrate(1000);
                    android.os.SystemClock.sleep(2000);
                }
	        }
	    }.start();
	    
	    AudioManager am=(AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
	    mVolume = am.getStreamVolume(AudioManager.STREAM_RING);
	    mModel = am.getRingerMode();
	    am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
	    if(mRingtone==null)
	        mRingtone=RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI);
	    new Thread(){
	        public void run(){
	            mRingtone.play();
	        }
	    }.start();
	    
	    DialogInterface.OnClickListener lis=new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==DialogInterface.BUTTON_POSITIVE){
                    stopRingAndVibrat();
                    ConnectionManager.getInstance(mContext).device2Device(Commands.CMD_RING_AND_VIBRAT, "ring_find_it");
                }                
            }};
        if (mDialog == null) {
            String deviceName = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                    DefaultSyncManager.getDefault().getLockedAddress())
                    .getName();
            mDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.remote_ring_title)
                    .setMessage(mContext.getString(R.string.remote_ring_msg, deviceName))
                    .setPositiveButton(R.string.ok, lis)
                    .create();
            mDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
	    try{
	        mDialog.show();
	    }catch(Exception e){
	        klilog.e(e.toString());
	    }
	    klilog.i("RingAndVibrat() ...");
	}
	
	private void stopRingAndVibrat(){
        if (!mShouldVibrat) {
            return;
        }
        mShouldVibrat=false;
        if(mRingtone.isPlaying()) mRingtone.stop();
        ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(2, mVolume, 0);
        ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setRingerMode(mModel);
        klilog.i("stopRingAndVibrat() ...");
	}
	
	private String lockScreen(){
		try{
			DevicePolicyManager devicePolicyManager = (DevicePolicyManager)mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
			devicePolicyManager.lockNow();
			return "true";
		}catch(Exception e){
			klilog.e(e.toString());
			Intent intent = new Intent(mContext, AddDeviceAdminActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
			return "false";
		}
	}
	
	private void requestInternet(final int cmd, final String url){
		new Thread(){

			@Override
			public void run() {
				super.run();
				klilog.i("Internet request: " + url);
				HttpGet get = new HttpGet(url);
				String result = null;
				HttpClient mHttpClient = new DefaultHttpClient();
				try {
					HttpResponse response = mHttpClient.execute(get);
					if(response.getStatusLine().getStatusCode() == 200){
						result = EntityUtils.toString(response.getEntity(), "UTF-8");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				ConnectionManager.getInstance().device2Device(cmd, result);
			}
			
		}.start();
		
	}
}
