package cn.ingenic.glasssync.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.ui.BindGlassActivity;

public class QRCodeActivity extends Activity{
    private final String TAG="QRCodeActivity";
    private static final boolean DEBUG = true;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private String mAddress;
    private DefaultSyncManager mManager;
    private BluetoothAdapter mAdapter;
    private BluetoothSocket mSocket;
    private BluetoothServerSocket mServerSocket;
    private Thread mServerThread;
    private boolean mIsConnect;
    private String mMsg;
    private String mMsg_bind = "00:00:00:00:00:00";
    final String SPP_UUID = "3385a711-ced3-470d-a1df-1e88ac06c29f";
    final String SDP_UUID = "4219ffae-0c05-4c5f-8f30-bc43a58f7f1d";
    private Intent intent;
    //private RelativeLayout other_bind;
    
    private FrameLayout mFrameLayout;
    private TextView mShowBtTextView;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	if(DEBUG) Log.d(TAG, "onCreate in");
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	setContentView(R.layout.fragment_item_bind);
	mManager = DefaultSyncManager.getDefault();
	mAdapter = BluetoothAdapter.getDefaultAdapter();
	//mFrameLayout = (FrameLayout)findViewById(R.id.root);

	TextView tv_sacnMessage = (TextView) findViewById(R.id.tv_sacnMessage);
	TextView tv_bindAddress = (TextView) findViewById(R.id.tv_bindAddress);
	TextView tv_bindMac = (TextView) findViewById(R.id.tv_bindMac);
	//TextView bind_glass = (TextView) findViewById(R.id.bind_glass);
	TextView tv_exit = (TextView) findViewById(R.id.tv_exit);
	//other_bind = (RelativeLayout) findViewById(R.id.other);
	//other_bind.setOnClickListener(this);
	List<TextView> list = new ArrayList<TextView>();
	//list.add(bind_glass);
	list.add(tv_bindAddress);
	list.add(tv_exit);
	list.add(tv_sacnMessage);
	Typeface(list);

	IntentFilter filter = new IntentFilter();
	filter.addAction(BluetoothDevice.ACTION_FOUND);
	filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
	filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
	filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
	filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
	registerReceiver(mBluetoothReceiver, filter);
	// View view = LayoutInflater.from(this).inflate(R.layout.fragment_item_bind_direct,null);
	// mShowBtTextView = (TextView)view.findViewById(R.id.tvDevices);
	// mFrameLayout.addView(view);
	// onClick_Search(null);
	connectByCode();
    }

    private void connectByCode() {
	mAddress = mAdapter.getAddress();
	mIsConnect = mManager.isConnect();

	String device_info = mAddress + "," + mIsConnect /*+ "," + mBind_Address*/;
	if(DEBUG) Log.d(TAG,"-**--device_info="+device_info);
	MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
	Map hints = new HashMap();
	hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
	BitMatrix bitMatrix = null;
	try {
	    bitMatrix = multiFormatWriter.encode(device_info,
						 BarcodeFormat.QR_CODE, 400, 400, hints);
	} catch (WriterException e) {
	}

	int width = bitMatrix.getWidth();
	int height = bitMatrix.getHeight();
	int[] pixels = new int[width * height];
	for (int y = 0; y < height; y++) {
	    int offset = y * width;
	    for (int x = 0; x < width; x++) {
		pixels[offset + x] = bitMatrix.get(x, y) ? BLACK : WHITE;
	    }
	}
	Bitmap bitmap = Bitmap.createBitmap(width, height,
					    Bitmap.Config.ARGB_8888);
	bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
	ImageView img = (ImageView) findViewById(R.id.image_view);
	img.setImageBitmap(bitmap);
    }

    @Override 
	public void onDestroy(){
	super.onDestroy();   
	if(DEBUG) Log.e(TAG,"---onDestroy");
	unregisterReceiver(mBluetoothReceiver);
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
	    @Override
		public void onReceive(Context context, Intent intent) {
		if(DEBUG) Log.d(TAG, "rcv " + intent.getAction());
		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
		    if(DEBUG) Log.d(TAG, "find ad bl devices");
		    BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (btDevice != null) {
		    	if(DEBUG)Log.d(TAG, "Name : " + btDevice.getName() + " Address: "+ btDevice.getAddress());
		    }
		} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
		    if(DEBUG) Log.d(TAG, "ACTION_BOND_STATE_CHANGED");
		    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (device != null) {
		    	if(DEBUG)Log.v(TAG, "Name : " + device.getName() + " Address: "+ device.getAddress());
		    	if(DEBUG)Log.v(TAG, "connectState:" + device.getBondState());
			if(device.getBondState() == BluetoothDevice.BOND_BONDED){
			    mManager.connect(device.getAddress());
			}
		    }
		} else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
		    if(DEBUG) Log.d(TAG, "ACTION_PAIRING_REQUEST");

		} else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE.equals(intent.getAction())) {
		    int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
						   DefaultSyncManager.IDLE);
		    boolean isConnect = (state == DefaultSyncManager.CONNECTED) ? true : false;
		    if(DEBUG) Log.d(TAG, isConnect + "    isConnect");
		    if (isConnect) {
			mManager.setLockedAddress(mManager.getLockedAddress());
			intent = new Intent(QRCodeActivity.this,Fragment_MainActivity.class);
			intent.putExtra("BondMACAddress",mManager.getLockedAddress());
			startActivity(intent);
			finish();
		    }
		}
	    }
	};

    public void Typeface(List<TextView> list) {
	Typeface typeface = Typeface.createFromAsset(this.getAssets(),
						     "fonts/iphone.ttf");
	for (TextView view : list) {
	    view.setTypeface(typeface);
	}
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_BACK) {
	    Intent intent=new Intent(QRCodeActivity.this,BindGlassActivity.class);
	    startActivity(intent);
	    finish();
	}
	return false;
    }

    // @Override
    // 	public void onClick(View v) {
    // 	switch (v.getId()) {
    // 	case R.id.other:
    // 	    Intent intent = new Intent(QRCodeActivity.this,
    // 				       BindGlassActivity.class);
    // 	    startActivity(intent);
    // 	    finish();
    // 	    break;
    // 	}
    // }
}
