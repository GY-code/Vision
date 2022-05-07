package t20220049.sw_vision.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skyfishjy.library.RippleBackground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//import t20220049.sw_vision.arm_controller.BluetoothActivity;
import t20220049.sw_vision.arm_controller.commen.Constants;
import t20220049.sw_vision.arm_controller.connect.BLEManager;
import t20220049.sw_vision.arm_controller.connect.BLEService;
import t20220049.sw_vision.arm_controller.model.ByteCommand;
import t20220049.sw_vision.arm_controller.uitls.BluetoothUtils;
import t20220049.sw_vision.arm_controller.uitls.LogUtil;
import t20220049.sw_vision.arm_controller.widget.PromptDialog;
import t20220049.sw_vision.arm_controller.widget.SearchDialog;
import t20220049.sw_vision.entrance.WebrtcUtil;
import t20220049.sw_vision.transfer.adapter.DeviceAdapter;
import t20220049.sw_vision.transfer.broadcast.DirectBroadcastReceiver;
import t20220049.sw_vision.transfer.callback.DirectActionListener;
import t20220049.sw_vision.transfer.client.WifiClientService;
import t20220049.sw_vision.transfer.client.WifiClientTask;
import t20220049.sw_vision.transfer.server.WifiServer;
import t20220049.sw_vision.transfer.server.WifiServerService;
import t20220049.sw_vision.transfer.util.WifiP2pUtils;
import t20220049.sw_vision.transfer.widget.LoadingDialog;

import t20220049.sw_vision.R;

//客户端
public class SendFileActivity extends BaseActivity implements SearchDialog.OnDeviceSelectedListener {

    private static final int RETRY_TIMES = 3;

//    private static final String TAG = BluetoothActivity.class.getSimpleName();
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

    private LinearLayout startLayout;

    private static final String TAG = "SendFileActivity";

    private WifiClientService wifiClientService;

    public static String groupOwnerIP;

    private String selfDeviceName = null;

    private static final int CODE_CHOOSE_FILE = 100;

    private WifiP2pManager wifiP2pManager;

    private WifiP2pManager.Channel channel;

    private WifiP2pInfo wifiP2pInfo;

    private boolean wifiP2pEnabled = false;

    private List<WifiP2pDevice> wifiP2pDeviceList;

    private DeviceAdapter deviceAdapter;

    private TextView tv_myDeviceName;

//    private TextView tv_myDeviceAddress;

    private TextView tv_myDeviceStatus;

    private TextView tv_status;

//    private Button btn_disconnect;
//
//    private Button btn_chooseFile;

    private LoadingDialog loadingDialog;

    private BroadcastReceiver broadcastReceiver;

    private WifiP2pDevice mWifiP2pDevice;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

    private void bindService() {
        Intent intent = new Intent(SendFileActivity.this, WifiClientService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private final DirectActionListener directActionListener = new DirectActionListener() {

        @Override
        public void wifiP2pEnabled(boolean enabled) {
            wifiP2pEnabled = enabled;
        }

        //已连接-回调
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            dismissLoadingDialog();
            wifiP2pDeviceList.clear();
            deviceAdapter.notifyDataSetChanged();
//            btn_disconnect.setEnabled(true);
//            btn_chooseFile.setEnabled(true);
            Log.e(TAG, "onConnectionInfoAvailable");
            Log.e(TAG, "onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed);
            Log.e(TAG, "onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner);
            Log.e(TAG, "onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
            StringBuilder stringBuilder = new StringBuilder();
//            stringBuilder.append("本设备-是否群主：");
//            stringBuilder.append(wifiP2pInfo.isGroupOwner ? "是群主" : "非群主");
//            stringBuilder.append("\n");
            stringBuilder.append("\n");
            stringBuilder.append("连接设备-IP地址：");//连接设备就是群主
            stringBuilder.append(wifiP2pInfo.groupOwnerAddress.getHostAddress());
            stringBuilder.append("\n");
            if (mWifiP2pDevice != null) {
                stringBuilder.append("连接设备-设备名：");
                stringBuilder.append(mWifiP2pDevice.deviceName);
                stringBuilder.append("\n");
                stringBuilder.append("连接设备-物理地址：");
                stringBuilder.append(mWifiP2pDevice.deviceAddress);
            }
            tv_status.setText(stringBuilder);
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                SendFileActivity.this.wifiP2pInfo = wifiP2pInfo;
            }

            groupOwnerIP = wifiP2pInfo.groupOwnerAddress.getHostAddress();

            Intent intent = new Intent(SendFileActivity.this, WifiClientService.class);
//            intent.putExtra("serverIP",wifiP2pInfo.groupOwnerAddress.getHostAddress());
            Log.i(TAG, wifiP2pInfo.groupOwnerAddress.getHostAddress());
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);

            startService(WifiClientService.class);
            Log.i(TAG, "sFlag");
            startLayout.setVisibility(View.VISIBLE);
        }

        @Override
        public void onDisconnection() {
            Log.e(TAG, "onDisconnection");
//            btn_disconnect.setEnabled(false);
//            btn_chooseFile.setEnabled(false);
            showToast("处于非连接状态");
            wifiP2pDeviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            tv_status.setText(null);
            SendFileActivity.this.wifiP2pInfo = null;

            if (wifiClientService != null) {
                unbindService(serviceConnection);
            }
            stopService(new Intent(SendFileActivity.this, WifiClientService.class));
        }

        @Override
        public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
            Log.e(TAG, "onSelfDeviceAvailable");
            Log.e(TAG, "DeviceName: " + wifiP2pDevice.deviceName);
            Log.e(TAG, "DeviceAddress: " + wifiP2pDevice.deviceAddress);
            Log.e(TAG, "Status: " + wifiP2pDevice.status);
            selfDeviceName = wifiP2pDevice.deviceName;
            tv_myDeviceName.setText("本设备-设备名称：" + wifiP2pDevice.deviceName);
//            tv_myDeviceAddress.setText("本设备-物理地址：" + wifiP2pDevice.deviceAddress);
            tv_myDeviceStatus.setText("本设备-连接状态：" + WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status));
        }

        //刷新RecyclerView
        @Override
        public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
            Log.e(TAG, "onPeersAvailable :" + wifiP2pDeviceList.size());
            List<WifiP2pDevice> wifiP2pDeviceControlList = new ArrayList<>(wifiP2pDeviceList);
            wifiP2pDeviceControlList.removeIf(wd -> !wd.isGroupOwner());
            SendFileActivity.this.wifiP2pDeviceList.clear();
            SendFileActivity.this.wifiP2pDeviceList.addAll(wifiP2pDeviceControlList);
            deviceAdapter.notifyDataSetChanged();
            loadingDialog.cancel();
        }

        @Override
        public void onChannelDisconnected() {
            Log.e(TAG, "onChannelDisconnected");
        }

    };

    class MsgCallBack implements Handler.Callback {//处理消息

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_CONNECT_SUCCEED:
                    LogUtil.i(TAG, "connected ");
//                    setState(R.string.bluetooth_state_connected);
                    Toast.makeText(getBaseContext(), R.string.bluetooth_state_connected, Toast.LENGTH_SHORT).show();
                    setState(true);
                    break;
                case t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_CONNECT_FAILURE:
                    if (connectTimes < RETRY_TIMES) {
                        connectTimes++;
                        mHandler.sendEmptyMessageDelayed(t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_CONNECT_RECONNECT, 300);
                    } else {
                        connectTimes = 0;
//                        setState(R.string.bluetooth_state_connect_failure);
                        Toast.makeText(getBaseContext(), R.string.bluetooth_state_connect_failure, Toast.LENGTH_SHORT).show();
                        setState(false);
                    }
                    break;
                case t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_CONNECT_RECONNECT:
                    LogUtil.i(TAG, "reconnect bluetooth" + mBluetoothDevice.getName() + " " + connectTimes);
                    bleManager.connect(mBluetoothDevice);
                    break;
                case t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_CONNECT_LOST:
//                    setState(R.string.bluetooth_state_disconnected);
                    Toast.makeText(getBaseContext(), R.string.disconnect_tips_succeed, Toast.LENGTH_SHORT).show();
                    setState(false);
                    break;
                case t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_SEND_COMMAND:
                    bleManager.send((ByteCommand)msg.obj);
                    Message sendMsg = mHandler.obtainMessage(t20220049.sw_vision.arm_controller.commen.Constants.MessageID.MSG_SEND_COMMAND, msg.arg1, -1, msg.obj);
                    mHandler.sendMessageDelayed(sendMsg, msg.arg1);
                    break;

                case Constants.MessageID.MSG_SEND_NOT_CONNECT:
                    Toast.makeText(getBaseContext(), R.string.send_tips_no_connected, Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);

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
        initView();
        initEvent();
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

    private void initEvent() {
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            finish();
            return;
        }
        channel = wifiP2pManager.initialize(this, getMainLooper(), directActionListener);
        broadcastReceiver = new DirectBroadcastReceiver(wifiP2pManager, channel, directActionListener);
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter());
    }

    private void initView() {
//        View.OnClickListener clickListener = v -> {
//            long id = v.getId();
//            if (id == R.id.btn_disconnect) {
//                disconnect(); //断开连接
//            } else if (id == R.id.btn_chooseFile) {
//                navToChosePicture(); //打开文件选择
//            }
//        };
        findViewById(R.id.sendDebug).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navToChosePicture();
            }
        });
        findViewById(R.id.action1_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendActionCmd(1);
            }
        });
        findViewById(R.id.action2_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendActionCmd(2);
            }
        });
        findViewById(R.id.action3_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendActionCmd(3);
            }
        });

        setTitle("发送文件");
        tv_myDeviceName = findViewById(R.id.tv_myDeviceName);
//        tv_myDeviceAddress = findViewById(R.id.tv_myDeviceAddress);
        tv_myDeviceStatus = findViewById(R.id.tv_myDeviceStatus);
        tv_status = findViewById(R.id.tv_status);
//        btn_disconnect = findViewById(R.id.btn_disconnect);
//        btn_chooseFile = findViewById(R.id.btn_chooseFile);
//        btn_disconnect.setOnClickListener(clickListener);
//        btn_chooseFile.setOnClickListener(clickListener);
        loadingDialog = new LoadingDialog(this);
        RecyclerView rv_deviceList = findViewById(R.id.rv_deviceList);
        wifiP2pDeviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(wifiP2pDeviceList);
        //点击设备进行连接
        deviceAdapter.setClickListener(position -> {
            mWifiP2pDevice = wifiP2pDeviceList.get(position);
            showToast(mWifiP2pDevice.deviceName);
            connect();
        });
        runOnUiThread(()->{
            showToast("点击上方按钮搜索设备");
        });
        rv_deviceList.setAdapter(deviceAdapter);
        rv_deviceList.setLayoutManager(new LinearLayoutManager(this));

//        findViewById(R.id.btnWifiList).setOnClickListener(view -> {
//            if (wifiP2pManager != null && channel != null) {
//                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
//            } else {
//                showToast("当前设备不支持Wifi Direct");
//            }
//        });
        findViewById(R.id.clientIcon).setOnClickListener(view -> {
            disconnect();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showToast("请先授予位置权限");
            }
            if (!wifiP2pEnabled) {
                showToast("需要先打开Wifi");
            }
            loadingDialog.show("正在搜索附近设备", true, false);
            wifiP2pDeviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            //搜寻附近带有 Wi-Fi P2P 的设备
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
//                    showToast("Success");
                }

                @Override
                public void onFailure(int reasonCode) {
//                    showToast("Failure");
                    loadingDialog.cancel();
                }
            });
        });
        findViewById(R.id.playLayout).setOnClickListener(v->{
            WebrtcUtil.callSingle(SendFileActivity.this, "ws://106.13.236.207:3000","123456", true,false);
        });

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        ImageView imageView=(ImageView)findViewById(R.id.clientIcon);
        rippleBackground.startRippleAnimation();

        startLayout = findViewById(R.id.playLayout);
        startLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        LogUtil.i(TAG, "onCreate");
        unbindService(mConnection);
        BLEManager.getInstance().unregister(this);
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (wifiClientService != null) {
            unbindService(serviceConnection);
        }
        stopService(new Intent(this, WifiClientService.class));
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
                                SendFileActivity.super.onBackPressed();
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
//            case R.id.set_btn:
//                break;

//            case R.id.action4_btn:
//                sendActionCmd(4);
//                break;
//
//            case R.id.action5_btn:
//                sendActionCmd(5);
//                break;
//
//            case R.id.action6_btn:
//                sendActionCmd(6);
//                break;
        }
    }

    public static void sendActionCmd(int index)//发送动作组命令
    {
        Log.e(TAG,"cmd: "+index);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_CHOOSE_FILE) {
            if (resultCode == RESULT_OK) {
                Uri fileUri = data.getData();
                Log.e(TAG, "文件路径：" + fileUri);
                if (wifiP2pInfo != null) {
                    if (WifiClientService.socket != null)
                        new WifiClientTask(this).execute(fileUri,"video");
//                    new WifiClientTask(this).execute(wifiP2pInfo.groupOwnerAddress.getHostAddress(), fileUri);
                }
            }
        }
    }

    private void connect() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showToast("请先授予位置权限");
            return;
        }
        WifiP2pConfig config = new WifiP2pConfig();
        if (config.deviceAddress != null && mWifiP2pDevice != null) {
            config.deviceAddress = mWifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            showLoadingDialog("正在连接 " + mWifiP2pDevice.deviceName);
            wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "connect onSuccess");

                }

                @Override
                public void onFailure(int reason) {
                    showToast("连接失败 " + reason);
                    dismissLoadingDialog();
                }
            });
        }
    }

    private void disconnect() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "disconnect onFailure:" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.e(TAG, "disconnect onSuccess");
                tv_status.setText(null);
//                btn_disconnect.setEnabled(false);
//                btn_chooseFile.setEnabled(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action, menu);
        return true;
    }

    //打开wifi按钮、搜索按钮
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long id = item.getItemId();
        if (id == R.id.menuDirectEnable) {
            if (wifiP2pManager != null && channel != null) {
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            } else {
                showToast("当前设备不支持Wifi Direct");
            }
            return true;
        } else if (id == R.id.menuDirectDiscover) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showToast("请先授予位置权限");
                return true;
            }
            if (!wifiP2pEnabled) {
                showToast("需要先打开Wifi");
                return true;
            }
            loadingDialog.show("正在搜索附近设备", true, false);
            wifiP2pDeviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            //搜寻附近带有 Wi-Fi P2P 的设备
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    showToast("Success");
                }

                @Override
                public void onFailure(int reasonCode) {
                    showToast("Failure");
                    loadingDialog.cancel();
                }
            });
            return true;
        }
        return true;
    }

    //打開系統相冊就
    private void navToChosePicture() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "video/*");
        startActivityForResult(intent, CODE_CHOOSE_FILE);
    }

}