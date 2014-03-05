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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
public class DeviceSettingActivity extends PreferenceActivity implements OnSeekBarChangeListener {
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
    private final int 	MSG_READ_RSSI=0;
    private final int 	MSG_READ_BATTERY=1;
    private     TelephonyManager manager ;  
    
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
 //   private int saveAlertLevel;
   // private int saveAlertTime;
    
  
    public TextView bat_id;
    public ImageView rssi_id;
    public SeekBar lossBar_id;
    public TextView lossText_id;
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
					updateBatteryInfo(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			//	displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}else if(BluetoothLeService.ACTION_READ_RSSI.equals(action)){
				//Log.v(BLE_TAG,"RSSI=" +intent.getStringExtra(BluetoothLeService.EXTRA_DATA)+ " "+bat_id);
			//	bat_id.setText(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				updateRssiView(Integer.parseInt(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)));
			}
		}
	};

	// If a given GATT characteristic is selected, check for supported features.
	// This sample
	// demonstrates 'Read' and 'Notify' features. See
	// http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for
	// the complete
	// list of supported characteristic features.
	private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			byte[] bb=new byte[1];
			
			if (mGattCharacteristics != null) {
				final BluetoothGattCharacteristic characteristic = mGattCharacteristics
						.get(groupPosition).get(childPosition);
				final int charaProp = characteristic.getProperties();
				System.out.println("charaProp = " + charaProp + ",UUID = "
						+ characteristic.getUuid().toString());
				Random r = new Random();

				if (characteristic.getUuid().toString()
						.equals("0000fff2-0000-1000-8000-00805f9b34fb")) {
						int time= 0;
						while((time=r.nextInt(9))<=0){
							
						}
						
						String data = time+","+"1,,,,,";
						characteristic.setValue(data.getBytes());
						mBluetoothLeService.wirteCharacteristic(characteristic);
				}				
				if (characteristic.getUuid().toString()
						.equals("0000fff1-0000-1000-8000-00805f9b34fb")) {
					int R = r.nextInt(255);
					int G = r.nextInt(255);
					int B = r.nextInt(255);
					int BB = r.nextInt(100);
					String data = R + "," + G + "," + B + "," + BB;
					while (data.length() < 18) {
						data += ",";
					}
					System.out.println(data);
					characteristic.setValue(data.getBytes());
					mBluetoothLeService.wirteCharacteristic(characteristic);
				}
				if (characteristic.getUuid().toString()
						.equals("0000fff3-0000-1000-8000-00805f9b34fb")) {
					int R = r.nextInt(255);
					int G = r.nextInt(255);
					int B = r.nextInt(255);
					int BB = r.nextInt(100);
					String data = R + "," + G + "," + B + "," + BB;
					while (data.length() < 18) {
						data += ",";
					}
					System.out.println("RT");
					characteristic.setValue("RT".getBytes());
					mBluetoothLeService.wirteCharacteristic(characteristic);
				}
				if (characteristic.getUuid().toString()
						.equals("00002a06-0000-1000-8000-00805f9b34fb")) {
					bb[0]=0;
					characteristic.setValue(2, 17, 0);					
					mBluetoothLeService.wirteCharacteristic(characteristic);
					

					System.out.println("send a");
				}
				if (characteristic.getUuid().toString()
						.equals("00002a19-0000-1000-8000-00805f9b34fb")) {
					//characteristic.setValue("1".getBytes());
//					mBluetoothLeService.getRssiVal();
					mBluetoothLeService.readCharacteristic(characteristic);
				}
					//leaf  // write find me
				if (characteristic.getUuid().toString()
						.equals("0000fff5-0000-1000-8000-00805f9b34fb")) {
					characteristic.setValue("S".getBytes());
					mBluetoothLeService.wirteCharacteristic(characteristic);
					System.out.println("send S");
				} else {

					if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
						// If there is an active notification on a
						// characteristic, clear
						// it first so it doesn't update the data field on the
						// user interface.
						if (mNotifyCharacteristic != null) {
							mBluetoothLeService.setCharacteristicNotification(
									mNotifyCharacteristic, false);
							mNotifyCharacteristic = null;
						}
						mBluetoothLeService.readCharacteristic(characteristic);

					}
				}
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

					if (characteristic.getUuid().toString().equals("0000fff6-0000-1000-8000-00805f9b34fb")||characteristic.getUuid().toString().equals("0000fff4-0000-1000-8000-00805f9b34fb")) {
						System.out.println("enable notification");
						mNotifyCharacteristic = characteristic;
						mBluetoothLeService.setCharacteristicNotification(
								characteristic, true);
						
					}
				}

				return true;
			}
			return false;
		}
	};
/*
	private void clearUI() {
		mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
		mDataField.setText(R.string.no_data);
	}
*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.gatt_services_characteristics);
		setContentView(R.layout.preference_head);
		// 从资源文件中添Preferences ，选择的值将会自动保存到SharePreferences
		addPreferencesFromResource(R.layout.device_setting2);
		mContext = this;
		bat_id=(TextView)findViewById(R.id.bat_text);
		rssi_id=(ImageView)findViewById(R.id.rssi_id);
		find_switch=(Switch)findViewById(R.id.switch1);
		lossBar_id=(SeekBar)findViewById(R.id.lossBar);
		lossText_id=(TextView)findViewById(R.id.lossText);
		lossBar_id.setOnSeekBarChangeListener(this);
		find_switch.setOnCheckedChangeListener(new OnCheckedChangeListener() {  
 	        @Override  
	        public void onCheckedChanged(CompoundButton buttonView,  
	                boolean isChecked) {  
	            // TODO Auto-generated method stub  
	            if (isChecked) {
	            	if(alertChar!=null){
	            		alertChar.setValue(2, 17, 0);					
					mBluetoothLeService.wirteCharacteristic(alertChar);
	            	}
	            	//Toast.makeText(mContext, "Find ", Toast.LENGTH_LONG).show();
	            } else {  
	            	if(alertChar!=null){
	            		alertChar.setValue(0, 17, 0);					
					mBluetoothLeService.wirteCharacteristic(alertChar);
	            	}
	            //	Toast.makeText(mContext, "Not Find", Toast.LENGTH_LONG).show();
	            }  
	        }  
	    });	
		SharedPreferences bleSettings=getPreferences(Activity.MODE_PRIVATE);
		saveLossRange=bleSettings.getInt("LossRange", 2);
		Log.i(BLE_TAG,"Create loss range= "+saveLossRange);
		lossBar_id.setProgress(saveLossRange);
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
		lossText_id.setText((progress+1)+" M");
	}
	//停止拖动
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		int progress=lossBar_id.getProgress();
		lossText_id.setText((progress+1)+" M");
		SharedPreferences barState=getPreferences(0);
		SharedPreferences.Editor batEditor=barState.edit();
		batEditor.putInt("LossRange", progress);
		batEditor.commit();
	}
	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences bleSettings=getPreferences(Activity.MODE_PRIVATE);
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

	public void updateBatteryInfo(String batInfo){
		bat_id.setText(batInfo);
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
		else if(rssi>-110)
			rssi_id.setImageDrawable(getResources().getDrawable(R.drawable.signal_0));
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
        	switch (state) {  
            case TelephonyManager.CALL_STATE_IDLE:  
 
                Log.i("leaf ","phone idle ");
                break;  
            case TelephonyManager.CALL_STATE_RINGING:  
                Log.i("leaf ","phone incoming call "+incomingNumber);
                break;  
            case TelephonyManager.CALL_STATE_OFFHOOK:  
                Log.i("leaf ","Phone is handup");
            default:  
                break;  
            }  
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
           if (cursor != null){  
                while (cursor.moveToNext()){
             	   StringBuilder sb = new StringBuilder();
             	   //_id为短信编号；address为手机号码；body为短信内容；time为时间，长整型的
             	   sb.append("_id=").append(cursor.getInt(cursor.getColumnIndex("_id")));
                   sb.append(",address=").append(cursor.getString(cursor.getColumnIndex("address")));
                   sb.append(";body=").append(cursor.getString(cursor.getColumnIndex("body")));
                   sb.append(";time=").append(cursor.getLong(cursor.getColumnIndex("date")));
                   Log.i("ReceiveSendSMS", sb.toString()); 

                } 
            } 
        }  
    }  

}
