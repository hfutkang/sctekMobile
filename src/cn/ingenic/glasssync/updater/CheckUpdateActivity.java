package cn.ingenic.glasssync.updater;

import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.updater.VersionListView.OnVersionCheckedListener;

/** the main activity for OTA update;
 *  UI to check update , and  UI list all version lists . 
 *  @author dfdun &  kli
 * */
public class CheckUpdateActivity extends Activity implements OnClickListener, OnVersionCheckedListener {
	private final static int MSG_SYNC_START = 1;
	private final static int MSG_SYNC_FINISHED = 2;
//	private final static int MSG_CHECK_UPDATE = 3;
//	private final static int MSG_CHECK_ROLLBACK = 4;
	private final static int MSG_GET_VERSION_LIST = 5;
	private final static int MSG_GET_WATCH_INFO=6;
	
	private final static int DELAY_TIME = 30000;
	
	private ProgressDialog mWaitingDialog;
	private UpdateManager mManager;
	private boolean mUpdate;
	private String mSelectedVersion;
	private Button mCheckButton;
	private Button mSelectButton;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SYNC_START:     // 1
				showCheckingDialog();
				Message sync_msg = mHandler.obtainMessage(MSG_SYNC_FINISHED);
				sync_msg.obj = msg.obj;
				mManager.sync(sync_msg);
				break;
			case MSG_SYNC_FINISHED:  // 2
				hideCheckingDialog();
				onSyncFinished(msg);
				break;
			case MSG_GET_VERSION_LIST:   // 5
				List<String> list = mManager.getVersionListBaseCurrent();
				showVersionList(list);
				break;
			case MSG_GET_WATCH_INFO: // 6
		        mCheckButton.setText(R.string.get_watch_failed);
		        Toast.makeText(CheckUpdateActivity.this, R.string.get_watch_failed, 1).show();
			    break;
			}
		}
	};

    private void showCheckingDialog() {
        if (mWaitingDialog == null) {
            mWaitingDialog = new ProgressDialog(CheckUpdateActivity.this);
            mWaitingDialog.setMessage(getResources().getString(
                    R.string.checking));
            mWaitingDialog.setCancelable(false);
        }
        if (!mWaitingDialog.isShowing())
            mWaitingDialog.show();
    }

	private void hideCheckingDialog(){
		if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
			mWaitingDialog.dismiss();
			mWaitingDialog = null;
		}
	}
	
	protected void onDestroy(){
        mHandler.removeMessages(MSG_GET_WATCH_INFO);
        hideCheckingDialog();
	    super.onDestroy();
	}
	
	private void showVersionList(List<String> list){
		VersionListView versionList = (VersionListView)findViewById(R.id.version_list);
		versionList.setVersionList(list);
		versionList.setOnVersionCheckedListener(this);
		ViewGroup deviceInfo = (ViewGroup)findViewById(R.id.device_info);
		ViewGroup versionInfo = (ViewGroup)findViewById(R.id.version_info);
        if (android.os.Build.VERSION.SDK_INT < 14) {
            deviceInfo.setVisibility(View.GONE);
            versionInfo.setVisibility(View.VISIBLE);
        } else {
            flipit(deviceInfo, versionInfo);
        }
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_update_check);
		mManager = UpdateManager.getInstance(this);
		mCheckButton = (Button)findViewById(R.id.btn_check_now);
		mCheckButton.setOnClickListener(this);
		mSelectButton = (Button)findViewById(R.id.btn_version_selected);
		mSelectButton.setOnClickListener(this);
		mCheckButton.setText(R.string.get_watch_info);
		mCheckButton.setEnabled(false);
		new AsyncTaskGetInfo().execute("");
		mHandler.sendEmptyMessageDelayed(MSG_GET_WATCH_INFO, DELAY_TIME);
	}

	@SuppressWarnings("unchecked")
    class AsyncTaskGetInfo extends android.os.AsyncTask{

        @Override
        protected void onPostExecute(Object result) {
            String r[] = (String[]) result;
            setupDeviceInfomation(r[0],r[1]);
        }

        @Override
        protected Object doInBackground(Object... params) {
            String r[]={mManager.getModel(),mManager.getCurrentVersion()};
            return r;
        }
	    
	}
	private void setupDeviceInfomation(String model,String version){
		TextView product = (TextView)findViewById(R.id.tv_product);
		TextView android_version = (TextView)findViewById(R.id.tv_android_version);
		TextView system_version = (TextView)findViewById(R.id.tv_system_version);
//		set device infomation which get from bluetooth
		product.setText("unknown".equals(model)?" ":model);
		android_version.setText(" ");
        system_version.setText("unknown".equals(version)?" ":version);
        if (!"unknown".equals(model)) {
            mCheckButton.setText(R.string.check_now);
            mCheckButton.setEnabled(true);
            mHandler.removeMessages(MSG_GET_WATCH_INFO);
        }
	}

	private void onSyncFinished(Message msg){
		switch (msg.arg1) {
		case UpdateManager.SYNC_SUCCESS:
			((Message)msg.obj).sendToTarget();
			break;
		case UpdateManager.SYNC_FAIL:
			Toast.makeText(CheckUpdateActivity.this,
					R.string.sync_failed, Toast.LENGTH_SHORT).show();
			break;
		}
	}

	private void translateToDownload(int mode, UpdateInfo info){
		Bundle bundle = new Bundle();
		bundle.putInt(UpdateDownloadActivity.EXTRAS_MODE, mode);
		bundle.putParcelable(UpdateDownloadActivity.EXTRAS_VERSION, info);
		Intent intent = new Intent(CheckUpdateActivity.this,
				UpdateDownloadActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_check_now:
		    //check bluetooth connection state
//            if (DefaultSyncManager.getDefault().getState() != DefaultSyncManager.CONNECTED) {
//                Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
//                break;
//            }
            // check whether the update infomation is already downloaded .
            UpdateInfo upin = UpdateUtils.getUpdateInfoCache(this);
            if (upin != null) { // use downloaded update infomation 
                startActivity(new Intent(this, UpdateInstallActivity.class));
            } else { // check via network
                Message msg = new Message();
                msg.what = MSG_SYNC_START;
                msg.obj = mHandler.obtainMessage(MSG_GET_VERSION_LIST);
                mHandler.sendMessage(msg);
            }
			break;
		case R.id.btn_version_selected:
			int mode = mUpdate ? UpdateDownloadActivity.MODE_UPDATE : UpdateDownloadActivity.MODE_ROLLBACK;
			translateToDownload(mode, mManager.getUpdateInfoTo(mSelectedVersion));
			break;
		}
	}

    private void flipit(final ViewGroup from, final ViewGroup to) {
        Interpolator accelerator = new AccelerateInterpolator();
        Interpolator decelerator = new DecelerateInterpolator();
        ObjectAnimator visToInvis = ObjectAnimator.ofFloat(from, "rotationY", 0f, 90f);
        visToInvis.setDuration(500);
        visToInvis.setInterpolator(accelerator);
        final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(to, "rotationY",
                -90f, 0f);
        invisToVis.setDuration(500);
        invisToVis.setInterpolator(decelerator);
        visToInvis.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                from.setVisibility(View.GONE);
                invisToVis.start();
                to.setVisibility(View.VISIBLE);
            }
        });
        visToInvis.start();
    }

	@Override
	public void OnVersionChanged(String version, boolean update) {
		mSelectedVersion = version;
		mUpdate = update;
		int res = update ? R.string.update_to_version : R.string.rollback_to_version;
		mSelectButton.setEnabled(true);
		mSelectButton.setText(getString(res, mSelectedVersion));
	}
}
