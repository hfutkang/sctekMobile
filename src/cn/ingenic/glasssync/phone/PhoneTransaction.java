package cn.ingenic.glasssync.phone;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.Transaction;
import cn.ingenic.glasssync.data.Projo;

import com.android.internal.telephony.ITelephony;

/** @author dfdun<br>
 * watch receive the data , and make some action depended on the data received 
 * */
public class PhoneTransaction extends Transaction {

    static int mState = 0 , mOldState = 0;
    static String mName,mIncomingNumber;
    @Override
    public void onStart(ArrayList<Projo> datas) {
        super.onStart(datas);
        int size = datas.size();
        if (1 == size) {
            Projo data = datas.get(0);
            int state = (Integer) data.get(PhoneColumn.state);

            String name = "", phoneNumber = "";
            switch (state) {
            case 21: // end call
                endCall(mContext);
                break;
            case 22: // answer call
                answerRingingCall(mContext);
                break;
            case 31: // send sms
                name = (String) data.get(PhoneColumn.name);
                phoneNumber = (String) data.get(PhoneColumn.phoneNumber);
                sendSms(phoneNumber, name);
            }
            if (LogTag.V) {
                Log.d(PhoneModule.TAG, "PhoneTransaction] received Commmand : "
                        + state);
            }
        }
    }

    private void sendSms(String number,String msg){
        PendingIntent pi=PendingIntent.getBroadcast(mContext, 0, new Intent(), 0);
        SmsManager sm=SmsManager.getDefault();
        sm.sendTextMessage(number, null, msg, pi, null);
        ContentValues values=new ContentValues(9);
        values.put("address",number);
        values.put("date", System.currentTimeMillis());
        values.put("type", 2);
        values.put("body", msg);
        mContext.getContentResolver().insert(Uri.parse("content://sms"), values);
        // below need permission that can not get
//        Uri uri = Uri.fromParts("smsto", number, null);
//        Intent intent = new Intent("com.android.mms.intent.action.SENDTO_NO_CONFIRMATION", uri);
//        intent.putExtra(Intent.EXTRA_TEXT, msg);
//        mContext.startService(intent);
    }

	
    private void endCall(Context context) { 
        Class<TelephonyManager> c = TelephonyManager.class;
        Method method = null;
        try {
            TelephonyManager tm = (TelephonyManager)context.getSystemService("phone");
            method = c.getDeclaredMethod("getITelephony", (Class[]) null);
            method.setAccessible(true);
            ITelephony iTel = (ITelephony) method.invoke(tm, (Object[]) null);
            iTel.endCall();
            Log.d(PhoneModule.TAG, "endCall ........");
        } catch (Exception e) {
            Log.e(PhoneModule.TAG, "OperatorCall()-" + e.toString());
        }
    }
    
    private synchronized void answerRingingCall(Context context) {
        Log.d(PhoneModule.TAG, "answer call ----------start-------");
        Class<TelephonyManager> c = TelephonyManager.class;
        Method method = null;
        try {
            TelephonyManager tm = (TelephonyManager)context.getSystemService("phone");
            method = c.getDeclaredMethod("getITelephony", (Class[]) null);
            method.setAccessible(true);
            ITelephony iTel = (ITelephony) method.invoke(tm, (Object[]) null);
            iTel.answerRingingCall();
            Log.d(PhoneModule.TAG, "answer call OK, use 1 .");
        }catch(Exception e){
            Log.e(PhoneModule.TAG, "answerRingingCall()-" + e.toString());
            try {
                Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                KeyEvent keyDown = new KeyEvent(0, 79);
                keyIntent.putExtra("android.intent.extra.KEY_EVENT", keyDown);
                context.sendOrderedBroadcast(keyIntent,
                        "android.permission.CALL_PRIVILEGED");

                KeyEvent keyUp = new KeyEvent(1, 79);
                keyIntent.putExtra("android.intent.extra.KEY_EVENT", keyUp);
                context.sendOrderedBroadcast(keyIntent,
                        "android.permission.CALL_PRIVILEGED");

                Intent plugIntent = new Intent(Intent.ACTION_HEADSET_PLUG);
                plugIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                plugIntent.putExtra("state", 1);
                plugIntent.putExtra("microphone", 1);
                plugIntent.putExtra("name", "Headset");
                context.sendOrderedBroadcast(plugIntent,
                        "android.permission.CALL_PRIVILEGED");

                keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                keyIntent.putExtra("android.intent.extra.KEY_EVENT", keyDown);
                context.sendOrderedBroadcast(keyIntent,
                        "android.permission.CALL_PRIVILEGED");

                keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                keyIntent.putExtra("android.intent.extra.KEY_EVENT", keyUp);
                context.sendOrderedBroadcast(keyIntent,
                        "android.permission.CALL_PRIVILEGED");

                plugIntent.putExtra("state", 0);
                context.sendOrderedBroadcast(plugIntent,
                        "android.permission.CALL_PRIVILEGED");
            } catch (Exception e1) {
                Log.e(PhoneModule.TAG, "answerRingingCall()-" + e1.toString());
                Intent ii = new Intent(Intent.ACTION_MEDIA_BUTTON);
                KeyEvent ke=new KeyEvent(1,79);
                ii.putExtra(Intent.EXTRA_KEY_EVENT, ke);
                context.sendOrderedBroadcast(ii, null);
            }
        }
      Log.d(PhoneModule.TAG, "answer call ----------end-------");
}
}
