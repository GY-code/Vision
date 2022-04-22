package t20220049.sw_vision.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.skyfishjy.library.RippleBackground;

import org.w3c.dom.Text;

import java.io.File;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import t20220049.sw_vision.entrance.WebrtcUtil;
import t20220049.sw_vision.transfer.broadcast.DirectBroadcastReceiver;
import t20220049.sw_vision.transfer.callback.DirectActionListener;
import t20220049.sw_vision.transfer.model.FileTransfer;
import t20220049.sw_vision.transfer.server.WifiServer;
import t20220049.sw_vision.transfer.server.WifiServerService;

import t20220049.sw_vision.R;
import t20220049.sw_vision.transfer.util.WifiP2pUtils;
import t20220049.sw_vision.utils.RecordUtil;

//控制端
public class ReceiveFileActivity extends BaseActivity {

    private TextView dvNameTv;
    private TextView dvStatusTv;

    public static Context context;

    public static File cacheDir;

    private static final String TAG = "ReceiveFileActivity";

    Collection<WifiP2pDevice> wifiP2pDeviceList = null;

    private ImageView iv_image;

    private TextView tv_log;

    private ProgressDialog progressDialog;

    private WifiP2pManager wifiP2pManager;

    private WifiP2pManager.Channel channel;

    private boolean connectionInfoAvailable;

    private BroadcastReceiver broadcastReceiver;

    private WifiServerService wifiServerService;

    //服务器回調 *進度條初始化、銷毀*
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WifiServerService.WifiServerBinder binder = (WifiServerService.WifiServerBinder) service;
            wifiServerService = binder.getService();
//            wifiServerService.setProgressChangListener(progressChangListener);
            wifiServerService.fileReceiveListener = fileReceiveListener;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (wifiServerService != null) {
                wifiServerService.setProgressChangListener(null);
                wifiServerService = null;
            }
//            bindService();
        }
    };

    //收到信号处理
    private final DirectActionListener directActionListener = new DirectActionListener() {
        @Override
        public void wifiP2pEnabled(boolean enabled) {
            log("wifiP2pEnabled: " + enabled);
        }

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            log("onConnectionInfoAvailable");
            log("isGroupOwner：" + wifiP2pInfo.isGroupOwner);
            log("groupFormed：" + wifiP2pInfo.groupFormed);
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionInfoAvailable = true;
//                if (wifiServerService != null) {
//                    log("start service");
//                    startService(WifiServerService.class);
//                }
            }
        }

        @Override
        public void onDisconnection() {
            connectionInfoAvailable = false;
            log("onDisconnection");
        }

        @Override
        public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
            dvNameTv.setText("设备名称： " + wifiP2pDevice.deviceName);
            dvStatusTv.setText("设备状态: " + WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status));
            log("onSelfDeviceAvailable");
            log(wifiP2pDevice.toString());
        }

        @Override
        public void onPeersAvailable(Collection<WifiP2pDevice> dl) {
            wifiP2pDeviceList = dl;
            log("onPeersAvailable,size:" + dl.size());
//            tv_log.setText("");
//            for (WifiP2pDevice wifiP2pDevice : dl) {
//                tv_log.append("设备名称： " + wifiP2pDevice.deviceName + "\n");
//                tv_log.append("设备地址： " + wifiP2pDevice.deviceAddress + "\n");
//                tv_log.append("设备状态： " + WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status) + "\n");
//                tv_log.append("\n");
////                log(wifiP2pDevice.toString());
//            }
        }

        @Override
        public void onChannelDisconnected() {
            log("onChannelDisconnected");
        }
    };

    //进度条变化
    private final WifiServerService.OnProgressChangListener progressChangListener = new WifiServerService.OnProgressChangListener() {

        //不断更新进度条，
        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final int progress) {
            runOnUiThread(() -> {
                progressDialog.setMessage("文件名： " + fileTransfer.getFileName());
                progressDialog.setProgress(progress);
                progressDialog.show();
            });
        }

        //文件传输完成后，在ui界面上，取消进度条，将接收到的图片放置在imgView上
        @Override
        public void onTransferFinished(final File file) {
            runOnUiThread(() -> {
                progressDialog.cancel();
                if (file != null && file.exists()) {
                    Glide.with(ReceiveFileActivity.this).load(file.getPath()).into(iv_image);
                }
            });
        }
    };

    public final WifiServer.FileReceiveListener fileReceiveListener = new WifiServer.FileReceiveListener() {
        @Override
        public void onFileReceiveFinished() {
            runOnUiThread(() -> {
                showToast("接收文件成功");
            });
        }

        @Override
        public void onFileReceiveFailed(String s) {
            runOnUiThread(() -> {
                showToast(s);
            });
        }

        @Override
        public void logMessage(String s) {
            runOnUiThread(() -> {
                log(s);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_file);
        initView();
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            finish();
            return;
        }


        channel = wifiP2pManager.initialize(this, getMainLooper(), directActionListener);
        if (ActivityCompat.checkSelfPermission(ReceiveFileActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        removeGroup();
        //建立群组
        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                log("createGroup onSuccess");
                dismissLoadingDialog();
                showToast("onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                log("createGroup onFailure: " + reason);
                dismissLoadingDialog();
                showToast("onFailure");
            }
        });
        broadcastReceiver = new DirectBroadcastReceiver(wifiP2pManager, channel, directActionListener);
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter());

        Log.i(TAG, "flag1");
        bindService();//start WifiServerService

//        if (wifiServerService != null) {
        log("start service");
        startService(WifiServerService.class);
//        }

//        findViewById(R.id.btnSendMsg).setOnClickListener(view -> {
//            Log.i(TAG,"HELLO MM");
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    for(WifiServer.MyClient client:WifiServer.clients){
//                        WifiServer.sendInstruction("instruction",client.clientIP);
//                    }
//                }
//            }).start();
//
//        });
        findViewById(R.id.cPlayLayout).setOnClickListener(v -> {
            WebrtcUtil.call(ReceiveFileActivity.this, "ws://106.13.236.207:3000", "123456");
        });

        final RippleBackground rippleBackground = (RippleBackground) findViewById(R.id.content0);
        rippleBackground.startRippleAnimation();

        dvNameTv = (TextView) findViewById(R.id.deviceNameText);
        dvStatusTv = (TextView) findViewById(R.id.deviceStateText);
//        tv.append(Build.DEVICE);
        cacheDir = getCacheDir();

        findViewById(R.id.content0).setOnClickListener(view -> {
            removeGroup();
        });

        context = this;
        new RecordUtil(context);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                wifiP2pManager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                        tv_log.setText("");
                        for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList.getDeviceList()) {
                            tv_log.append("设备名称： " + wifiP2pDevice.deviceName + "\n");
                            tv_log.append("设备地址： " + wifiP2pDevice.deviceAddress + "\n");
                            tv_log.append("设备状态： " + WifiP2pUtils.getDeviceStatus(wifiP2pDevice.status) + "\n");
                            tv_log.append("\n");
                        }
                    }
                });
            }
        }, 500, 1500);
    }

    private void initView() {
        setTitle("接收文件");
        iv_image = findViewById(R.id.iv_image);
        tv_log = findViewById(R.id.tv_log);
//      刷新，先移除再创建
        findViewById(R.id.refreshButton).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(ReceiveFileActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            removeGroup();
            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    log("createGroup onSuccess");
                    dismissLoadingDialog();
//                    showToast("onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    log("createGroup onFailure: " + reason);
                    dismissLoadingDialog();
//                    showToast("onFailure");
                }
            });
        });
//        移除群组
//        findViewById(R.id.btnRemoveGroup).setOnClickListener(v -> removeGroup());
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在接收文件");
        progressDialog.setMax(100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (wifiServerService != null) {
//            wifiServerService.setProgressChangListener(null);
//            unbindService(serviceConnection);
//        }
//        unregisterReceiver(broadcastReceiver);
//        stopService(new Intent(this, WifiServerService.class));
//        if (connectionInfoAvailable) {
//            removeGroup();
//        }
    }

    private void removeGroup() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                log("removeGroup onSuccess");
//                showToast("onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                log("removeGroup onFailure");
//                showToast("onFailure");
            }
        });
    }

    public void log(String log) {
//        tv_log.append(log + "\n");
//        tv_log.append("----------" + "\n");
        return;
    }

    private void bindService() {
        Intent intent = new Intent(ReceiveFileActivity.this, WifiServerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "flag2");
    }

}