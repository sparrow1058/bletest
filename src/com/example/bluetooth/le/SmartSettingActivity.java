package com.example.bluetooth.le;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SmartSettingActivity extends PreferenceActivity implements OnPreferenceChangeListener{
    
	private String BLE_TAG="LEAF BLE";
	private CheckBoxPreference  lossFlagCheckBox;
    private CheckBoxPreference  shakeFlagCheckBox;
    private CheckBoxPreference  phoneAlertCheckBox;
    private CheckBoxPreference  msgAlertCheckBox;
    private ListPreference	alertLevelList;
    private ListPreference	responseTimeList;
    public SharedPreferences bleSettings;
  
    private final String lossFlagKey="loss_check";
    private final String shakeFlagKey="shake_check";
    private final String phoneAlertKey="phone_check";
    private final String msgAlertKey="msg_check";
    private final String alertLevelKey="alert_list";
    private final String responseTimeKey="time_list";
    
    private int saveLossRange;
    private String alertLevelValue;
    private String responseTimeValue;
    private boolean lossFlag;
    private boolean shakeFlag;
    private boolean phoneFlag;
    private boolean msgFlag;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.gatt_services_characteristics);
		setContentView(R.layout.smart_setting);
		// 从资源文件中添Preferences ，选择的值将会自动保存到SharePreferences
		addPreferencesFromResource(R.layout.device_setting2);
	//	createPreferenceUI();
	}
	public void onDestory(){
		final Intent intent = new Intent(this, DeviceSettingActivity.class);
		
	}	
private void createPreferenceUI(){
	    
	    lossFlagCheckBox=(CheckBoxPreference)findPreference(lossFlagKey);
	    shakeFlagCheckBox=(CheckBoxPreference)findPreference(shakeFlagKey);
	    phoneAlertCheckBox=(CheckBoxPreference)findPreference(phoneAlertKey);
	    msgAlertCheckBox=(CheckBoxPreference)findPreference(msgAlertKey);
	    alertLevelList=(ListPreference)findPreference(alertLevelKey);
	    responseTimeList=(ListPreference)findPreference(responseTimeKey);
        //为各个Preference注册监听接口  
	    lossFlagCheckBox.setOnPreferenceChangeListener(this);  
	    shakeFlagCheckBox.setOnPreferenceChangeListener(this);  
	    phoneAlertCheckBox.setOnPreferenceChangeListener(this);  
	    msgAlertCheckBox.setOnPreferenceChangeListener(this);  
	    alertLevelList.setOnPreferenceChangeListener(this);  
	    responseTimeList.setOnPreferenceChangeListener(this); 
	//	bleSettings=getPreferences(Activity.MODE_PRIVATE);
	    bleSettings=PreferenceManager.getDefaultSharedPreferences(this); 
		saveLossRange=bleSettings.getInt("LossRange", 2);
		alertLevelValue=bleSettings.getString(alertLevelKey, "mid");
		responseTimeValue=bleSettings.getString(responseTimeKey, "50ms");
		lossFlag=bleSettings.getBoolean(lossFlagKey, true);
		shakeFlag=bleSettings.getBoolean(shakeFlagKey, true);
		phoneFlag=bleSettings.getBoolean(phoneAlertKey, false);
		msgFlag=bleSettings.getBoolean(msgAlertKey, false);
		Log.i(BLE_TAG,"all state ="+ saveLossRange+ " "+ alertLevelValue+ " "+responseTimeValue+ " "+lossFlag+ " "+shakeFlag+ " "+phoneFlag+ " ");
	}

public boolean onPreferenceChange(Preference preference, Object newValue) {
	// TODO 自动生成的方法存根
       //判断是哪个Preference改变了  
    if(preference.getKey().equals(lossFlagKey))  
    {  
    	lossFlag=(Boolean)newValue;
        Log.v("SystemSetting", "lossFlagKey is changed");  
    } 
    return true;
}	
}
