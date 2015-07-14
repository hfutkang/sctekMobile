package cn.ingenic.glasssync;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.LinkedList; 

import org.xmlpull.v1.XmlPullParserException;

import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.utils.HanLangCmdChannel;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;
import cn.ingenic.glasssync.appmanager.AppManagerModule;
import cn.ingenic.glasssync.ime.ImeSyncModule;
import cn.ingenic.glasssync.calendar.CalendarModule;
import cn.ingenic.glasssync.contactslite.ContactsLiteModule;
import cn.ingenic.glasssync.devicemanager.DeviceModule;
import cn.ingenic.glasssync.phone.PhoneModule;
import cn.ingenic.glasssync.screen.control.ScreenControlModule;
import cn.ingenic.glasssync.screen.screenshare.ScreenModule;
import cn.ingenic.glasssync.screen.live.LiveModule;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.sms.SmsModule;
import cn.ingenic.glasssync.smssend.SMSSendModule;
import cn.ingenic.glasssync.updater.UpdaterModule;
import cn.ingenic.glasssync.utils.internal.XmlUtils;
import cn.ingenic.glasssync.multimedia.MultiMediaManager;
import cn.ingenic.glasssync.lbs.GlassSyncLbsManager;
import cn.ingenic.glasssync.share.ShareModule;
import cn.ingenic.glasssync.share.VoiceShareModule;
import cn.ingenic.glasssync.weather.PositionModule;
import cn.ingenic.glasssync.weather.HTTPGetModule;
import cn.ingenic.glasssync.lbs.GlassSyncLbsModule;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import android.widget.Toast;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import java.util.List;

public class SyncApp extends Application implements
		Enviroment.EnviromentCallback {
	
	public static final String SHARED_FILE_NAME = "Install";
	private List<Activity> mActivityList = new LinkedList<Activity>();
	public static SyncApp mInstance;
	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
		if (LogTag.V) {
			Log.d(LogTag.APP, "Sync App created.");
		}
		String processName =getProcessName(this, android.os.Process.myPid());
		if (processName != null) {
		    boolean defaultProcess = processName.equals("cn.ingenic.glasssync");
		    if (!defaultProcess) 
			return;		     
		}
		Enviroment.init(this);
		DefaultSyncManager manager = DefaultSyncManager.init(this);
		SystemModule systemModule = new SystemModule();

		//share
		ShareModule sharemodule=ShareModule.getInstance(this);
		VoiceShareModule.getInstance(this);

		  //for get position by baiduSDK
		// PositionModule positionModule=PositionModule.getInstance(this);
		// HTTPGetModule httpgetModule=HTTPGetModule.getInstance(this);
		// GlassSyncLbsModule.getInstance(this);


		if (manager.registModule(systemModule)) {
		 	Log.i(LogTag.APP, "SystemModule is registed.");
		 }

		DeviceModule dm = DeviceModule.getInstance();
	        if (manager.registModule(dm)) {
		    Log.i(LogTag.APP, "DeviceModule  registed");
		}

		SyncModule contactLite = ContactsLiteModule.getInstance(this);
		contactLite.getMidTableManager().startObserve();
		
		// app manager
		AppManagerModule.getInstance(this);
		
		// //sms
		// SmsModule module=SmsModule.getInstance(this);
                // SMSSendModule sendModule = SMSSendModule.getInstance(this);
		// module.getMidTableManager().startObserve();
		
		//screen controller
		// ScreenControlModule.getInstance(this);
		// ScreenModule.getInstance(this);

		LiveModule.getInstance(this);
		
		// ImeSyncModule.getInstance(this);
		if (android.os.Build.VERSION.SDK_INT >= 14) {
		    //CalendarModule calm = new CalendarModule();
			// if (manager.registModule(calm)) {
			// 	Log.i(LogTag.APP, "CalendarModule registed");
			// }
		}

		XmlResourceParser parser = getResources().getXml(R.xml.modules);
		try {
			XmlUtils.beginDocument(parser, "modules");
			loadModules(parser);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			parser.close();
		}
		// Intent intent = new Intent(
		// DefaultSyncManager.RECEIVER_ACTION_SYNC_SERVICE_COMPLETE);
		// sendBroadcast(intent);


		    MultiMediaManager mmmg = MultiMediaManager.getInstance(this);
		    // GlassSyncLbsManager gslbs = GlassSyncLbsManager.getInstance(this);

		    GlassDetect gdt = GlassDetect.getInstance(this);
		    
	//init hanlang cmd channel
		    HanLangCmdChannel.getInstance(this);
		    
	}

	private void loadModules(XmlResourceParser parser) {
		if (parser != null) {
			while (true) {
				try {
					XmlUtils.nextElement(parser);
					if (!"module".equals(parser.getName())) {
						break;
					}
					registModule(parser);
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void registModule(XmlResourceParser parser) {
		String className = parser.getAttributeValue(null, "class");
		try {
			Class c = Class.forName(className);
			Constructor constructor = c.getConstructor(Context.class);
			constructor.newInstance(this);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	@Override
	public Enviroment createEnviroment() {
		return new PhoneEnviroment(this);
	}
        private String getProcessName(Context cxt, int pid) {
	    ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
	    List<RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
	    if (runningApps == null) {
		return null;
	    }
	    for (RunningAppProcessInfo procInfo : runningApps) {
		if (procInfo.pid == pid) {
		    return procInfo.processName;
		}
	    }
	    return null;
	}

	public static SyncApp getInstance(){
		return mInstance;
	}
	public void addActivity(Activity activity) { 
		mActivityList.add(activity); 
	}

	public void removeActivity(Activity activity) { 
		mActivityList.remove(activity); 
	}
	public void exitAllActivity() { 
            try { 
		    for (Activity activity:mActivityList) { 
			    if (activity != null) 
				    activity.finish(); 
		    } 
	    } catch (Exception e) { 
		    e.printStackTrace(); 
	    } finally { 
		    // System.exit(0); 
	    } 
	}
    
}
