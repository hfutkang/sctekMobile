package cn.ingenic.glasssync.ui;

import java.util.ArrayList;
import java.util.HashMap;
import cn.ingenic.glasssync.multimedia.OpenFileActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import cn.ingenic.glasssync.R;
public class MediaAdapter extends BaseAdapter{
    private final String TAG="MediaAdapter";
    private final boolean DEBUG=true;
    private Context mContext;
    private ArrayList<HashMap<String, Object>> mListItem;
    private boolean mIsVideo = false;
    private int mWidthHalf;
    public MediaAdapter(Context context,ArrayList<HashMap<String, Object>> listItem, boolean isVideo) {
	mContext=context;
	mListItem = listItem;
	mIsVideo = isVideo;
	DisplayMetrics metric = new DisplayMetrics();
	((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metric);
	mWidthHalf = (metric.widthPixels)/2;
    }

        @Override
	public int getCount() {
	    if(DEBUG)Log.d(TAG,"getcount="+(mListItem.size() + 1) / 2);
	    return (mListItem.size() + 1) / 2;
	}

        @Override
	public Object getItem(int arg0) {
	    return arg0;
	}

        @Override
	public long getItemId(int position) {
	    return position;
	}

        @Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    if(DEBUG)Log.d(TAG,"getView----in position="+position+" mListItem.size="+mListItem.size());
	    ViewHolder holder;
	    if(convertView==null){
		if(DEBUG)Log.d(TAG,"getView----convertView==null");
		holder=new ViewHolder();
		convertView = LayoutInflater.from(mContext).inflate(
								R.xml.mulstyle, null);
		/* left */
		// HashMap hm1 = mListItem.get(position * 2);
		// String path1 = (String) hm1.get((Object) "path");
		// Bitmap btmp1 = BitmapFactory.decodeFile(path1);
		holder.leftImage =new ImageView(mContext);
		// holder.leftImage.setTag(path1);
		holder.leftImage.setPadding(10, 5, 5, 5);// l t r b
		// holder.leftImage.setImageBitmap(btmp1);
		holder.leftImage.setScaleType(ImageView.ScaleType.FIT_XY);
		holder.leftImage.setLayoutParams(new LayoutParams(mWidthHalf,
								  mWidthHalf * 3 / 4));
		holder.leftImage.setOnClickListener(picOnclick);
		if (mIsVideo) {
		    holder.leftVideo= new ImageView(mContext);
		      //holder.leftVideo.setImageResource(R.drawable.ic_video_tag);
		    holder.leftVideo.setLayoutParams(new LayoutParams(
								      LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		    holder.leftVideo.setScaleType(ImageView.ScaleType.CENTER);
		    holder.leftVideoView= new FrameLayout(mContext);
		    holder.leftVideoView.setLayoutParams(new LayoutParams(mWidthHalf,
									  mWidthHalf * 3 / 4));
		    holder.leftVideoView.addView(holder.leftImage);
		    holder.leftVideoView.addView(holder.leftVideo);
		    ((LinearLayout) convertView).addView(holder.leftVideoView);
		} else {
		    ((LinearLayout) convertView).addView(holder.leftImage);
		}
		// if (mListItem.size() < position * 2 + 2) {
		//     return convertView;
		// }
		/* right */
		// HashMap hm2 = mListItem.get(position * 2 + 1);
		// String path2 = (String) hm2.get((Object) "path");
		// Bitmap btmp2 = BitmapFactory.decodeFile(path2);
		holder.rightImage= new ImageView(mContext);
		// holder.rightImage.setTag(path2);
		holder.rightImage.setPadding(5, 5, 10, 5); // ltrb
		// holder.rightImage.setImageBitmap(btmp2);
		holder.rightImage.setScaleType(ImageView.ScaleType.FIT_XY);
		holder.rightImage.setLayoutParams(new LayoutParams(mWidthHalf,
								   mWidthHalf * 3 / 4));
		holder.rightImage.setOnClickListener(picOnclick);
		if (mIsVideo) {
		    holder.rightVideo = new ImageView(mContext);
		      //holder.rightVideo.setImageResource(R.drawable.ic_video_tag);
		    holder.rightVideo.setLayoutParams(new LayoutParams(
								       LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		    holder.rightVideo.setScaleType(ImageView.ScaleType.CENTER);
		    
		    holder.rightVideoView = new FrameLayout(mContext);
		    holder.rightVideoView.setLayoutParams(new LayoutParams(mWidthHalf,
									   mWidthHalf * 3 / 4));
		    holder.rightVideoView.addView(holder.rightImage);
		    holder.rightVideoView.addView(holder.rightVideo);
		    ((LinearLayout) convertView).addView(holder.rightVideoView);
		} else {
		    ((LinearLayout) convertView).addView(holder.rightImage);
		}
		  //if(DEBUG)Log.e(TAG, "--path1:" + path1 + " --path2:" + path2);
		convertView.setTag(holder);
	    }else{
		holder=(ViewHolder) convertView.getTag();
	    }

	    if(mListItem.size() >= position * 2 + 2){
		HashMap hm2 = mListItem.get(position * 2 + 1);
		if(hm2!=null){
		    String path2 = (String) hm2.get((Object) "path");
		    Bitmap btmp2 = BitmapFactory.decodeFile(path2);
		    Log.d(TAG,"--path1="+mListItem.get(position * 2).get((Object) "path"));
		    Log.d(TAG,"----position="+position+"-path2="+path2);
		    holder.rightImage.setTag(path2);
		    holder.rightImage.setImageBitmap(btmp2);
		    if (mIsVideo) {
			holder.rightVideo.setImageResource(R.drawable.ic_video_tag);
		    }
		}

	    }

	    HashMap hm1 = mListItem.get(position * 2);
	    String path1 = (String) hm1.get((Object) "path");
	    Bitmap btmp1 = BitmapFactory.decodeFile(path1);
	    Log.d(TAG,"----position="+position+"-path1="+path1);
	    holder.leftImage.setTag(path1);
	    holder.leftImage.setImageBitmap(btmp1);	    
	    if (mIsVideo) {
		holder.leftVideo.setImageResource(R.drawable.ic_video_tag);
	    }


	    if(DEBUG)Log.d(TAG,"getView----convertView.getTag");
	   	
	    return convertView;
	}
    
    public OnClickListener picOnclick = new OnClickListener() {
	        @Override
	        public void onClick(View v) {
		    String picPath = (String) v.getTag();
		    if(DEBUG)Log.e(TAG, "picPath:" + picPath);
		    Intent intent = new Intent(mContext, OpenFileActivity.class);
		    intent.putExtra("path", picPath);
		    mContext.startActivity(intent);
		}
	};
    class ViewHolder {
	ImageView leftImage,rightImage,leftVideo,rightVideo;
	FrameLayout leftVideoView,rightVideoView;
    }
}

