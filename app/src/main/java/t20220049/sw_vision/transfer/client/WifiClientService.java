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
import java.net.InetSocketAddress;
import java.net.Socket;

import t20220049.sw_vision.transfer.common.Constants;
import t20220049.sw_vision.ui.SendFileActivity;

public class WifiClientService extends IntentService {

    public static BufferedReader serverIn;
    public static PrintWriter serverOut;
    public static final String TAG = "WifiClientService";

    public static Socket socket = null;
    private String clientIP = null;

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
            Log.i(TAG,"listening..");
            try {
                serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                serverOut = new PrintWriter(socket.getOutputStream());
                while (true) {
//                    if(serverIn.)
                        serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String aLine = serverIn.readLine();
                    if (aLine != null) {
                        Log.i(TAG, "instruction received: " + aLine);
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
            socket.connect((new InetSocketAddress(serverIP, Constants.PORT)),10000);

            ClientRunnable r = new ClientRunnable();
            Thread t = new Thread(r);
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

}