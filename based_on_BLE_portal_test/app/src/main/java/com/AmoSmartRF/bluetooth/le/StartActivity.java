package com.AmoSmartRF.bluetooth.le;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.lin.bluetooth.le.R;
import com.AmoSmartRF.bluetooth.le.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class StartActivity extends AppCompatActivity {

    private final static String TAG = "StartActivity"; // StartActivity.class.getSimpleName();

    private final String ACTION_NAME_RSSI = "AMOMCU_RSSI"; // 其他文件广播的定义必须一致
    private final String ACTION_CONNECT = "AMOMCU_CONNECT";    // 其他文件广播的定义必须一致

    public static final int REFRESH = 0x000001;

    private final String APP_VER = "AmoSmartRF蓝牙APP v1.2 20161203";    // 其他文件广播的定义必须一致

    // SmartRF 开发板的按键值定义
    final static int BLE_KEY_UP = 1;
    final static int BLE_KEY_DOWN = 16;
    final static int BLE_KEY_LEFT = 8;
    final static int BLE_KEY_RIGHT = 2;
    final static int BLE_KEY_CENTER = 4;
    final static int BLE_KEY_S1 = 32;
    final static int BLE_KEY_RELEASE = 0;

    // 根据rssi 值计算距离， 只是参考作用， 不准确---amomcu
    TextView tv_rssi = null;
    static final int rssibufferSize = 10;
    int[] rssibuffer = new int[rssibufferSize];
    int rssibufferIndex = 0;
    boolean rssiUsedFalg = false;

    static byte keyValue_save = 0;

    static Handler mHandler = new Handler();
    // 设备名称
    static boolean DeviceNameFlag = false;
    static String DeviceName = null;

    // dht11 传感器数据， 包含温度与湿度
    static byte[] dht11_Sensor = new byte[4];

    // adc 采样数据， 分别为AIN4与AIN5的通道数据，也就是p0.4与p0.5的管脚输入的adc数据
    static byte[] adc4_value = new byte[2];
    static byte[] adc5_value = new byte[2];
    static TextView start_txt_ADC4ADC5 = null;



    // 退出线程标记
    boolean bExitThread = false;
    final MyOpenHelper myhelper = new MyOpenHelper(this);

    private long data_rate = 500;  //保存数据的频率
    //定义保存数据线程
    Thread thread_save_data = new Thread() {
        private ContentValues values;
        private Date date;
        private SimpleDateFormat format;
        private String data_str;

        public void run() {

            while (true) {
                SQLiteDatabase db = myhelper.getReadableDatabase();
                SystemClock.sleep(data_rate);
                db.beginTransaction();
                try {
                    values = new ContentValues();
                    date = new Date();
                    format = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
                    data_str = format.format(date);
                    values.put("time", data_str);
                    values.put("volt", adc4_volt_str);
                    db.insert("data1", null, values);
                    if (sava_data_checked) {
                        db.setTransactionSuccessful();
                    }
                } finally {
                    db.endTransaction();
                }

            }

        }
    };
    private static String adc4_volt_str;
    private static boolean sava_data_checked = false;
    private static double adc4_volt;
    private SimpleDateFormat formater = new SimpleDateFormat("ss");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
        tv_rssi = (TextView) findViewById(R.id.rssi_tv);
        thread_save_data.start();//开启保存数据的线程

        start_txt_ADC4ADC5 = (TextView) findViewById(R.id.start_txt_ADC0ADC1); //rssi值
        final LineChart chart = (LineChart) findViewById(R.id.chart);
        Description desc = new Description();
        desc.setText("传感器实时数据");
        desc.setTextSize(17);
        chart.setDescription(desc);
        final Handler mhd = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                chart.invalidate();
            }
        };
        new Thread() {
            public void run() {
                Random rd = new Random();
                List<Entry> entries = new ArrayList<>();
                float i = 1;
                while(true){
                    if(entries.size()>30){
                        entries.remove(0);
                    }
                    float y = rd.nextInt(330)*0.01f;
                    if(sava_data_checked)
                        entries.add(new Entry(i++, (float) adc4_volt));
                    else
                        entries.add(new Entry(i++, y));
                    LineDataSet dataSet = new LineDataSet(entries, "时间轴 /秒");
                    dataSet.setColor(Color.RED);
                    LineData lineData = new LineData(dataSet);
                    chart.setData(lineData);
                    mhd.sendMessage(new Message());
                    SystemClock.sleep(data_rate);
                }
            }
        }.start();
        adc4_value[0] = 0;
        adc4_value[1] = 0;

        adc5_value[0] = 0;
        adc5_value[1] = 0;

        dht11_Sensor[0] = 0;
        dht11_Sensor[1] = 0;
        dht11_Sensor[2] = 0;
        dht11_Sensor[3] = 0;

        registerBoradcastReceiver();

        //设置保存数据的switch事件；

        ((Switch) findViewById(R.id.switch_save_data)).setOnCheckedChangeListener(
                new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            sava_data_checked = true;
                            Description d = new Description();
                            d.setText("传感器实时数据");
                            d.setTextSize(17);
                            chart.setDescription(d);
                        } else {
                            sava_data_checked = false;
                            Description d = new Description();
                            d.setText("随机示例数据");
                            d.setTextSize(17);
                            chart.setDescription(d);
                        }
                    }

                }
        );
        /*((Switch) findViewById(R.id.led1_switch))
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        // TODO Auto-generated method stub
                        Log.i(TAG, "led1_switch isChecked = " + isChecked);
                        if (isChecked) {
                            ledx_value[0] = 0x11;
                        } else {
                            ledx_value[0] = 0x10;
                        }
                        DeviceScanActivity.WriteCharX(
                                DeviceScanActivity.gattCharacteristic_char1,
                                ledx_value);

                        // 发现数据发送不够稳定， 再发一次, 笔者认为， 不稳定的原因主要是多线程操作导致的发送与接收冲突，你可以修改成单线程发送与接收即可 ---阿莫
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        DeviceScanActivity.WriteCharX(
                                DeviceScanActivity.gattCharacteristic_char1,
                                ledx_value);
                    }
                });*/

        /*((Switch) findViewById(R.id.led2_switch))
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        // TODO Auto-generated method stub
                        Log.i(TAG, "led2_switch isChecked = " + isChecked);
                        if (isChecked) {
                            ledx_value[0] = 0x21;
                        } else {
                            ledx_value[0] = 0x20;
                        }
                        DeviceScanActivity.WriteCharX(
                                DeviceScanActivity.gattCharacteristic_char1,
                                ledx_value);

                        // 发现数据发送不够稳定， 再发一次, 笔者认为， 不稳定的原因主要是多线程操作导致的发送与接收冲突，你可以修改成单线程发送与接收即可 ---阿莫

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        DeviceScanActivity.WriteCharX(
                                DeviceScanActivity.gattCharacteristic_char1,
                                ledx_value);
                    }
                });*/
		
		/*((Switch) findViewById(R.id.led3_switch))
		.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				Log.i(TAG, "led3_switch isChecked = " + isChecked);
				if (isChecked) {
					ledx_value[0] = 0x41;
				} else {
					ledx_value[0] = 0x40;
				}
				DeviceScanActivity.WriteCharX(
						DeviceScanActivity.gattCharacteristic_char1,
						ledx_value);

				// 发现数据发送不够稳定， 再发一次
            	try {  
                    Thread.sleep(100);  
                } catch (InterruptedException e) {  
                    e.printStackTrace();  
                }
				DeviceScanActivity.WriteCharX(
						DeviceScanActivity.gattCharacteristic_char1,
						ledx_value);
			}
		});*/
        // 继电器开关操作
//		((Switch) findViewById(R.id.relay_switch))
//		.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView,
//					boolean isChecked) {
//				// TODO Auto-generated method stub
//				Log.i(TAG, "relay_switch isChecked = " + isChecked);
//				if (isChecked) {
//					relay_value[0] = 0x44;
//				} else {
//					relay_value[0] = 0x43;
//				}
//
//				DeviceScanActivity.WriteCharX(
//						DeviceScanActivity.gattCharacteristic_char1,
//						relay_value);
//
//				// 发现数据发送不够稳定， 再发一次, 笔者认为， 不稳定的原因主要是多线程操作导致的发送与接收冲突，你可以修改成单线程发送与接收即可 ---阿莫
//            	try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//				DeviceScanActivity.WriteCharX(
//						DeviceScanActivity.gattCharacteristic_char1,
//						ledx_value);
//			}
//		});


        // 连读百分比函数
      /*  seekBar_pwmvalue.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            //第一个时OnStartTrackingTouch,在进度开始改变时执行
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            //第二个方法onProgressChanged是当进度发生改变时执行d
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int i = seekBar.getProgress();
                Log.i(TAG, "seekBar_pwmvalue = " + i);

                byte[] PwmValue = new byte[4];
                PwmValue[0] = PwmValue[1] = PwmValue[2] = PwmValue[3] = (byte) i;
                DeviceScanActivity.WriteCharX(
                        DeviceScanActivity.gattCharacteristic_charA,
                        PwmValue);
//				start_edit_SetPWM.setText("" + Utils.bytesToHexString(PwmValue).toUpperCase());
                // 这里延时一下，避免发送得太快
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //第三个是onStopTrackingTouch,在停止拖动时执行
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int i = seekBar.getProgress();
                Log.i(TAG, "seekBar_pwmvalue = " + i);

                byte[] PwmValue = new byte[4];
                PwmValue[0] = PwmValue[1] = PwmValue[2] = PwmValue[3] = (byte) i;
                DeviceScanActivity.WriteCharX(
                        DeviceScanActivity.gattCharacteristic_charA,
                        PwmValue);
//				start_edit_SetPWM.setText("" + Utils.bytesToHexString(PwmValue).toUpperCase());
                // 这里延时一下，避免发送得太快
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });*/

        new MyThread().start();
    }

    // 接收 rssi 的广播
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(ACTION_NAME_RSSI)) {
                int rssi = intent.getIntExtra("RSSI", 0);

                // 以下这些参数我 amomcu 自己设置的， 不太具有参考意义，
                //实际上我的本意就是根据rssi的信号前度计算以下距离，
                //以便达到定位目的， 但这个方法并不准  ---amomcu---------20150411

                int rssi_avg = 0;
                int distance_cm_min = 10; // 距离cm -30dbm
                int distance_cm_max_near = 1500; // 距离cm -90dbm
                int distance_cm_max_middle = 5000; // 距离cm -90dbm
                int distance_cm_max_far = 10000; // 距离cm -90dbm
                int near = -72;
                int middle = -80;
                int far = -88;
                double distance = 0.0f;

                if (true) {
                    rssibuffer[rssibufferIndex] = rssi;
                    rssibufferIndex++;

                    if (rssibufferIndex == rssibufferSize)
                        rssiUsedFalg = true;

                    rssibufferIndex = rssibufferIndex % rssibufferSize;

                    if (rssiUsedFalg == true) {
                        int rssi_sum = 0;
                        for (int i = 0; i < rssibufferSize; i++) {
                            rssi_sum += rssibuffer[i];
                        }

                        rssi_avg = rssi_sum / rssibufferSize;

                        if (-rssi_avg < 35)
                            rssi_avg = -35;

                        if (-rssi_avg < -near) {
                            distance = distance_cm_min
                                    + ((-rssi_avg - 35) / (double) (-near - 35))
                                    * distance_cm_max_near;
                        } else if (-rssi_avg < -middle) {
                            distance = distance_cm_min
                                    + ((-rssi_avg - 35) / (double) (-middle - 35))
                                    * distance_cm_max_middle;
                        } else {
                            distance = distance_cm_min
                                    + ((-rssi_avg - 35) / (double) (-far - 35))
                                    * distance_cm_max_far;
                        }
                    }
                }
                String s = "当前RSSI值: " + rssi_avg + " dbm" + ", " + "距离: " + (int) distance + " cm";
                tv_rssi.setText(s);
            } else if (action.equals(ACTION_CONNECT)) {
                int status = intent.getIntExtra("CONNECT_STATUC", 0);

                Log.i(TAG, "ACTION_CONNECT status = " + status);

                if (status == 0) {
//					getActionBar().setTitle("已断开连接，请返回然后重新连接");
//					connect_state.setText("已断开连接，请退出本界面后重新连接");
                    getActionBar().setTitle(APP_VER);
//					Toast toast = Toast.makeText(getApplicationContext(), "已断开连接",
//							2000);
//					toast.setGravity(Gravity.CENTER, 0, 0);
//					toast.show();
//					
//					finish();
                } else {
//					connect_state.setText("已连接设备, 当前设备居有低功耗功能，电流仅为100uA左右");
//					getActionBar().setTitle("已连接设备");
                }
            }
        }
    };


    //查看数据按钮
    public void bt_look(View v) {
        Intent intent = new Intent(this, ShowDate.class);

        startActivity(intent);
        System.out.println("开启新的activity了....");
    }

    //清空数据按钮实现
    public void bt_tranct_data(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("真的要清空已保存的数据么 ?");
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = myhelper.getReadableDatabase();
                db.delete("data1", null, null);
            }
        });
        builder.setNegativeButton("点错了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();

    }

    //更新速度按钮实现
    public void bt_setspeed(View v) {
        AlertDialog.Builder builder = new Builder(this);
        final String[] items = new String[]{"1秒/次", "2秒/次", "5秒/次", "10秒/次", "20秒/次", "30秒/次", "60秒/次"};
        int checkedItem = 3;
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
//				System.out.println("click开始");
                switch (which) {
                    case 0:
                        data_rate = 1000;
                        break;

                    case 1:
                        data_rate = 2000;
                        break;

                    case 2:
                        data_rate = 5000;
                        break;

                    case 3:
                        data_rate = 10000;
                        break;

                    case 4:
                        data_rate = 20000;
                        break;

                    case 5:
                        data_rate = 30000;
                    case 6:
                        data_rate = 60000;
                }
                dialog.dismiss();

            }

        };
        builder.setTitle("设置数据采集频率；");
        builder.setSingleChoiceItems(items, checkedItem, listener);
        builder.show();
    }

    public void registerBoradcastReceiver() {
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTION_NAME_RSSI);
        myIntentFilter.addAction(ACTION_CONNECT);
        //注册广播
        registerReceiver(mBroadcastReceiver, myIntentFilter);
    }


    public static synchronized void onCharacteristicRead(BluetoothGatt gatt,
                                                         BluetoothGattCharacteristic characteristic) {
        // Log.i(TAG, "onCharacteristicRead str = " + str);

        if (DeviceScanActivity.gattCharacteristic_keydata.equals(characteristic)) {// 按键
            byte[] key_value = new byte[1];
            key_value = characteristic.getValue();
            Log.i(TAG, "key_value[0] = " + key_value[0]);
            keyValue_save = key_value[0];
        } else if (DeviceScanActivity.gattCharacteristic_char5.equals(characteristic)) {

        } else if (DeviceScanActivity.gattCharacteristic_char6
                .equals(characteristic)) {
            // Log.i(TAG, "onCharacteristicRead str = " + str);
            int i = characteristic.getValue().length;

            dht11_Sensor = characteristic.getValue();
//			Log.i(TAG, "dht11_Sensor[2] = " + dht11_Sensor[2]);
        } else if (DeviceScanActivity.gattCharacteristic_char7
                .equals(characteristic)) {
            int i = characteristic.getValue().length;
            DeviceName = Utils.bytesToString(characteristic.getValue());
            DeviceNameFlag = true;
//			Log.i(TAG, "DeviceName = " + DeviceName);
        } else if (DeviceScanActivity.gattCharacteristic_char9
                .equals(characteristic)) {// adc0 adc1 数据
            byte[] adc4_adc5_value = new byte[4];
            adc4_adc5_value = characteristic.getValue();
            adc4_value[0] = adc4_adc5_value[0];
            adc4_value[1] = adc4_adc5_value[1];
//			adc5_value[0] = adc4_adc5_value[2];
//			adc5_value[1] = adc4_adc5_value[3];
//			byte[] weight_g = new byte[4];
//			weight_g = characteristic.getValue();
//			int weight = Utils.byteArrayToInt(weight_g, 0);	
//			iWeight_g = (double)weight;
//			iWeight_g /= 10.0;
//			
//			Log.i(TAG, "iWeight_g = " + iWeight_g + "g");			
        } else {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public synchronized void run() {
                // 显示设备名称
                if (DeviceNameFlag == true) {
                    DeviceNameFlag = false;
//					start_edit_SetDeviceName.setText(DeviceName);
                }

                // 显示当前温湿度
//                String current_temperature = "当前温度：" + dht11_Sensor[2] + "."
//                        + dht11_Sensor[3] + "℃";
//                String current_humitidy = "当前湿度：" + dht11_Sensor[0] + "."
//                        + dht11_Sensor[1] + "%";
//                start_txt_temperature.setText(current_temperature + "      "
//                        + current_humitidy);

                // 显示当前adc4 adc5的值

                byte[] adc_value = new byte[4];
                // 计算adc4的对应的电压值

                adc_value[3] = adc4_value[1];
                adc_value[2] = adc4_value[0];
                adc_value[1] = 0;
                adc_value[0] = 0;
                // 注意CC254x单片机的adc为13位有效的adc采样最大值为2的13次方=8192，参考电压为供电电压3.3V，所以计算公式如下： ---阿莫
                adc4_volt = Utils.byteArrayToInt(adc_value, 0) * 3.30 / 8192;
                // 格式化
                DecimalFormat df = new DecimalFormat("#0.0000V");
                adc4_volt_str = df.format(adc4_volt);


                // 计算adc5的对应的电压值
				/*adc_value[3] = adc5_value[1];
				adc_value[2] = adc5_value[0];
				adc_value[1] = 0;
				adc_value[0] = 0;				
				// 注意CC254x单片机的adc为13位有效的adc采样最大值为2的13次方=8192，参考电压为供电电压3.3V，所以计算公式如下： ---阿莫
				double adc5_volt = Utils.byteArrayToInt(adc_value,0)*3.3/8192;
			    // 格式化	
//				DecimalFormat df = new DecimalFormat("#.00V");
				String adc5_volt_str =df.format(adc5_volt);				
				*/
                String current_adc4 = "当前P0.4的ADC读数：0x" + Utils.bytesToHexString(adc4_value) + "，电压：" + adc4_volt_str;
//				String current_adc5 = "当前P0.5的ADC读数：0x" + Utils.bytesToHexString(adc5_value) + "，电压：" + adc5_volt_str;
                start_txt_ADC4ADC5.setText(current_adc4);

//				start_txt_ADC0ADC1.setText("重量：" + String.format("%.1f",iWeight_g) + "克");				

                // 显示按键状态
                switch (keyValue_save) {
                    case BLE_KEY_UP:
//					board_info_log.setText("按键信息: [上] BLE_KEY_UP");
//					board_info_log.setTextColor(Color.rgb(255, 0, 0)); //   变红颜色
                        break;
                    case BLE_KEY_DOWN:
//					board_info_log.setText("按键信息: [下] BLE_KEY_DOWN");
//					board_info_log.setTextColor(Color.rgb(255, 0, 0)); //   变红颜色
                        break;
                    case BLE_KEY_LEFT:
//					board_info_log.setText("按键信息: [左] BLE_KEY_LEFT");
//					board_info_log.setTextColor(Color.rgb(255, 0, 0)); //   变红颜色
                        break;
                    case BLE_KEY_RIGHT:
//					board_info_log.setText("按键信息: [右] BLE_KEY_RIGHT");
//					board_info_log.setTextColor(Color.rgb(255, 0, 0)); //   变红颜色
                        break;
                    case BLE_KEY_CENTER:
//					board_info_log.setText("按键信息: [中] BLE_KEY_CENTER");
//					board_info_log.setTextColor(Color.rgb(255, 0, 0)); //   变红颜色
                        break;
                    case BLE_KEY_S1:
//					board_info_log.setText("按键信息: [S1] BLE_KEY_S1");
//					board_info_log.setTextColor(Color.rgb(255, 0, 0)); //   变红颜色
                        break;
                    case BLE_KEY_RELEASE:
//					board_info_log.setText("按键信息: [无] BLE_KEY_RELEASE");
//					board_info_log.setTextColor(Color.rgb(0, 0, 255));
                        break;
                }
            }
        });
    }

    //导出数据库数据到txt文件
    public void exportData(View view) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss导出的文件");
        String s = simpleDateFormat.format(System.currentTimeMillis());
        MyOpenHelper myhelper = new MyOpenHelper(this);
        SQLiteDatabase db = myhelper.getReadableDatabase();
        Cursor cursor = db.query("data1", null, null, null, null, null, null);
        try {
            FileWriter writer = new FileWriter(new File("/sdcard/" + s + ".txt"));
            while (cursor.moveToNext()) {
                writer.append("序号：" + cursor.getInt(0) + "\t");
                writer.append("日期：" + cursor.getString(1) + "\t");
                writer.append("电压：" + cursor.getDouble(2) + "\n");
            }
            writer.close();
            Toast.makeText(this, "已导出数据于SD卡下Data文件中", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 线程， 发送消息
    public class MyThread extends Thread {
        public void run() {
            int count = 0;
            int count_start_read = 10;


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 读取设备名称
            UpdateDeviceName();

            while (!Thread.currentThread().isInterrupted()) {
//            	Message msg = null;
//        		msg.what = REFRESH;  
//              mHandler.sendMessage(msg);

                if (bExitThread) {
                    break;
                }

                if (count_start_read == 0) {   // 每个一秒钟读一次
                    if (count == 10) {
                        DeviceScanActivity.ReadCharX(DeviceScanActivity.gattCharacteristic_char6);
                    } else if (count == 20) {
                        count = 0;
                        DeviceScanActivity.ReadCharX(DeviceScanActivity.gattCharacteristic_char9);
                    }

                    count++;
                } else {
                    count_start_read--;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.i(TAG, "MyThread out...");
        }
    }


    @SuppressWarnings("unused")
    private void UpdateDeviceName() {
        DeviceScanActivity
                .ReadCharX(DeviceScanActivity.gattCharacteristic_char7);
    }

    private void SetTemperatureNotifyUpdate(boolean enable) {
        DeviceScanActivity.setCharacteristicNotification(
                DeviceScanActivity.gattCharacteristic_char6, enable);
    }

    // @Override
    // protected void onResume() {
    // Log.i(TAG, "---> onResume");
    // super.onResume();
    // }
    //
    // @Override
    // protected void onPause() {
    // Log.i(TAG, "---> onPause");
    // super.onPause();
    // }
    //
    @Override
    protected void onStop() {
        Log.i(TAG, "---> onStop");
        SetTemperatureNotifyUpdate(false);
        bExitThread = true;
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }
    //
    //
    // @Override
    // protected void onDestroy() {
    // Log.i(TAG, "---> onDestroy");
    // super.onDestroy();
    // SetTemperatureNotifyUpdate(false);
    // }
    //
}
