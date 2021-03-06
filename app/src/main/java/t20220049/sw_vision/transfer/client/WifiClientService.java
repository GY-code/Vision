package t20220049.sw_vision.transfer.client;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;

import t20220049.sw_vision.arm_controller.ControlCenter;
import t20220049.sw_vision.transfer.common.Constants;
import t20220049.sw_vision.ui.CollectActivity;
import t20220049.sw_vision.ui.SendFileActivity;
import t20220049.sw_vision.utils.RecordUtil;

//对应采集端，发送文件
public class WifiClientService extends IntentService {

    public static BufferedReader serverIn;
    public static PrintWriter serverOut;
    public static final String TAG = "WifiClientService";

    public static Socket socket = null;
    private String clientIP = null;
    private static WeakReference<CollectActivity> CollectActivityWeakRef;

    public static void setBaseActivityWeakRef(CollectActivity activity) {
        CollectActivityWeakRef = new WeakReference<>(activity);
    }

    public WifiClientService() {
        super("WifiClientService");
    }

    public class WifiClientBinder extends Binder {
        public WifiClientService getService() {
            return WifiClientService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new WifiClientBinder();
    }

    public class ClientRunnable implements Runnable {
        @Override
        public void run() {
            //不断侦听服务器发送来的指令
//            if(clientIP !=null){
//                serverOut.println("initialize device name");
//                serverOut.println(clientIP);
//            }
            Log.i(TAG, "listening..");
            try {
                serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                serverOut = new PrintWriter(socket.getOutputStream());
                RecordUtil recordUtil = new RecordUtil(getApplicationContext());
                while (true) {
//                    if(serverIn.)
                    serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String instruction = serverIn.readLine();
                    if (instruction == null) {
                        continue;
                    }
                    Log.e(TAG,"look: "+instruction);
                    switch (instruction) {
                        case "photo":
                            new Thread(() -> {
                                CollectActivityWeakRef.get().CallTakePicture(true, true);
                            }).start();
                            break;
                        case "start":
                            CollectActivityWeakRef.get().CallSetVideoStart();
                            break;
                        case "stop":
                            CollectActivityWeakRef.get().CallSetVideoEnd(true, true);
                            break;
                        case "UP":
                            ControlCenter.getInstance().moveUp();
                            break;
                        case "DOWN":
                            ControlCenter.getInstance().moveDown();
                            break;
                        case "LEFT":
                            ControlCenter.getInstance().moveLeft();
                            break;
                        case "RIGHT":
                            ControlCenter.getInstance().moveRight();
                            break;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String serverIP = SendFileActivity.groupOwnerIP;
        Log.i(TAG, serverIP);
        try {
            socket = new Socket();
            socket.bind(null);
            socket.connect((new InetSocketAddress(serverIP, Constants.PORT)), 10000);

            ClientRunnable r = new ClientRunnable();
            Thread t = new Thread(r);
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}