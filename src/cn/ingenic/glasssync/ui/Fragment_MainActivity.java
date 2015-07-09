package cn.ingenic.glasssync.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.screen.Fragment_Screen;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import cn.ingenic.glasssync.ui.BindGlassActivity;
import com.tencent.mm.sdk.platformtools.Log;
import cn.ingenic.glasssync.lbs.GlassSyncLbsManager;
public class Fragment_MainActivity extends FragmentActivity {
	private final String TAG="Fragment_MainActivity";

        protected final static int MESSAGE_UNBIND = 2;

	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what == MESSAGE_UNBIND){
			    Intent i = new Intent(mContext,BindGlassActivity.class);
			    startActivity(i);
			    finish();
			}
		}
	};

        protected Handler getHandler(){
	    return mHandler;
        }
	private PagerSlidingTabStrip tabs;
	private ViewPager pager;
	private MyPagerAdapter adapter;

	private Drawable oldBackground = null;
	private int currentColor = 0xFF09bfff;
    private String mBondBTAddress="";
    private Context mContext;
    public static Fragment_MainActivity sMainActivity = null;
    
    private final BroadcastReceiver mBindStateReceiver = new BroadcastReceiver() {
    	    @Override
    		public void onReceive(Context context, Intent intent) {
    		if(intent.getAction().equals(DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED)){
    		    Log.d(TAG, "---receive RECEIVER_ACTION_DISCONNECTED");
    		    mHandler.sendEmptyMessageDelayed(MESSAGE_UNBIND, 0);
    		}
    	    }
    	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sMainActivity = this;
		mContext = this;
		mBondBTAddress = getIntent().getStringExtra("BondMACAddress");
		// Log.d(TAG, "onCreat");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.fragment_menu1);

		tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		pager = (ViewPager) findViewById(R.id.pager);
		adapter = new MyPagerAdapter(getSupportFragmentManager());

		pager.setAdapter(adapter);

		final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
				.getDisplayMetrics());
		pager.setPageMargin(pageMargin);

		tabs.setIndicatorColor(currentColor);
		tabs.setIndicatorHeight(4);
		tabs.setViewPager(pager);
	        GlassSyncLbsManager.getInstance(this);
		  /*listen broadcast from DefaultSyncManager*/
		IntentFilter filter = new IntentFilter();
		filter.addAction(DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED);
		registerReceiver(mBindStateReceiver, filter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    //getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mBindStateReceiver);
		sMainActivity = null;
		Log.e("Fragment_MainActivity","---onDestroy in");
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		//switch (item.getItemId()) {

//		case R.id.action_contact:
//			QuickContactFragment dialog = new QuickContactFragment();
//			dialog.show(getSupportFragmentManager(), "QuickContactFragment");
//			return true;
//
//		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentColor", currentColor);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		currentColor = savedInstanceState.getInt("currentColor");
		//changeColor(currentColor);
	}

	private Drawable.Callback drawableCallback = new Drawable.Callback() {
		@Override
		public void invalidateDrawable(Drawable who) {
			getActionBar().setBackgroundDrawable(who);
		}

		@Override
		public void scheduleDrawable(Drawable who, Runnable what, long when) {
			mHandler.postAtTime(what, when);
		}

		@Override
		public void unscheduleDrawable(Drawable who, Runnable what) {
			mHandler.removeCallbacks(what);
		}
	};

	public class MyPagerAdapter extends FragmentPagerAdapter {

	    private final String[] TITLES = {getString(R.string.title_bind), getString(R.string.title_sync),
	    				     getString(R.string.title_media),getString(R.string.title_screen)};

	    public MyPagerAdapter(FragmentManager fm) {
		super(fm);
	    }

	    @Override
		public CharSequence getPageTitle(int position) {
		return TITLES[position];
	    }

	    @Override
		public int getCount() {
		return TITLES.length;
	    }

	    @Override
	    public Fragment getItem(int position) {
		if(position == 0) return new Fragment_Bind();	
		else if(position == 1) return new Fragment_Setting();
		else if(position == 2) return new Fragment_Media();
		else if(position == 3) return new Fragment_Screen();
		else return null;
	    }

	}

}