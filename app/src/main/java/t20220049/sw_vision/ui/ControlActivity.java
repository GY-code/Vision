package t20220049.sw_vision.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import t20220049.sw_vision.transfer.server.WifiServer;
import t20220049.sw_vision.utils.TimerManager;
import t20220049.sw_vision.utils.VideoFragment;
import t20220049.sw_vision.utils.CameraService;
import t20220049.sw_vision.utils.RecordUtil;
import t20220049.sw_vision.utils.VideoFragmentManager;
import t20220049.sw_vision.utils.WatchUtils;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群聊界面
 * 支持 9 路同時通信
 */
public class ControlActivity extends AppCompatActivity implements IViewCallback {

    private FrameLayout wr_video_view;

    private WebRTCManager manager;
    private Map<String, SurfaceViewRenderer> _videoViews = new HashMap<>();
    private Map<String, LinearLayout> _outerViews = new HashMap<>();
    private Map<String, ProxyVideoSink> _sinks = new HashMap<>();
    private Map<String, TextView> _textViews = new HashMap<>();
    private Map<String, VideoFileRenderer> _vfrs = new HashMap<>();
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

    ImageView switch_hang_up;
    ImageView photoButton;
    ImageView videoButton;
    ImageView patternButton;
    private Chronometer mChronometer;
    private int videoState = 0;

    // 上拉框显示
    RelativeLayout bottomSheet;
    BottomSheetBehavior behavior;
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
        int stat; // 1 collect, 2 collectReady, 3 collecting
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
            String collectUrl = "@drawable/caiji";
            String collectReadyUrl = "@drawable/caijizhong";
            String collectingUrl = "@drawable/caijizhunbei";


            Drawable collect = getResources().getDrawable(R.drawable.ic_caiji);
            Drawable collectReady = getResources().getDrawable(R.drawable.ic_caijizhunbei);
            Drawable collecting = getResources().getDrawable(R.drawable.ic_caijizhong);

            if (device.stat == 1) holder.selectButton.setImageDrawable(collect);
            else if (device.stat == 2) holder.selectButton.setImageDrawable(collectReady);
            else holder.selectButton.setImageDrawable(collecting);

            holder.selectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    for (int i = 0; i < deviceList.size(); i++) {
//                        deviceList.get(i).stat = 1;
//                    }
//
//                    if (!activateVideo) device.stat = 2;
//                    else device.stat = 3;

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

    boolean isVideo = false;

    void requestCollect(String ins) {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("ins", ins);
        String json = jsonParam.toJSONString();
        MediaType mediaType = MediaType.Companion.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
        WatchUtils.sendOkHttpResponse("http://192.168.3.45:8000/server/ins/", requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: ");
                e.getStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String data = response.body().string();
                System.out.println(data);
                //服务器返回信息做对应处理
                if (data.equals("success")) {
                    Looper.prepare();
                    if (ins.equals("photo")) {
                        Log.e(TAG, "onResponse: 已拍照");
                        Toast.makeText(ControlActivity.this, "已拍照", Toast.LENGTH_SHORT).show();
                    }
                    if (ins.equals("video")) {
                        if (!isVideo) {
                            Toast.makeText(ControlActivity.this, "已开始录像", Toast.LENGTH_SHORT).show();
                            isVideo = true;
                        } else {
                            Toast.makeText(ControlActivity.this, "已结束录像", Toast.LENGTH_SHORT).show();
                            isVideo = false;
                        }
                    }
                    Looper.loop();
                } else if (data.equals("fail")) {
                    Looper.prepare();
                    Toast.makeText(ControlActivity.this, "采集端未连接", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        });
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
        super.onCreate(savedInstanceState);
        RecordUtil.setControlActivityWeakRef(ControlActivity.this);
//        setContentView(R.layout.wr_activity_chat_room);
        setContentView(R.layout.acticity_control);
        showText = findViewById(R.id.showText);

//        userIdList.add("_all");
//        streamList.add(null);

        initView();
        initVar();
        initListner();
//        initService();
//        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
//        replaceFragment(chatRoomFragment);
        startCall();
        ru = new RecordUtil(getApplicationContext());

        RxFFmpegInvoke.getInstance().setDebug(true);
    }

    private void changeRecordCapture(String s) {
        if (userIdList.contains(s)) {
            cutRecordCapture();
            String preUserId = userIdList.get(currentIndex);
            int preIndex = currentIndex;
            currentIndex = userIdList.indexOf(s);

            mDevicesList.get(preIndex).stat = 1;
            if (!activateVideo)
                mDevicesList.get(currentIndex).stat = 2;
            else
                mDevicesList.get(currentIndex).stat = 3;
            runOnUiThread(() -> {
                deviceAdapter.notifyDataSetChanged();
            });
//            showText.setText("正在录制第" + currentIndex + "个视频");
            if (_textViews.get(preUserId) != null) {
                _textViews.get(preUserId).setText("");
            }
            if (activateVideo) {
                _textViews.get(s).setText("正在录制");
            }
        }
    }

    private void endRecordCapture() {
        cutRecordCapture();
        mDevicesList.get(currentIndex).stat = 2;
        runOnUiThread(() -> {
            deviceAdapter.notifyDataSetChanged();
        });
        for (String id : _textViews.keySet()) {
            TextView tw = _textViews.get(id);
            if (tw != null) {
                tw.setText("");
            }
        }

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
//        Toast.makeText(ControlActivity.this,
//                userIdList.get(currentIndex) + " Start at: " + Double.toString(startTime) + "\nRecord: " + Double.toString(durance) + "seconds",
//                Toast.LENGTH_LONG).show();
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

        switch_hang_up = findViewById(R.id.switch_hang_up);
        videoButton = findViewById(R.id.video_button);
        photoButton = findViewById(R.id.photo_button);
        patternButton = findViewById(R.id.pattern);
        mChronometer = (Chronometer) findViewById(R.id.video_chronometer);

        //底部抽屉栏展示地址
        bottomSheet = findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);

        Device device = new Device();
        device.type = "None";
        device.name = "查看全部";
        device.userId = "_all";
        device.ip = "";
        device.stat = 1;

//        mDevicesList.add(device);

        deviceAdapter = new deviceAdapter(mDevicesList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(ControlActivity.this);
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
            requestCollect("photo");
        });
        videoButton.setOnClickListener(v -> {
            requestCollect("video");

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

        patternButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initPopWindow(view);
            }
        });
    }

    private void initPopWindow(View v) {
        View view = LayoutInflater.from(ControlActivity.this).inflate(R.layout.pattern_menu, null, false);
        Button btn_joint = (Button) view.findViewById(R.id.pattern_joint);
        Button btn_pano = (Button) view.findViewById(R.id.pattern_pano);
        Button btn_prime = (Button) view.findViewById(R.id.pattern_prime);
        //1.构造一个PopupWindow，参数依次是加载的View，宽高
        final PopupWindow popWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

//        popWindow.setAnimationStyle(R.anim.anim_pop);  //设置加载动画

        //这些为了点击非PopupWindow区域，PopupWindow会消失的，如果没有下面的
        //代码的话，你会发现，当你把PopupWindow显示出来了，无论你按多少次后退键
        //PopupWindow并不会关闭，而且退不出程序，加上下述代码可以解决这个问题
        popWindow.setTouchable(true);
        popWindow.setTouchInterceptor(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });
        popWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));    //要为popWindow设置一个背景才有效


        //设置popupWindow显示的位置，参数依次是参照View，x轴的偏移量，y轴的偏移量
        popWindow.showAsDropDown(v, 50, 0);

        //设置popupWindow里的按钮的事件
        btn_pano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 1;
                Toast.makeText(ControlActivity.this, "当前为全景模式", Toast.LENGTH_SHORT).show();
                popWindow.dismiss();
            }
        });
        btn_joint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 0;
                Toast.makeText(ControlActivity.this, "当前为拼图模式", Toast.LENGTH_SHORT).show();
                popWindow.dismiss();
            }
        });
        btn_prime.setOnClickListener(view1 -> {
            mode = -1;
            Toast.makeText(ControlActivity.this, "当前为原图模式", Toast.LENGTH_SHORT).show();
            popWindow.dismiss();
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
        device.stat = 2;

        mDevicesList.add(device);
        runOnUiThread(() -> {
            deviceAdapter.notifyDataSetChanged();
        });

        runOnUiThread(() -> {
            addView(userId, stream);
        });
        switchCamera();
        toggleMic(false);

    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String userId) {
        userIdList.add(userId);
        streamList.add(stream);

        Device device = new Device();

        device.type = userId.substring(0, 5);
        device.name = "采集端" + Integer.toString(userIdList.size() - 1);
        device.userId = userId;
        device.ip = getIPFromUserId(userId);
        device.stat = 1;

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
//        renderer.setMirror(true);
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
//        wr_video_view.addView(renderer, 0);
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);

        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        text.setText("");

        view.addView(text);
        view.addView(renderer);

        _outerViews.put(id, view);
        _textViews.put(id, text);
        wr_video_view.addView(view, 0);

        regenerateView();
    }

    private void removeView(String userId) {
        ProxyVideoSink sink = _sinks.get(userId);
        SurfaceViewRenderer renderer = _videoViews.get(userId);
        LinearLayout outView = _outerViews.get(userId);
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
        _textViews.remove(userId);
        _infos.remove(new MemberBean(userId));
        wr_video_view.removeView(outView);

        regenerateView();

    }

    private void regenerateView() {
        int size = _infos.size();
        for (int i = 0; i < size; i++) {
            MemberBean memberBean = _infos.get(i);
//            SurfaceViewRenderer renderer1 = _videoViews.get(memberBean.getId());
            LinearLayout view = _outerViews.get(memberBean.getId());
            if (view != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.setMargins(10, 10, 10, 10);
                layoutParams.gravity = 1;
                view.setLayoutParams(layoutParams);
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
        for (String id : _videoViews.keySet()) {
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
