package t20220049.sw_vision.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import t20220049.sw_vision.R;
import t20220049.sw_vision.bean.MediaType;
import t20220049.sw_vision.webRTC_utils.ProxyVideoSink;

public class TrackingActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    EglBase _rootEglBase = EglBase.create();
    PeerConnectionFactory _factory;
    private ProxyVideoSink localRender;
    private SurfaceViewRenderer local_view;
    private VideoTrack _localVideoTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        getSupportFragmentManager().addOnBackStackChangedListener(this);
//        if (savedInstanceState == null)
//            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new TrackerCameraFragment(), "camera").commit();
//        else
//            onBackStackChanged();

        Button button = findViewById(R.id.button5);
        _factory = createConnectionFactory();
        local_view = findViewById(R.id.test_local_view);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VideoCapturer videoCapturer = new TestCapturer(getApplicationContext());
                initLocalView();
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", _rootEglBase.getEglBaseContext());
                VideoSource videoSource = _factory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
                _localVideoTrack = _factory.createVideoTrack("ARDAMSv0", videoSource);
                _localVideoTrack.addSink(localRender);


                localRender.setTarget(local_view);
            }
        });
    }

    private void initLocalView() {
        local_view.init(_rootEglBase.getEglBaseContext(), null);
        local_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        local_view.setZOrderMediaOverlay(true);
        local_view.setMirror(true);
        localRender = new ProxyVideoSink();
    }

    private PeerConnectionFactory createConnectionFactory() {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                        .createInitializationOptions());

        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        encoderFactory = new DefaultVideoEncoderFactory(
                _rootEglBase.getEglBaseContext(),
                true,
                true);
        decoderFactory = new DefaultVideoDecoderFactory(_rootEglBase.getEglBaseContext());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(getApplicationContext()).createAudioDeviceModule())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, null);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
        }
    }
}