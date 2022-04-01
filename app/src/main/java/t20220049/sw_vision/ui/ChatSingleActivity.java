package t20220049.sw_vision.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import t20220049.sw_vision.CameraService;
import t20220049.sw_vision.wtc_meeting.IViewCallback;
import t20220049.sw_vision.wtc_meeting.PeerConnectionHelper;
import t20220049.sw_vision.wtc_meeting.ProxyVideoSink;
import t20220049.sw_vision.R;
import t20220049.sw_vision.wtc_meeting.WebRTCManager;
import t20220049.sw_vision.utils.PermissionUtil;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 单聊界面
 * 1. 一对一视频通话
 * 2. 一对一语音通话
 */
public class ChatSingleActivity extends AppCompatActivity {
    private SurfaceViewRenderer local_view;
    private SurfaceViewRenderer remote_view;
    private ProxyVideoSink localRender;
    private ProxyVideoSink remoteRender;

    private WebRTCManager manager;

    private boolean videoEnable;
    private boolean isSwappedFeeds;

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

    private static final String TAG = "ChatSingleActivity";

    public static void openActivity(Activity activity, boolean videoEnable) {
        Intent intent = new Intent(activity, ChatSingleActivity.class);
        intent.putExtra("videoEnable", videoEnable);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.wr_activity_chat_single);
        setContentView(R.layout.activity_collection_terminal);
        initVar();
        initListener();
        initService();

    }


    private int previewX, previewY;
    private int moveX, moveY;
    Intent serviceIntent;
    private void initService(){
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
                cameraService=null;
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
        photoButton=findViewById(R.id.shoot);
        videoButton=findViewById(R.id.video);
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
            }
        });

        switch_hang_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hangUp();
            }
        });
        photoButton.setOnClickListener(v->{
            if(cameraService!=null){
                Toast.makeText(getBaseContext(),"拍照",Toast.LENGTH_SHORT).show();
                cameraService.takePicture();
            }
        });
        videoButton.setOnClickListener(v->{
            if(cameraService!=null){
//                cameraService.activateRecord();

                if(videoState == 0){
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
    protected void havePhoto(){
        SurfaceViewRenderer mySurfaceViewRenderer=local_view;
        if (mySurfaceViewRenderer!=null)
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

    private void savePhoto(Bitmap bitmap){
        long curTime =new Date().getTime();
        SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "record_photo "+sdf.format(curTime));
        String fileName =  "record_photo "+sdf.format(curTime)+".png";
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, fileName, fileName);
        runOnUiThread(()->{
            Toast.makeText(getApplicationContext(),"已保存图片到相册",Toast.LENGTH_SHORT).show();
        });
        File appDir = new File(getApplicationContext().getFilesDir()+"");
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

    private void startCall() {
        manager = WebRTCManager.getInstance();
        manager.setCallback(new IViewCallback() {
            @Override
            public void onSetLocalStream(MediaStream stream, String socketId) {
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(localRender);
                }

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
                    ChatSingleActivity.this.finish();
                });

            }
        });
        boolean isNeedOverLay=PermissionUtil.isNeedOverLay(ChatSingleActivity.this);
        if (!PermissionUtil.isNeedRequestPermission(ChatSingleActivity.this)&!isNeedOverLay) {
            manager.joinRoom(getApplicationContext(), rootEglBase);
        }else if(isNeedOverLay){
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
        if(cameraService!=null) {
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
