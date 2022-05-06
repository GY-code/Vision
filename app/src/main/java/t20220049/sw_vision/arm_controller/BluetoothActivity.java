package t20220049.sw_vision.arm_controller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import t20220049.sw_vision.arm_controller.commen.Constants;
import t20220049.sw_vision.arm_controller.connect.BLEManager;
import t20220049.sw_vision.arm_controller.connect.BLEService;
import t20220049.sw_vision.arm_controller.model.ByteCommand;
import t20220049.sw_vision.arm_controller.uitls.BluetoothUtils;
import t20220049.sw_vision.arm_controller.uitls.LogUtil;
import t20220049.sw_vision.arm_controller.widget.PromptDialog;
import t20220049.sw_vision.arm_controller.widget.SearchDialog;

import java.util.Timer;
import java.util.TimerTask;

import t20220049.sw_vision.R;

public class BluetoothActivity extends Activity implements SearchDialog.OnDeviceSelectedListener {

    private static final int RETRY_TIMES = 3;

    private static final String TAG = BluetoothActivity.class.getSimpleName();
    private boolean confirm;

    private ImageButton btStateBtn;

    public static BluetoothAdapter mBluetoothAdapter = null;

    public static BLEManager bleManager;

    public static BluetoothDevice mBluetoothDevice;

    private Handler mHandler;

    /**
     * 连接次数
     */
    private int connectTimes;
    /**
     * 蓝牙连接状态
     */
    public static boolean isConnected;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.i(TAG, "BLE Service connected");
            BLEService bleService = ((BLEService.BLEBinder) service).getService();
            BLEManager.getInstance().init(bleService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.w(TAG, "BLE Service disconnected");
            BLEManager.getInstance().destroy();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lsc6);
        if (!BluetoothUtils.isSupport(BluetoothAdapter.getDefaultAdapter())) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
        btStateBtn = (ImageButton) findViewById(R.id.bluetooth_btn);
        Intent intent = new Intent(this, BLEService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        BLEManager.getInstance().register(this);
        mHandler = new Handler(new MsgCallBack());
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectTimes = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        bleManager = BLEManager.getInstance();
        isConnected = bleManager.isConnected();
        bleManager.setHandler(mHandler);
        LogUtil.i(TAG, "onResume isConnected= " + isConnected);
        if (isConnected)
            btStateBtn.setImageResource(R.drawable.bluetooth_connected);
        else
            btStateBtn.setImageResource(R.drawable.bluetooth_disconnected);
    }

    @Override
    protected void onDestroy() {
        LogUtil.i(TAG, "onCreate");
        unbindService(mConnection);
        BLEManager.getInstance().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        LogUtil.i(TAG, "bond state = " + device.getBondState());
        mBluetoothDevice = device;
        bleManager.connect(device);
//        setState(R.string.bluetooth_state_connecting);
        Toast.makeText(this, R.string.bluetooth_state_connecting, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (BLEManager.getInstance().isConnected()) {
            PromptDialog.create(this, getFragmentManager(), getString(R.string.exit_tips_title),
                    getString(R.string.exit_tips_content), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (DialogInterface.BUTTON_POSITIVE == which) {
                                BLEManager.getInstance().stop();
                                BluetoothActivity.super.onBackPressed();
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }
                    });
        } else {
            if (!confirm) {
                confirm = true;
                Toast.makeText(this, R.string.exit_remind, Toast.LENGTH_SHORT).show();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        confirm = false;
                    }
                }, 2000);
            } else {
                Intent intent = new Intent(this, BLEService.class);
                stopService(intent);
                BLEManager.getInstance().destroy();
                super.onBackPressed();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }

    private void mayRequestLocation()//安卓6.0及以上系统，搜索蓝牙BLE设备需要开启定位权限，否则不能搜索到
    {
        if (mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int checkCallPhonePermission = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
                if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    }
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                    return;
                } else {

                }
            } else {

            }
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.bluetooth_btn:
                mayRequestLocation();
                if (mBluetoothAdapter.isEnabled()) {
                    if (isConnected) {
                        PromptDialog.create(getBaseContext(), getFragmentManager(), getString(R.string.disconnect_tips_title),
                                getString(R.string.disconnect_tips_connect), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (DialogInterface.BUTTON_POSITIVE == which) {
                                            bleManager.stop();
                                        }
                                    }
                                });
                    } else {
                        SearchDialog.createDialog(getFragmentManager(), this);
                    }
                } else {
                    Toast.makeText(getBaseContext(), R.string.tips_open_bluetooth, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                }
                break;

            case R.id.set_btn:
                break;

            case R.id.action1_btn:
                sendActionCmd(1);
                break;

            case R.id.action2_btn:
                sendActionCmd(2);
                break;

            case R.id.action3_btn:
                sendActionCmd(3);
                break;

            case R.id.action4_btn:
                sendActionCmd(4);
                break;

            case R.id.action5_btn:
                sendActionCmd(5);
                break;

            case R.id.action6_btn:
                sendActionCmd(6);
                break;
        }
    }

    private void sendActionCmd(int index)//发送动作组命令
    {
        //帧头     length  type  num         timeLo timeHi id          posLo  posHi
        byte[] byteArray1 = {0x55, 0x55, 0x08, 0x03, 0x01, (byte) 0xe8, 0x03, 0x01, (byte) 0xf4, 0x01};       //500
        byte[] byteArray2 = {0x55, 0x55, 0x08, 0x03, 0x01, (byte) 0xd0, 0x07, 0x01, (byte) 0xdc, 0x05};       //1500
        byte[] byteArray3 = {0x55, 0x55, 0x08, 0x03, 0x01, (byte) 0xb8, 0x0b, 0x01, (byte) 0xc4, 0x09};       //2500

        //        byte[] byteArray = {0x55, 0x55, 0x05, 0x06, 0x00, 0x01, 0x00};
//        byteArray[4] = (byte) (index & 0xff);
        ByteCommand.Builder builder = new ByteCommand.Builder();
        switch (index) {
            case 1:
                builder.addCommand(byteArray1, 100);
                break;
            case 2:
                builder.addCommand(byteArray2, 100);
                break;
            case 3:
                builder.addCommand(byteArray3, 100);
                break;
        }
//        builder.addCommand(byteArray, 100);
        bleManager.send(builder.createCommands());
    }

    private void setState(boolean isConnected) {//设置蓝牙状态图片
        LogUtil.i(TAG, "isConnected = " + isConnected);
        if (isConnected) {
            this.isConnected = true;
            btStateBtn.setImageResource(R.drawable.bluetooth_connected);
        } else {
            this.isConnected = false;
            btStateBtn.setImageResource(R.drawable.bluetooth_disconnected);
        }
    }

    class MsgCallBack implements Handler.Callback {//处理消息

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MessageID.MSG_CONNECT_SUCCEED:
                    LogUtil.i(TAG, "connected ");
//                    setState(R.string.bluetooth_state_connected);
                    Toast.makeText(getBaseContext(), R.string.bluetooth_state_connected, Toast.LENGTH_SHORT).show();
                    setState(true);
                    break;
                case Constants.MessageID.MSG_CONNECT_FAILURE:
                    if (connectTimes < RETRY_TIMES) {
                        connectTimes++;
                        mHandler.sendEmptyMessageDelayed(Constants.MessageID.MSG_CONNECT_RECONNECT, 300);
                    } else {
                        connectTimes = 0;
//                        setState(R.string.bluetooth_state_connect_failure);
                        Toast.makeText(getBaseContext(), R.string.bluetooth_state_connect_failure, Toast.LENGTH_SHORT).show();
                        setState(false);
                    }
                    break;
                case Constants.MessageID.MSG_CONNECT_RECONNECT:
                    LogUtil.i(TAG, "reconnect bluetooth" + mBluetoothDevice.getName() + " " + connectTimes);
                    bleManager.connect(mBluetoothDevice);
                    break;
                case Constants.MessageID.MSG_CONNECT_LOST:
//                    setState(R.string.bluetooth_state_disconnected);
                    Toast.makeText(getBaseContext(), R.string.disconnect_tips_succeed, Toast.LENGTH_SHORT).show();
                    setState(false);
                    break;
                case Constants.MessageID.MSG_SEND_COMMAND:
                    bleManager.send((ByteCommand)msg.obj);
                    Message sendMsg = mHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND, msg.arg1, -1, msg.obj);
                    mHandler.sendMessageDelayed(sendMsg, msg.arg1);
                    break;

                case Constants.MessageID.MSG_SEND_NOT_CONNECT:
                     Toast.makeText(getBaseContext(), R.string.send_tips_no_connected, Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        }
    }
}
