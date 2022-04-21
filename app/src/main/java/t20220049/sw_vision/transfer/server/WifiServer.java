package t20220049.sw_vision.transfer.server;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.webrtc.ContextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import t20220049.sw_vision.transfer.client.WifiClientService;
import t20220049.sw_vision.transfer.model.FileTransfer;
import t20220049.sw_vision.transfer.util.Md5Util;
import t20220049.sw_vision.ui.ControlActivity;
import t20220049.sw_vision.ui.ReceiveFileActivity;
import t20220049.sw_vision.utils.JointBitmap;
import t20220049.sw_vision.utils.Pano;
import t20220049.sw_vision.utils.RecordUtil;
import t20220049.sw_vision.utils.VideoFragment;
import t20220049.sw_vision.utils.VideoFragmentManager;
import t20220049.sw_vision.utils.VideoHandleManager;

public class WifiServer extends Thread {
    private static final String TAG = "WifiServer";
    public static final int PHOTO = 1;
    public static final int VIDEO = 2;
    static ServerSocket serverSocket = null;//自己的ServerSocket
    Socket clientSocket = null;//自己的ServerSocket对应的(client)Socket
    static int clientsNum = 0;//当前有多少个客户端连接
    //客户端Sockets的List
    public static ArrayList<MyClient> clients = new ArrayList<>();
    public MyClient mClient;

    public FileReceiveListener fileReceiveListener;
    private ArrayList<String> photoWL = new ArrayList<>();
    private ArrayList<String> videoWL = new ArrayList<>();

    ObjectInputStream objectInputStream;
    FileOutputStream fileOutputStream;
    BufferedReader in;
    PrintWriter out;
    InputStream inputStream;
    OutputStream outputStream;

    public class MyClient {
        public Socket client = null;
        public String clientIP = "default ip";
        public String clientUserID;

        public MyClient(Socket client, String clientIP) {
            this.client = client;
            this.clientIP = clientIP;
        }
    }

    public WifiServer(ServerSocket serverSocket, Socket clientSocket, String clientIP) {
        clientsNum++;
        mClient = new MyClient(clientSocket, clientIP);
        clients.add(mClient);
        Log.i(TAG, "a client has connected.(" + clientsNum + " clients now.)");
        this.clientSocket = clientSocket;
        if (WifiServer.serverSocket == null)
            WifiServer.serverSocket = serverSocket;
    }

    public void run() {
        Looper.prepare();
        String inputLine;
        try {
            for (MyClient mc : clients) {
                photoWL.add(mc.clientIP);
                videoWL.add(mc.clientIP);
            }
            inputStream = clientSocket.getInputStream();
            outputStream = clientSocket.getOutputStream();

            //获取客户端输入流
            in = new BufferedReader(new InputStreamReader(inputStream));
            out = new PrintWriter(outputStream);
            //不断监听客户端输入
            label:
            while (true) { //(inputLine = in.readLine())!=null
//                if(in==null)
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                inputLine = in.readLine();
//                Log.i(TAG,"recieve message[from client " + mClient.clientIP + "]: " + inputLine);
                //处理客户端的各种请求
                if (inputLine == null) {
                    continue;
                }
                switch (inputLine) {
                    case "sendPhoto":
                        Log.e(TAG, "request send photo");
                        fileReceiveListener.logMessage("request send photo");
//                    String ss = in.readLine();
//                    fileReceiveListener.logMessage(ss);
//                    Log.e(TAG,ss);
//                    if(!in.readLine().equals("continue")){
//                        continue;
//                    }
//                    Thread.sleep(800);
                        Log.e(TAG, "MyFlag");
                        fileReceiveListener.logMessage("MyFlag");
                        receiveFile("photo");
                        fileReceiveListener.logMessage("eend");
                        Log.e(TAG, "eend");
                        break;
                    case "sendVideo":
                        receiveFile("video");
                        break;
                    case "sendFile":
                        receiveFile("file");
                        break;
                    case "userID":
                        Log.e(TAG, "receive user id");
                        mClient.clientUserID = in.readLine();
                        Log.d(TAG, "收到userId: " + mClient.clientUserID);
                        break;
                    case "quit":
                        break label;
                }

            }
//            in.close();
//            clientSocket.close();
        } catch (IOException e) {
            fileReceiveListener.logMessage(e.getMessage());
            e.printStackTrace();
            clientsNum--;
            clients.remove(mClient);
            Log.i(TAG, "a client quits.");
        }
    }

    public interface FileReceiveListener {
        void onFileReceiveFinished();

        void onFileReceiveFailed(String s);

        void logMessage(String s);
    }

    public void setListener(FileReceiveListener fileReceiveListener) {
        this.fileReceiveListener = fileReceiveListener;
    }

    private void receiveFile(String type) {
        File file = null;
        try {
//            InputStream inputStream;
//            ObjectInputStream objectInputStream;
//            FileOutputStream fileOutputStream;
            Log.e(TAG, "客户端IP地址 : " + clientSocket.getInetAddress().getHostAddress());
//            inputStream = clientSocket.getInputStream();
            Log.e(TAG, "HJKLL");
            objectInputStream = new ObjectInputStream(inputStream);
            Log.e(TAG, "HJKLL");
            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
            Log.e(TAG, "待接收的文件: " + fileTransfer);
            String name = fileTransfer.getFileName();

            //将文件存储至指定位置
            if (type.equals("photo")) {
                RecordUtil.clearFile(RecordUtil.remotePhotoPath + name);
                file = new File(RecordUtil.remotePhotoPath, name);
            } else if (type.equals("video")) {
                RecordUtil.clearFile(RecordUtil.remoteVideoPath + name);
                file = new File(RecordUtil.remoteVideoPath, name);
            } else {
                RecordUtil.clearFile(ReceiveFileActivity.cacheDir + name);
                file = new File(ReceiveFileActivity.cacheDir, name);
            }
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
                if (progress == 100)
                    break;
            }
            fileOutputStream.close();

//            serverSocket.close();
//            inputStream.close();
//            objectInputStream.close();
//            fileOutputStream.close();
//            serverSocket = null;
            Log.e(TAG, "文件接收成功，文件的MD5码是：" + Md5Util.getMd5(file));
            if (fileReceiveListener != null) {
                fileReceiveListener.onFileReceiveFinished();
            }
            String[] alias = name.split("\\.", 2);
            String address = clientSocket.getInetAddress().getHostAddress();
            if (alias[1].equals("png")) {
                String clientID = "";
                for (MyClient myClient :
                        clients) {
                    if (myClient.clientIP.equals(address)) {
                        clientID = myClient.clientUserID;
                    }
                }
                Log.e(TAG, "receive photo from " + address + " " + clientID);
                photoWL.remove(address);
                if (photoWL.isEmpty()) {
                    Log.e(TAG, "photo all received! ");
                    for (MyClient mc : clients) {
                        photoWL.add(mc.clientIP);
                    }

                    Log.e(TAG, String.valueOf(ControlActivity.mode));
                    if (ControlActivity.mode == 0) {
                        JointBitmap jointBitmap = new JointBitmap();
                        String photoPath[] = new String[clients.size() + 1];
                        String photoName[] = new String[clients.size() + 1];
                        photoPath[0] = RecordUtil.remotePhotoPath;
                        photoName[0] = RecordUtil.getMyId() + ".png";
                        for (int i = 1; i < (clients.size() + 1); i++) {
                            photoPath[i] = RecordUtil.remotePhotoPath;
                            photoName[i] = clients.get(i - 1).clientUserID + ".png";
                            Log.e(TAG, photoPath[i]);
                            Log.e(TAG, photoName[i]);
                        }
                        jointBitmap.receiveFile(photoPath, photoName);
                        jointBitmap.jointPhoto();
                        RecordUtil.ControlActivityWeakRef.get().runOnUiThread(()->{
                            Toast.makeText(RecordUtil.ControlActivityWeakRef.get().getApplicationContext(),"已将拼接照片存储在相册",Toast.LENGTH_SHORT).show();
                        });
                    } else if (ControlActivity.mode == 1) {
                        Pano panorama = new Pano();

                        String[] mImagePath = new String[]{RecordUtil.remotePhotoPath + RecordUtil.getMyId() + ".png",
                                RecordUtil.remotePhotoPath + clients.get(0).clientUserID + ".png"};

                        panorama.mergeBitmap(mImagePath, new Pano.onStitchResultListener() {

                            @Override
                            public void onSuccess(Bitmap bitmap) {
//                                Toast.makeText(Pano.this,"图片拼接成功！",Toast.LENGTH_LONG).show();
                                Log.e(TAG, "图片拼接成功！");
                                RecordUtil recordUtil = new RecordUtil(ContextUtils.getApplicationContext());
                                recordUtil.savePhoto2Gallery(bitmap);
                            }

                            @Override
                            public void onError(String errorMsg) {
//                                Toast.makeText(Pano.this,"图片拼接失败！",Toast.LENGTH_LONG).show();
                                Log.e(TAG, "图片拼接失败！");
                                System.out.println(errorMsg);
                            }
                        });
                        RecordUtil.ControlActivityWeakRef.get().runOnUiThread(()->{
                            Toast.makeText(RecordUtil.ControlActivityWeakRef.get().getApplicationContext(),"已将融合照片存储在相册",Toast.LENGTH_SHORT).show();
                        });

                    } else if (ControlActivity.mode == -1) {
                        String photoPath[] = new String[clients.size() + 1];
                        String photoName[] = new String[clients.size() + 1];
                        photoPath[0] = RecordUtil.remotePhotoPath;
                        photoName[0] = RecordUtil.getMyId() + ".png";
                        RecordUtil recordUtil = new RecordUtil(ContextUtils.getApplicationContext());
                        recordUtil.savePhoto2Gallery(BitmapFactory.decodeFile(photoPath[0] + photoName[0]));
                        for (int i = 1; i < (clients.size() + 1); i++) {
                            photoPath[i] = RecordUtil.remotePhotoPath;
                            photoName[i] = clients.get(i - 1).clientUserID + ".png";
                            recordUtil.savePhoto2Gallery(BitmapFactory.decodeFile(photoPath[i] + photoName[i]));
                        }
                        RecordUtil.ControlActivityWeakRef.get().runOnUiThread(()->{
                            Toast.makeText(RecordUtil.ControlActivityWeakRef.get().getApplicationContext(),"已将原图照片存储在相册",Toast.LENGTH_SHORT).show();
                        });


                    }
                }
                photoWL.remove(address);
            } else {
                videoWL.remove(address);
                if (videoWL.isEmpty()) {
                    Log.e(TAG, "video all received! ");

                    if (!VideoFragmentManager.getInstance().isComplete()) {
                        Log.e("zsy", "Fragment error occur");
                        return;
                    }
                    ArrayList<VideoFragment> fragments = VideoFragmentManager.getInstance().getFragments();
                    VideoFragmentManager.getInstance().clear();

                    VideoHandleManager
                            .getInstance()
                            .cutVideosAndCombine(fragments,
                                    "output.mp4",
                                    RecordUtil.remoteVideoPath);

                    RecordUtil util = new RecordUtil(ContextUtils.getApplicationContext());
                    util.saveVideo2Gallery(RecordUtil.remoteVideoPath + "output.mp4", ContextUtils.getApplicationContext());
                    RecordUtil.ControlActivityWeakRef.get().runOnUiThread(()->{
                        Toast.makeText(RecordUtil.ControlActivityWeakRef.get().getApplicationContext(),"已将融合视频存储在相册",Toast.LENGTH_SHORT).show();
                    });

                    for (MyClient mc : clients) {
                        videoWL.add(mc.clientIP);
                    }
                }
            }
//            Toast.makeText(ReceiveFileActivity.context.getApplicationContext(),"接收文件成功",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            fileReceiveListener.logMessage(e.getMessage());
            Log.e(TAG, "文件接收 Exception: " + e.getMessage());
        }
//        } finally {
//            clean();
//            if (progressChangListener != null) {
//                progressChangListener.onTransferFinished(file);
//            }
//            //再次启动服务，等待客户端下次连接
//            startService(new Intent(this, WifiServerService.class));
//        }
    }

    public static void sendInstruction(String instruction, String clientIP) {

        PrintWriter out;
        System.out.println("this:" + clientIP);
        for (MyClient c : clients) {
            System.out.println(c.clientIP);
            if (c.clientIP.equals(clientIP)) {
                try {
                    out = new PrintWriter(c.client.getOutputStream(), true);
                    out.println(instruction);
                    Log.i(TAG, "a instruction has sent.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
