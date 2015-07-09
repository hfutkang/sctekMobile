package com.sctek.smartglasses.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaData implements Parcelable{
	
	public String url;
	public String name;
	
	public MediaData() {};
	
	public MediaData(Parcel in) {
		
		this.url = in.readString();
		this.name = in.readString();
		
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(url);
		dest.writeString(name);
		
	}
	
	public static final Parcelable.Creator<MediaData> CREATOR = new Creator<MediaData>() {
		
		@Override
		public MediaData[] newArray(int size) {
			// TODO Auto-generated method stub
			return new MediaData[size];
		}
		
		@Override
		public MediaData createFromParcel(Parcel source) {
			// TODO Auto-generated method stub
			return new MediaData(source);
		}
	};

}
