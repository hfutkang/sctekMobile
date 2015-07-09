package cn.ingenic.glasssync.ui;

import cn.ingenic.glasssync.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import android.util.Log;

public class ElementView extends RelativeLayout{
    private final String TAG="ElementView";
    private TextView mTitle;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    public static final int COLUMN_STATUS_UNCHECK = 0;
    public static final int COLUMN_STATUS_CHECKING = 1;
    public static final int COLUMN_STATUS_CHECKED = 2;
    private RelativeLayout mRoot;
    private final int DEFAULT_COLOR = getResources().getColor(R.color.default_text_color);
    private final int CHECKING_COLOR = getResources().getColor(R.color.checking_text_color);

    public ElementView(Context context) {
	this(context, null);
    }
    
    public ElementView(Context context, AttributeSet attrs) {
	super(context, attrs);
	LayoutInflater inflater = (LayoutInflater) context
	    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	inflater.inflate(R.layout.setting_element, this, true);
	mTitle = (TextView) findViewById(R.id.title);
	mProgressBar = (ProgressBar) findViewById(R.id.progress);
	mRoot = (RelativeLayout)findViewById(R.id.root);
	mImageView = (ImageView) findViewById(R.id.iv_contact_complete);
    }
	
    public void setTitle(String title){
	mTitle.setText(title);
    }



    public void setShow(int status){
	switch(status){
	case COLUMN_STATUS_UNCHECK:
	    mTitle.setTextColor(DEFAULT_COLOR);
	    mImageView.setVisibility(View.GONE);
	    mRoot.setBackgroundResource(R.drawable.bgcolor_default);
	    mProgressBar.setVisibility(View.GONE);
	    break;
	case COLUMN_STATUS_CHECKING:
	    mTitle.setTextColor(CHECKING_COLOR);
	    mProgressBar.setVisibility(View.VISIBLE);
	    break;
	case COLUMN_STATUS_CHECKED:
	    mTitle.setTextColor(DEFAULT_COLOR);
	    mImageView.setVisibility(View.VISIBLE);
	    mRoot.setBackgroundResource(R.drawable.bgcolor_checked);
	    mProgressBar.setVisibility(View.GONE);
	    break;
	default:
	    break;
	}
    }
}
