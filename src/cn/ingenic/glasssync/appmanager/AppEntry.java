package cn.ingenic.glasssync.appmanager;

import java.io.ByteArrayOutputStream;

import cn.ingenic.glasssync.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class AppEntry {
	private String mAppName;
	private String mAppSize;
	private String mPackageName;
	private byte[] iconArray;
	private boolean mIsSystem;
	private int mEnableSetting;
	private int[] mSimpleSize={2,4,8};
	public AppEntry(String name,String size,String packageName,byte[] bytes,boolean isSystem,int enableSetting){
		this.mAppName=name;
		this.mAppSize=size;
		this.mPackageName=packageName;
		this.iconArray=bytes;
		this.mIsSystem=isSystem;
		this.mEnableSetting=enableSetting;
	}
	
	public String getApplicationName(){
		return mAppName;
	}
	
	public String getApplicationSize(){
		return mAppSize;
	}
	public String getPackageName(){
		return mPackageName;
	}
	
	public int getEnableSetting(){
		return mEnableSetting;
	}
	
	public Drawable getApplicationIcon(Resources resources){
		if(iconArray==null||iconArray.length==0)return null;
		try{
			Bitmap bitmap=BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
			return new BitmapDrawable(resources,bitmap);
		}catch(OutOfMemoryError  oom){
			
			BitmapFactory.Options options=new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length,options);
			Log.e("ApplicationManager","OutOfMemoryError 1 height is :"+options.outHeight+"  ,and width is :"+options.outWidth);
			
			try{
				Bitmap bitmap=getSimpleBitmap(options,mSimpleSize[0]);
				return new BitmapDrawable(resources,bitmap);
			}catch(OutOfMemoryError  oom2){
				Log.e("ApplicationManager","OutofMemoryError 2 !");
				try{
					Bitmap bitmap=getSimpleBitmap(options,mSimpleSize[1]);
					return new BitmapDrawable(resources,bitmap);
				}catch(OutOfMemoryError  oom3){
					Log.e("ApplicationManager","OutOfMemoryError 3!");
				}
				
			}
			
		}
		
		
		return null;
	}
	
	private Bitmap getSimpleBitmap(BitmapFactory.Options options,int size){
		options.inSampleSize = size;
        options.inJustDecodeBounds = false;
        options.inDither = true;
        options.inPreferredConfig = null;//Bitmap.Config.ARGB_8888;
        Bitmap bitmap=BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length,options);
        return bitmap;
	}
	
	public boolean getIsSystem(){
		return mIsSystem;
	}


}
