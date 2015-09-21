package cn.ingenic.glasssync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import com.sctek.smartglasses.ui.BindHanlangActivity;
import com.sctek.smartglasses.ui.MainActivity;
import android.view.View;
import android.view.View.OnTouchListener;
import android.content.Context;
import android.util.Log;
import android.app.Dialog;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.provider.Settings;
// import cn.ingenic.glasssync.ui.BindGlassActivity;
import com.sctek.smartglasses.ui.BindGlassActivity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.widget.Toast;
import cn.ingenic.glasssync.SyncApp;
import cn.ingenic.glasssync.ui.Fragment_MainActivity;
import cn.ingenic.glasssync.utils.MyDialog;
public class WelcomeActivity extends Activity {
	private Editor mEditor;
	private static final int REQUEST_ENABLE_BT = 0;
	private BluetoothAdapter mAdapter;
        private boolean mFirst = true;
	private final String TAG = "WelcomeActivity";
        private Dialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome_activity);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBluetoothReceiver, filter);
		mAdapter = BluetoothAdapter.getDefaultAdapter();

		if(mAdapter.isEnabled()==false){
		    mDialog = new MyDialog(this, R.style.MyDialog,getApplication().getResources().getString(R.string.dialog_title), getApplication().getResources().getString(R.string.dialog_ok),getApplication().getResources().getString(R.string.dialog_cancle),new MyDialog.LeaveMeetingDialogListener() {
			    @Override
				public void onClick(View view) {
				// TODO Auto-generated method stub
				Log.d(TAG, "Dialog=====click" + view.getId()
				      + "dialog_tv_cancel_two"
				      + R.id.dialog_tv_cancel_two
				      + "dialog_tv_ok" + R.id.dialog_tv_ok);
				switch (view.getId()) {
				case R.id.dialog_tv_ok:
				    mDialog.cancel();
				    finish();
				    break;
				case R.id.dialog_tv_cancel_two:
				    mDialog.cancel();
				    Intent intent =  new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);  
				    startActivity(intent);
				
				    break;
				    
				}
			    }
			});
		    mDialog.setCanceledOnTouchOutside(false);
		    mDialog.setCancelable(false);
		    mDialog.show();
		    return;
		}
		startActivity();
	}

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
		    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						   BluetoothAdapter.ERROR);
		    if (state == BluetoothAdapter.STATE_ON){
			startActivity();
		    }
		}
	    }
	};

	public DataInputStream Terminal(String command) throws Exception {
		Process process = Runtime.getRuntime().exec("su");
		OutputStream outstream = process.getOutputStream();
		DataOutputStream DOPS = new DataOutputStream(outstream);
		InputStream instream = process.getInputStream();
		DataInputStream DIPS = new DataInputStream(instream);
		String temp = command + "\n";
		DOPS.writeBytes(temp);
		DOPS.flush();
		DOPS.writeBytes("exit\n");
		DOPS.flush();
		process.waitFor();
		return DIPS;
	}

	private void startActivity() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
			    DefaultSyncManager mManager=DefaultSyncManager.getDefault();

			    if (!mManager.getLockedAddress().equals("")) {
					Intent intent = new Intent(WelcomeActivity.this,
							MainActivity.class);
					startActivity(intent);
					finish();
				} else {
					Intent intent = new Intent(WelcomeActivity.this,
							BindHanlangActivity.class);
					startActivity(intent);
					finish();
				}

			}
		}, 2000);
	}
    @Override
	protected void onDestroy() {
	super.onDestroy();
       	    unregisterReceiver(mBluetoothReceiver);
    }
    //When the user returns from the Setting , the Bluetooth is off  will exit the application
    @Override
    	protected void onResume() {
    	super.onResume();
    	if(mFirst){ 
    	    mFirst = false;
    	    return;
    	}
    	if(mAdapter.isEnabled()==false){
    	    Toast.makeText(this, R.string.bluetooth_off,
    			   Toast.LENGTH_SHORT).show();
    	    finish();
    	}
}
}
