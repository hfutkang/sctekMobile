package cn.ingenic.glasssync.sms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.Column;
import cn.ingenic.glasssync.services.mid.DefaultColumn;
import cn.ingenic.glasssync.services.mid.KeyColumn;
import cn.ingenic.glasssync.services.mid.MidException;
import cn.ingenic.glasssync.services.mid.SimpleMidSrcManager;

public class SmsMidManager extends SimpleMidSrcManager {
	
	private Canonical mCanonical;
	private Thread mThread;

	public SmsMidManager(Context ctx, SyncModule module) {
		super(ctx, module);
	}
	
	private List<Column> columnList;
	
	private class Canonical{
		public Map<Long,String> mCanonicalMap;
		public Canonical(){
			mCanonicalMap=init();
		}
		
		private Map<Long,String> init(){
			Cursor cursor=mContext.getContentResolver().query(Sms.Canonical.sAllCanonical, null, null, null, null);
			if(cursor.getCount()==0){
				cursor.close();
				return null;
			}
			Map<Long,String> canonicalMap=new HashMap<Long,String>();
			cursor.moveToFirst();
			do{
				long id=cursor.getLong(cursor.getColumnIndex(Sms.Canonical.ID));
				String address=cursor.getString(cursor.getColumnIndex(Sms.Canonical.ADDRESS));
				canonicalMap.put(id, address);
			}while(cursor.moveToNext());
			cursor.close();
			return canonicalMap;
		}
		
		public Map<Long,String> getCanonicalMap(){
			return mCanonicalMap;
		}
		public void refresh(){
			mCanonicalMap=init();
		}
	}
	
	private class Thread{
		
		private Map<Long,ThreadEntry> mThreadMap;
		public Thread(){
			mThreadMap=init();
		}
		
		private Map<Long,ThreadEntry> init(){
			Cursor cursor=mContext.getContentResolver().query(Sms.Thread.sAllThreadsUri, ThreadEntry.THREAD_PROJECTION, null, null, null);
			if(cursor.getCount()==0){
				cursor.close();
				return null;
			}
			Map<Long,ThreadEntry> map=new HashMap<Long,ThreadEntry>();
			cursor.moveToFirst();
			do{
				ThreadEntry te=new ThreadEntry(cursor);
				map.put(te.getId(), te);
			}while(cursor.moveToNext());
			cursor.close();
			return map;
		}
		
		public Map<Long,ThreadEntry> getThreadMap(){
			return mThreadMap;
		}
		
		public void refresh(){
			mThreadMap=init();
		}
	}

	@Override
	protected List<Column> getSrcColumnList() {

		if(columnList!=null){
			columnList.clear();
		}else{
			columnList=new ArrayList<Column>();
		}
		columnList.add(new DefaultColumn(Sms.MidColumns.READ, Column.INTEGER));
		columnList.add(new DefaultColumn(Sms.MidColumns.TYPE, Column.INTEGER));
		columnList.add(new DefaultColumn(Sms.MidColumns.ERROR_CODE,Column.INTEGER));
		columnList.add(new DefaultColumn(Sms.MidColumns.BODY,Column.STRING));
		
		return columnList;
	}


	@Override
	protected Uri[] getSrcObservedUris() {
		// TODO Auto-generated method stub
		return new Uri[] { Uri.parse("content://sms"),
				Uri.parse("content://mms-sms/conversations") };
	}

	@Override
	protected Cursor getSrcDataCursor(Set keySet) {
		mCanonical=new Canonical();
		mThread=new Thread();
		if(keySet==null){
			return mContext.getContentResolver().query(Uri.parse("content://sms"),
					null, null, null, null);
		}
		Iterator<Long> iterator=keySet.iterator();
		String selections=null;
		while(iterator.hasNext()){
			long keyId=iterator.next();
			if(selections==null){
				selections=String.valueOf(keyId);
			}else{
				selections=selections+","+keyId;
			}
		}
		return mContext.getContentResolver().query(Uri.parse("content://sms"), null, 
				Sms.ID+" IN ("+selections+")", null, null);
		
	}
	
	
	
	

	@Override
	protected SyncData[] appendSrcSyncData(Set<Integer> positons, Cursor source)
			throws MidException {
		// TODO Auto-generated method stub
		Log.i("Ingenic_Sms","appendSrcSyncData size is :"+source.getCount());
		Iterator<Integer> iterator=positons.iterator();
	    String[] args=new String[source.getCount()];
	    int count=0;
		while(iterator.hasNext()){
			int position=iterator.next();
			source.moveToPosition(position);
			
			long tId=source.getLong(source.getColumnIndex(Sms.SendColumns.THREAD_ID));
			
			if(String.valueOf(tId)!=null){
				args[count]=String.valueOf(tId);
				count++;
			}
			
		}
		
		return getThreadDataArray("_id IN ("+compareStringArray(args)+")",null);
		
	}
	
	private String compareStringArray(String[] oldArray){
		ArrayList<String> keyList=new ArrayList<String>();
		String newSelection=null;
		for(String old:oldArray){
			if(!keyList.contains(old)){
				keyList.add(old);
				if(newSelection==null){
					newSelection=old;
				}else{
					newSelection=newSelection+","+old;
				}
				
			}
		}
		return newSelection;
	}
	
	private SyncData[] getThreadDataArray(String selection,String[] selectionArgs){
	   
		Cursor threadCursor=mContext.getContentResolver().query(Sms.Thread.sAllThreadsUri, null, selection, selectionArgs, null);
	
		if(threadCursor.getCount()==0){
			Log.e("SmsMidManager", "no datas compare to "+selection+" !!!");
			threadCursor.close();
			return null;
		}
		threadCursor.moveToFirst();
		
		mCanonical.refresh();
		
		SyncData[] dataArray=new SyncData[threadCursor.getCount()];
		int count=0;
		do{
			SyncData syncData=getThreadSyncData(threadCursor);
			dataArray[count]=syncData;
			count++;
		}while(threadCursor.moveToNext());
		threadCursor.close();
		Log.e("Ingenic_Sms","after appented  dataArray size is :"+dataArray.length);
		return dataArray;
	}
	
	
	private SyncData getThreadSyncData(Cursor cursor){
		long id=cursor.getLong(cursor.getColumnIndex(Sms.Thread.ID));
		String snippet=cursor.getString(cursor.getColumnIndex(Sms.Thread.SNIPPET));
	
		int error=cursor.getInt(cursor.getColumnIndex(Sms.Thread.ERROR));
		long date=cursor.getLong(cursor.getColumnIndex(Sms.Thread.DATE));
		int read=cursor.getInt(cursor.getColumnIndex(Sms.Thread.READ));
		int message_count=cursor.getInt(cursor.getColumnIndex(Sms.Thread.MESSAGE_COUNT));
		int type=cursor.getInt(cursor.getColumnIndex(Sms.Thread.TYPE));
		String recipient=cursor.getString(cursor.getColumnIndex(Sms.Thread.RECIPIENT_IDS));
		SyncData syncData=new SyncData();
		syncData.putLong(Sms.Thread.ID, id);
		syncData.putString(Sms.Thread.SNIPPET, snippet);
		syncData.putInt(Sms.Thread.ERROR, error);
		syncData.putLong(Sms.Thread.DATE, date);
		syncData.putInt(Sms.Thread.READ, read);
		syncData.putInt(Sms.Thread.MESSAGE_COUNT, message_count);
		syncData.putInt(Sms.Thread.TYPE, type);
		
		String[] addId=recipient.split(" ");
		
		String[] addArray=new String[addId.length];
		
		for(int i=0;i<addId.length;i++){
			String address=mCanonical.getCanonicalMap().get(Long.valueOf(addId[i]));
			addArray[i]=address;
		}
		syncData.putStringArray(Sms.Thread.RECIPIENT_ADS_KEY, addArray);
		return syncData;
	}
	
	
	
	private void executeSmsSendData(SyncData syncData,Cursor source){
//		long id=source.getLong(source.getColumnIndex(Sms.ID));
		String address=source.getString(source.getColumnIndex(Sms.SendColumns.ADDRESS));

		int errorCode=source.getInt(source.getColumnIndex(Sms.SendColumns.ERROR_CODE));
		int read=source.getInt(source.getColumnIndex(Sms.SendColumns.READ));
		int seen=source.getInt(source.getColumnIndex(Sms.SendColumns.SEEN));
		int type=source.getInt(source.getColumnIndex(Sms.SendColumns.TYPE));
		long date=source.getLong(source.getColumnIndex(Sms.SendColumns.DATE));
		String body=source.getString(source.getColumnIndex(Sms.SendColumns.BODY));
		long threadId=source.getLong(source.getColumnIndex(Sms.SendColumns.THREAD_ID));
//		syncData.putLong(Sms.SmsKey.ID_KEY, id);
		
		//draft has no address
		if(address==null&&mThread.getThreadMap().get(threadId)!=null){
			ThreadEntry te=mThread.getThreadMap().get(threadId);
			String[] recipientIdArray=te.getRecipientIds().split(" ");
			int l=recipientIdArray.length;
			if(l==1){
				address=mCanonical.getCanonicalMap().get(Long.valueOf(recipientIdArray[0]));
			}else{
				String add="";
				for(int i=0;i<l;i++){
					long reId=Long.valueOf(recipientIdArray[i]);
					if(i==0){
						add=mCanonical.getCanonicalMap().get(reId);
					}else{
						add=add+","+mCanonical.getCanonicalMap().get(reId);
					}
				}
//				syncData.putString(Sms.SendColumns.ADDRESS2, add);
				address=add;
			}
			
		 
		}
		syncData.putString(Sms.SendColumns.ADDRESS, address);
		syncData.putInt(Sms.SendColumns.ERROR_CODE, errorCode);
		syncData.putInt(Sms.SendColumns.READ, read);
		syncData.putInt(Sms.SendColumns.SEEN, seen);
		syncData.putInt(Sms.SendColumns.TYPE, type);
		syncData.putLong(Sms.SendColumns.DATE, date);
		syncData.putString(Sms.SendColumns.BODY, body);
		syncData.putLong(Sms.THREAD_KEY, threadId);
	}

	@Override
	protected void fillSrcSyncData(SyncData data, Cursor source)
			throws MidException {
		executeSmsSendData(data,source);
	}

	@Override
	protected KeyColumn getSrcKeyColumn() {
		return super.getSrcKeyColumn();
	}


	@Override
	protected String getMidAuthorityName() {
		return "sms_source";
	}

	
	

}
