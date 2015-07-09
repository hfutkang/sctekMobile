package cn.ingenic.glasssync.sms;

import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidSrcContentProvider;

public class SmsMidProvider extends MidSrcContentProvider {

	@Override
	public SyncModule getSyncModule() {
		// TODO Auto-generated method stub
		return SmsModule.getInstance(
				getContext().getApplicationContext());
	}

}
