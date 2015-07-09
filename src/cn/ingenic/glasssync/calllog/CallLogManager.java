package cn.ingenic.glasssync.calllog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.devicemanager.Commands;
import cn.ingenic.glasssync.devicemanager.ConnectionManager;
import cn.ingenic.glasssync.devicemanager.klilog;

public class CallLogManager {
	
	private final static long TRANS_TIME = 20*1000;
	private final static long ONE_CALLLOG_TIME = 250;

	public final static boolean DEBUG = true;

	public final static String CALL_LOG_PREF = "calllogmanager";
	public final static String PREF_DATA_CHAOS = "datachaos";
	
	public final static int RES_SUCCESS = 1;
	public final static int RES_FAILED = 2;
	public final static int RES_FAILED_WITH_DATA_CHAOS = 3;
	public final static int RES_TIME_OUT = 4;
	   
    public final static int NOTIFICATION_ID = 0x123;
	public final static String TRANSLATE_FILE_NAME = "calllogdata";
	
	private final static int MSG_SYNC_START = 1;
	private final static int MSG_SYNC_FINISH = 2;
	private final static int MSG_CLEAR_FINISH = 5;
	private final static int MSG_LOAD_FILE = 3;
	private final static int MSG_LOAD_FINISH = 4;
	private final static int MSG_CLEAR_DATA = 6;
	private final static int MSG_CLEAR_CACHE = 7;
	private final static int MSG_CLEAR_TIME_OUT= 8;
	private final static int MSG_SYNC_TIME_OUT = 9;

	private static int THREAD_COUNT = 0;
	
	private static CallLogManager sInstance;
	private Context mContext;
	private CacheHelper mCacheHelper;
	private ContentObserver mObserver;
	private SyncTask mSyncTask;
	private ClearWatchDataTask mClearTask;
	private boolean mStoreSync = false;
	
	private boolean mCleared = false;
	
	private boolean mIgnoreMode = false;
	
	private SyncLock mWatchSyncRes = new SyncLock();
	private SyncLock mWatchClearRes = new SyncLock();
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case MSG_SYNC_START:
				startSync();
				break;
			case MSG_CLEAR_FINISH:
				onClearTaskFinsh(msg.arg1);
				break;
			case MSG_SYNC_FINISH:
				onSyncTaskFinsh(msg.arg1);
				break;
			case MSG_CLEAR_CACHE:
				mCacheHelper.clearTableCalls();
				break;
			case MSG_CLEAR_TIME_OUT:
				watchClearFinished(RES_TIME_OUT);
				break;
			case MSG_SYNC_TIME_OUT:
				watchSyncFinished(RES_TIME_OUT);
				break;
			}
		}
		
	};
	
	private class SyncLock{
		Integer value = 0;
	}
	
	private CallLogManager(Context context){
		mContext = context;
		boolean isWatch = DefaultSyncManager.isWatch();
		if(isWatch){
			
		}else{
			mCacheHelper = new CacheHelper(mContext);
			mObserver = new CallLogObserver(mHandler);
			mContext.getContentResolver().registerContentObserver(Calls.CONTENT_URI, true, mObserver);
		}
	}
	
	public static CallLogManager getInstance(Context context){
		if(sInstance == null){
			sInstance = new CallLogManager(context);
		}
		return sInstance;
	}
	
	public void watchSyncFinished(int res){
		mWatchSyncRes.value = res;
		synchronized(mWatchSyncRes){
			mWatchSyncRes.notifyAll();
			klilog.i("mWatchSyncRes notify");
		}
	}
	
	public void watchClearFinished(int res) {
		klilog.i("watchClearFinished");
		mWatchClearRes.value = res;
		synchronized(mWatchClearRes){
			mWatchClearRes.notifyAll();
			klilog.i("mWatchClearRes notify");
		}
	}
	
	/**
	 * Sync call log data.
	 */
	public void sync(){
		mHandler.sendEmptyMessage(MSG_SYNC_START);
	}
	
	public void syncIgnoreMode(){
		mIgnoreMode = true;
		sync();
	}
	
	/**
	 * Clear data of cache and remote
	 */
	public void reset(){
		klilog.i("call log reset");
		stopClearTask();
		stopSyncTask();
		mHandler.sendEmptyMessage(MSG_CLEAR_CACHE);
		mCleared = false;
		mStoreSync = false;
	}
	
	synchronized private void startSync() {
		klilog.i("Sync start! THREAD_COUNT:"+THREAD_COUNT);
		
		if(hasDataChaos()){
			klilog.i("Data chaos, reset and continue sync");
			reset();
		}
		
		//Has another sync msg. return.
		if (mStoreSync || mClearTask != null) {
			klilog.i("sync break, mStoreSync:"+mStoreSync+", clearing:"+(mClearTask != null));
			return;
		}

		//Task is exist. Store msg, return.
		if (mSyncTask != null) {
			klilog.i("sync break, Task is running and can not cancel.");
			mStoreSync = true;
			return;
		}
		
		if(needToClearWatchData()){
			mStoreSync = true;
			klilog.i("ClearWatchDataTask will start");
			mClearTask = new ClearWatchDataTask();
			mClearTask.execute("");
			THREAD_COUNT++;
		}else{
			//Start task.
			klilog.i("SyncTask will start");
			mSyncTask = new SyncTask();
			mSyncTask.execute(0);
			THREAD_COUNT++;
		}
		
	}

	private boolean needToClearWatchData(){
		klilog.i("needToClearWatchData  mCleared:"+mCleared);
		if(mCleared){
			return false;
		}
		
		SQLiteDatabase db = mCacheHelper.getReadableDatabase();
		boolean res = true;
		try {
			Cursor c = db.query(CacheHelper.TABLE_CALLS, new String[]{Calls._ID}, null, null, null, null, null);
			int count = c.getCount();
			res = count == 0;
			c.close();
		} catch (Exception e) {
			klilog.e("table calls not exist, reset.");
			db.close();
			reset();
		}
		db.close();
		return res;
	}

	synchronized private void onClearTaskFinsh(int res){
		klilog.i("finish clear res:"+res);
		klilog.i("Clear finish! Has store msg ? "+mStoreSync);
		mClearTask = null;
		mWatchClearRes.value = 0;
		switch(res){
		case RES_SUCCESS:
			setDataChaos(false);
			if(mStoreSync){
				mStoreSync = false;
				mCleared = true;
				sync();
			}
			break;
		case RES_FAILED:
			setDataChaos(false);
			mCleared = false;
			mStoreSync = false;
			break;
		case RES_FAILED_WITH_DATA_CHAOS:
			setDataChaos(true);
			mCleared = false;
			mStoreSync = false;
			break;
		}
	}
	
	synchronized private void onSyncTaskFinsh(int res){
		klilog.i("finish sync res:"+res);
		klilog.i("Sync finish! Has store msg ? "+mStoreSync);
		mSyncTask = null;
		mWatchSyncRes.value = 0;
		switch(res){
		case RES_SUCCESS:
		case RES_FAILED:
			setDataChaos(false);
			break;
		case RES_FAILED_WITH_DATA_CHAOS:
			setDataChaos(true);
			break;
		}
		
		if(mStoreSync){
			mStoreSync = false;
			sync();
		}
	}
	
	private boolean hasDataChaos(){
		SharedPreferences prefs = mContext.getSharedPreferences(CALL_LOG_PREF, Context.MODE_PRIVATE);
		return prefs.getBoolean(PREF_DATA_CHAOS, false);
	}
	
	private void setDataChaos(boolean res){
		SharedPreferences prefs = mContext.getSharedPreferences(CALL_LOG_PREF, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PREF_DATA_CHAOS, res);
		editor.commit();
	}
	
	private class ClearWatchDataTask extends AsyncTask<String, Integer, Integer>{

		private int res;

		@Override
		protected Integer doInBackground(String... arg0) {
			klilog.i("clear task running.");

			ConnectionManager.getInstance().device2Device(Commands.CMD_CLEAR_CALL_LOG, "all");
			
			if(mWatchClearRes.value == 0){
				synchronized(mWatchClearRes){
					try {
						klilog.i("wait for watch");
						mHandler.sendEmptyMessageDelayed(MSG_CLEAR_TIME_OUT, TRANS_TIME);
						mWatchClearRes.wait();
						if(mWatchClearRes.value != RES_TIME_OUT){
							klilog.i("remove time out message");
							mHandler.removeMessages(MSG_CLEAR_TIME_OUT);
						}
						if(this.isCancelled()){
							return mWatchClearRes.value;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			klilog.i("notified");
			switch(mWatchClearRes.value){
			case RES_SUCCESS:
				break;
			case RES_FAILED:
				break;
			case RES_FAILED_WITH_DATA_CHAOS:
				break;
			}
			res = mWatchClearRes.value;
			return res;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			THREAD_COUNT--;
			super.onPostExecute(result);
			klilog.i("onPostExecute");
			onTaskFinished(res);
		}

		@Override
		protected void onCancelled(Integer result) {
			super.onCancelled(result);
			klilog.i("onCancelled");
			onTaskFinished(res);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}
		
		private void onTaskFinished(int res){
			Message msg = mHandler.obtainMessage(MSG_CLEAR_FINISH);
			msg.arg1 = res;
			msg.sendToTarget();
		}

	}
	
	private class SyncTask extends AsyncTask<Integer, Integer, Integer>{
		
		private List<Integer> changes;
		private JSONObject changeData;

		private int res = RES_SUCCESS;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Integer... arg0) {

			klilog.i("Sync task preparing.");
			//1. Get changes index
			klilog.i("1. Get changes index start");
			if(this.isCancelled()) return RES_FAILED;
			long start = System.currentTimeMillis();
			ArrayList<Integer> id_new2read = new ArrayList<Integer>();
			HashMap<Integer, String> id_update_name = new HashMap<Integer, String>();
			try {
				changes = getChanges(id_new2read,id_update_name);
//				changes = filterChanges(changes);
			} catch (Exception e) {
				e.printStackTrace();
				klilog.e(e.toString());
				res = RES_FAILED;
				return res;
			}
			klilog.i("2.Get changes index finish. Spent:"+String.valueOf(System.currentTimeMillis() - start));
			//1. end
			
			//2.notify watch has new sync in SAVING_POWER_MODE
			/*
            if (DefaultSyncManager.getDefault().getCurrentMode() == DefaultSyncManager.SAVING_POWER_MODE) {
            	ConnectionManager.device2Device(Commands.CMD_SYNC_HAS_NEW_SYNC, ""+changes.size());
            }
            */

			//2. filter changes
			/*
			klilog.i("2. Filter changes start");
			start = System.currentTimeMillis();
			if(mIgnoreMode){
				mIgnoreMode = false;
			}else{
				changes = modeFilter(changes);
			}
			klilog.i("2. Filter changes finish. Spent:"+String.valueOf(System.currentTimeMillis() - start));
			*/
			//2. end
			
			if(changes.size() == 0 && id_update_name.size() == 0
					&& id_new2read.size() == 0 ){
				klilog.i("no changes, cancel sync task");
				return RES_SUCCESS;
			}
			
			//3. Get changes data
			if(this.isCancelled()) return RES_FAILED;
			klilog.i("3. Get changes data start.");
			start = System.currentTimeMillis();
            changeData = collectData(changes ,id_new2read,id_update_name);
			klilog.i("3. Get changes data finish. Spent:"+String.valueOf(System.currentTimeMillis() - start));
			//3. end
			
			//4. Send sync cmd
			if(this.isCancelled()) return RES_FAILED;
			klilog.i("4. Send sync cmd start.");
			start = System.currentTimeMillis();
			sendSyncCmd(changeData.toString());
			if(mWatchSyncRes.value == 0){
				synchronized(mWatchSyncRes){
					try {
						klilog.i("wait for watch");
						mHandler.sendEmptyMessageDelayed(MSG_SYNC_TIME_OUT, TRANS_TIME+ONE_CALLLOG_TIME*changes.size());
						mWatchSyncRes.wait();
						if(mWatchSyncRes.value != RES_TIME_OUT){
							klilog.i("remove time out message");
							mHandler.removeMessages(MSG_SYNC_TIME_OUT);
						}
						if(this.isCancelled()){
							return mWatchSyncRes.value;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			klilog.i("notified");
			klilog.i("4. Send sync cmd finish. Spent:"+String.valueOf(System.currentTimeMillis() - start));
			//4. end
			
			switch(mWatchSyncRes.value){
			case RES_SUCCESS:
				//5. update cache
				klilog.i("5. Update cache start.");
				start = System.currentTimeMillis();
				updateCache(changeData);
				klilog.i("5. Update cache finish. Spent:"+String.valueOf(System.currentTimeMillis() - start));
				//5. end
				break;
			case RES_FAILED:
				break;
			case RES_FAILED_WITH_DATA_CHAOS:
				break;
			}
			res = mWatchSyncRes.value;
			klilog.i("sync result:"+res);
			return res;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			THREAD_COUNT--;
			super.onPostExecute(result);
			klilog.i("onPostExecute");
			onTaskFinished(res);
		}

		@Override
		protected void onCancelled(Integer result) {
			super.onCancelled(result);
			klilog.i("onCancelled");
			onTaskFinished(res);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}
		
		private void onTaskFinished(int res){
			Message msg = mHandler.obtainMessage(MSG_SYNC_FINISH);
			msg.arg1 = res;
			msg.sendToTarget();
		}
	}

	
	private void sendSyncCmd(String data){
		ConnectionManager.getInstance().device2Device(Commands.CMD_SYNC_CALL_LOG, data);
	}
	
	private void updateCache(JSONObject data){
		SQLiteDatabase db = mCacheHelper.getWritableDatabase();
		try {
			JSONArray add = data.getJSONArray("add");
			JSONArray del = data.getJSONArray("del");
			JSONArray new2 = data.getJSONArray("new2read");
			JSONArray names = data.getJSONArray("update_name");
			db.beginTransaction();
			for (int i = 0; i < del.length(); i++) {
				db.delete(CacheHelper.TABLE_CALLS,
						Calls._ID + "=" + del.getInt(i), null);
			}
			for (int i = 0; i < add.length(); i++) {
				JSONObject one = add.getJSONObject(i);
				ContentValues cv = new ContentValues();
				cv.put(Calls._ID, one.getInt(Calls._ID));
				cv.put(Calls.CACHED_NAME,one.getString(Calls.CACHED_NAME));
				cv.put(Calls.NEW, one.getInt(Calls.NEW));
				db.insert(CacheHelper.TABLE_CALLS, null, cv);
			}
			for (int i = 0; i < new2.length(); i++) {
				ContentValues cv = new ContentValues(1);
				cv.put(Calls.NEW, 0);
				db.update(CacheHelper.TABLE_CALLS, cv, " _id = "+new2.getInt(i), null);
			}
			for (int i = 0; i < names.length(); i++) {
				JSONObject one = names.getJSONObject(i);
				ContentValues cv = new ContentValues(2);
				try{	// if get error, putNull()
					cv.put(Calls.CACHED_NAME, one.getString(Calls.CACHED_NAME));
				}catch(JSONException je){
					klilog.e("updateCache() get null name.so putNull()..{normal}"+je.toString());
					cv.putNull(Calls.CACHED_NAME);
				}
				db.update(CacheHelper.TABLE_CALLS, cv, " _id = " + one.getInt("_id"), null);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (JSONException je) {
			klilog.e("updateCache() error.11."+je.toString());
		}
		db.close();
	}
	
	
	/**
	 * 	Get data of changes. 
	 */
	
	private JSONObject collectData(List<Integer> changes, ArrayList<Integer> id_new2read,HashMap<Integer,String> id_update_name){
		JSONObject root = new JSONObject();
		JSONArray delCallLog = new JSONArray();
		JSONArray addCallLog = new JSONArray();
		JSONArray new2readCalls = new JSONArray();
		JSONArray updateCallLog = new JSONArray();

		ContentResolver resolver = mContext.getContentResolver();
		Cursor c = resolver.query(Calls.CONTENT_URI, null, null, null, null);
		
		for(int item:changes){
			if(item < 0){		//if item < 0, put it into data;
				delCallLog.put(abs(item));
				continue;
			}else if(item > 0){		//if item > 0, find data;
				boolean found = false;
				//Traversal cursor until find the data.
				c.moveToFirst();
				do{
					found = c.getInt(c.getColumnIndex(Calls._ID)) == item;
					if(!found){
						if(!c.moveToNext()){
							klilog.i("go to the end, break.");
							break;
						}
					}
				}while(!found);
				
				if(found){
					addCallLog.put(CallLogUtils.cursor2Json(c));
				}
			}
		}
		
		c.close();
		
		for(int id : id_new2read)
			new2readCalls.put(id);
		
		Iterator<Integer> its = id_update_name.keySet().iterator();
		while(its.hasNext()){
			int id = (Integer)its.next();
			String name=id_update_name.get(id);
			JSONObject json=new JSONObject();
			try {
				json.put(Calls._ID, id);
				json.put(Calls.CACHED_NAME, name);
			} catch (JSONException e) {
				klilog.e("(616 error)"+e.toString());
			}
			updateCallLog.put(json);
		}
		
		try {
			root.put("del", delCallLog);
			root.put("add", addCallLog);
			root.put("new2read", new2readCalls);// id that missed calls need to set 0 from 1
			root.put("update_name",updateCallLog);
//			klilog.i(root.toString());
			return root;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	private int abs(int num){
		if(num < 0){
			return -num;
		}else{
			return num;
		}
	}

	/**
	 * Compare datas from CalllogProvider and Cache
	 */
	private List<Integer> getChanges(ArrayList<Integer> id_new2read,HashMap<Integer,String> id_update_name) {
		List<Integer> changes = new ArrayList<Integer>();
		int length;

		ContentResolver resolver = mContext.getContentResolver();		/* 0    ,      1          ,   2      ,  3     */
		Cursor cLocal = resolver.query(Calls.CONTENT_URI, new String[]{Calls._ID,Calls.CACHED_NAME,Calls.NEW,Calls.TYPE}, null, null, null);

		SQLiteDatabase db = mCacheHelper.getReadableDatabase();			/*  0   ,      1          ,   2    */
		Cursor cCache = db.query(CacheHelper.TABLE_CALLS, new String[]{Calls._ID,Calls.CACHED_NAME,Calls.NEW}, null, null, null, null, Calls._ID + " asc");

		boolean localEnable = cLocal.moveToLast();
		boolean cacheEnable = cCache.moveToLast();

		if (!localEnable && !cacheEnable) { // all empty
			cLocal.close();
			cCache.close();
			db.close();
			return changes;
		} else if (localEnable && !cacheEnable) { // only local has calllog
			length = cLocal.getCount();
			changes = getIntArrayFromCursor(cLocal, length);
		} else if (!localEnable && cacheEnable) { // only cache has calllog
			length = cCache.getCount();
			changes = getChanges(new ArrayList<Integer>(),
					getIntArrayFromCursor(cCache, length));
		} else { 								// all have calllog
			length = Math.max(cLocal.getCount(), cCache.getCount());
			changes = getChanges(getIntArrayFromCursor(cLocal, length),
					getIntArrayFromCursor(cCache, length));
		}
		setMissedCallId(id_new2read,cLocal,cCache);
		updateNamesForId(id_update_name,cLocal,cCache);
		cLocal.close();
		cCache.close();
		db.close();
		return changes;
	}
	
	private List<Integer> getIntArrayFromCursor(Cursor c, int length) {
		List<Integer> ids = new ArrayList<Integer>();
		c.moveToFirst();
		do{
			int m = c.getInt(0);
			ids.add(m);
		}while(c.moveToNext());

		return ids;
	}
	
	/**
	 * set all id and name  if the name of this call log has changed
	 * @param id_name   store id and name
	 * @param cLocal    call log on Phone
	 * @param cCache    cache call log, also means call log on Watch.*/
	private void updateNamesForId(HashMap<Integer,String> id_name,Cursor cLocal, Cursor cCache){
		if(cLocal.getCount() == 0)
			return;
		cLocal.moveToFirst();
		do {
			int id = cLocal.getInt(0);
			String name = cLocal.getString(1);
			boolean name_changed = false;
			if (cCache.moveToFirst())
				do {
					int idc = cCache.getInt(0);
					String namec = cCache.getString(1);
					if (idc == id) {
						if (TextUtils.isEmpty(name)) {	// name empty, but namec not empty
							if (!TextUtils.isEmpty(namec))
								name_changed = true;
						} else if (!name.equals(namec)) {//name not empty, equal them.
							name_changed = true;
						}
						break;
					}

				} while (cCache.moveToNext());

			if (name_changed){
				id_name.put(id, name);
				klilog.i("add [id, name] add : id=" + id + ",name=" + name);
			}
		} while (cLocal.moveToNext());
	}
	
	/**
	 * get all id for missed call that Calls.NEW changed from 1 to 0.
	 * @param ids  the List to store id
	 * @param cLocal  call log on Phone
	 * @param cCache  cache call log, also means call log on Watch.
	 * */
	private void setMissedCallId(ArrayList<Integer> ids,Cursor cLocal, Cursor cCache) {

		if(cLocal.getCount() == 0)
			return;
		cLocal.moveToFirst();
		do {
			int id = cLocal.getInt(0), _new = cLocal.getInt(2), type = cLocal
					.getInt(3);
			if (type == Calls.MISSED_TYPE && _new == 0) {// already read missed
															// call
				boolean add = false;
				if (cCache.moveToFirst())
					do {
						int idc = cCache.getInt(0);
						if (idc == id) {
							if (1 == cCache.getInt(2)) // not read missed call
								add = true;
							break;
						}
					} while (cCache.moveToNext());
				if (add){
					ids.add(id);
				}
			}
		} while (cLocal.moveToNext());
	}
	
	private List<Integer> getChanges(List<Integer> from, List<Integer> to){
		List<Integer> changes = new ArrayList<Integer>();
		int from_size = from.size(), to_size = to.size();
		for (int i = 0; i < from_size; i++) {
			if (!to.contains(from.get(i))) // we need to add THIS to watch
				changes.add(from.get(i));
		}
		for (int j = 0; j < to_size; j++){
			if (!from.contains(to.get(j))) // we need to delete THIS on watch
				changes.add(to.get(j) * (-1));
		}
		return changes;
	}

	private class CallLogObserver extends ContentObserver{
		private Handler handler;

		public CallLogObserver(Handler handler) {
			super(handler);
			this.handler = handler;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			handler.removeMessages(MSG_SYNC_START);
			handler.sendEmptyMessageDelayed(MSG_SYNC_START, 1000);
		}
	}
	
	public void onDisconnected(){
//		stopClearTask();
//		stopSyncTask();
//		mStoreSync = false;
	}
	
	private void stopClearTask(){
		if(mClearTask != null){
			mWatchClearRes.value = 0;
			synchronized(mWatchClearRes){
				mWatchClearRes.notifyAll();
				klilog.i("mWatchClearRes notify");
			}
			mClearTask = null;
		}
	}
	
	private void stopSyncTask(){
		if(mSyncTask != null){
			mSyncTask.cancel(true);
			watchSyncFinished(RES_FAILED_WITH_DATA_CHAOS);
			mSyncTask = null;
		}
	}
}
