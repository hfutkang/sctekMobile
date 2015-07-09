package cn.ingenic.glasssync.appmanager;

import java.util.HashMap;
import java.util.Map;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.appmanager.PhoneCommon.SimpleBase;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class ApplicationManagerActivity2 extends ActionBarActivity {
	private final boolean DEBUG=true;
	private final String APP="ApplicationManager";

	private ViewPager mViewPager;
	private ActionBar mActionBar;
	private MessageSender mSender;
	private AllApplicationFragment mAllApplicationFragment;
	private InstallApplicationFragment mInstallApplicationFragment;
	private ConnectHandler mConnectHandler;
	private long DELAY_TIME=30*1000;
	
//	private AppCache mAppCache;

	private interface TabStates {
		public final static int INSTALL_APP = 0;
		public final static int ALL_APP = 1;
		public final static int COUNT = 2;
	}
	
	private interface ConnectMsg{
		public final static int CONNECT=0;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.application_manager);

		mViewPager = (ViewPager) findViewById(R.id.tab_pager);
		initTab();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowHomeEnabled(false);
		mViewPager.setAdapter(new AppTabAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new AppPagerChangeListener());
		mViewPager.setCurrentItem(TabStates.INSTALL_APP);
		
		mSender=MessageSender.getInstance(this);

		
	}
	
	public void initData(){
		mSender.getAppInfos(this);
		startConnect();
	}
	

	private void initTab() {
		mActionBar = this.getSupportActionBar();
		Tab installTab = mActionBar.newTab();
		installTab.setText(R.string.install_app_tab);
		installTab.setContentDescription(R.string.install_app_tab_description);
		installTab.setTabListener(mTabListener);
		mActionBar.addTab(installTab);
		Tab allTab = mActionBar.newTab();
		allTab.setText(R.string.all_app_tab);
		allTab.setContentDescription(R.string.all_app_tab_description);
		allTab.setTabListener(mTabListener);
		mActionBar.addTab(allTab);
		startConnect();
	}
	
	private void startConnect(){
		HandlerThread thread=new HandlerThread("app_connect_msg");
		thread.start();
		mConnectHandler=new ConnectHandler(thread.getLooper());
		mConnectHandler.sendEmptyMessageDelayed(ConnectMsg.CONNECT, DELAY_TIME);
	}
	
	private class ConnectHandler extends Handler{
		public ConnectHandler(Looper looper){
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case ConnectMsg.CONNECT:
				mSender.sendConnectMessage();
				mConnectHandler.sendEmptyMessageDelayed(ConnectMsg.CONNECT, DELAY_TIME);
				break;
			}
		}
		
		
	}

	private TabListener mTabListener = new TabListener() {


		@Override
		public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (mViewPager != null)
				mViewPager.setCurrentItem(tab.getPosition());
			
			
		}

		@Override
		public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		}

	};

	private class AppPagerChangeListener implements OnPageChangeListener {

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			if(DEBUG)Log.i(APP,"ApplicationManagerActivity2 onPageSelected position is :"+position);
			getSupportActionBar().selectTab(getSupportActionBar().getTabAt(position));
			AppCache appCache=AppCache.getInstance();
			if (position == PhoneCommon.SimpleBase.ALL) {
				if (appCache.getAllList() == null
						|| appCache.getAllList().size() == 0)
					mSender.getAppInfos(position);
			} else if (position == PhoneCommon.SimpleBase.INSTALL) {
				if (appCache.getInstallList() == null
						|| appCache.getInstallList().size() == 0)
					mSender.getAppInfos(position);
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}
		

	}

	private class AppTabAdapter extends FragmentPagerAdapter {
		
		android.support.v4.app.FragmentTransaction ft;

		public AppTabAdapter(FragmentManager fm) {
			super(fm);
			ft=fm.beginTransaction();
		}

		@Override
		public Fragment getItem(int position) {
			Fragment f=null;
			if (position == TabStates.ALL_APP) {
				mAllApplicationFragment=new AllApplicationFragment();
//				ft.add(R.id.tab_pager, f, "ALL_APP");
				ft.show(mAllApplicationFragment);
				return mAllApplicationFragment;
			} else if (position == TabStates.INSTALL_APP) {
				mInstallApplicationFragment=new InstallApplicationFragment();
//				ft.add(R.id.tab_pager, f, "INSTALL_APP");
				ft.show(mInstallApplicationFragment);
				return mInstallApplicationFragment;
			}
			Log.e(APP,"no Fragment position !!!!! +postions is :"+position);
			return f;
		}

		@Override
		public int getCount() {
			return TabStates.COUNT;
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		FragmentManager fm=getSupportFragmentManager();
		FragmentTransaction ft=fm.beginTransaction();
		ft.hide(mAllApplicationFragment);
		ft.hide(mInstallApplicationFragment);
		mConnectHandler.removeMessages(ConnectMsg.CONNECT);
	}
	
	

	

	

	
}
