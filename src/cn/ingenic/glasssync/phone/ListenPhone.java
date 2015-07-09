package cn.ingenic.glasssync.phone;

import java.util.ArrayList;
import java.util.EnumSet;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.text.TextUtils;
import android.util.Log;
import cn.ingenic.glasssync.Config;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.data.DefaultProjo;
import cn.ingenic.glasssync.data.Projo;
import cn.ingenic.glasssync.data.ProjoType;

/**
 * listen Phone's state, send data to Watch
 * */
public class ListenPhone extends PhoneStateListener {

    Context mContext;
    int mOld=0,mNew=0;
    String mOutCallNumber, mOutCallName;
    private static ListenPhone mInstance;
    
    private ListenPhone(Context context){
        mContext= context;
    }
    
    static ListenPhone getInstance(Context context){
        if(mInstance==null){
            mInstance = new ListenPhone(context);
        }
        return mInstance;
    }
    
    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        mOld = mNew;
        mNew = state;
        if (mOld == mNew) {
            // || (mOld == 2 && mNew == 1)) ,
            return;
        }
        String name = getNameFormNumber(incomingNumber);
        if((mOld == 0 && mNew == 2)){
            name = mOutCallName;
            incomingNumber = mOutCallNumber;
        }
        //  send (data) to the Watch client
        Projo projo = new DefaultProjo(EnumSet.allOf(PhoneColumn.class), ProjoType.DATA);
        projo.put(PhoneColumn.state, state);
        projo.put(PhoneColumn.name, name);
        projo.put(PhoneColumn.phoneNumber, incomingNumber);
        Config config = new Config(PhoneModule.PHONE);
        ArrayList<Projo> datas = new ArrayList<Projo>(1);
        datas.add(projo);
        DefaultSyncManager.getDefault().request(config, datas);
        if (LogTag.V) {
            Log.d(PhoneModule.TAG, "ListenPhone ], state : "+state + " ,"+incomingNumber);
        }
    }

    void setOutCall(String number, String name) {
        mOutCallNumber = number;
        mOutCallName = name;
    }
    
    private String getNameFormNumber(String number){
        String name="";
        if(TextUtils.isEmpty(number)) return "";
        String []pro ={ContactsContract.PhoneLookup.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cur = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, pro,
                ContactsContract.CommonDataKinds.Phone.NUMBER+" = '"+number+"'", null, null);
        if (cur.moveToFirst()) {
            int nameIndex = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            name = cur.getString(nameIndex);
        }
        cur.close();
        Log.i(PhoneModule.TAG, "get name for number is "+name);
        return name;
    }

}
