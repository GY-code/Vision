package t20220049.sw_vision.transfer.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import t20220049.sw_vision.transfer.model.FileTransfer;
import t20220049.sw_vision.transfer.util.Md5Util;
import t20220049.sw_vision.ui.ReceiveFileActivity;

public class WifiServer extends Thread{
    private static final String TAG = "WifiServer";
    static ServerSocket serverSocket = null;//自己的ServerSocket
    Socket clientSocket = null;//自己的ServerSocket对应的(client)Socket
    static int clientsNum = 0;//当前有多少个客户端连接
    //客户端Sockets的List
    public static ArrayList<MyClient> clients = new ArrayList<>();
    public MyClient mClient;

    public class MyClient {
        public Socket client = null;
        public String clientIP = "default ip";
        public String clientUserID;
        public MyClient (Socket client,String clientIP) {
            this.client = client;
            this.clientIP = clientIP;
        }
    }

    public WifiServer(ServerSocket serverSocket, Socket clientSocket, String clientIP){
        clientsNum++;
        mClient = new MyClient(clientSocket,clientIP);
        clients.add(mClient);
        Log.i(TAG,"a client has connected.(" + clientsNum + "clients now.)");
        this.clientSocket = clientSocket;
        if(WifiServer.serverSocket==null)
            WifiServer.serverSocket = serverSocket;
    }

    public void run(){
        BufferedReader in = null;
        PrintWriter out = null;
        String inputLine;
        try{
            //获取客户端输入流
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //不断监听客户端输入
            while((inputLine = in.readLine())!=null){
                Log.i(TAG,"recieve message[from client " + mClient.clientIP + "]: " + inputLine);
                //处理客户端的各种请求
                if(inputLine.equals("sendFile")){
                    Log.i(TAG,"request send file");
                    receiveFile();
                } else if (inputLine.equals("userID")){
                    Log.i(TAG,"send user id");
                    String userID = in.readLine();
                    mClient.clientUserID = userID;
                }
            }
            in.close();
            clientSocket.close();
        } catch (IOException e){
            e.printStackTrace();
            clientsNum--;
            clients.remove(clientSocket);
            Log.i(TAG,"a client quits.");
        }
    }

    private void receiveFile() {
        File file = null;
        try {
            InputStream inputStream;
            ObjectInputStream objectInputStream;
            FileOutputStream fileOutputStream;
            Log.e(TAG, "客户端IP地址 : " + clientSocket.getInetAddress().getHostAddress());
            inputStream = clientSocket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
            Log.e(TAG, "待接收的文件: " + fileTransfer);
            String name = fileTransfer.getFileName();

            //将文件存储至指定位置
            file = new File(ReceiveFileActivity.cacheDir, name);
            fileOutputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            long total = 0;
            int progress;
            while ((len = inputStream.read(buf)) != -1) {
                fileOutputStream.write(buf, 0, len);
                total += len;
                progress = (int) ((total * 100) / fileTransfer.getFileLength());
                Log.e(TAG, "文件接收进度: " + progress);
//                if (progressChangListener != null) {
//                    progressChangListener.onProgressChanged(fileTransfer, progress);
//                }
            }

//            serverSocket.close();
            inputStream.close();
            objectInputStream.close();
            fileOutputStream.close();
//            serverSocket = null;
            Log.e(TAG, "文件接收成功，文件的MD5码是：" + Md5Util.getMd5(file));
        } catch (Exception e) {
            Log.e(TAG, "文件接收 Exception: " + e.getMessage());
        } finally {
//            clean();
//            if (progressChangListener != null) {
//                progressChangListener.onTransferFinished(file);
//            }
//            //再次启动服务，等待客户端下次连接
//            startService(new Intent(this, WifiServerService.class));
        }
    }

    public static void sendInstruction(String instruction,String clientIP) {

        PrintWriter out;
        System.out.println("this:"+clientIP);
        for(MyClient c:clients){
            System.out.println(c.clientIP);
            if (c.clientIP.equals(clientIP)){
                try {
                    out = new PrintWriter(c.client.getOutputStream(),true);
                    out.println(instruction);
                    Log.i(TAG,"a instruction has sent.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
