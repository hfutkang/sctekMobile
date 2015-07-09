package cn.ingenic.glasssync.updater;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cn.ingenic.glasssync.ILocalBinder;
import cn.ingenic.glasssync.IRemoteBinder;
import cn.ingenic.glasssync.RemoteBinderException;
import cn.ingenic.glasssync.data.RemoteParcel;

/**
 * for update sync; run on watch client
 * @author dfdun*/
public class UpdaterRemoteServiceImpl implements ILocalBinder, IUpdaterRemoteService {
	
	private static final int BASE = 0;
	private static final int UpdaterRemoteServiceImpl_get = BASE + 1;

	private Context mContext;
	public UpdaterRemoteServiceImpl(Context context){
	    mContext = context;
	}
	
    @Override
    public String get(String name) {
        Log.i("OtaUpdater", "UpdaterRemoteServiceImpl] get(), Key :" + name);
        if ("currentVersion".equals(name))
            return android.os.Build.DISPLAY;// "mensa-2.2.2.2.2";
        else if ("model".equals(name)) {
            return android.os.Build.MODEL;
        } else if ("update".equals(name)) { // to install update file
            ;
        }
        return "key error";
    }

	public RemoteParcel onTransact(int code, RemoteParcel request) {
		RemoteParcel reply = new RemoteParcel();
		switch (code) {
		case UpdaterRemoteServiceImpl_get:
			String arg = request.readString();
			String result = get(arg);
			reply.writeString(result);
			break;
		}
		return reply;
	}
	
	static IUpdaterRemoteService asRemoteInterface(IRemoteBinder binder) {
		return new UpdaterRemoteServiceProxy(binder);
	}

	public static class UpdaterRemoteServiceProxy implements IUpdaterRemoteService {
		
		private final IRemoteBinder mRemoteBinder;
		
		private UpdaterRemoteServiceProxy(IRemoteBinder remoteBinder) {
			mRemoteBinder = remoteBinder;
		}
		
		@Override
		public String get(String name) {
			RemoteParcel request = new RemoteParcel();
			
			request.writeString(name);
			RemoteParcel reply;
			try {
				reply = mRemoteBinder.transact(UpdaterRemoteServiceImpl_get, request);
				if(reply == null)
					return null;
				return reply.readString();
			} catch (RemoteBinderException e) {
				return null;
			}
		}
		
	}

}
