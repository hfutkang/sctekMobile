package cn.ingenic.glasssync.appmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.appmanager.PhoneCommon.SimpleBase;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SimpleFragment extends Fragment implements OnItemLongClickListener {

	public final boolean DEBUG = true;
	public final String APP = "ApplicationManager";
	private View mView;
	protected GridView mGridView;
	private Context mContext;
	private TextView mEmpty;
	private Map<String, AppEntry> mMap = null;
	// public SimpleFragment(Context context){
	// this.mContext=context;
	// }

	private MySimpleAdapter mMySimpleAdapter;

	private ProgressDialog mProgressDialog;

	protected void showDialog() {
		mProgressDialog = new ProgressDialog(getActivity());
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setMessage(getString(R.string.download_message));
		// mProgressDialog.setCancelable(false);
		mProgressDialog.show();
	}
	
	public void showUnInstallDialog(){
		mProgressDialog = new ProgressDialog(getActivity());
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setMessage(getString(R.string.uninstall_toast));
		mProgressDialog.show();
	}
	
	protected void stopDialog() {
		if (mProgressDialog != null)
			mProgressDialog.hide();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	protected void setEmpty() {
		mEmpty.setVisibility(View.VISIBLE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.simple_application_manager, null);
		mGridView = (GridView) mView.findViewById(R.id.all_app);
		mEmpty = (TextView) mView.findViewById(R.id.empty);
		mGridView.setOnItemLongClickListener(this);
		mMySimpleAdapter = new MySimpleAdapter(getActivity(),
				new ArrayList<AppEntry>());
		mGridView.setAdapter(mMySimpleAdapter);

		return mView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mProgressDialog != null)
			mProgressDialog.dismiss();
	}

	protected void notifyChange(Map<String, AppEntry> map) {
		if (DEBUG)
			Log.i(APP, "Fragment notifyChange list size is :" + map.size());
		if (mEmpty.isShown()) {
			mEmpty.setVisibility(View.GONE);
		}
		mMap = map;
		mMySimpleAdapter.setMap(getList(map));
		mMySimpleAdapter.notifyDataSetChanged();

	}

	private ArrayList<AppEntry> getList(Map<String, AppEntry> map) {
		ArrayList<AppEntry> list = new ArrayList<AppEntry>();
		for (String pkgName : map.keySet()) {
			AppEntry ae = map.get(pkgName);
			list.add(ae);
		}
		return list;
	}

	protected class MySimpleAdapter extends BaseAdapter {

		private ArrayList<AppEntry> mmShowList;
		private LayoutInflater mInflater;

		public MySimpleAdapter(Context context, ArrayList<AppEntry> list) {
			getShowList(list);
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		private void getShowList(ArrayList<AppEntry> list){
			ArrayList<AppEntry> showList=new ArrayList<AppEntry>();
			for(AppEntry app:list){
				if (!app.getIsSystem()
						||app.getEnableSetting() != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
					showList.add(app);
				}
			}
			this.mmShowList=showList;
		}

		@Override
		public int getCount() {
			return mmShowList.size();
		}

		@Override
		public Object getItem(int position) {
			return mmShowList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public void setMap(ArrayList<AppEntry> list) {
			getShowList(list);
		}

		@Override
		public View getView(int position, View view, ViewGroup viewGroup) {
//			if (mmList == null || mmList.size() == 0)
//				return view;
			
			AppEntry appinfo = mmShowList.get(position);
			
//			if (appinfo.getIsSystem()
//					&& appinfo.getEnableSetting() == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
//				
//				return view;
//			}
			View v = mInflater.inflate(R.layout.application_item, null);
			ImageView imageView = (ImageView) v.findViewById(R.id.app_icon);
			TextView textView = (TextView) v.findViewById(R.id.app_name);

			

			

			Drawable icon = appinfo.getApplicationIcon(getActivity()
					.getResources());

			if (icon == null) {
				imageView.setBackgroundResource(R.drawable.ic_launcher);
			} else {
				imageView.setImageDrawable(icon);
			}

			textView.setText(appinfo.getApplicationName());
			return v;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		if (mMap == null)
			return false;
		AppEntry ae = getList(mMap).get(position);
		if (ae.getIsSystem()) {
			if (DEBUG)
				Log.i(APP, "system app can not unInstall !");
			return false;
		}
		showUnInstallDialog(ae);
		return false;
	}

	private void showUnInstallDialog(final AppEntry ae) {
		Builder mAlertDialog = new AlertDialog.Builder(getActivity());
		mAlertDialog.setTitle(R.string.uninstall_dialog_title);
		mAlertDialog.setMessage(getString(R.string.uninstall_dialog_message));
		mAlertDialog.setPositiveButton(R.string.uninstall_dialog_ok,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						sendUnInstallCommon(ae);
					}

				});
		mAlertDialog.setNegativeButton(R.string.uninstall_dialog_cancel,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		mAlertDialog.show();
	}

	private void sendUnInstallCommon(AppEntry ae) {
		String packageName = ae.getPackageName();
		MessageSender sender = MessageSender.getInstance(getActivity());
		sender.sendUnInstallMessage(packageName,this);
	}

}
