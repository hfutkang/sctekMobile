package cn.ingenic.glasssync.phone;

import cn.ingenic.glasssync.LogTag;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;


public class OutCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String number=intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            String name=getNameFormNumber(context,number);
        //  set data For ListenPhone to use
            ListenPhone.getInstance(context).setOutCall(number, name);
            if (LogTag.V) {
                Log.d(PhoneModule.TAG, "ListenPhone. OutCallReceiver]  OutCall : "+number);
            }
        }
    }
    
    private String getNameFormNumber(Context c, String number){
        String name="";
        String []pro ={ContactsContract.PhoneLookup.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cur = c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, pro,
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