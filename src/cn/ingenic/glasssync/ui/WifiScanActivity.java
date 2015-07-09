package cn.ingenic.glasssync.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ingenic.glasssync.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class WifiScanActivity extends Activity {

    private ListView lv;
    private String TAG = "WifiScanDialog";
    private ArrayList <Map<String,Object>> mWifiItem = new ArrayList<Map<String,Object>>();
    /** These values are matched in string arrays -- changes must be kept in sync */
    private static final int SECURITY_NONE = 0;
    private static final int SECURITY_WEP = 1;
    private static final int SECURITY_PSK = 2;
    private static final int SECURITY_EAP = 3;
    
    private enum PskType {
        UNKNOWN,
	    WPA,
	    WPA2,
	    WPA_WPA2
	    }
    PskType pskType = PskType.UNKNOWN;

    @Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.wifi_scan_list);
	getWifiResult();
	lv = (ListView) findViewById(R.id.scan_list);
	lv.setAdapter(new SimpleAdapter(this,mWifiItem,R.layout.wifi_scan_item,new String[]{"SSID","SECURITY_STRING"},new int []{R.id.wifiName,R.id.wifiSecurity}));
	lv.setOnItemClickListener(new OnItemClickListener() {
		@Override
		    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					    long arg3) {
		    showWifiPasswordDialog((mWifiItem.get(arg2).get("SSID")).toString());
		}
	    });
    }
    private void getWifiResult(){
	WifiManager wifiManager = (WifiManager)this.getSystemService(this.WIFI_SERVICE);
	List<ScanResult> results = wifiManager.getScanResults();
	if(results != null){
	    for(ScanResult result : results){
		if (result.SSID == null || result.SSID.length() == 0
		    || result.capabilities.contains("[IBSS]")) {
		    continue;
		}
		int security = getSecurity(result);
		boolean found = false;
		for(Map<String, Object> item:mWifiItem){
		    if(item.get("SSID").equals(result.SSID.toString()) && Integer.parseInt(item.get("SECURITY_INT").toString()) == security){
			found = true;
			break;
		    }
		}
		if(!found){
		    Map<String, Object> map = new HashMap<String, Object>();
		    map.put("SSID",result.SSID.toString());
		    map.put("SECURITY_INT", security);
		    map.put("SECURITY_STRING", getSecurityString(security,result));
		    mWifiItem.add(map);
		}
	    }
	}else {
	    Toast.makeText(this.getApplicationContext(),R.string.wifi_list_null,
			   Toast.LENGTH_SHORT).show();
	    setResult(RESULT_CANCELED);
	    finish();

	}
	
    }
    private  int getSecurity(ScanResult result) {
	if (result.capabilities.contains("WEP")) {
	    return SECURITY_WEP;
	} else if (result.capabilities.contains("PSK")) {
	    return SECURITY_PSK;
	} else if (result.capabilities.contains("EAP")) {
	    return SECURITY_EAP;
	}
	return SECURITY_NONE;
    }
    private String getSecurityString(int security,ScanResult result) {//get the security type of the wifi
        Context context = this.getApplicationContext();
        pskType = getPskType(result);
        String SecurityString="";
        switch(security) {
	case SECURITY_EAP:
	    SecurityString = context.getString(R.string.wifi_security_short_eap);
	    break;
	case SECURITY_PSK:
	    switch (pskType) {
	    case WPA:
		SecurityString = context.getString(R.string.wifi_security_short_wpa);
		break;
	    case WPA2:
		SecurityString = context.getString(R.string.wifi_security_short_wpa2);
		break;
	    case WPA_WPA2:
		SecurityString = context.getString(R.string.wifi_security_short_wpa_wpa2);
		break;
	    case UNKNOWN:
	    default:
		SecurityString = context.getString(R.string.wifi_security_short_psk_generic);
		break;
	    }
	    break;
	case SECURITY_WEP:
	    SecurityString = context.getString(R.string.wifi_security_short_wep);
	    break;
	case SECURITY_NONE:
	    SecurityString = context.getString(R.string.wifi_free);
	    break;
	default:
	    break;
        }
	String securityStrFormat = context.getString(R.string.wifi_security_none);
        if (security != SECURITY_NONE) {
	    securityStrFormat = context.getString(R.string.wifi_secured_first_item);
	}else{
	    return SecurityString;
	}
        StringBuilder summary = new StringBuilder();
        summary.append(String.format(securityStrFormat, SecurityString));
        return summary.toString();
    }
    private static  PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            return PskType.UNKNOWN;
        }
    }
    private void showWifiPasswordDialog(String str){
        final EditText inputServer = new EditText(this);
        inputServer.setFocusable(true);
	final String WifiName = str;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	String title = getResources().getString(R.string.wifi_dialog_title);  
        String dialogTitle = String.format(title,WifiName);
	builder.setTitle(dialogTitle);
	builder.setView(inputServer);
	builder.setPositiveButton(R.string.wifi_dialog_ok,new DialogInterface.OnClickListener() {	
		public void onClick(DialogInterface dialog, int which) {
		    String wifiPassword = inputServer.getText().toString();
		    Intent intent = new Intent();
		    intent.putExtra("wifiName", WifiName);
		    intent.putExtra("wifiPassword", wifiPassword);
		    setResult(RESULT_OK,intent);
		    finish();
		}
	    });
	builder.setNegativeButton(R.string.wifi_dialog_cancle,new DialogInterface.OnClickListener() {	
		public void onClick(DialogInterface dialog, int which) {
		    setResult(RESULT_CANCELED);
		    finish();
		}
	    });
	builder.show();
    }
}