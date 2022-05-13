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
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
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
    private static final int REQUEST_CAMERA_PERMISSION = 200;

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
//                VideoCapturer videoCapturer = new DetectCapturer(getApplicationContext());
                VideoCapturer videoCapturer = createVideoCapture();
                initLocalView();
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", _rootEglBase.getEglBaseContext());
                VideoSource videoSource = _factory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
                videoCapturer.startCapture(1280, 960, 0);
                _localVideoTrack = _factory.createVideoTrack("ARDAMSv0", videoSource);
                localRender.setSurfaceHandler(surfaceTextureHelper.getHandler());
                _localVideoTrack.addSink(localRender);

                localRender.setTarget(local_view);
//                cameraTest();
//                Log.i("zsy", "Test complete");
            }
        });
    }

    private void initLocalView() {
        local_view.init(_rootEglBase.getEglBaseContext(), null);
        local_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        local_view.setZOrderMediaOverlay(true);
//        local_view.setMirror(true);
        localRender = new ProxyVideoSink();
        localRender.setDetect(true);
    }

    private VideoCapturer createVideoCapture() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapture(new Camera2Enumerator(getApplicationContext()));
        } else {
            videoCapturer = createCameraCapture(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(getApplicationContext());
    }

//    private void cameraTest() {
//        VideoCapturer videoCapturer = new DetectCapturer(getApplicationContext());
//        EglBase rootEglBase = EglBase.create();
//
//        PeerConnectionFactory.initialize(
//                PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
//                        .createInitializationOptions());
//        final VideoEncoderFactory encoderFactory;
//        final VideoDecoderFactory decoderFactory;
//        encoderFactory = new DefaultVideoEncoderFactory(
//                rootEglBase.getEglBaseContext(),
//                true,
//                true);
//        decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
//        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
//        PeerConnectionFactory factory = PeerConnectionFactory.builder()
//                                    .setOptions(options)
//                                    .setAudioDeviceModule(JavaAudioDeviceModule.builder(getApplicationContext()).createAudioDeviceModule())
//                                    .setVideoEncoderFactory(encoderFactory)
//                                    .setVideoDecoderFactory(decoderFactory)
//                                    .createPeerConnectionFactory();
//        ProxyVideoSink localRender = new ProxyVideoSink(false);
//
//
//        VideoTrack localVideoTrack;
//
//        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
//        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
//        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
//        localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
//        localVideoTrack.addSink(localRender);
//
//    }

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
        if (ActivityCompat.checkSelfPermission(TrackingActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(TrackingActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            AlertDialog.Builder builder = new AlertDialog.Builder(TrackingActivity.this);
            builder.setTitle("Camera permission required");
            builder.setMessage("This app uses camera only for object tracking and sent the object location to your BLE connected device, the information from your camera is not sent or collected anywhere else");
            builder.setPositiveButton(android.R.string.ok,
                    (dialog, which) -> requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION));
            builder.show();
        }
    }
}