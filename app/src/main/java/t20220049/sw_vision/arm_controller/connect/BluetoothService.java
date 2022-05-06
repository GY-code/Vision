package t20220049.sw_vision.arm_controller.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import t20220049.sw_vision.arm_controller.commen.Constants;
import t20220049.sw_vision.arm_controller.model.Command;
import t20220049.sw_vision.arm_controller.uitls.Configs;
import t20220049.sw_vision.arm_controller.uitls.LogUtil;
import t20220049.sw_vision.arm_controller.uitls.TimerLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 类注释
 *
 * @author lucas
 * @date 2015年8月22日 下午5:34:14
 */
public class BluetoothService {

    private BluetoothAdapter mAdapter;
    /**
     * 用于发送消息的HandlerThread
     */
    private HandlerThread mHandlerThread;
    /**
     * 用户与HandlerThread绑定，发送数据的handler
     */
    private Handler sendHandler;

    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    /**
     * 蓝牙连接状态
     */
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private static final String TAG = BluetoothService.class.getSimpleName();

    private static BluetoothService instance;

    public static BluetoothService getInstance() {
        if (instance == null)
            instance = new BluetoothService();
        return instance;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    private BluetoothService() {
        super();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandlerThread = new HandlerThread("sendThread");
        mHandlerThread.start();
        sendHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {

            @Override
            public synchronized boolean handleMessage(Message msg) {
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_HANDLED, msg.arg1, msg.arg2).sendToTarget();
                switch (msg.what) {
                    case Constants.MessageID.MSG_SEND_DATA:
                        if (mConnectedThread != null) {
                            mConnectedThread.write((byte[]) msg.obj);
                        }
                        break;
                    case Constants.MessageID.MSG_SEND_STRING:
                        if (mConnectedThread != null) {
                            long delay = msg.getData().getLong("delayMills", 0);
                            mConnectedThread.write((String) msg.obj, delay);
                        }
                        break;
                    case Constants.MessageID.MSG_SEND_COMMAND_LIST:
                    case Constants.MessageID.MSG_SEND_COMMAND_STOP:
                        if (mConnectedThread != null) {
                            List<Command> list = (List<Command>) msg.obj;
                            for (Command command : list) {
                                mConnectedThread.write(command.getCommandStr(), command.getDelay());
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }

    public synchronized void send(String data) {
        if (mState != STATE_CONNECTED) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_STRING, data).sendToTarget();
    }

    public synchronized void send(String data, long delayMills) {
        if (mState != STATE_CONNECTED) {
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

    public synchronized void send(List<Command> commands) {
        if (mState != STATE_CONNECTED) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND_LIST, commands).sendToTarget();
    }

    public synchronized void send(List<Command> commands, int CommandID) {
        if (mState != STATE_CONNECTED) {
            if (mHandler != null)
                mHandler.obtainMessage(Constants.MessageID.MSG_SEND_NOT_CONNECT).sendToTarget();
            return;
        }
        sendHandler.obtainMessage(Constants.MessageID.MSG_SEND_COMMAND_LIST, CommandID, -1, commands).sendToTarget();
    }

    public synchronized void sendNoRemove(List<Command> commands) {
        if (mState != STATE_CONNECTED) {
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

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        LogUtil.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        if (mHandler != null)
            mHandler.obtainMessage(Constants.MessageID.MSG_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        LogUtil.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        LogUtil.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        LogUtil.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        if (mHandler != null)
            mHandler.obtainMessage(Constants.MessageID.MSG_CONNECT_SUCCEED).sendToTarget();

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        LogUtil.d(TAG, "stop");

        if (mAdapter != null) {
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
            }
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public synchronized void destroy() {
        stop();

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        sendHandler = null;
        mHandler = null;
        instance = null;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        if (mHandler != null)
            mHandler.obtainMessage(Constants.MessageID.MSG_CONNECT_FAILURE).sendToTarget();

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        if (mHandler != null)
            mHandler.obtainMessage(Constants.MessageID.MSG_CONNECT_LOST).sendToTarget();

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(Configs.LOROT_UUID);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(Configs.LOROT_UUID);
                }
            } catch (IOException e) {
                LogUtil.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            LogUtil.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                LogUtil.e(TAG, "connect failure --- " + e);
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    LogUtil.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                LogUtil.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            LogUtil.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                LogUtil.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            LogUtil.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    if (mHandler != null)
                        mHandler.obtainMessage(Constants.MessageID.MSG_RECEIVE_DATA, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    LogUtil.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
            LogUtil.i(TAG, "connected thread end");
        }

        public void cancel() {
//            try {
//                if (mmInStream != null) {
//                    LogUtil.i(TAG, "mmInStream.close()");
//                    mmInStream.close();
//                    mmInStream = null;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                LogUtil.e(TAG, "close() of connect socket failed", e);
//            }
//            try {
//                if (mmOutStream != null) {
//                    LogUtil.i(TAG, "mmOutStream.close()");
//                    mmOutStream.close();
//                    mmOutStream = null;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                LogUtil.e(TAG, "close() of connect socket failed", e);
//            }
            try {
                if (mmSocket != null) {
                    LogUtil.i(TAG, "mmSocket.close()");
                    mmSocket.close();
                    mmSocket = null;
                }
            } catch (IOException e) {
                LogUtil.e(TAG, "close() of connect socket failed", e);
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to send
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                TimerLog.logTime("send " + buffer);

                // Share the sent message back to the UI Activity
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_SUCCESS, buffer).sendToTarget();
            } catch (IOException e) {
                LogUtil.e(TAG, "Exception during send", e);
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_FAILURE, -1, -1, buffer).sendToTarget();
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to send
         */
        public void write(String buffer) {
            try {
                mmOutStream.write(buffer.getBytes());
                TimerLog.logTime("send " + buffer);

                // Share the sent message back to the UI Activity
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_SUCCESS, buffer).sendToTarget();
            } catch (IOException e) {
                LogUtil.e(TAG, "Exception during send", e);
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_FAILURE, -1, -1, buffer).sendToTarget();
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to send
         */
        public void write(String buffer, long delay) {
            try {
                mmOutStream.write(buffer.getBytes());
                TimerLog.logTime("send " + buffer);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Share the sent message back to the UI Activity
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_SUCCESS, buffer).sendToTarget();
            } catch (IOException e) {
                LogUtil.e(TAG, "Exception during send", e);
                if (mHandler != null)
                    mHandler.obtainMessage(Constants.MessageID.MSG_SEND_FAILURE, -1, -1, buffer).sendToTarget();
            }
        }

    }
}
