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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import t20220049.sw_vision.transfer.server.WifiServer;
import t20220049.sw_vision.utils.TimerManager;
import t20220049.sw_vision.utils.TransferUtil;
import t20220049.sw_vision.utils.VideoFragment;
import t20220049.sw_vision.utils.CameraService;
import t20220049.sw_vision.utils.RecordUtil;
import t20220049.sw_vision.utils.VideoFragmentManager;
import t20220049.sw_vision.webRTC_utils.IViewCallback;
import t20220049.sw_vision.webRTC_utils.PeerConnectionHelper;
import t20220049.sw_vision.webRTC_utils.ProxyVideoSink;
import t20220049.sw_vision.R;
import t20220049.sw_vision.webRTC_utils.WebRTCManager;
import t20220049.sw_vision.bean.MemberBean;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 群聊界面
 * 支持 9 路同時通信
 */
public class ControlActivity extends AppCompatActivity implements IViewCallback {

    private FrameLayout wr_video_view;

    private WebRTCManager manager;
    private Map<String, SurfaceViewRenderer> _videoViews = new HashMap<>();
    private Map<String, ProxyVideoSink> _sinks = new HashMap<>();
    private Map<String, VideoFileRenderer> _vfrs = new HashMap<>();
    private Map<String, String> startVideoTimes = new HashMap<>();
    private Map<String, String> endVideoTimes = new HashMap<>();
    private List<MemberBean> _infos = new ArrayList<>();
    private List<MediaStream> streamList = new ArrayList<>();
    private List<VideoFragment> fragmentList = new ArrayList<>();
    private List<String> userIdList = new ArrayList<>();
    private int currentIndex = 0;
    private VideoTrack _localVideoTrack;
    private TextView showText;

    private int mScreenWidth;
    private String myId;
    private EglBase rootEglBase;

    ImageView switch_camera;
    ImageView switch_hang_up;
    ImageView photoButton;
    ImageView videoButton;
    private Chronometer mChronometer;
    private int videoState = 0;

    // 上拉框显示
    RelativeLayout bottomSheet;
    BottomSheetBehavior behavior;
    RecyclerView v1;
    deviceAdapter deviceAdapter;
    List<Device> mDevicesList = new ArrayList<>();
    boolean isMirrror = true;
    RecordUtil ru;

    private ServiceConnection conn;
    private CameraService cameraService;
    Intent serviceIntent;

    public static int mode = 0;


    public class Device {
        String type;
        String name;
        String userId;
        String ip;
    }


    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ControlActivity.class);
        activity.startActivity(intent);
    }

    class deviceAdapter extends RecyclerView.Adapter<MyViewHolder> {

        private final List<Device> deviceList;

        public deviceAdapter(List<Device> deviceList) {
            this.deviceList = deviceList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(ControlActivity.this, R.layout.device_list, null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }


        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
            Device device = this.deviceList.get(position);
            holder.mType.setText(device.name);
            holder.mName.setText(device.ip);
            holder.selectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeRecordCapture(deviceList.get(position).userId);
                }
            });
        }


        @Override
        public int getItemCount() {
            return mDevicesList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mType;
        TextView mName;
        ImageView selectButton;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mType = itemView.findViewById(R.id.txt_mType);
            mName = itemView.findViewById(R.id.txt_mName);
            selectButton = itemView.findViewById(R.id.select_button);
        }
    }

    private String getIPFromUserId(String userId) {
        for (int i = 0; i < WifiServer.clients.size(); i++) {
            WifiServer.MyClient client = WifiServer.clients.get(i);
            if (client.clientUserID != null && client.clientUserID.equals(userId)) {
                return client.clientIP;
            }
        }

        return "localhost";
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.wr_activity_chat_room);
        setContentView(R.layout.acticity_control);
        showText = findViewById(R.id.showText);

        userIdList.add("_all");
        streamList.add(null);

        initView();
        initVar();
        initListner();
        initService();
//        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
//        replaceFragment(chatRoomFragment);
        startCall();
        ru = new RecordUtil(getApplicationContext());

        RxFFmpegInvoke.getInstance().setDebug(true);
    }

    private void changeRecordCapture(String s) {
        if (userIdList.contains(s)) {
            cutRecordCapture();
            currentIndex = userIdList.indexOf(s);
            showText.setText("正在录制第" + currentIndex + "个视频");
        }
    }

    private void endRecordCapture() {
        cutRecordCapture();
        VideoFragmentManager.getInstance().setFragments((ArrayList<VideoFragment>) fragmentList);
//        Log.e("zsy", "fragmentList is complete ? " + VideoFragmentManager.getInstance().isComplete());
        Log.e("zsy", "fragmentList already set");
    }

    private void cutRecordCapture() {
        double[] cuts = TimerManager.getInstance().cut();
        double startTime = cuts[0];
        double durance = cuts[1];
        String filename = userIdList.get(currentIndex) + ".mp4";
        fragmentList.add(new VideoFragment(startTime, durance, filename));
        Toast.makeText(ControlActivity.this,
                userIdList.get(currentIndex) + " Start at: " + Double.toString(startTime) + "\nRecord: " + Double.toString(durance) + "seconds",
                Toast.LENGTH_LONG).show();
    }

    protected void havePhoto() {
        for (String userId : _videoViews.keySet()) {
            SurfaceViewRenderer svr = _videoViews.get(userId);
            if (svr != null)
                svr.addFrameListener(new EglRenderer.FrameListener() {
                    @Override
                    public void onFrame(Bitmap bitmap) {
                        runOnUiThread(() -> {
                            ru.savePhoto2Gallery(ControlActivity.this, bitmap, userId);
                            svr.removeFrameListener(this);
                        });
                    }
                }, 1);
        }
    }


    private void initView() {
        wr_video_view = findViewById(R.id.wr_video_view);

        switch_camera = findViewById(R.id.switch_camera);
        switch_hang_up = findViewById(R.id.switch_hang_up);
        videoButton = findViewById(R.id.video_button);
        photoButton = findViewById(R.id.photo_button);
        mChronometer = (Chronometer) findViewById(R.id.video_chronometer);

        //底部抽屉栏展示地址
        bottomSheet = findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        v1 = findViewById(R.id.recyclerview);

        Device device = new Device();
        device.type = "None";
        device.name = "查看全部";
        device.userId = "_all";
        device.ip = "";

        mDevicesList.add(device);

        deviceAdapter = new deviceAdapter(mDevicesList);

        v1.setAdapter(deviceAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(ControlActivity.this);
        v1.setLayoutManager(layoutManager);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, @BottomSheetBehavior.State int newState) {
                behavior.setState(newState);
            }


            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.d("BottomSheetDemo", "slideOffset:" + slideOffset);
            }
        });
    }

    private void initVar() {
        // 设置宽高比例
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (manager != null) {
            mScreenWidth = manager.getDefaultDisplay().getWidth();
        }
        wr_video_view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mScreenWidth));
        rootEglBase = EglBase.create();

    }

    private void initListner() {
        // 转换摄像头
        switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

        // 挂断
        switch_hang_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(ControlActivity.this, "finish~", Toast.LENGTH_LONG).show();
//                endRecordCapture();
                hangUp();
            }
        });

        photoButton.setOnClickListener(v -> {
            if (RecordUtil.isFullDefinition) {
                if (cameraService != null) {
                    Toast.makeText(getBaseContext(), "控制所有端拍照", Toast.LENGTH_SHORT).show();
                    cameraService.takePicture(false, false);
                    TransferUtil.S2C("photo");
                }
            } else {
                ru.catchPhoto(ControlActivity.this, _videoViews.get(myId));
            }
        });
        videoButton.setOnClickListener(v -> {
            if (!activateVideo) {
                TimerManager.getInstance().restart();
                showText.setText("正在录制第" + currentIndex + "个视频");
                ru.setVideoStart(_vfrs.get(myId), _localVideoTrack, rootEglBase);
                activateVideo = true;

                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "开始录制", Toast.LENGTH_SHORT).show();
                });
                TransferUtil.S2C("start");
            } else {
                endRecordCapture();
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "finish capturing", Toast.LENGTH_SHORT).show();
                });
                ru.terminateVideo(_vfrs.get(myId), _localVideoTrack, rootEglBase, ControlActivity.this,false,false);
                activateVideo = false;
                TransferUtil.S2C("stop");
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
        });
    }

    boolean activateVideo = false;
    private static final String TAG = "ChatRoomActivity";

    private void startCall() {
        manager = WebRTCManager.getInstance();
        manager.setCallback(this);

        if (!PermissionUtil.isNeedRequestPermission(ControlActivity.this)) {
            manager.joinRoom(getApplicationContext(), rootEglBase);
        }

    }

    @Override
    public void onSetLocalStream(MediaStream stream, String userId) {
        List<VideoTrack> videoTracks = stream.videoTracks;
        if (videoTracks.size() > 0) {
            _localVideoTrack = videoTracks.get(0);
        }
        myId = userId;
        RecordUtil.setMyId(userId);
        userIdList.add(userId);
        streamList.add(stream);

        currentIndex = 1;

        Device device = new Device();

        device.type = userId.substring(0, 5);
        device.name = "控制端";
        device.userId = userId;
        device.ip = getIPFromUserId(userId);

        mDevicesList.add(device);
        runOnUiThread(() -> {
            deviceAdapter.notifyDataSetChanged();
        });

        runOnUiThread(() -> {
            addView(userId, stream);
        });


    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String userId) {
        userIdList.add(userId);
        streamList.add(stream);

        Device device = new Device();

        device.type = userId.substring(0, 5);
        device.name = "采集端" + Integer.toString(userIdList.size()-2);
        device.userId = userId;
        device.ip = getIPFromUserId(userId);

        mDevicesList.add(device);
        runOnUiThread(() -> {
            deviceAdapter.notifyDataSetChanged();
        });

        runOnUiThread(() -> {
            addView(userId, stream);
        });

    }

    @Override
    public void onCloseWithId(String userId) {
        int pos = userIdList.indexOf(userId);
        userIdList.remove(userId);
        streamList.remove(pos - 1);
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).userId.equals(userId)) {
                mDevicesList.remove(i);
            }
        }

        runOnUiThread(() -> {
            deviceAdapter.notifyDataSetChanged();
        });
        runOnUiThread(() -> {
            removeView(userId);
        });



    }


    private void addView(String id, MediaStream stream) {
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(ControlActivity.this);
        renderer.init(rootEglBase.getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderer.setMirror(true);
        // set render
        ProxyVideoSink sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        if (stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(sink);
            VideoFileRenderer vfr = null;
            _vfrs.put(id, vfr);
        }
        _videoViews.put(id, renderer);
        _sinks.put(id, sink);
        _infos.add(new MemberBean(id));
//        wr_video_view.addView(renderer);  改动
        wr_video_view.addView(renderer, 0);
        int size = _infos.size();
        for (int i = 0; i < size; i++) {
            MemberBean memberBean = _infos.get(i);
            SurfaceViewRenderer renderer1 = _videoViews.get(memberBean.getId());
            if (renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = getWidth(size);
                layoutParams.width = getWidth(size);
                layoutParams.leftMargin = getX(size, i);
                layoutParams.topMargin = getY(size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }


    private void removeView(String userId) {
        ProxyVideoSink sink = _sinks.get(userId);
        SurfaceViewRenderer renderer = _videoViews.get(userId);
        VideoFileRenderer vfr = _vfrs.get(userId);
        if (sink != null) {
            sink.setTarget(null);
        }
        if (renderer != null) {
            renderer.release();
        }
        if (vfr != null) {
            vfr.release();
        }
        _sinks.remove(userId);
        _videoViews.remove(userId);
        _infos.remove(new MemberBean(userId));
        wr_video_view.removeView(renderer);


        int size = _infos.size();
        for (int i = 0; i < _infos.size(); i++) {
            MemberBean memberBean = _infos.get(i);
            SurfaceViewRenderer renderer1 = _videoViews.get(memberBean.getId());
            if (renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = getWidth(size);
                layoutParams.width = getWidth(size);
                layoutParams.leftMargin = getX(size, i);
                layoutParams.topMargin = getY(size, i);
                renderer1.setLayoutParams(layoutParams);
            }

        }

    }

    private int getWidth(int size) {
        if (size <= 4) {
            return mScreenWidth / 2;
        } else if (size <= 9) {
            return mScreenWidth / 3;
        }
        return mScreenWidth / 3;
    }

    private int getX(int size, int index) {
        if (size <= 4) {
            if (size == 3 && index == 2) {
                return mScreenWidth / 4;
            }
            return (index % 2) * mScreenWidth / 2;
        } else if (size <= 9) {
            if (size == 5) {
                if (index == 3) {
                    return mScreenWidth / 6;
                }
                if (index == 4) {
                    return mScreenWidth / 2;
                }
            }

            if (size == 7 && index == 6) {
                return mScreenWidth / 3;
            }

            if (size == 8) {
                if (index == 6) {
                    return mScreenWidth / 6;
                }
                if (index == 7) {
                    return mScreenWidth / 2;
                }
            }
            return (index % 3) * mScreenWidth / 3;
        }
        return 0;
    }

    private int getY(int size, int index) {
        if (size < 3) {
            return mScreenWidth / 4;
        } else if (size < 5) {
            if (index < 2) {
                return 0;
            } else {
                return mScreenWidth / 2;
            }
        } else if (size < 7) {
            if (index < 3) {
                return mScreenWidth / 2 - (mScreenWidth / 3);
            } else {
                return mScreenWidth / 2;
            }
        } else if (size <= 9) {
            if (index < 3) {
                return 0;
            } else if (index < 6) {
                return mScreenWidth / 3;
            } else {
                return mScreenWidth / 3 * 2;
            }

        }
        return 0;
    }


    @Override  // 屏蔽返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
//        hangUp();
//        return super.onKeyDown(keyCode,event);
    }

    @Override
    protected void onDestroy() {
        exit();
        super.onDestroy();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
//        manager.beginTransaction()
//                .replace(R.id.wr_container, fragment)
//                .commit();

    }

    // 切换摄像头
    public void switchCamera() {
        if (cameraService != null)
            cameraService.switchCamera();
        manager.switchCamera();
        isMirrror = !isMirrror;
        for (String id :
                _videoViews.keySet()) {
            _videoViews.get(id).setMirror(isMirrror);
        }
    }

    // 挂断
    public void hangUp() {
        exit();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        manager.toggleMute(enable);
    }

    // 免提
    public void toggleSpeaker(boolean enable) {
        manager.toggleSpeaker(enable);
    }

    // 打开关闭摄像头
    public void toggleCamera(boolean enableCamera) {
        if (_localVideoTrack != null) {
            _localVideoTrack.setEnabled(enableCamera);
        }

    }

    private void exit() {
        manager.exitRoom();
        for (SurfaceViewRenderer renderer : _videoViews.values()) {
            renderer.release();
        }
        for (ProxyVideoSink sink : _sinks.values()) {
            sink.setTarget(null);
        }
        _videoViews.clear();
        _sinks.clear();
        _infos.clear();
        _vfrs.clear();

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
