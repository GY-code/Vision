package t20220049.sw_vision.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import t20220049.sw_vision.service.CameraService;
import t20220049.sw_vision.transfer.client.WifiClientService;
import t20220049.sw_vision.utils.RecordUtil;
import t20220049.sw_vision.webRTC_utils.IViewCallback;
import t20220049.sw_vision.webRTC_utils.PeerConnectionHelper;
import t20220049.sw_vision.webRTC_utils.ProxyVideoSink;
import t20220049.sw_vision.R;
import t20220049.sw_vision.webRTC_utils.WebRTCManager;
import t20220049.sw_vision.utils.PermissionUtil;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 单聊界面
 * 1. 一对一视频通话
 * 2. 一对一语音通话
 */
public class CollectActivity extends AppCompatActivity {
    private SurfaceViewRenderer local_view;
    private SurfaceViewRenderer remote_view;
    private ProxyVideoSink localRender;
    private ProxyVideoSink remoteRender;

    private WebRTCManager manager;

    private boolean videoEnable;
    private boolean isSwappedFeeds;
    private boolean isMirror = true;

    private EglBase rootEglBase;
    private Chronometer mChronometer;
    private ImageView back;
    private ImageView switch_camera;
    private ImageView switch_hang_up;
    private ImageView photoButton;
    private ImageView videoButton;
    private int videoState = 0;

    private ServiceConnection conn;
    private CameraService cameraService;
    private RecordUtil ru;
    boolean activateVideo = false;
    private static final String TAG = "ChatSingleActivity";

    public static void openActivity(Activity activity, boolean videoEnable) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.putExtra("videoEnable", videoEnable);
        activity.startActivity(intent);
    }

    String srcPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.wr_activity_chat_single);
        setContentView(R.layout.activity_collect);
        initVar();
        initListener();
        initService();
        ru=new RecordUtil(getApplicationContext());
        ru.clear();
    }


    private int previewX, previewY;
    private int moveX, moveY;
    Intent serviceIntent;

    private void initService() {
        conn = new ServiceConnection() {
            /**
             * 与服务器端交互的接口方法 绑定服务的时候被回调，在这个方法获取绑定Service传递过来的IBinder对象，
             * 通过这个IBinder对象，实现宿主和Service的交互。
             */
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "绑定成功调用：onServiceConnected");
                // 获取Binder
                CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
                cameraService = binder.getService();
            }

            /**
             * 当取消绑定的时候被回调。但正常情况下是不被调用的，它的调用时机是当Service服务被意外销毁时，
             * 例如内存的资源不足时这个方法才被自动调用。
             */
            @Override
            public void onServiceDisconnected(ComponentName name) {
                cameraService = null;
            }
        };
        serviceIntent = new Intent(this, CameraService.class);
        bindService(serviceIntent, conn, Service.BIND_AUTO_CREATE);
//        startService(serviceIntent);
    }

    private void initVar() {
        Intent intent = getIntent();
        videoEnable = intent.getBooleanExtra("videoEnable", false);

//        ChatSingleFragment chatSingleFragment = new ChatSingleFragment();
//        replaceFragment(chatSingleFragment, videoEnable);
        rootEglBase = EglBase.create();
        if (videoEnable) {
            local_view = findViewById(R.id.local_view_render);
            remote_view = findViewById(R.id.remote_view_render);

            // 本地图像初始化
            local_view.init(rootEglBase.getEglBaseContext(), null);
            local_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            local_view.setZOrderMediaOverlay(true);
            local_view.setMirror(true);
            localRender = new ProxyVideoSink();
            //远端图像初始化
            remote_view.init(rootEglBase.getEglBaseContext(), null);
            remote_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            remote_view.setMirror(true);
            remoteRender = new ProxyVideoSink();
//            setSwappedFeeds(true);
            //后加
            setSwappedFeeds(false);

//            local_view.setOnClickListener(v -> setSwappedFeeds(!isSwappedFeeds));
        }

        startCall();
        switch_camera = findViewById(R.id.switch_camera);
        switch_hang_up = findViewById(R.id.switch_hang_up);
        photoButton = findViewById(R.id.shoot);
        videoButton = findViewById(R.id.video);
        back = findViewById(R.id.back);
        mChronometer = (Chronometer) findViewById(R.id.record_chronometer);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
//        if (videoEnable) {
//            // 设置小视频可以移动
//            local_view.setOnTouchListener((view, motionEvent) -> {
//                switch (motionEvent.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        previewX = (int) motionEvent.getX();
//                        previewY = (int) motionEvent.getY();
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//                        int x = (int) motionEvent.getX();
//                        int y = (int) motionEvent.getY();
//                        moveX = (int) motionEvent.getX();
//                        moveY = (int) motionEvent.getY();
//                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) local_view.getLayoutParams();
//                        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0); // Clears the rule, as there is no removeRule until API 17.
//                        lp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
//                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
//                        lp.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
//                        int left = lp.leftMargin + (x - previewX);
//                        int top = lp.topMargin + (y - previewY);
//                        lp.leftMargin = left;
//                        lp.topMargin = top;
//                        view.setLayoutParams(lp);
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        if (moveX == 0 && moveY == 0) {
//                            view.performClick();
//                        }
//                        moveX = 0;
//                        moveY = 0;
//                        break;
//                }
//                return true;
//            });
//        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hangUp();
            }
        });

        switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
                if (cameraService != null)
                    cameraService.switchCamera();
            }
        });

        switch_hang_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hangUp();
            }
        });
        photoButton.setOnClickListener(v -> {
            if (cameraService != null) {
                Toast.makeText(getBaseContext(), "拍照", Toast.LENGTH_SHORT).show();
                cameraService.takePicture();
            }
        });
        videoButton.setOnClickListener(v -> {
            if (cameraService != null) {
//                cameraService.activateRecord(remote_view);
                if (!activateVideo) {
                    setVideoStart();
                    activateVideo = true;
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "开始录制", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    terminateVideo();
                    activateVideo = false;
                }
                if (videoState == 0) {
                    //setFormat设置用于显示的格式化字符串。
                    //替换字符串中第一个“%s”为当前"MM:SS"或 "H:MM:SS"格式的时间显示。
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.setFormat("%s");
                    mChronometer.setVisibility(View.VISIBLE);
                    mChronometer.start();
                    videoState = 1;
                } else {
                    mChronometer.stop();
                    mChronometer.setVisibility(View.INVISIBLE);
                    videoState = 0;
                }
            }
        });
    }

    String startVideoTime, endVideoTime;

    private void setVideoStart() {
        File file = new File(srcPath + "local.y4m");
        try {
            vfr = new VideoFileRenderer(getApplicationContext().getFilesDir().getAbsolutePath() + "/" + "local" + ".y4m",
                    PeerConnectionHelper.VIDEO_RESOLUTION_WIDTH, PeerConnectionHelper.VIDEO_RESOLUTION_HEIGHT, rootEglBase.getEglBaseContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        localTrack.addSink(vfr);
//        String raw_info = RxFFmpegInvoke.getInstance().getMediaInfo(srcPath + "local" + ".y4m");
//        String[] raw_list = raw_info.split(";");
//        int dur;
//        try {
//            dur = Integer.parseInt(raw_list[4].split("=|ms| |\\.")[1]);
//
//        } catch (NumberFormatException e) {
//            dur = 0;
//        }
//        startVideoTime = dur / 1000 + "." + (dur - dur / 1000);
    }

    private void setVideoEnd() {
        if (vfr != null) {
            localTrack.removeSink(vfr);
            vfr.release();
        }
    }

    private void terminateVideo() {
        setVideoEnd();
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "结束录制", Toast.LENGTH_SHORT).show();
        });
        new Thread(() -> {
            String text = "ffmpeg -i " + srcPath + "local" + ".y4m " + srcPath + "local" + ".mp4";
            Log.d(TAG, "terminateVideo: " + text);
            String[] commands = text.split(" ");
            RxFFmpegInvoke.getInstance().runCommand(commands, new RxFFmpegInvoke.IFFmpegListener() {
                @Override
                public void onFinish() {
                    Log.d(TAG, "onFinish: " + text);
                    insertVideo(srcPath + "local" + ".mp4", getBaseContext());
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "已保存到相册", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onProgress(int progress, long progressTime) {
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(String message) {
                }
            });
        }).start();

    }

    private static final String VIDEO_BASE_URI = "content://media/external/video/media";

    private void insertVideo(String videoPath, Context context) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        Uri newUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", new File(videoPath));
//        retriever.setDataSource(getApplicationContext(), newUri);// videoPath 本地视频的路径
//
        retriever.setDataSource(videoPath);
        int nVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int nVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        int duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        long dateTaken = System.currentTimeMillis();
        File file = new File(videoPath);
        String title = file.getName();
        String filename = file.getName();
        String mime = "video/mp4";
        ContentValues mCurrentVideoValues = new ContentValues(9);
        mCurrentVideoValues.put(MediaStore.Video.Media.TITLE, title);
        mCurrentVideoValues.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues.put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        mCurrentVideoValues.put(MediaStore.Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues.put(MediaStore.Video.Media.DATA, videoPath);
        mCurrentVideoValues.put(MediaStore.Video.Media.WIDTH, nVideoWidth);
        mCurrentVideoValues.put(MediaStore.Video.Media.HEIGHT, nVideoHeight);
        mCurrentVideoValues.put(MediaStore.Video.Media.RESOLUTION, Integer.toString(nVideoWidth) + "x" + Integer.toString(nVideoHeight));
        mCurrentVideoValues.put(MediaStore.Video.Media.SIZE, new File(videoPath).length());
        mCurrentVideoValues.put(MediaStore.Video.Media.DURATION, duration);
        ContentResolver contentResolver = context.getContentResolver();
        Uri videoTable = Uri.parse(VIDEO_BASE_URI);
        Uri uri = contentResolver.insert(videoTable, mCurrentVideoValues);
        writeFile(videoPath, mCurrentVideoValues, contentResolver, uri);
    }

    private void writeFile(String imagePath, ContentValues values, ContentResolver contentResolver, Uri item) {
        try (OutputStream rw = contentResolver.openOutputStream(item, "rw")) {
            // Write data into the pending image.
            Sink sink = Okio.sink(rw);
            BufferedSource buffer = Okio.buffer(Okio.source(new File(imagePath)));
            buffer.readAll(sink);
            values.put(MediaStore.Video.Media.IS_PRIVATE, 0);
            contentResolver.update(item, values, null, null);
            new File(imagePath).delete();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Cursor query = getContentResolver().query(item, null, null, null);
                if (query != null) {
                    int count = query.getCount();
                    Log.d("writeFile", "writeFile result :" + count);
                    query.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void havePhoto() {
        SurfaceViewRenderer mySurfaceViewRenderer = local_view;
        if (mySurfaceViewRenderer != null)
            mySurfaceViewRenderer.addFrameListener(new EglRenderer.FrameListener() {
                @Override
                public void onFrame(Bitmap bitmap) {
                    runOnUiThread(() -> {
                        savePhoto(bitmap);
                        mySurfaceViewRenderer.removeFrameListener(this);
                    });
                }
            }, 1);
    }

    private void savePhoto(Bitmap bitmap) {
        long curTime = new Date().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "record_photo "+sdf.format(curTime));
        String fileName = "record_photo " + sdf.format(curTime) + ".png";
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, fileName, fileName);
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "已保存图片到相册", Toast.LENGTH_SHORT).show();
        });
        File appDir = new File(getApplicationContext().getFilesDir() + "");
        if (!appDir.exists()) appDir.mkdir();
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void setSwappedFeeds(boolean isSwappedFeeds) {
        this.isSwappedFeeds = isSwappedFeeds;
        localRender.setTarget(isSwappedFeeds ? remote_view : local_view);
//        localRender.setTarget(null);
        remoteRender.setTarget(isSwappedFeeds ? local_view : remote_view);
    }

    VideoFileRenderer vfr = null;
    VideoTrack localTrack = null;
    private void sendId2Control(String id){
        new Thread(()->{
            WifiClientService.serverOut.println("userID");
            WifiClientService.serverOut.flush();
            WifiClientService.serverOut.println(id);
            WifiClientService.serverOut.flush();
        }).start();
    }
    private void startCall() {
        manager = WebRTCManager.getInstance();
        manager.setCallback(new IViewCallback() {
            @Override
            public void onSetLocalStream(MediaStream stream, String socketId) {

                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(localRender);
                    localTrack = stream.videoTracks.get(0);

                }
                sendId2Control(socketId);
                Log.d(TAG, "onSetLocalStream: send id");
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                }
            }

            @Override
            public void onAddRemoteStream(MediaStream stream, String socketId) {
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(remoteRender);
                }
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                    runOnUiThread(() -> setSwappedFeeds(false));
                }
            }

            @Override
            public void onCloseWithId(String socketId) {
                runOnUiThread(() -> {
                    disConnect();
                    CollectActivity.this.finish();
                });

            }
        });
        boolean isNeedOverLay = PermissionUtil.isNeedOverLay(CollectActivity.this);
        if (!PermissionUtil.isNeedRequestPermission(CollectActivity.this) & !isNeedOverLay) {
            manager.joinRoom(getApplicationContext(), rootEglBase);
        } else if (isNeedOverLay) {
            finish();
        }


    }

//    private void replaceFragment(Fragment fragment, boolean videoEnable) {
//        Bundle bundle = new Bundle();
//        bundle.putBoolean("videoEnable", videoEnable);
//        fragment.setArguments(bundle);
//        FragmentManager manager = getSupportFragmentManager();
//        manager.beginTransaction()
//                .replace(R.id.wr_container, fragment)
//                .commit();
//
//    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }


    // 切换摄像头
    public void switchCamera() {
        manager.switchCamera();
        isMirror = !isMirror;
        remote_view.setMirror(isMirror);
        local_view.setMirror(isMirror);
    }

    // 挂断
    public void hangUp() {
        disConnect();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        manager.toggleMute(enable);
    }

    // 扬声器
    public void toggleSpeaker(boolean enable) {
        manager.toggleSpeaker(enable);

    }

    @Override
    protected void onDestroy() {
        disConnect();
        if (cameraService != null) {
            cameraService = null;
            unbindService(conn);
//            stopService(serviceIntent);
        }
        super.onDestroy();

    }

    private void disConnect() {
        manager.exitRoom();
        if (localRender != null) {
            localRender.setTarget(null);
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.setTarget(null);
            remoteRender = null;
        }
        if (vfr != null) {
            vfr.release();
        }

        if (local_view != null) {
            local_view.release();
            local_view = null;
        }
        if (remote_view != null) {
            remote_view.release();
            remote_view = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            Log.i(PeerConnectionHelper.TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        manager.joinRoom(getApplicationContext(), rootEglBase);

    }
}
