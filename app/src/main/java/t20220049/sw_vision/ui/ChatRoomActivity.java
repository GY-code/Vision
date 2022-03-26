package t20220049.sw_vision.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import t20220049.sw_vision.ControlVideo;
import t20220049.sw_vision.wtc_meeting.IViewCallback;
import t20220049.sw_vision.wtc_meeting.PeerConnectionHelper;
import t20220049.sw_vision.wtc_meeting.ProxyVideoSink;
import t20220049.sw_vision.R;
import t20220049.sw_vision.wtc_meeting.WebRTCManager;
import t20220049.sw_vision.bean.MemberBean;
import t20220049.sw_vision.utils.PermissionUtil;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
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
public class ChatRoomActivity extends AppCompatActivity implements IViewCallback {

    private FrameLayout wr_video_view;

    private WebRTCManager manager;
    private Map<String, SurfaceViewRenderer> _videoViews = new HashMap<>();
    private Map<String, ProxyVideoSink> _sinks = new HashMap<>();
    private List<MemberBean> _infos = new ArrayList<>();
    private VideoTrack _localVideoTrack;

    private int mScreenWidth;
    private String myId;
    private EglBase rootEglBase;

    ImageView switch_camera;
    ImageView switch_hang_up;
    ImageView photoButton;
    ImageView videoButton;

    // 上拉框显示
    RelativeLayout bottomSheet;
    BottomSheetBehavior behavior;
    RecyclerView v1;
    deviceAdapter deviceAdapter;
    List<Device> mDevicesList = new ArrayList<>();

    public class Device {
        String type;
        String name;
    }

    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }

    class deviceAdapter extends RecyclerView.Adapter<MyViewHolder> {
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(ChatRoomActivity.this, R.layout.device_list, null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Device device = mDevicesList.get(position);
            holder.mType.setText(device.type);
            holder.mName.setText(device.name);
        }

        @Override
        public int getItemCount() {
            return mDevicesList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mType;
        TextView mName;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mType = itemView.findViewById(R.id.txt_mType);
            mName = itemView.findViewById(R.id.txt_mName);
        }
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
        setContentView(R.layout.acticity_video);

        initView();
        initVar();
        initListner();
//        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
//        replaceFragment(chatRoomFragment);
        startCall();

    }

    protected void havePhoto() {
        SurfaceViewRenderer mySurfaceViewRenderer = _videoViews.get(myId);
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


    private void initView() {
        wr_video_view = findViewById(R.id.wr_video_view);

        switch_camera = findViewById(R.id.switch_camera);
        switch_hang_up = findViewById(R.id.switch_hang_up);
        videoButton = findViewById(R.id.video_button);
        photoButton = findViewById(R.id.photo_button);

        //底部抽屉栏展示地址
        bottomSheet = findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        v1 = findViewById(R.id.recyclerview);

        for (int i = 0; i < 20; i++) {
            Device device = new Device();
            device.type = "标题" + i;
            device.name = "内容" + i;
            mDevicesList.add(device);
        }

        deviceAdapter = new deviceAdapter();
        v1.setAdapter(deviceAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(ChatRoomActivity.this);
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
                hangUp();
            }
        });

        photoButton.setOnClickListener(v -> {
            havePhoto();
        });


    }

    private void startCall() {
        manager = WebRTCManager.getInstance();
        manager.setCallback(this);

        if (!PermissionUtil.isNeedRequestPermission(ChatRoomActivity.this)) {
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
        runOnUiThread(() -> {
            addView(userId, stream);
        });
    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String userId) {
        runOnUiThread(() -> {
            addView(userId, stream);
        });


    }

    @Override
    public void onCloseWithId(String userId) {
        runOnUiThread(() -> {
            removeView(userId);
        });


    }


    private void addView(String id, MediaStream stream) {
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(ChatRoomActivity.this);
        renderer.init(rootEglBase.getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderer.setMirror(true);
        // set render
        ProxyVideoSink sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        if (stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(sink);
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
        if (sink != null) {
            sink.setTarget(null);
        }
        if (renderer != null) {
            renderer.release();
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
        manager.switchCamera();
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
