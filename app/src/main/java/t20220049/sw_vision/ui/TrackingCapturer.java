package t20220049.sw_vision.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Tracker;
import org.opencv.video.TrackerDaSiamRPN;
import org.opencv.video.TrackerGOTURN;
import org.opencv.video.TrackerMIL;
import org.webrtc.CapturerObserver;
import org.webrtc.FileVideoCapturer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TrackingCapturer implements VideoCapturer {

    private final String TAG = "TrackingCapturer";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    enum Drawing{
        DRAWING,
        TRACKING,
        CLEAR,
    }

    private org.webrtc.CapturerObserver capturerObserver;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private final Size CamResolution = new Size(1280,720);
    private CameraCaptureSession mCaptureSession;
    private Mat mInitImage;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private Mat mImageGrab;
    private Bitmap mBitmapGrab = null;
    private org.opencv.core.Rect mInitRectangle = null;
    private Point[] mPoints = new Point[2];
    private boolean mProcessing = false;
    private TrackerCameraFragment.Drawing mDrawing = TrackerCameraFragment.Drawing.DRAWING;
    private boolean mTargetLocked = false;
    private boolean mShowCordinate = false;
    private AppCompatActivity activity;
    private SurfaceViewRenderer viewRenderer;

    private Tracker mTracker;

    private final String mSelectedTracker = "TrackerMIL";

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final CaptureResult partialResult) {}

                @Override
                public void onCaptureCompleted(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final TotalCaptureResult result) {}
            };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraOpenCloseLock.release();
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
//            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader reader){
            final Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }

            if(mProcessing){
                image.close();
                return;
            }

            mProcessing = true;

            // image to byte array
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[bb.remaining()];
            bb.get(data);
            mImageGrab = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
            org.opencv.core.Core.transpose(mImageGrab, mImageGrab);
            org.opencv.core.Core.flip(mImageGrab, mImageGrab, 1);
            org.opencv.imgproc.Imgproc.resize(mImageGrab, mImageGrab, new org.opencv.core.Size(240,320));

            tick(mImageGrab);

            image.close();
            processing();
        }
    };

    private void tick(Mat img) {

        Mat temp = new Mat(img.rows(), img.cols(), CvType.CV_8U, new Scalar(4));

        Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGB2YUV_I420);

//        VideoFrame.I420Buffer i420Buf = yuvConverter.convert(buffer);
//
//        VideoFrame videoFrame = new VideoFrame(i420Buf, 0, lastFrameReceived.getTimestampNs());
//
//        ogCapturerObserver.onFrameCaptured(videoFrame);


    }

    public TrackingCapturer(AppCompatActivity activity, SurfaceViewRenderer viewRenderer, Mat mInitImage, Rect mInitRectangle) {
        this.activity = activity;
        this.viewRenderer = viewRenderer;
        this.mInitImage = mInitImage;
        this.mInitRectangle = mInitRectangle;
        initTracker();
    }

    private void initTracker() {
        switch (mSelectedTracker) {
            case "TrackerMedianFlow":
//                    mTracker = TrackerMedianFlow.create();
                break;
            case "TrackerCSRT":
//                    mTracker = TrackerCSRT.create();
                break;
            case "TrackerKCF":
//                    mTracker = Tracker.create();
                break;
            case "TrackerDaSiamRPN":
                mTracker = TrackerDaSiamRPN.create();
                break;
            case "TrackerGOTURN":
                mTracker = TrackerGOTURN.create();
                break;
            case "TrackerMIL":
                mTracker = TrackerMIL.create();
                break;
        }
        assert mInitImage != null;
        assert mInitRectangle != null;
        mTracker.init(mInitImage, mInitRectangle);
    }

    private void processing() {
        org.opencv.core.Rect trackingRectangle = new org.opencv.core.Rect(0, 0, 1,1);
        mTracker.update(mImageGrab, trackingRectangle);

        mPoints[0].x = (int)(trackingRectangle.x*(float)viewRenderer.getWidth()/(float)mImageGrab.cols());
        mPoints[0].y = (int)(trackingRectangle.y*(float)viewRenderer.getHeight()/(float)mImageGrab.rows());
        mPoints[1].x = mPoints[0].x+ (int)(trackingRectangle.width*(float)viewRenderer.getWidth()/(float)mImageGrab.cols());
        mPoints[1].y = mPoints[0].y +(int)(trackingRectangle.height*(float)viewRenderer.getHeight()/(float)mImageGrab.rows());
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            imageDimension = CamResolution;
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Camera permission required");
                builder.setMessage("This app uses camera only for object tracking and sent the object location to your BLE connected device, the information from your camera is not sent or collected anywhere else");
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION));
                builder.show();
                return;
            }
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
        Log.e(TAG, "openCamera 0");
    }


    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int width, int height, int framerate) {

    }

    @Override
    public void stopCapture() throws InterruptedException {

    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isScreencast() {
        return false;
    }


}
