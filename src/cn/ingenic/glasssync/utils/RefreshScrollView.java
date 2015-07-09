package cn.ingenic.glasssync.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.animation.RotateAnimation;
import android.view.View.OnTouchListener;
import android.view.animation.LinearInterpolator; 
import android.view.MotionEvent; 
import android.view.View.MeasureSpec; 
import android.os.AsyncTask;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;  
import cn.ingenic.glasssync.R;

public class RefreshScrollView extends ScrollView implements OnTouchListener {
    private int headerHeight;
    private int lastHeaderPadding;
    private ProgressBar headProgress;
    private ImageView arrowImg;
    private TextView tipsTxt;
    private RotateAnimation tipsAnimation;
    private RotateAnimation reverseAnimation;
    private int headerState = DONE;  
    private boolean isBack;
    private ScrollView sc;
    private LinearLayout header;
    static final private int RELEASE_To_REFRESH = 0;  
    static final private int PULL_To_REFRESH = 1;  
    static final private int REFRESHING = 2;  
    static final private int DONE = 3;  
    public RefreshScrollView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
        // LinearLayout globleLayout = (LinearLayout) findViewById(R.id.globleLayout);  
	// // sc = (ScrollView) globleLayout.findViewById(R.id.scrollView);  
        // LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
          
        //  header = (LinearLayout) inflater.inflate(R.layout.pull_to_refresh, null);  
	// header = this;
        // measureView(header);  
	LayoutInflater inflate = LayoutInflater.from(context);
	inflate.inflate(R.layout.pull_to_refresh, this);

        headerHeight = getMeasuredHeight();  
        lastHeaderPadding = (-1*headerHeight); //最后一次调用Move Header的Padding  
        setPadding(0, lastHeaderPadding, 0, 0);  
        invalidate();  
        // globleLayout.addView(header,0);  
        headProgress = (ProgressBar) this.findViewById(R.id.head_progressBar);  
        arrowImg = (ImageView) this.findViewById(R.id.head_arrowImageView);  
        arrowImg.setMinimumHeight(50);  
        arrowImg.setMinimumWidth(50);  
        tipsTxt = (TextView) this.findViewById(R.id.head_tipsTextView);  
          	Log.d("RefreshScrollView", "---------3" );
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
          	Log.d("RefreshScrollView", "---------3" );
    }  
                
 @Override  
     public boolean onTouch(View v, MotionEvent event) {  
     int beginY=0;  
     switch (event.getAction()) {  
     case MotionEvent.ACTION_MOVE:  
	   //sc.getScrollY == 0  scrollview 滑动到头了   
	   //lastHeaderPadding > (-1*headerHeight) 表示header还没完全隐藏起来时  
	   //headerState != REFRESHING 当正在刷新时  
	 if((sc.getScrollY() == 0 || lastHeaderPadding > (-1*headerHeight)) && headerState != REFRESHING) {  
	       //拿到滑动的Y轴距离  
	     int interval = (int) (event.getY() - beginY);  
	       //是向下滑动而不是向上滑动  
	     if (interval > 0) {  
		 interval = interval/2;//下滑阻力  
		 lastHeaderPadding = interval + (-1*headerHeight);  
		 setPadding(0, lastHeaderPadding, 0, 0);  
		 if(lastHeaderPadding > 0) {  
		       //txView.setText("我要刷新咯");  
		     headerState = RELEASE_To_REFRESH;  
		       //是否已经更新了UI  
		     if(! isBack) {  
			 isBack = true;  //到了Release状态，如果往回滑动到了pull则启动动画  
			 changeHeaderViewByState();  
		     }  
		 } else {  
		     headerState = PULL_To_REFRESH;  
		     changeHeaderViewByState();  
		       //txView.setText("看到我了哦");  
		       //sc.scrollTo(0, headerPadding);  
		 }  
	     }  
	 }  
	 break;  
     case MotionEvent.ACTION_DOWN:  
	   //加上下滑阻力与实际滑动距离的差（大概值）  
	 beginY = (int) ((int) event.getY() + sc.getScrollY()*1.5);  
	 break;  
     case MotionEvent.ACTION_UP:  
	 if (headerState != REFRESHING) {  
	     switch (headerState) {  
	     case DONE:  
		   //什么也不干  
		 break;  
	     case PULL_To_REFRESH:  
		 headerState = DONE;  
		 lastHeaderPadding = -1*headerHeight;  
		 setPadding(0, lastHeaderPadding, 0, 0);  
		 changeHeaderViewByState();  
		 break;  
	     case RELEASE_To_REFRESH:  
		 isBack = false; //准备开始刷新，此时将不会往回滑动  
		 headerState = REFRESHING;  
		 changeHeaderViewByState();  
		 onRefresh();  
		 break;  
	     default:  
		 break;  
	     }  
	 }  
	 break;  
     }  
       //如果Header是完全被隐藏的则让ScrollView正常滑动，让事件继续否则的话就阻断事件  
     if(lastHeaderPadding > (-1*headerHeight) && headerState != REFRESHING) {  
	 return true;  
     } else {  
	 return false;  
     }  
 }  

    private void changeHeaderViewByState() {  
        switch (headerState) {  
        case PULL_To_REFRESH:  
            // 是由RELEASE_To_REFRESH状态转变来的  
            if (isBack) {  
                isBack = false;  
                arrowImg.startAnimation(reverseAnimation);  
                tipsTxt.setText("下拉刷新");  
            }  
            tipsTxt.setText("下拉刷新");  
            break;  
        case RELEASE_To_REFRESH:  
            arrowImg.setVisibility(View.VISIBLE);  
            headProgress.setVisibility(View.GONE);  
            tipsTxt.setVisibility(View.VISIBLE);  
            arrowImg.clearAnimation();  
            arrowImg.startAnimation(tipsAnimation);  
            tipsTxt.setText("松开刷新");  
            break;  
        case REFRESHING:  
            lastHeaderPadding = 0;  
            setPadding(0, lastHeaderPadding, 0, 0);  
            invalidate();  
            headProgress.setVisibility(View.VISIBLE);  
            arrowImg.clearAnimation();  
            arrowImg.setVisibility(View.INVISIBLE);  
            tipsTxt.setText("正在刷新...");  
            break;  
        case DONE:  
            lastHeaderPadding = -1 * headerHeight;  
            setPadding(0, lastHeaderPadding, 0, 0);  
            invalidate();  
            headProgress.setVisibility(View.GONE);  
            arrowImg.clearAnimation();  
            arrowImg.setVisibility(View.VISIBLE);  
            tipsTxt.setText("下拉刷新");  
            break;  
        default:  
            break;  
        }  
    }  
    private void onRefresh() {  
        new AsyncTask<Void, Void, Void>() {  
            protected Void doInBackground(Void... params) {  
                try {  
                    Thread.sleep(2000);  
                } catch (Exception e) {  
                    e.printStackTrace();  
                }  
                return null;  
            }  
  
            @Override  
            protected void onPostExecute(Void result) {  
                onRefreshComplete();  
            }  
  
        }.execute();  
    }  
    public void onRefreshComplete() {  
        headerState = DONE;  
        changeHeaderViewByState();  
    }  
    //由于OnCreate里面拿不到header的高度所以需要手动计算  
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
}