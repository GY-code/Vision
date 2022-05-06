package t20220049.sw_vision.arm_controller.connect;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import t20220049.sw_vision.arm_controller.commen.Constants;
import t20220049.sw_vision.arm_controller.model.ByteCommand;
import t20220049.sw_vision.arm_controller.uitls.LogUtil;
import t20220049.sw_vision.arm_controller.uitls.TimerLog;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙4.0管理类
 *
 * @author lucas
 */
public class BLEManager {

    private static final String TAG = BLEManager.class.getSimpleName();

    public static final String HC_08_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String HC_08_SEND_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    /**
     * HC-06模块特征值最大接收字符数
     */
    private static final int DATA_MAX_LEN = 20;

    /**
     * 用于发送消息的HandlerThread
     */
    private HandlerThread mHandlerThread;
    /**
     * 用户与HandlerThread绑定，发送数据的handler
     */
    private Handler sendHandler;

    private BLEService mBleService;
    private BluetoothGattCharacteristic sendCharacteristic;
    private Handler mHandler;

    private static BLEManager instance;

    private static boolean isRegistered = false;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i(TAG, "receive action = " + action);
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) { // Gatt已连接
                LogUtil.i(TAG, "connected !");
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_CONNECT_SUCCEED).sendToTarget();
            } else if (BLEService.ACTION_GATT_CONNECT_FAIL.equals(action)) { // Gatt连接失败
                LogUtil.w(TAG, "connect failed!");
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_CONNECT_LOST).sendToTarget();
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) { // Gatt已断开
                LogUtil.w(TAG, "connection break!");
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_CONNECT_LOST).sendToTarget();
//                sendHandler.sendEmptyMessageDelayed(Constants.MessageID.MSG_CONNECT_RECONNECT, 1000); // 一秒后自动重连
            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) { // Gatt服务更新
                if (mBleService == null)
                    return;
                List<BluetoothGattService> services = mBleService.getSupportedGattServices();
                for (BluetoothGattService service : services) {
                    String uuid = service.getUuid().toString();
                    if (HC_08_UUID.equals(uuid)) { // 获取发送数据的特征值
                        sendCharacteristic = service.getCharacteristic(UUID.fromString(HC_08_SEND_UUID));
                        mBleService.setCharacteristicNotification(sendCharacteristic, true, new BLEService.NotificationListener() {
                            @Override
                            public void onNotification(BluetoothGattCharacteristic characteristic) {
                                BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(UUID.fromString(CONFIG_UUID));
                                gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBleService.writeDescriptor(gattDescriptor);
                            }
                        });
                    }
                }
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) { // Gatt数据到达
                byte[] ByteRecive = intent.getByteArrayExtra(BLEService.EXTRA_DATA);
                if(ByteRecive.length >= 6) {}
               // LogUtil.d(TAG, "data received = " + data);
//            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
//                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
//                int pState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
//                LogUtil.i(TAG, "state = " + state + " , pState = " + pState);
            }
        }
    };

    public static BLEManager getInstance() {
        if (instance == null) {
            instance = new BLEManager();
        }
        return instance;
    }

    private BLEManager() {
        mHandlerThread = new HandlerThread("sendThread");
        mHandlerThread.start();
        sendHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {

            @Override
            public synchronized boolean handleMessage(Message msg) {
                if (mHandler != null) {
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_HANDLED, msg.arg1, msg.arg2).sendToTarget();
                    switch (msg.what) {
                        case Constants.MessageID.MSG_SEND_DATA:
                            write((byte[]) msg.obj, 0);
                            break;
                        case Constants.MessageID.MSG_SEND_STRING:
                            long delay = msg.getData().getLong("delayMills", 0);
                            write((String) msg.obj, delay);
                            break;
                        case Constants.MessageID.MSG_SEND_COMMAND_LIST:
                        case Constants.MessageID.MSG_SEND_COMMAND_STOP:
                            List<ByteCommand> list = (List<ByteCommand>) msg.obj;
                            for (ByteCommand command : list)
                            {
                                write(command.getCommandByteBuffer(), command.getDelay());
                            }
                            break;
                        case Constants.MessageID.MSG_CONNECT_RECONNECT:
                            if (mBleService != null) {
                                mBleService.reconnect();
                            }
                        case Constants.MessageID.MSG_SEND_BYTECOMMAND:
                            ByteCommand CommandSend =(ByteCommand)msg.obj;
                            write(CommandSend.getCommandByteBuffer(),100/*CommandSend.getDelay()*/);
                            break;
                    }
                }
                return true;
            }
        });
    }

    public void init(BLEService bleService) {
        this.mBleService = bleService;
        this.mBleService.init();
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void register(Context context) {
        if (isRegistered)
            return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLEService.ACTION_GATT_CONNECTED);
        filter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BLEService.ACTION_DATA_AVAILABLE);
//        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter);
        isRegistered = true;
    }

    public void unregister(Context context) {
        if (context != null) {
            context.unregisterReceiver(mReceiver);
            isRegistered = false;
        }
    }

    /**
     * 连接设备
     *
     * @param device 蓝牙设备
     * @return 是否进行了连接操作
     */
    public boolean connect(BluetoothDevice device) {
        if (mBleService != null && device != null) {
            mBleService.connect(device.getAddress());
            return true;
        }
        return false;
    }

    /**
     * 发送字符串
     *
     * @param data 字符串
     */
    public synchronized void send(String data) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_STRING, data).sendToTarget();
    }
    /**
     * 发送二进制数组
     *
     * @param data 字符串
     */
    public synchronized void send(ByteCommand data) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_BYTECOMMAND, data).sendToTarget();
    }
    /**
     * 延时发送字符串
     *
     * @param data       字符串
     * @param delayMills 延时 ms
     */
    public synchronized void send(String data, long delayMills) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        Message msg = sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_STRING, data);
        Bundle bundle = new Bundle();
        bundle.putLong("delayMills", delayMills);
        msg.setData(bundle);
        sendHandler.sendMessage(msg);
    }

    /**
     * 发送指令
     *
     * @param commands 指令集合
     */
    /*
    public synchronized void send(List<Command> commands) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND_LIST, commands).sendToTarget();
    }
*/
    /**
     * 发送二进制指令  add by yan
     *
     * @param commands 指令集合
     */
    public synchronized void send(List<ByteCommand> commands) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND_LIST, commands).sendToTarget();
    }

    /**
     * 发送指令
     *
     * @param commands  指令集合
     * @param CommandID 指令id
     */
    public synchronized void send(List<ByteCommand> commands, int CommandID) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND_LIST, CommandID, -1, commands).sendToTarget();
    }

    /**
     * 发送不能取消的命令
     *
     * @param commands 命令列表
     */
    public synchronized void sendNoRemove(List<ByteCommand> commands) {
        if (!isConnected()) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND_STOP, commands).sendToTarget();
    }

    /**
     * 取消所有等待发送的信息
     */
    public synchronized void removeAll() {
        sendHandler.removeMessages(Constants.MessageID.MSG_SEND_COMMAND_LIST);
    }

    public boolean isConnected() {
        return mBleService != null && mBleService.getConnectState() == BluetoothProfile.STATE_CONNECTED;
    }

    /**
     * 停止扫描，断开连接
     */
    public synchronized void stop() {
        LogUtil.d(TAG, "stop");
        if (mBleService != null) {
            mBleService.disconnect();
        }
    }

    /**
     * 释放所有资源
     */
    public synchronized void destroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        if (mBleService != null) {
            mBleService.close();
        }
        mReceiver = null;
        sendHandler = null;
        mHandler = null;
        instance = null;
    }

    public boolean write(String data, long delay) {
        return write(data.getBytes(), delay);
    }

    public boolean write(byte[] data, long delay) {
        if (sendCharacteristic == null) {
            LogUtil.w(TAG, "sendCharacteristic is null, send data[" + new String(data) + "] fail");
            return false;
        }
        byte[][] dataArrays = separateData(data);
        for (byte[] bs : dataArrays) {
            sendCharacteristic.setValue(bs);
            mBleService.writeCharacteristic(sendCharacteristic);
        }
        TimerLog.logTime("send " + new String(data));
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    public boolean write(ByteBuffer buf, long delay) {
        if (sendCharacteristic == null) {
           // LogUtil.w(TAG, "sendCharacteristic is null, send data[" + new String(data) + "] fail");
            return false;
        }
        //byte[][] dataArrays = separateData(data);
       // for (byte[] bs : dataArrays) {
        byte[] bs = new byte[buf.capacity()];
        buf.get(bs,0,buf.capacity());
        sendCharacteristic.setValue(bs);
        mBleService.writeCharacteristic(sendCharacteristic);
      //  }

        //TimerLog.logTime("send " + new String(data));
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public BluetoothGattCharacteristic buildWriteCharacteristic(byte[] data) {
        BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(
                UUID.fromString(HC_08_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        chara.setValue(data);
        return chara;
    }

    private byte[][] separateData(byte[] bytesData) {
        int len = (int) Math.ceil((float) bytesData.length / DATA_MAX_LEN);
        byte[][] bytes = new byte[len][];
        int total = bytesData.length;
        int offset = 0;
        byte[] buff;
        for (int i = 0; i < len; i++) {
            if (offset + DATA_MAX_LEN <= total) {
                buff = new byte[DATA_MAX_LEN];
                System.arraycopy(bytesData, offset, buff, 0, DATA_MAX_LEN);
                offset += DATA_MAX_LEN;
            } else {
                int last = total - offset;
                buff = new byte[last];
                System.arraycopy(bytesData, offset, buff, 0, last);
                offset = total;
            }
            bytes[i] = buff;
        }
        return bytes;
    }
}
