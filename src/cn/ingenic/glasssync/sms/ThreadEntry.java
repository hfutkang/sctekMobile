package cn.ingenic.glasssync.sms;

import cn.ingenic.glasssync.sms.Sms.Thread;
import android.database.Cursor;

public class ThreadEntry {
	
	private Long id;
	private Long date;
	private int messageCount;
	private String snippet;
	private String recipient_ids;
	private int error;
	private int read;
	private int type;
	
	
	public static String[] THREAD_PROJECTION={
		Thread.ID,
		Thread.DATE,
		Thread.MESSAGE_COUNT,
		Thread.SNIPPET,
		Thread.RECIPIENT_IDS,
		Thread.ERROR,
		Thread.READ,
		Thread.TYPE,
	};
	
	public ThreadEntry(Cursor cursor){
		id=cursor.getLong(cursor.getColumnIndex(Thread.ID));
		date=cursor.getLong(cursor.getColumnIndex(Thread.DATE));
		messageCount=cursor.getInt(cursor.getColumnIndex(Thread.MESSAGE_COUNT));
		snippet=cursor.getString(cursor.getColumnIndex(Thread.SNIPPET));
		recipient_ids=cursor.getString(cursor.getColumnIndex(Thread.RECIPIENT_IDS));
		error=cursor.getInt(cursor.getColumnIndex(Thread.ERROR));
		read=cursor.getInt(cursor.getColumnIndex(Thread.READ));
		type=cursor.getInt(cursor.getColumnIndex(Thread.TYPE));
	}
	
	public Long getId(){
		return id;
	}
	
	public long getDate(){
		return date;
	}
	
	public int getMessageCount(){
		return messageCount;
	}
	
	public String getSnippet(){
		return snippet;
	}
	public String getRecipientIds(){
		return recipient_ids;
	}
	
	public int getError(){
		return error;
	}
	public int getRead(){
		return read;
	}
	public int getType(){
		return type;
	}

}
