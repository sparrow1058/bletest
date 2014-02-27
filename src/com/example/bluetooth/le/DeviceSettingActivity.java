package com.example.bluetooth.le;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.Toast;

public class DeviceSettingActivity extends PreferenceActivity {
   
    /**�Զ��岼��A**/
    Preference preference0 = null;
    
    /**�Զ��岼��B**/
    Preference preference1 = null;
    
    private Switch find_switch=null;
    
    Context mContext = null;
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.preference_head);
	// ����Դ�ļ�����Preferences ��ѡ���ֵ�����Զ����浽SharePreferences
	addPreferencesFromResource(R.layout.device_setting2);
	mContext = this;
	find_switch=(Switch)findViewById(R.id.switch1);
	find_switch.setOnCheckedChangeListener(new OnCheckedChangeListener() {  
		  
        @Override  
        public void onCheckedChanged(CompoundButton buttonView,  
                boolean isChecked) {  
            // TODO Auto-generated method stub  
            if (isChecked) {  
            	Toast.makeText(mContext, "Find ", Toast.LENGTH_LONG).show();
            } else {  
            	Toast.makeText(mContext, "Not Find", Toast.LENGTH_LONG).show();
            }  
        }  
    }); 
    }
}
