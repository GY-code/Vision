package t20220049.sw_vision.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
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
import org.webrtc.Camera2Capturer;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CapturerObserver;
import org.webrtc.FileVideoCapturer;
import org.webrtc.JavaI420Buffer;
import org.webrtc.Logging;
import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import t20220049.sw_vision.utils.RecordUtil;


public class TestCapturer implements VideoCapturer {
    private final String TAG = "TestCapturer";

    private org.webrtc.CapturerObserver capturerObserver;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private final Size CamResolution = new Size(1280,720);
    private CameraCaptureSession mCaptureSession;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private Mat mImageGrab = new Mat();
//    private Bitmap mBitmapGrab = null;
    private org.opencv.core.Rect mInitRectangle = null;
    private Point[] mPoints = new Point[2];
    private boolean mProcessing = false;
    private TrackerCameraFragment.Drawing mDrawing = TrackerCameraFragment.Drawing.DRAWING;
    private boolean mShowCordinate = false;
//    private AppCompatActivity activity;
//    private SurfaceViewRenderer viewRenderer;
    private Context _context;
    private boolean saveFlag = true;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraOpenCloseLock.release();
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
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

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    Log.d(TAG, "Capture failed: " + failure);
                }
            };
    private SurfaceTextureHelper mSurfaceTextureHelper;
    private Surface surface;

//    public TestCapturer(AppCompatActivity activity, SurfaceViewRenderer viewRenderer) {
//        this.activity = activity;
//        this.viewRenderer = viewRenderer;
//
////        openCamera();
////        startBackgroundThread();
//    }

    public TestCapturer(Context context) {
        this._context = context;

//        openCamera();
//        startBackgroundThread();
    }


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

            Log.e(TAG, "enter image available");

            // image to byte array
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[bb.remaining()];
            bb.get(data);

//            Bitmap bitmapImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);
//            if (saveFlag) {
//                saveFlag = false;
//                Log.i(TAG, "Bitmap width: " + bitmapImage.getWidth()
//                        + ", height: " + bitmapImage.getHeight()
//                        + ", has alpha: " + bitmapImage.hasAlpha());
//                saveImg(bitmapImage, "raw_bitmap.jpg");
//            }

            mImageGrab = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
            org.opencv.core.Core.transpose(mImageGrab, mImageGrab);
//            org.opencv.core.Core.flip(mImageGrab, mImageGrab, 1);
//            org.opencv.imgproc.Imgproc.resize(mImageGrab, mImageGrab, new org.opencv.core.Size(240,320));

            tick(mImageGrab);

            image.close();
            mProcessing = false;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void tick(Mat img) {

        try {
            int width = img.cols();
            int height = img.rows();

            // RGB to I420
            Mat temp = new Mat(width, height, CvType.CV_8U, new Scalar(4));
            Imgproc.cvtColor(img, temp, Imgproc.COLOR_RGBA2YUV_IYUV);

            Log.e(TAG, "RGB to UV");

            if (saveFlag) {
                saveFlag = false;
                saveImg(img, "hh.jpg");
                saveImg(temp, "hhhh.jpg");
//                RecordUtil recordUtil = new RecordUtil(_context);
//                RecordUtil.localPhoto = "hhh";
//                Bitmap mBitmap = null;
//                mBitmap = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(temp, mBitmap);
//                Log.e(TAG, "enter save");
//
//                Log.e(TAG, "finish save");
            }

//             Mat to byte array
            int bufferSize = temp.channels()*temp.cols()*temp.rows();
            byte [] buffer = new byte[bufferSize];
            temp.get(0,0,buffer); // get all the pixels

            Log.e(TAG, "Mat to byte array");
//
//             byte array to Video Frame
            long timestampNS = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            NV21Buffer NVBuffer = new NV21Buffer(buffer, width, height, null);
            VideoFrame videoFrame = new VideoFrame(NVBuffer, 0, timestampNS);


//            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//
//            Utils.matToBitmap(temp, bmp);
//
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
//
//            YuvConverter yuvConverter = new YuvConverter();
//            TextureBufferImpl buffer = new TextureBufferImpl(width, height,
//                    VideoFrame.TextureBuffer.Type.RGB, textures[0],  matrix,
//                    textureHelper.getHandler(), yuvConverter, null);

            Log.e(TAG, "byte array to Video Frame");

            capturerObserver.onFrameCaptured(videoFrame);

            Log.e(TAG, "capture frame");
        } catch (Exception e) {
            e.printStackTrace();
        }

//        VideoFrame.I420Buffer i420Buf = yuvConverter.convert(buffer);
//        VideoFrame videoFrame = new VideoFrame(i420Buf, 0, lastFrameReceived.getTimestampNs());
//        ogCapturerObserver.onFrameCaptured(videoFrame);

    }

    private void saveImg(Mat srcImg,String fileName) {
        //先把Mat转成Bitmap
        Bitmap mBitmap = null;
        mBitmap = Bitmap.createBitmap(srcImg.cols(), srcImg.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(srcImg, mBitmap);

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(_context.getFilesDir()+"/"+fileName);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.e(TAG, "output file");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void saveImg(Bitmap mBitmap,String fileName) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(_context.getFilesDir()+"/"+fileName);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.e(TAG, "output file");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void openCamera() {
        CameraManager manager = (CameraManager) _context.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            imageDimension = CamResolution;
            // Add permission for camera and let user grant the permission
//            if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
////                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
//                AlertDialog.Builder builder = new AlertDialog.Builder(_context);
//                builder.setTitle("Camera permission required");
//                builder.setMessage("This app uses camera only for object tracking and sent the object location to your BLE connected device, the information from your camera is not sent or collected anywhere else");
////                builder.setPositiveButton(android.R.string.ok,
////                        (dialog, which) -> activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION));
//                builder.show();
//                return;
//            }
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

    protected void createCameraPreview() {
        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mSurfaceTextureHelper.setTextureSize((int) CamResolution.width, (int) CamResolution.height);
            surface = new Surface(mSurfaceTextureHelper.getSurfaceTexture());
            previewRequestBuilder.addTarget(surface);

            imageReader = ImageReader.newInstance((int) CamResolution.width, (int) CamResolution.height, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
            previewRequestBuilder.addTarget(imageReader.getSurface());

            Log.e(TAG, "Set preview finish");

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    Log.e(TAG, "enter on configured");
                    if (null == cameraDevice) {
                        return;
                    }
                    mCaptureSession = cameraCaptureSession;
                    // Auto focus should be continuous for camera preview.
                    previewRequestBuilder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    // Flash is automatically enabled when necessary.
                    previewRequestBuilder.set(
                            CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                    // Finally, we start displaying the camera preview.
                    previewRequest = previewRequestBuilder.build();

                    try {
                        cameraCaptureSession.setRepeatingRequest(previewRequest, captureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(_context, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);

            for (int i=0; i<mPoints.length;i++){
                mPoints[i] = new Point(0,0);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        Log.e(TAG, "Video Capturer initialized");
        this.capturerObserver = capturerObserver;
        this.mSurfaceTextureHelper = surfaceTextureHelper;
        startBackgroundThread();
        openCamera();


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
