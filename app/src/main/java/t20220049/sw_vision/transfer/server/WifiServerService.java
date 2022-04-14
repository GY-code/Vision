package t20220049.sw_vision.transfer.server;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import t20220049.sw_vision.transfer.common.Constants;
import t20220049.sw_vision.transfer.model.FileTransfer;

//后台：收文件
public class WifiServerService extends IntentService {

    private static final String TAG = "WifiServerService";

    private ServerSocket serverSocket;

    private InputStream inputStream;

    private ObjectInputStream objectInputStream;

    private FileOutputStream fileOutputStream;

    private OnProgressChangListener progressChangListener;

    public WifiServerService() {
        super("WifiServerService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new WifiServerBinder();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        WifiServer.clients.clear();
        try {
            Log.i(TAG,"server started..");
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(Constants.PORT));//端口1995
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIPAddress = clientSocket.getInetAddress().getHostAddress();
                WifiServer server = new WifiServer(serverSocket,clientSocket,clientIPAddress);
                server.start();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        clean();
//        File file = null;
//        try {
//            serverSocket = new ServerSocket();
//            serverSocket.setReuseAddress(true);
//            serverSocket.bind(new InetSocketAddress(Constants.PORT));//端口1995
//            Log.e(TAG, "服务器已启动。");
//            Socket client = serverSocket.accept();//等待连接
//
//            Log.e(TAG, "客户端IP地址 : " + client.getInetAddress().getHostAddress());
//            inputStream = client.getInputStream();
//            objectInputStream = new ObjectInputStream(inputStream);
//            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
//            Log.e(TAG, "待接收的文件: " + fileTransfer);
//            String name = fileTransfer.getFileName();
//
//            //将文件存储至指定位置
//            file = new File(getCacheDir(), name);
//            fileOutputStream = new FileOutputStream(file);
//            byte[] buf = new byte[1024];
//            int len;
//            long total = 0;
//            int progress;
//            while ((len = inputStream.read(buf)) != -1) {
//                fileOutputStream.write(buf, 0, len);
//                total += len;
//                progress = (int) ((total * 100) / fileTransfer.getFileLength());
//                Log.e(TAG, "文件接收进度: " + progress);
//                if (progressChangListener != null) {
//                    progressChangListener.onProgressChanged(fileTransfer, progress);
//                }
//            }
//
//            serverSocket.close();
//            inputStream.close();
//            objectInputStream.close();
//            fileOutputStream.close();
//            serverSocket = null;
//            inputStream = null;
//            objectInputStream = null;
//            fileOutputStream = null;
//            Log.e(TAG, "文件接收成功，文件的MD5码是：" + Md5Util.getMd5(file));
//        } catch (Exception e) {
//            Log.e(TAG, "文件接收 Exception: " + e.getMessage());
//        } finally {
//            clean();
//            if (progressChangListener != null) {
//                progressChangListener.onTransferFinished(file);
//            }
//            //再次启动服务，等待客户端下次连接
//            startService(new Intent(this, WifiServerService.class));
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clean();
    }

    public void setProgressChangListener(OnProgressChangListener progressChangListener) {
        this.progressChangListener = progressChangListener;
    }

    private void clean() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (objectInputStream != null) {
            try {
                objectInputStream.close();
                objectInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
                fileOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnProgressChangListener {

        //当传输进度发生变化时
        void onProgressChanged(FileTransfer fileTransfer, int progress);

        //当传输结束时
        void onTransferFinished(File file);

    }

    public class WifiServerBinder extends Binder {
        public WifiServerService getService() {
            return WifiServerService.this;
        }
    }

}
