package cn.ingenic.glasssync.ui;

import cn.ingenic.glasssync.R;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent; 
import android.widget.ScrollView;
import android.util.AttributeSet;
import android.os.Handler;
import android.os.Message;
import android.view.View; 
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.animation.RotateAnimation;
import android.view.View.OnTouchListener;
import android.view.animation.LinearInterpolator; 
import android.view.View.MeasureSpec; 
import android.view.ViewGroup;
import android.view.LayoutInflater;

public class MyScrollView extends ScrollView {
    private static final String TAG = "MyScrollView";
    private static final Boolean DEBUG = true;
    private Context mContext;
    private Handler mHandler;

    private float beginY = 0;
    private int headerHeight;
    private int lastHeaderPadding;
    private ProgressBar headProgress;
    private ImageView arrowImg;
    private TextView tipsTxt;
    private RotateAnimation tipsAnimation;
    private RotateAnimation reverseAnimation;

    private boolean isBack;
    private LinearLayout header;

    static final private int RELEASE_To_REFRESH = 0;  
    static final private int PULL_To_REFRESH = 1;  
    static final private int REFRESHING = 2;  
    static final private int DONE = 3;  
    private int headerState = REFRESHING;  

    public MyScrollView(Context context) {
        super(context);
	mContext = context;
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
	mContext = context;
    }

    public void refreshStart() {  
        headerState = REFRESHING;  
        changeHeaderViewByState();  
    }  

    public void refreshEnd() {  
        headerState = DONE;  
        changeHeaderViewByState();  
    }  

    public LinearLayout refreshInit(LinearLayout globleLayout,Handler handler) {  
	mHandler = handler;
        LayoutInflater inflater = (LayoutInflater) mContext  
	    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
          
        header = (LinearLayout) inflater.inflate(R.layout.pull_to_refresh, null);  
        measureView(header);  
        headerHeight = header.getMeasuredHeight();  
        lastHeaderPadding = (-1*headerHeight); //最后一次调用Move Header的Padding  
        header.setPadding(0, lastHeaderPadding, 0, 0);  
        header.invalidate();  
	globleLayout.addView(header,0);  
          
        headProgress = (ProgressBar) header.findViewById(R.id.head_progressBar);  
        arrowImg = (ImageView) header.findViewById(R.id.head_arrowImageView);  
        arrowImg.setMinimumHeight(50);  
        arrowImg.setMinimumWidth(50);  
        tipsTxt = (TextView) header.findViewById(R.id.head_tipsTextView);  
	  //箭头转动动画  
        tipsAnimation = new RotateAnimation(0, -180,  
					    RotateAnimation.RELATIVE_TO_SELF, 0.5f,  
					    RotateAnimation.RELATIVE_TO_SELF, 0.5f);  
        tipsAnimation.setInterpolator(new LinearInterpolator());  
        tipsAnimation.setDuration(200);     //动画持续时间  
        tipsAnimation.setFillAfter(true);   //动画结束后保持动画  
	  //箭头反转动画  
        reverseAnimation = new RotateAnimation(-180, 0,  
					       RotateAnimation.RELATIVE_TO_SELF, 0.5f,  
					       RotateAnimation.RELATIVE_TO_SELF, 0.5f);  
        reverseAnimation.setInterpolator(new LinearInterpolator());  
        reverseAnimation.setDuration(200);  
        reverseAnimation.setFillAfter(true);  
	
	return header;
    }
    private float startY=0;
    @Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
	switch (event.getAction()) {  
	case MotionEvent.ACTION_DOWN:  
	    if(DEBUG) Log.d(TAG,"onInterceptTouchEvent ACTION_DOWN");
	    startY = event.getY();
	    onTouchEvent(event);	    
	    break;  
	case MotionEvent.ACTION_MOVE:  
	    if(DEBUG) Log.d(TAG,"onInterceptTouchEvent ACTION_MOVE");
	    int interval = (int) (event.getY() - startY);
	    if(interval > 10 || interval < -10){
		if(DEBUG) Log.d(TAG,"onInterceptTouchEvent return true");
		return true;
	    }
	    onTouchEvent(event);
	    break;
	case MotionEvent.ACTION_UP:  
	    if(DEBUG) Log.d(TAG,"onInterceptTouchEvent ACTION_UP");
	    break;
	}
	return false;
    }

    @Override  
	public boolean onTouchEvent( MotionEvent event) {  
	switch (event.getAction()) {
	case MotionEvent.ACTION_DOWN: 
	    if(DEBUG) Log.d(TAG,"onTouchEvent ACTION_DOWN");
	      //加上下滑阻力与实际滑动距离的差（大概值）  
	    beginY = (int) ((int) event.getY() + getScrollY()*1.5);  
	    break;    
	case MotionEvent.ACTION_MOVE:  
	    if(DEBUG) Log.d(TAG,"onTouchEvent ACTION_MOVE");
	    if((getScrollY() == 0 || lastHeaderPadding > (-1*headerHeight)) && headerState != REFRESHING) {  
		  //拿到滑动的Y轴距离  
		int interval = (int) (event.getY() - beginY);  
		  //是向下滑动而不是向上滑动  
		if (interval > 0) {
		    interval = interval/2;//下滑阻力  
		    lastHeaderPadding = interval + (-1*headerHeight);  
		    header.setPadding(0, lastHeaderPadding, 0, 0);  
		    if(lastHeaderPadding > 0) {  
			headerState = RELEASE_To_REFRESH;  
			  //是否已经更新了UI  
			if(! isBack) {  
			    isBack = true;  //到了Release状态，如果往回滑动到了pull则启动动画  
			    changeHeaderViewByState();  
			}  
		    } else {  
			headerState = PULL_To_REFRESH;  
			changeHeaderViewByState();  
			fullScroll(ScrollView.FOCUS_UP);//滚动到顶部
		    }  
		    return true;
		}  
	    }  
	    break;  
	case MotionEvent.ACTION_UP:  
	    if(DEBUG) Log.d(TAG,"onTouchEvent ACTION_UP");
	    if (headerState != REFRESHING) {  
		switch (headerState) {  
		case DONE:  //什么也不干  
		    break;  
		case PULL_To_REFRESH:  
		    headerState = DONE;  
		    lastHeaderPadding = -1*headerHeight;  
		    header.setPadding(0, lastHeaderPadding, 0, 0);  
		    changeHeaderViewByState();  
		    break;  
		case RELEASE_To_REFRESH:  
		    isBack = false; //准备开始刷新，此时将不会往回滑动  
		    headerState = REFRESHING;  
		    notifyWork();
		    changeHeaderViewByState();  
		    break;  
		default:  
		    break;  
		}  
	    }  
	    break;  
	}  
	super.onTouchEvent(event);
	return true;
    }

    private void changeHeaderViewByState() {  
        switch (headerState) {  
        case PULL_To_REFRESH:  
	      // 是由RELEASE_To_REFRESH状态转变来的  
            if (isBack) {  
                isBack = false;  
                arrowImg.startAnimation(reverseAnimation);  
            }  
            tipsTxt.setText(mContext.getString(R.string.pullto_refresh));  
            break;  
        case RELEASE_To_REFRESH:  
            arrowImg.setVisibility(View.VISIBLE);  
            headProgress.setVisibility(View.GONE);  
            tipsTxt.setVisibility(View.VISIBLE);  
            arrowImg.clearAnimation();  
            arrowImg.startAnimation(tipsAnimation);  
            tipsTxt.setText(mContext.getString(R.string.release_refresh));  
            break;  
        case REFRESHING:  
            lastHeaderPadding = 0;  
            header.setPadding(0, lastHeaderPadding, 0, 0);  
            header.invalidate();  
            headProgress.setVisibility(View.VISIBLE);  
            arrowImg.clearAnimation();  
            arrowImg.setVisibility(View.INVISIBLE);  
            tipsTxt.setText(mContext.getString(R.string.refreshing));  
            break;  
        case DONE:  
            lastHeaderPadding = -1 * headerHeight;  
            header.setPadding(0, lastHeaderPadding, 0, 0);  
            header.invalidate();  
            headProgress.setVisibility(View.GONE);  
            arrowImg.clearAnimation();  
            arrowImg.setVisibility(View.VISIBLE);  
            tipsTxt.setText(mContext.getString(R.string.pullto_refresh));  
            break;  
        default:  
            break;  
        }  
    }

    private void measureView(View childView) {  
        ViewGroup.LayoutParams p = childView.getLayoutParams();  
        if (p == null) {  
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,  
					   ViewGroup.LayoutParams.WRAP_CONTENT);  
        }  
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);  
        int height = p.height;  
        int childHeightSpec;  
        if (height > 0) {  
            childHeightSpec = MeasureSpec.makeMeasureSpec(height,  
							  MeasureSpec.EXACTLY);  
        } else {  
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,  
							  MeasureSpec.UNSPECIFIED);  
        }  
        childView.measure(childWidthSpec, childHeightSpec);  
    }

    private void notifyWork(){
	Message msg = mHandler.obtainMessage();
	msg.what = BindGlassActivity.REQUEST_SCAN_DEVICE;
	mHandler.sendMessageDelayed(msg,0);
    }
   
}
