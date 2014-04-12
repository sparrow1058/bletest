package com.example.bluetooth.le;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class SmartMainActivity extends Activity implements OnClickListener{
	private final static String TAG = "leaf Smart Main";
	public static final String BLE_TAG="leaf ble";
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final String BATTERY_UUID="00002a19-0000-1000-8000-00805f9b34fb";
    private final String ALERT_UUID="00002a06-0000-1000-8000-00805f9b34fb";
    private final String TXPWR_UUID="00002a07-0000-1000-8000-00805f9b34fb";
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

	private BluetoothGattCharacteristic mNotifyCharacteristic;
	public BluetoothGattCharacteristic alertChar;
	public BluetoothGattCharacteristic batteryChar;
    
	private String mDeviceName;
	private String mDeviceAddress;
    private final int 	MSG_READ_RSSI=0;
    private final int 	MSG_READ_BATTERY=1;
	private boolean mConnected = false;
	private ImageView rssi_id;
	private ImageView bat_id;
	private ImageButton bt_find;
	private ImageButton bt_setting;
	private ImageButton bt_info;
	private ImageButton bt_off;
	
	
	private boolean find_flag;
	private SoundPool sp;	//define a soundpool
	private int s_dog,s_monk,s_duck,s_cat;	//set sournd id
	public SharedPreferences bleSettings;
	  
    private final String lossFlagKey="loss_check";
    private final String shakeFlagKey="shake_check";
    private final String phoneAlertKey="phone_check";
    private final String msgAlertKey="msg_check";
    private final String alertLevelKey="alert_list";
    private final String responseTimeKey="time_list";
    private final String lossRangeKey="LossRange";
    private boolean lossFlag;
    private boolean shakeFlag;
    private boolean phoneFlag;
    private boolean msgFlag;
    
    
    private int saveLossRange;
	private BluetoothLeService mBluetoothLeService;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.smart_main);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		rssi_id=(ImageView)findViewById(R.id.rssi_id);
		bat_id=(ImageView)findViewById(R.id.bat_id);
		bt_find=(ImageButton)findViewById(R.id.bt_find);
		bt_info=(ImageButton)findViewById(R.id.bt_info);
		bt_off=(ImageButton)findViewById(R.id.bt_off);
		bt_setting=(ImageButton)findViewById(R.id.bt_setting);
		bt_find.setOnClickListener(this);
		bt_info.setOnClickListener(this);
		bt_setting.setOnClickListener(this);
		bt_off.setOnClickListener(this);
		
        sp=new SoundPool(10,AudioManager.STREAM_SYSTEM,1);
        s_dog=sp.load(this,R.raw.dog,1);		
        bleSettings=PreferenceManager.getDefaultSharedPreferences(this); 
		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		boolean bll = bindService(gattServiceIntent, mServiceConnection,
				BIND_AUTO_CREATE);
		if (bll) {
			timer.schedule(task, 5000, 500);
			System.out.println("---------------");
		} else {
			System.out.println("===============");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.i(BLE_TAG, "Connect request result=" + result);
		}
		saveLossRange=bleSettings.getInt("LossRange", 2);
//		alertLevelValue=bleSettings.getString(alertLevelKey, "mid");
	//	responseTimeValue=bleSettings.getString(responseTimeKey, "50ms");
		lossFlag=bleSettings.getBoolean(lossFlagKey, true);
//		shakeFlag=bleSettings.getBoolean(shakeFlagKey, true);
		phoneFlag=bleSettings.getBoolean(phoneAlertKey, false);
		msgFlag=bleSettings.getBoolean(msgAlertKey, false);
		Log.i(TAG,"flags "+ saveLossRange+ lossFlag + phoneFlag +msgFlag);
	}
	@Override
	public void onClick(View v) {
		// TODO 自动生成的方法存根
		if(v==bt_find){
			if(find_flag)
				alertSet(2);
			else
				alertSet(0);
			find_flag=!find_flag;
		}else if(v==bt_setting){
			Intent intent = new Intent(this, SmartSetActivity.class);
			startActivity(intent);
		}else if(v==bt_info){
			
		}else if(v==bt_off){
			finish();
			//showDialog();
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
		timer.cancel();		//del the timer
		mBluetoothLeService = null;
		Log.i(TAG,"smart main exit");
	}
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
	
	// build a service connection
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
	//define a BroadCasetreceiver
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			System.out.println("action = " + action);
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				//clearUI();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}else if(BluetoothLeService.ACTION_READ_RSSI.equals(action)){
				int rssi=Integer.parseInt(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				updateRssiView(rssi);
				Log.v(BLE_TAG,"RSSI= " +mBluetoothLeService.remoteRssi+" extra= "+rssi);
			}
		}
	};
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
	
	//define a intentFilter
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_READ_RSSI);
		return intentFilter;
	}
	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
			//	mConnectionState.setText(resourceId);
			}
		});
	}
	private void displayData(String data) {
		if (data != null) {
			Log.i(TAG,data);		//leaf debug
		}
	}

	//define the new Timer handle
	private final Timer timer = new Timer();
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
    	case 2:
    	//	Log.i(BLE_TAG,"BATTERY="+mBluetoothLeService.remoteRssi);
    	//	mBluetoothLeService.readCharacteristic(batteryChar);
    		break;
    	}
//        super.handleMessage(msg);  
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
		//rssi_view.setText(String.valueOf(rssi));
	rssi=Math.abs(rssi);
	if(rssi>(saveLossRange*3))
		midAlert(true);
	else
		midAlert(false);
		
}
public void alertSet(int level)
{
	if(alertChar!=null){
		alertChar.setValue(level, 17, 0);
		mBluetoothLeService.wirteCharacteristic(alertChar);
	}
}
private void midAlert(boolean on)
{
	if(on)
		s_dog=sp.play(s_dog, 1, 1, 0, 20, 1);
	else
		sp.stop(s_dog);
}
private void showDialog()
{
	new AlertDialog.Builder(this)
    .setTitle("Smartronix")
    .setMessage("确定退出?")
    .setIcon(R.drawable.bt_off)
    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int whichButton) {
    setResult(RESULT_OK);//确定按钮事件
    finish();
    }
    })
    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int whichButton) {
     //取消按钮事件
    }
    })
    .show();
}


}
