package t20220049.sw_vision.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
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

import t20220049.sw_vision.entrance.WebrtcUtil;
import t20220049.sw_vision.transfer.adapter.DeviceAdapter;
import t20220049.sw_vision.transfer.broadcast.DirectBroadcastReceiver;
import t20220049.sw_vision.transfer.callback.DirectActionListener;
import t20220049.sw_vision.transfer.client.WifiClientService;
import t20220049.sw_vision.transfer.common.Constants;
import t20220049.sw_vision.transfer.client.WifiClientTask;
import t20220049.sw_vision.transfer.server.WifiServer;
import t20220049.sw_vision.transfer.server.WifiServerService;
import t20220049.sw_vision.transfer.util.WifiP2pUtils;
import t20220049.sw_vision.transfer.widget.LoadingDialog;

import t20220049.sw_vision.R;

//客户端
public class SendFileActivity extends BaseActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        initView();
        initEvent();
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
                    showToast("Success");
                }

                @Override
                public void onFailure(int reasonCode) {
                    showToast("Failure");
                    loadingDialog.cancel();
                }
            });
        });
        findViewById(R.id.playLayout).setOnClickListener(v->{
            WebrtcUtil.callSingle(SendFileActivity.this, "ws://106.13.236.207:3000","123456", true);
        });

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        ImageView imageView=(ImageView)findViewById(R.id.clientIcon);
        rippleBackground.startRippleAnimation();

        startLayout = findViewById(R.id.playLayout);
        startLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if (wifiClientService != null) {
            unbindService(serviceConnection);
        }
        stopService(new Intent(this, WifiClientService.class));
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
                        new WifiClientTask(this).execute(fileUri,"photo");
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
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, CODE_CHOOSE_FILE);
    }

}