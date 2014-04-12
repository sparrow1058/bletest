package com.example.bluetooth.le;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SmartSetActivity extends Activity implements OnClickListener, OnSeekBarChangeListener{
	private String BLE_TAG="leaf smart set";
	private ImageView bt_loss;
	private ImageView bt_msg;
	private ImageView bt_call;
	private SeekBar loss_bar;
	private TextView loss_view;
	public SharedPreferences bleSettings;
  
    private final String lossFlagKey="loss_check";
    private final String shakeFlagKey="shake_check";
    private final String phoneAlertKey="phone_check";
    private final String msgAlertKey="msg_check";
    private final String alertLevelKey="alert_list";
    private final String responseTimeKey="time_list";
    private final String lossRangeKey="LossRange";
    private  String lossStrPre="";
    
    
    private int saveLossRange;
    private String alertLevelValue;
    private String responseTimeValue;
    private boolean lossFlag;
    private boolean shakeFlag;
    private boolean phoneFlag;
    private boolean msgFlag;
    
    private final int LOSS_ID =0;
    private final int MSG_ID	=1;
    private final int CALL_ID =2;
    private final int LOSSRANGE_ID =3;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.gatt_services_characteristics);
		setContentView(R.layout.smart_set);
		bt_loss=(ImageView)findViewById(R.id.bt_loss);
		bt_msg=(ImageView)findViewById(R.id.bt_msg);
		bt_call=(ImageView)findViewById(R.id.bt_call);
		bt_loss.setOnClickListener(this);
		bt_msg.setOnClickListener(this);
		bt_call.setOnClickListener(this);
		loss_bar=(SeekBar)findViewById(R.id.lossBar);
		loss_bar.setOnSeekBarChangeListener(this);
		loss_view=(TextView)findViewById(R.id.loss_view);
		
		lossStrPre=this.getString(R.string.loss_range);
		// 从资源文件中添Preferences ，选择的值将会自动保存到SharePreferences
	//	addPreferencesFromResource(R.layout.device_setting2);
	//	createPreferenceUI();
		bleSettings=PreferenceManager.getDefaultSharedPreferences(this); 
		loadDefaultUI();
	}
	public void onDestory(){
		//final Intent intent = new Intent(this, DeviceSettingActivity.class); loadDefaultUI
		Log.i(BLE_TAG,"all state ="+ saveLossRange+ " "+ alertLevelValue+ " "+responseTimeValue+ " "+lossFlag+ " "+shakeFlag+ " "+phoneFlag+ " ");
	}

	public void onClick(View v) {
		// TODO 自动生成的方法存根
		Log.i("smart set","leaf view click"+v);
		if(v==bt_loss){
			lossFlag=!lossFlag;
			updateSettingUI(LOSS_ID,lossFlag);
			updateSettingPreference(LOSS_ID,lossFlag);
		}
		if(v==bt_msg){
			msgFlag=!msgFlag;
			updateSettingUI(MSG_ID,msgFlag);
			updateSettingPreference(MSG_ID,msgFlag);
		}
		if(v==bt_call){
			phoneFlag=!phoneFlag;
			updateSettingUI(CALL_ID,phoneFlag);
			updateSettingPreference(CALL_ID,phoneFlag);
		}
		
	}	
private void loadDefaultUI(){
	    
       //为各个Preference注册监听接口  

	//	bleSettings=getPreferences(Activity.MODE_PRIVATE);
		saveLossRange=bleSettings.getInt("LossRange", 2);
		updateSettingPreference(LOSSRANGE_ID,saveLossRange);	
//		alertLevelValue=bleSettings.getString(alertLevelKey, "mid");
	//	responseTimeValue=bleSettings.getString(responseTimeKey, "50ms");
		lossFlag=bleSettings.getBoolean(lossFlagKey, true);
		updateSettingUI(LOSS_ID,lossFlag);
//		shakeFlag=bleSettings.getBoolean(shakeFlagKey, true);
		phoneFlag=bleSettings.getBoolean(phoneAlertKey, false);
		updateSettingUI(CALL_ID,phoneFlag);
		msgFlag=bleSettings.getBoolean(msgAlertKey, false);
		updateSettingUI(MSG_ID,msgFlag);
		
		Log.i(BLE_TAG,"all state ="+lossFlag+ " "+ " "+phoneFlag+ " "+msgFlag);
	}
private void updateSettingUI(int id,boolean flag)
{
	int resid=0;
	if(flag)
		resid=R.drawable.bt_on;
	else
		resid=R.drawable.bt_off;
	
	switch (id)
	{	
		case LOSS_ID:
			bt_loss.setImageResource(resid);
			break;
		case MSG_ID:
			bt_msg.setImageResource(resid);
			break;
		case CALL_ID:
			bt_call.setImageResource(resid);
			break;
	}
}
private void updateSettingPreference(int id,boolean flag)
{
	
	SharedPreferences.Editor batEditor=bleSettings.edit();
	switch (id)
	{	
		case LOSS_ID:
			batEditor.putBoolean(lossFlagKey, flag);
			batEditor.commit();
			break;
		case MSG_ID:
			batEditor.putBoolean(msgAlertKey, flag);
			batEditor.commit();
			break;
		case CALL_ID:
			batEditor.putBoolean(phoneAlertKey, flag);
			batEditor.commit();
			break;
	}
}
	private void updateSettingPreference(int id,int  value)
	{
		
		SharedPreferences.Editor batEditor=bleSettings.edit();
		switch (id)
		{	
			case LOSSRANGE_ID:
				batEditor.putInt(lossRangeKey, value);
				batEditor.commit();
				break;
		}
	
	}
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		// TODO 自动生成的方法存根
		//String lossStr=getResources().getString(R.string.loss_range);
		loss_view.setText(lossStrPre+(progress+1)+" M");
	}
	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO 自动生成的方法存根
		
	}
	@Override
		public void onStopTrackingTouch(SeekBar seekBar)
		{
//			
			int progress=loss_bar.getProgress();
			loss_view.setText(lossStrPre);
			//SharedPreferences barState=getPreferences(0);
			updateSettingPreference(LOSSRANGE_ID,progress);	
			
	}
}
