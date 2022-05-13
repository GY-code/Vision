package t20220049.sw_vision.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;

import org.webrtc.CapturerObserver;
import org.webrtc.Logging;
import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import t20220049.sw_vision.R;
import t20220049.sw_vision.arm_controller.ControlCenter;

public class DetectCapturer implements VideoCapturer {
    final String TAG = "DetectCapturer";

    CapturerObserver mCapturerObserver;
    SurfaceTextureHelper mSurfaceTextureHelper;
    Context _context;
    private String cameraId;
    private final Size CamResolution = new Size(720, 860);
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;
    private ImageReader imageReader;
    private boolean mProcessing = false;
    private CameraCaptureSession mCaptureSession;
    private Surface surface;
    private Mat mImageGrab = new Mat();
    private CascadeClassifier classifier;
    private int mAbsoluteFaceSize = 0;
    private boolean setSurfaceTextureHelper = false;

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
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
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraOpenCloseLock.release();
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
            initFaceClassifier();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if (cameraDevice != null) {
                cameraOpenCloseLock.release();
                cameraDevice.close();
                cameraDevice = null;
            }
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

            final Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }

            if (mProcessing) {
                image.close();
                return;
            }

            mProcessing = true;

//            Log.e(TAG, "enter image available");

            // image to byte array
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[bb.remaining()];

            bb.get(data);

            mImageGrab = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
            org.opencv.core.Core.transpose(mImageGrab, mImageGrab);

            image.close();

            detectFace(mImageGrab);

            mProcessing = false;
        }
    };

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final CaptureResult partialResult) {
                }

                @Override
                public void onCaptureCompleted(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final TotalCaptureResult result) {

                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    Log.d(TAG, "Capture failed: " + failure);
                }
            };


    public DetectCapturer(Context context) {
        this._context = context;
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) _context.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];

            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission not get");
                return;
            }
            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
            Log.e(TAG, "openCamera 0");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }

    }

    private void createCameraPreview() {
        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mSurfaceTextureHelper.setTextureSize((int) CamResolution.width, (int) CamResolution.height);
            surface = new Surface(mSurfaceTextureHelper.getSurfaceTexture());
            previewRequestBuilder.addTarget(surface);



            imageReader = ImageReader.newInstance((int) CamResolution.width, (int) CamResolution.height, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
            previewRequestBuilder.addTarget(imageReader.getSurface());

            Log.e(TAG, "Set preview finish");

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), sessionStateCallback, mBackgroundHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void listenForTextureFrames() {
        if (!setSurfaceTextureHelper) {
            setSurfaceTextureHelper = true;
            mSurfaceTextureHelper.startListening((VideoFrame frame) -> {

                // Undo the mirror that the OS "helps" us with.
                // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)

                mCapturerObserver.onFrameCaptured(frame);



                frame.release();
            });
        }
    }

    int count = 0;



    private void detectFace(Mat mat) {
        float mRelativeFaceSize = 0.2f;

        int width = mat.cols();
        int height = mat.rows();

        Mat gray = new Mat(width, height, CvType.CV_8UC1);

        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);

        if (mAbsoluteFaceSize == 0) {
            int rows = gray.rows();
            if (Math.round(rows * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(rows * mRelativeFaceSize);
            }
        }
        MatOfRect faces = new MatOfRect();
        if (classifier != null)
            classifier.detectMultiScale(gray, faces, 1.1, 3, 0,
                    new Size(), new Size());

        Rect[] facesArray = faces.toArray();
        Scalar faceRectColor = new Scalar(0, 255, 0, 255);

        boolean flag = true;

        for (Rect faceRect : facesArray) {
            int x = faceRect.x + faceRect.width/2;
            int y = faceRect.y + faceRect.height/2;

            Log.e(TAG, "Detect face width: " + (double) x/width + ", height: " + (double) y/height);
            if (flag) {
                flag = false;
                ControlCenter.getInstance().moveArm((double)x/width, (double)y/height);
            }
//            System.out.println("fff" + 1 + 5);
            Imgproc.rectangle(mat, faceRect.tl(), faceRect.br(), faceRectColor, 1);
        }
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

    private void initFaceClassifier() {
        try {
            Log.e(TAG, "initFaceClassifier");
            InputStream is = _context.getResources()
                    .openRawResource(R.raw.lbpcascade_frontalface_improved);
            File cascadeDir = _context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.mCapturerObserver = capturerObserver;
        this.mSurfaceTextureHelper = surfaceTextureHelper;
        listenForTextureFrames();
//        startBackgroundThread();
//        openCamera();
//        listenForTextureFrames();
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        Log.i(TAG, "start capture");
        startBackgroundThread();
        openCamera();
    }

    @Override
    public void stopCapture() throws InterruptedException {
        if (mCaptureSession != null) {
            Logging.d(TAG, "Stop capture: Nulling session");
            mCaptureSession.close();
            cameraDevice.close();
            mCaptureSession = null;
            mCapturerObserver.onCapturerStopped();

        } else {
            Logging.d(TAG, "Stop capture: No session open");
        }
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
