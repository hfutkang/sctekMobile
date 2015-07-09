package cn.ingenic.glasssync.ui.view;
    
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Runnable;

import android.app.Activity;

import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.database.Cursor;
import android.content.Context;  
import android.util.Log;
import android.util.AttributeSet;  
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;
import android.widget.ImageView;  
import android.widget.FrameLayout;
import android.widget.ListView;  
import android.widget.TextView;  
import android.widget.BaseAdapter;
import android.graphics.Typeface;
import android.provider.MediaStore;

import cn.ingenic.glasssync.R;
import cn.ingenic.glasssync.multimedia.MultiMediaModule;
import cn.ingenic.glasssync.ui.MediaAdapter;  
import cn.ingenic.glasssync.ui.Fragment_Media;  
import cn.ingenic.glasssync.ui.view.MyListView.OnRefreshListener;  

public class MultiMediaPictureItem extends FrameLayout 
    implements MyListView.OnRefreshListener {  
    private final String TAG = "MultiMediaPictureItem";
    private Context mContext;
    private MediaAdapter mPictureAdapter;
    private MyListView mDefaultView;
    private MyListView mPictureListView;
    private ArrayList<HashMap<String, Object>> mPictureListItem;

    private boolean mStopThread = false;
    private boolean mDisplayDefault = true;
    private final int MESSAGE_UPDATE_PICTURE_LIST = 1;
    private final int MESSAGE_QUERY_DATA_FINISH=2;
    private final int MESSAGE_REFRESH=3;
    private final int MESSAGE_PICTURE_NOFIND=4;

    String[] picColu = new String[] { MediaStore.Images.Media._ID,
				      MediaStore.Images.Media.DATA };
    private Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		switch(msg.what){
		case MESSAGE_UPDATE_PICTURE_LIST:
		    if(mDisplayDefault){
			Log.d(TAG,"---mPictureListItem.size()="+mPictureListItem.size());
		    	mDefaultView.setVisibility(View.GONE);
		    	mPictureListView.setVisibility(View.VISIBLE);
			mDisplayDefault = false;
		    }
		    Log.d(TAG,"---notifyDataSetChanged");
		    mPictureAdapter.notifyDataSetChanged();
		    break;
		case MESSAGE_REFRESH:
		    mPictureListItem.clear();
		    scanFile();
		    break;
		case MESSAGE_PICTURE_NOFIND:
		    mDisplayDefault = true;
		    mDefaultView.setVisibility(View.VISIBLE);
		    mPictureListView.setVisibility(View.INVISIBLE);
		    break;
		}		
	    }
	};

    public MultiMediaPictureItem(Context context) {  
	this(context, null);
    }  
  
    public MultiMediaPictureItem(Context context, AttributeSet attrs) {  
        super(context, attrs);  
	mContext = context;
        init(context);  
    }  
  
    private void init(Context context) {  
	LayoutInflater inflater = (LayoutInflater) context
	    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	inflater.inflate(R.layout.fragment_item_media_picture, this, true);

	mPictureListItem = new ArrayList<HashMap<String, Object>>();
	mPictureListView = (MyListView) findViewById(R.id.lv_picture);

	mPictureAdapter = new MediaAdapter(context, mPictureListItem, false);
	mPictureListView.setAdapter(mPictureAdapter);
	mPictureListView.setonRefreshListener(this);

	mDefaultView = (MyListView)findViewById(R.id.default_picture);
	ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();  
	HashMap<String, Object> map = new HashMap<String, Object>();

	// map.put("img", R.drawable.pic_no);
	// map.put("text", R.string.picture_nofind);
	list.add(map);

	  // MyAdapter adapter = new SimpleAdapter(mContext, list, R.layout.media_default_view, 
	  // 					  new String[] { "img", "text" },  
	  // 					  new int[] {R.id.iv_default,R.id.tv_picture_null});
        MyAdapter adapter = new MyAdapter(mContext, list);
        mDefaultView.setAdapter(adapter);
	mDefaultView.setonRefreshListener(this);

	scanFile();  
    }  
  
    @Override  
	public void onRefresh() {  
	new AsyncTask<Void, Void, Void>() {  
	    protected Void doInBackground(Void... params) {  
		try {  
		    MultiMediaModule mmm = MultiMediaModule.getInstance(mContext);
		    mmm.setImageAutoSync(true);
		} catch (Exception e) {  
		    e.printStackTrace();  
		}  
		return null;  
	    }  
	    
	    @Override  
		protected void onPostExecute(Void result) {  
		if(mDefaultView.getVisibility() == View.VISIBLE)
		    mDefaultView.onRefreshComplete();  
		else{
		    mPictureListView.onRefreshComplete();  
		}
		mHandler.sendEmptyMessage(MESSAGE_REFRESH);
	    }  
	}.execute(null, null, null);  
    }  
    private void scanFile() {
	new Thread(new Runnable() {
		@Override
		    public void run() {
		    StringBuilder where = new StringBuilder();
		    where.append(MediaStore.Images.Media.DATA + " like ?");
		    String whereVal[] = { "%" + "IGlass/Thumbnails" + "%" };
		    Cursor cs = mContext.getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, picColu, where.toString(), whereVal, null);
		    if (cs == null || cs.getCount() == 0) {
			Log.d(TAG,"-------no thumbnails found in IGlass/.");
			mHandler.sendEmptyMessage(MESSAGE_PICTURE_NOFIND);
			return;
		    }
		    int id_idx = cs
			.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
		    int path_idx = cs
			.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    if (cs.moveToLast()) {
			do {
			    Long id = cs.getLong(id_idx);
			    String path = cs.getString(path_idx);
			    Log.d(TAG, "id:" + id + " path:" + path);
			    HashMap<String, Object> map = new HashMap<String, Object>();
			    map.put("id", id);
			    map.put("path", path);
			    if (!Fragment_Media.check_video(path)) {
				Log.d(TAG, "is pic");
				mPictureListItem.add(map);
				mHandler.sendEmptyMessage(MESSAGE_UPDATE_PICTURE_LIST);
			    }
			} while (cs.moveToPrevious() && !mStopThread);
		    }
		    cs.close();
		    if(!mStopThread)
		    	mHandler.sendEmptyMessage(MESSAGE_QUERY_DATA_FINISH);
		}
	    }).start();
    }

    public void stopThread(){
	mStopThread = true;
    }

    public void addItem(HashMap<String, Object> map){
	mPictureListItem.add(map);
	mHandler.sendEmptyMessage(MESSAGE_UPDATE_PICTURE_LIST);
    }

    public void setUserInVisible(){
	mDefaultView.onRefreshComplete();
	mPictureListView.onRefreshComplete();
    }

    public class MyAdapter extends BaseAdapter{
	private Context mContext;
	private ArrayList<HashMap<String, Object>> mListItem;
	public MyAdapter(Context context,ArrayList<HashMap<String, Object>> listItem) {
	    mContext=context;
	    mListItem = listItem;
	}

        @Override
	    public int getCount() {
	    return mListItem.size();
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
	    if(convertView == null){
		convertView = LayoutInflater.from(mContext).inflate(R.layout.media_default_view, null);

		TextView tv = (TextView)convertView.findViewById(R.id.tv_picture_null);
		Typeface typeface = Typeface.createFromAsset(mContext.getAssets(),"fonts/iphone.ttf");
		tv.setTypeface(typeface);
		
		convertView.setLayoutParams(new ListView.LayoutParams(LayoutParams.FILL_PARENT,parent.getHeight()));
	    }
	    return convertView;
	}
    }
}  