/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.example.bluetooth.le.R;


/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
@SuppressLint("NewApi")
public class DeviceSettingActivity extends PreferenceActivity implements OnSeekBarChangeListener, OnPreferenceChangeListener, OnClickListener {
	private final static String TAG = DeviceControlActivity.class
			.getSimpleName();

	public static final String BLE_TAG="leaf ble";
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
    private final String BATTERY_UUID="00002a19-0000-1000-8000-00805f9b34fb";
    private final String ALERT_UUID="00002a06-0000-1000-8000-00805f9b34fb";
    private final String TXPWR_UUID="00002a07-0000-1000-8000-00805f9b34fb";
    

    private final String lossFlagKey="loss_check";
    private final String shakeFlagKey="shake_check";
    private final String phoneAlertKey="phone_check";
    private final String msgAlertKey="msg_check";
    private final String alertLevelKey="alert_list";
    private final String responseTimeKey="time_list";
    
    private final int 	MSG_READ_RSSI=0;
    private final int 	MSG_READ_BATTERY=1;
    
    private     TelephonyManager manager ;  
	private SoundPool sp;	//define a soundpool
	private int s_dog,s_monk,s_duck,s_cat;	//set sournd id
	private TextView mConnectionState;
	private TextView mDataField;
	private String mDeviceName;
	private String mDeviceAddress;
	private ExpandableListView mGattServicesList;
	private BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;


	public BluetoothGattService alertGattService;
	public BluetoothGattService batteryGattService;
	public BluetoothGattCharacteristic alertChar;
	public BluetoothGattCharacteristic batteryChar;
	private Switch find_switch=null;
    private Context mContext = null;
    //private 
    private int saveLossRange;
    private String alertLevelValue;
    private String responseTimeValue;
    private boolean lossFlag;
    private boolean shakeFlag;
    private boolean phoneFlag;
    private boolean msgFlag;
  
    private int rssi_count;
    public ImageView bat_id;
    public TextView rssi_view;
        public ImageView rssi_id;
    public SeekBar lossBar_id;
    public TextView lossText_id;
    private CheckBoxPreference  lossFlagCheckBox;
    private CheckBoxPreference  shakeFlagCheckBox;
    private CheckBoxPreference  phoneAlertCheckBox;
    private CheckBoxPreference  msgAlertCheckBox;
    private ListPreference	alertLevelList;
    private ListPreference	responseTimeList;
    
    public SharedPreferences bleSettings;
    
	// Code to manage Service lifecycle.
    private final Timer timer = new Timer();
    private final Timer bat_timer=new Timer();
        Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {  
            // TODO Auto-generated method stub  
            // 要做的事情  
        	Log.i(BLE_TAG,"msg.what= "+msg.what);
        	switch(msg.what)
        	{
        	case MSG_READ_RSSI:

        		if(mBluetoothLeService!=null)
        			mBluetoothLeService.readRemoteRssi();
        		break;
        	case MSG_READ_BATTERY:
        	//	Log.i(BLE_TAG,"BATTERY="+mBluetoothLeService.remoteRssi);
        		mBluetoothLeService.readCharacteristic(batteryChar);
        		break;
        	}
//            super.handleMessage(msg);  
        }  
    }; 
    TimerTask task = new TimerTask() {  
        @Override  
        public void run() {  
            // TODO Auto-generated method stub
        	Message tMsg=new Message(); 
        	tMsg.what=MSG_READ_RSSI;
            handler.sendMessage(tMsg);
    		//if(mBluetoothLeService!=null)
    		//	mBluetoothLeService.readRemoteRssi();
        }  
    };
    TimerTask bat_task = new TimerTask() {  
        @Override  
        public void run() {  
            // TODO Auto-generated method stub
        //	Message tMsg=new Message(); 
        	//tMsg.what=MSG_READ_BATTERY;
            //handler.sendMessage(tMsg);
        	Log.i(BLE_TAG," read battery level");
            if(mBluetoothLeService!=null)
            	mBluetoothLeService.readCharacteristic(batteryChar);
            //if(mBluetoothLeService!=null)
    		//	mBluetoothLeService.readRemoteRssi();
        }  
    }; 
    //timer.schedule(task, 2000, 2000);
    //timer.cancel(); 
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(BLE_TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {

			mBluetoothLeService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	//ACTION_READ_RSSI: // read the rssi
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			String extra_uuid="";
			int rssiValue = 0;
			System.out.println("action = " + action);
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
			//	updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
			//	updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				updateBatteryInfo(0);
				updateRssiView(-1000);

			//	clearUI();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayGattServices(mBluetoothLeService
						.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				extra_uuid=intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
				Log.v(BLE_TAG,"extra uuid= "+extra_uuid);
				if (extra_uuid.equals("00002a19"))
				{
					String batLevel=intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
					Log.i("leaf battery ","level="+batLevel);
					updateBatteryInfo(20);	
					//updateBatteryInfo(batLevel);
				//	updateBatteryInfo(Integer.parseInt(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)));
				}	
					//	displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}else if(BluetoothLeService.ACTION_READ_RSSI.equals(action)){
				//Log.v(BLE_TAG,"RSSI=" +intent.getStringExtra(BluetoothLeService.EXTRA_DATA)+ " "+bat_id);
			//	bat_id.setText(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				updateRssiView(Integer.parseInt(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)));
			}
		}
	};
	private void clearUI() {

	}
	@SuppressWarnings("deprecation")
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
		
		lossBar_id.setProgress(saveLossRange);
		
		
	}
	public void alertSet(int level)
	{
		if(alertChar!=null){
			alertChar.setValue(level, 17, 0);
			mBluetoothLeService.wirteCharacteristic(alertChar);
		}
	}
	public void midAlert(boolean on)
	{
		if(on)
			s_dog=sp.play(s_dog, 1, 1, 0, 20, 1);
		else
			sp.stop(s_dog);
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.gatt_services_characteristics);
		setContentView(R.layout.preference_head);
		// 从资源文件中添Preferences ，选择的值将会自动保存到SharePreferences
		addPreferencesFromResource(R.layout.device_setting2);
		mContext = this;
		bat_id=(ImageView)findViewById(R.id.battery_id);
		rssi_id=(ImageView)findViewById(R.id.rssi_id);
		rssi_id.setOnClickListener(this);
		rssi_view=(TextView)findViewById(R.id.rssi_view);
		rssi_view.setVisibility(View.GONE);
		find_switch=(Switch)findViewById(R.id.switch1);
		lossBar_id=(SeekBar)findViewById(R.id.lossBar);
		lossText_id=(TextView)findViewById(R.id.lossText);
		lossBar_id.setOnSeekBarChangeListener(this);
        sp=new SoundPool(10,AudioManager.STREAM_SYSTEM,1);
        s_dog=sp.load(this,R.raw.dog,1);
		find_switch.setOnCheckedChangeListener(new OnCheckedChangeListener() {  
 	        @Override  
	        public void onCheckedChanged(CompoundButton buttonView,  
	                boolean isChecked) {  
	            // TODO Auto-generated method stub  
	            if (isChecked) {
	            	alertSet(2);

	            	
	            	//Toast.makeText(mContext, "Find ", Toast.LENGTH_LONG).show();
	            } else {  
	            	alertSet(0);
	            	
	            //	Toast.makeText(mContext, "Not Find", Toast.LENGTH_LONG).show();
	            }  
	        }  
	    });	
		createPreferenceUI();

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		//((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		//mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
		//mGattServicesList.setOnChildClickListener(servicesListClickListner);
		//mConnectionState = (TextView) findViewById(R.id.connection_state);
		//mDataField = (TextView) findViewById(R.id.data_value);

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		boolean bll = bindService(gattServiceIntent, mServiceConnection,
				BIND_AUTO_CREATE);
		if (bll) {
			System.out.println("---------------");
			bat_timer.schedule(bat_task, 8000,100000);
    		timer.schedule(task, 5000, 50000);
		} else {
			System.out.println("===============");
    		timer.schedule(task, 5000, 5000);
		}
	//监听 电话，短信消息	
        SmsContent content = new SmsContent(new Handler());  
        //注册短信变化监听  
        this.getContentResolver().registerContentObserver(Uri.parse("content://sms/"), true, content); 
        //获取电话服务  
        manager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);  
        // 手动注册对PhoneStateListener中的listen_call_state状态进行监听  
        manager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);  
     //   
        
	}
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
	{
		String lossStr=getResources().getString(R.string.loss_range);
		lossText_id.setText(lossStr+": "+(progress+1)+" M");
	}
	//停止拖动
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		int progress=lossBar_id.getProgress();
		lossText_id.setText((progress+1)+" M");
		//SharedPreferences barState=getPreferences(0);
		saveLossRange=progress+1;
		SharedPreferences barState=PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor batEditor=barState.edit();
		batEditor.putInt("LossRange", progress);
		batEditor.commit();
	}
	@Override
	protected void onResume() {
		super.onResume();
//		SharedPreferences bleSettings=getPreferences(Activity.MODE_PRIVATE);
		saveLossRange=bleSettings.getInt("LossRange", 1);
		Log.i(BLE_TAG,"resume loss range= "+saveLossRange);
		
		lossBar_id.setProgress(saveLossRange);
		
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.i(BLE_TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			mBluetoothLeService.connect(mDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mBluetoothLeService.disconnect();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void updateBatteryInfo(int  batCapcity){
		if(batCapcity==0)
			bat_id.setImageDrawable(getResources().getDrawable(R.drawable.battery_bg));
		else if(batCapcity<20)
			bat_id.setImageDrawable(getResources().getDrawable(R.drawable.battery_bg2));
		else if(batCapcity<50)
			bat_id.setImageDrawable(getResources().getDrawable(R.drawable.battery_bg3));
		else if(batCapcity<80)	
			bat_id.setImageDrawable(getResources().getDrawable(R.drawable.battery_bg4));
		else if(batCapcity<98)	
			bat_id.setImageDrawable(getResources().getDrawable(R.drawable.battery_full));
	}
	public void updateRssiView(int rssi){
		if(rssi>-60)
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_5));
		else if(rssi>-70)
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_4));
		else if(rssi>-80)
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_3));
		else if(rssi>-90)
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_2));
		else if(rssi>-100)
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_1));
		else
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_0));
			rssi_view.setText(String.valueOf(rssi));
		rssi=Math.abs(rssi);
		if(rssi>(saveLossRange*3))
			midAlert(true);
		else
			midAlert(false);
			
	}
/*
	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectionState.setText(resourceId);
			}
		});
	}

	private void displayData(String data) {
		if (data != null) {
			mDataField.setText(data);
		}
	}
*/
	// Demonstrates how to iterate through the supported GATT
	// Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the
	// ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = getResources().getString(
				R.string.unknown_service);
		String unknownCharaString = getResources().getString(
				R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME,
					SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				Log.i(BLE_TAG,"UUID= "+uuid);
				if(uuid.equals(ALERT_UUID)){
					alertChar=gattCharacteristic;
					//Log.i(BLE_TAG,"leaf alertChar= "+alertChar);
				}
				if(uuid.equals(BATTERY_UUID)){
					batteryChar=gattCharacteristic;
				//	Log.i(BLE_TAG,"leaf batteryChar= "+batteryChar);
				}
				currentCharaData.put(LIST_NAME,SampleGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);				
				gattCharacteristicGroupData.add(currentCharaData);
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_READ_RSSI);
		return intentFilter;
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO 自动生成的方法存根
		
	}
    class MyPhoneStateListener extends PhoneStateListener{  
    	  
        @Override  
        public void onCallStateChanged(int state, String incomingNumber) {  
        	int phoneAlert=0;
        	switch (state) {  
            case TelephonyManager.CALL_STATE_IDLE:  
            	phoneAlert=0;
                Log.i("leaf ","phone idle ");
                break;  
            case TelephonyManager.CALL_STATE_RINGING:
            	phoneAlert=2;
                Log.i("leaf ","phone incoming call "+incomingNumber);
                break;  
            case TelephonyManager.CALL_STATE_OFFHOOK:
            	phoneAlert=0;
                Log.i("leaf ","Phone is handup");
            default:  
                break;  
            }
        	if(phoneFlag==true)
        		alertSet(phoneAlert);
            super.onCallStateChanged(state, incomingNumber);  
        }  
          
    } 
    class SmsContent extends ContentObserver{  
        private Cursor cursor = null;  
        public SmsContent(Handler handler) {
			// TODO 自动生成的构造函数存根
        	super(handler); 
        }
	    public void onChange(boolean selfChange) {  
            // TODO Auto-generated method stub  
            super.onChange(selfChange);  
          Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null,
        		  "read=?",  new String[]{"0"}, null);
         if(msgFlag==true){
          if (cursor != null){ 
        	   alertSet(2);
        	   new Handler().postDelayed(new Runnable(){    
        		    public void run() {    
        		    //execute the task    
       		    	 alertSet(0);
        		    }    
        		 }, 15000);
        	  /* 
                while (cursor.moveToNext()){
             	   StringBuilder sb = new StringBuilder();
             	   //_id为短信编号；address为手机号码；body为短信内容；time为时间，长整型的
             	   sb.append("_id=").append(cursor.getInt(cursor.getColumnIndex("_id")));
                   sb.append(",address=").append(cursor.getString(cursor.getColumnIndex("address")));
                   sb.append(";body=").append(cursor.getString(cursor.getColumnIndex("body")));
                   sb.append(";time=").append(cursor.getLong(cursor.getColumnIndex("date")));
                   Log.i("ReceiveSendSMS", sb.toString()); 

                }
               */  
            } 
        }  }
	    
    }
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO 自动生成的方法存根
	       //判断是哪个Preference改变了  
        if(preference.getKey().equals(lossFlagKey))  
        {  
        	lossFlag=(Boolean)newValue;
            Log.v("SystemSetting", "lossFlagKey is changed");  
        }  
        else if(preference.getKey().equals(shakeFlagKey))  
        {  
            shakeFlag=(Boolean)newValue;
        	Log.v("SystemSetting", "shakeFlagKey is changed");  
        }
        else if(preference.getKey().equals(phoneAlertKey))  
        { 
        	phoneFlag=(Boolean)newValue;
            Log.v("SystemSetting", "phoneAlertKey is changed");  
        }
        else if(preference.getKey().equals(msgAlertKey))  
        {  
        	msgFlag=(Boolean)newValue;
            Log.v("SystemSetting", "msgAlertKey is changed");  
        }
        else if(preference.getKey().equals(responseTimeKey))  
        {  
        	responseTimeValue=String.valueOf(newValue);
            Log.v("SystemSetting", "responseTimeKeyis changed");  
        }
        else if(preference.getKey().equals(alertLevelKey))  
        {  
        	alertLevelValue=String.valueOf(newValue);
        	Log.v("SystemSetting", "alertLevelKey is changed");  
        }
        else
        	return false;
	
		return true;
	}
	@Override
	public void onClick(View v) {
		// TODO 自动生成的方法存根
		if(v==rssi_id)
		{
			rssi_count++;
			if(rssi_count>=7)
				rssi_view.setVisibility(View.VISIBLE);
			else
				rssi_view.setVisibility(View.GONE);
			if(rssi_count>=5){
		     	   new Handler().postDelayed(new Runnable(){    
		   		    public void run() {    
		   		    //execute the task    
		   		    	rssi_count=0;
		   		    }    
		   		 }, 3000);
			}
		}
	}
 

}
