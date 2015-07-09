package cn.ingenic.glasssync.calllog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CallLog.Calls;

public class CacheHelper extends SQLiteOpenHelper {
	final static String TABLE_CALLS = "calls";
	private final static String SQL_CREATE_TABLE_CALLS = "CREATE TABLE if not exists "
			+TABLE_CALLS+" (" + Calls._ID         +	" INTEGER," +
								Calls.CACHED_NAME +	" TEXT," +
								Calls.NEW         +	" INTEGER);";
	
	public CacheHelper(Context context){
		super(context, "calldatabase", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_TABLE_CALLS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
	}

	void clearTableCalls() {
		synchronized (this) {
			SQLiteDatabase db = getWritableDatabase();
			db.execSQL("delete from "+TABLE_CALLS);
//			db.execSQL(SQL_CREATE_TABLE_CALLS);
			db.close();
		}
	}
}
