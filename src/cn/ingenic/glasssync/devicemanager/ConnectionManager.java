package cn.ingenic.glasssync.devicemanager;

import java.util.ArrayList;
import java.util.EnumSet;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoType;

public class ConnectionManager {
	private static ConnectionManager sInstance;
	private Context mContext;
	
	private Handler mHandleCallback = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.arg2){
			case Commands.CMD_SYNC_CALL_LOG:
			case Commands.CMD_SYNC_CALL_LOG_REQUEST:
			case Commands.CMD_CLEAR_CALL_LOG:
				klilog.i("msg callback, cmd:"+msg.arg2+", res:"+msg.arg1);
				if(msg.arg1 != DefaultSyncManager.SUCCESS){
					DeviceModule.getInstance().getTransation().handleCommandOnPhone(msg.arg2, 2+""); //notify failed
				}
				break;
			}
		}
		
	};
	
	private ConnectionManager(Context context){
		mContext = context;
	}
	
	public static ConnectionManager getInstance(Context context){
		if(sInstance == null){
			sInstance = new ConnectionManager(context);
		}
		return sInstance;
	}
	
	public static ConnectionManager getInstance(){
		return sInstance;
	}
	
	public void device2Device(final int cmd, String data) {
		klilog.i("device 2 device; cmd:" + cmd + ", data:" + data);
		Projo projo = new DefaultProjo(EnumSet.allOf(DeviceColumn.class),
				ProjoType.DATA);
		projo.put(DeviceColumn.command, cmd);
		projo.put(DeviceColumn.data, data);
		Config config = new Config(DeviceModule.MODULE, Commands.getCmdFeature(cmd));
		config.mCallback = mHandleCallback.obtainMessage(1);
		config.mCallback.arg2 = cmd;
		ArrayList<Projo> datas = new ArrayList<Projo>(1);
		datas.add(projo);
		DefaultSyncManager.getDefault().request(config, datas);

	}
	
	public void apps2Device(int cmd, String data) {
		// request
		klilog.i("request: cmd: " + cmd + "; data: " + data);
		device2Device(cmd, data);
	}
	
	/*
	public void sendFile(File sendFile){
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
        FileInputStream fis;
        try {
            fis = new FileInputStream(sendFile);
            manager.sendFile(DeviceModule.MODULE, sendFile.getName(), (int)sendFile.length(), fis);
        } catch (FileNotFoundException e) {
        	klilog.e("file send error");
        }
	}
	
	public void sendFile(InputStream is, String name){
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
        try {
        	int length = is.available();
            manager.sendFile(DeviceModule.MODULE, name, length, is);
        } catch (FileNotFoundException e) {
			e.printStackTrace();
        	klilog.e("file send error");
        } catch (IOException e) {
			e.printStackTrace();
        	klilog.e("file send error");
		}
	}*/
	
	public static void device2Apps(Context context, int cmd, String data){
		Intent intent = new Intent();
		intent.setAction(Commands.getCmdAction(cmd));
		intent.putExtra("cmd", cmd);
		intent.putExtra("data", data);
		context.sendBroadcast(intent);
		klilog.i("broadcast send. cmd:"+cmd+", data:"+data);
	}
}
