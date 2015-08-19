package cn.ingenic.glasssync.screen;

import cn.ingenic.glasssync.R;
import android.util.Log;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.content.Intent;
import cn.ingenic.glasssync.screen.screenshare.ScreenModule;
import cn.ingenic.glasssync.screen.control.ScreenControlView;

public class Fragment_Screen extends Fragment implements OnClickListener {
    private static final String TAG = "ScreenShareControlActivity";
    private View fragmentView;
    private TextView mScreenButton, mLiveButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    
	if (fragmentView == null) {
	    fragmentView = inflater.inflate(R.layout.screen_share_controller_button, null);
	}	  
	ViewGroup parent = (ViewGroup) fragmentView.getParent();
	if (parent != null) {
	    parent.removeView(fragmentView);
	}	

	mScreenButton = (TextView)fragmentView.findViewById(R.id.screenButton);
	mScreenButton.setOnClickListener(this);

	mLiveButton = (TextView)fragmentView.findViewById(R.id.liveButton);
	mLiveButton.setOnClickListener(this);

	return fragmentView;
    }

    @Override
    public void onDestroyView() {
	super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
	switch (v.getId()) {
	case R.id.screenButton:
	    Intent intent = new Intent(getActivity(), ScreenShareControlActivity.class);
	    startActivity(intent);	
	    break;
	case R.id.liveButton:
	    Intent liveIntent = new Intent(getActivity(), LiveDisplayActivity.class);
	    startActivity(liveIntent);	
	    break;
	default:
	    break;
	}
    }
}
