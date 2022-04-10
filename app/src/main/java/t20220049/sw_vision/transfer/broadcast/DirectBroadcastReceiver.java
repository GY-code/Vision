package t20220049.sw_vision.transfer.broadcast;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import t20220049.sw_vision.transfer.callback.DirectActionListener;


public class DirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DirectBroadcastReceiver";
    private final WifiP2pManager mWifiP2pManager;
    private final WifiP2pManager.Channel mChannel;
    private final DirectActionListener mDirectActionListener;

    public DirectBroadcastReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, DirectActionListener directActionListener) {
        mWifiP2pManager = wifiP2pManager;
        mChannel = channel;
        mDirectActionListener = directActionListener;
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        //p2p是否可用
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        //对等节点list变化
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        //p2p连接状态变化
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //本设备信息变化
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                // 用于指示 Wifi P2P 是否可用
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -100);
                    //可用
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        mDirectActionListener.wifiP2pEnabled(true);
                    }
                    //不可用
                    else {
                        mDirectActionListener.wifiP2pEnabled(false);
                        List<WifiP2pDevice> wifiP2pDeviceList = new ArrayList<>();
                        mDirectActionListener.onPeersAvailable(wifiP2pDeviceList);//更新deviceList
                    }
                    break;
                }
                // 对等节点列表发生了变化
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mWifiP2pManager.requestPeers(mChannel, peers -> mDirectActionListener.onPeersAvailable(peers.getDeviceList()));//更新deviceList
                    break;
                }
                // Wifi P2P 的连接状态发生了改变：自己与其他设备连接or断开连接
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo != null && networkInfo.isConnected()) {
                        //获取连接信息
                        mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                mDirectActionListener.onConnectionInfoAvailable(info);
                            }
                        });
                        Log.e(TAG, "已连接p2p设备");
                    } else {
                        mDirectActionListener.onDisconnection();
                        Log.e(TAG, "与p2p设备已断开连接");
                    }
                    break;
                }
                //本设备的设备信息发生了变化
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
                    WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    mDirectActionListener.onSelfDeviceAvailable(wifiP2pDevice);
                    break;
                }
            }
        }
    }

}