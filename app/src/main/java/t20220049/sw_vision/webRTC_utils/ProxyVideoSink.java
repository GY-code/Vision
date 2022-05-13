package t20220049.sw_vision.webRTC_utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.webrtc.GlRectDrawer;
import org.webrtc.GlTextureFrameBuffer;
import org.webrtc.GlUtil;
import org.webrtc.JavaI420Buffer;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.VideoSink;
import org.webrtc.YuvConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import t20220049.sw_vision.R;
import t20220049.sw_vision.arm_controller.ControlCenter;
import t20220049.sw_vision.transfer.app.AppMaster;

/**
 * Created by GuYi on 2022/4/4.
 * android_shuai@163.com
 */
public class ProxyVideoSink implements VideoSink {
    private static final String TAG = "dds_ProxyVideoSink";
    private VideoSink target;
    private YuvConverter yuvConverter = new YuvConverter();
    private Handler viewHandler;
    private int mAbsoluteFaceSize = 0;
    private Context _context = AppMaster.getInstance().getAppContext();
    private CascadeClassifier classifier;
    private boolean isClassifierInit = false;
    private boolean isDetect = false;

    public void setSurfaceHandler(Handler handler) {
        viewHandler = handler;
    }

    public ProxyVideoSink() {
//        isControl = isC;
//        initFaceClassifier();
    }

    public void setDetect(boolean flag) {
        isDetect = flag;
    }

    private final Matrix drawMatrix = new Matrix();
    // Used for bitmap capturing.
    private final GlTextureFrameBuffer bitmapTextureFramebuffer =
            new GlTextureFrameBuffer(GLES20.GL_RGBA);

    @Override
    synchronized public void onFrame(VideoFrame frame) {
        if (target == null) {
            Logging.d(TAG, "Dropping frame in proxy because target is null.");
            return;
        }



        if (isDetect) {
            frame = convertFrame(frame);

            if (!isClassifierInit) {
                isClassifierInit = true;
                initFaceClassifier();
            }
        }



        target.onFrame(frame);
        frame.release();
    }

    synchronized public void setTarget(VideoSink target) {
        this.target = target;
    }

    boolean flag = true;

    public Bitmap videoFrame2Bitmap(VideoFrame frame) {
        drawMatrix.reset();
        drawMatrix.preTranslate(0.5f, 0.5f);
        drawMatrix.preScale(-1f, -1f); // We want the output to be upside down for Bitmap.
        drawMatrix.preTranslate(-0.5f, -0.5f);

        final int scaledWidth = (int) (1 * frame.getRotatedWidth());
        final int scaledHeight = (int) (1 * frame.getRotatedHeight());

        try{
            bitmapTextureFramebuffer.setSize(scaledWidth, scaledHeight);
        }catch (IllegalStateException e){
            e.printStackTrace();
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bitmapTextureFramebuffer.getFrameBufferId());
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, bitmapTextureFramebuffer.getTextureId(), 0);

        GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        VideoFrameDrawer frameDrawer = new VideoFrameDrawer();

        RendererCommon.GlDrawer drawer = new GlRectDrawer();

        frameDrawer.drawFrame(frame, drawer, drawMatrix, 0 /* viewportX */,
                0 /* viewportY */, scaledWidth, scaledHeight);

        final ByteBuffer bitmapBuffer = ByteBuffer.allocateDirect(scaledWidth * scaledHeight * 4);
        GLES20.glViewport(0, 0, scaledWidth, scaledHeight);
        GLES20.glReadPixels(
                0, 0, scaledWidth, scaledHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkNoGLES2Error("EglRenderer.notifyCallbacks");

        final Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(bitmapBuffer);

//        if (flag) {
//            flag = false;
//            saveImg(bitmap, "fuck.jpg");
//        }

        frame.release();

        return bitmap;
    }

    private Bitmap detectFace(Bitmap bitmap) {
        Mat mat = bitmap2Mat(bitmap);

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
            classifier.detectMultiScale(gray, faces, 1.1, 2, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        else Log.e(TAG, "classifier not define");

        Rect[] facesArray = faces.toArray();
        Scalar faceRectColor = new Scalar(0, 255, 0, 255);

        boolean flag = true;

        for (Rect faceRect : facesArray) {

            drawRect(bitmap, faceRect.x, faceRect.y, faceRect.width, faceRect.height);

            int xCenter = faceRect.x + faceRect.width/2;
            int yCenter = faceRect.y + faceRect.height/2;


            if (flag) {
                flag = false;
                ControlCenter.getInstance().moveArm((double)xCenter/width, (double)yCenter/height);
            }
//            System.out.println("fff" + 1 + 5);
            Imgproc.rectangle(mat, faceRect.tl(), faceRect.br(), faceRectColor, 1);
        }


        return bitmap;
    }

//    private void moveArm(double x, double y) {
//
//    }

    private void drawRect(Bitmap bitmap, int x, int y, int width, int height) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10); //线的宽度
        canvas.drawRect(x, y, x+width, y+height, paint);
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


    private Mat bitmap2Mat(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        return mat;
    }

    private VideoFrame bitmap2VideoFrame(Bitmap bitmap) {
        final int[] texture = new int[1];
        GLES20.glGenTextures(1,texture,0);

        Matrix matrix = new Matrix();
        matrix.postScale(-1f,-1f);

        // Bind to the texture in OpenGL
        TextureBufferImpl buffer = new TextureBufferImpl(bitmap.getWidth(),bitmap.getHeight(), VideoFrame.TextureBuffer.Type.RGB, texture[0], matrix, viewHandler, yuvConverter, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);

        Log.e(TAG,"fuc55k");
//        Canvas canvas = new Canvas(bitmap);
//        Paint paint = new Paint();
//        paint.setColor(Color.BLUE);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(100); //线的宽度
//        canvas.drawRect(200, 500, 900, 1500, paint);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

//        bitmap.recycle();

        assert viewHandler != null;

        VideoFrame.I420Buffer i420Buf = yuvConverter.convert(buffer);

        long timestampNS = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());

        VideoFrame frame = new VideoFrame(i420Buf, 0, timestampNS);

        GLES20.glDeleteTextures(1,texture,0);

        return frame;

    }

    public VideoFrame convertFrame(VideoFrame frame) {

        Bitmap bitmap = videoFrame2Bitmap(frame);

        bitmap = detectFace(bitmap);

        VideoFrame videoFrame = bitmap2VideoFrame(bitmap);

        return videoFrame;
    }


    private void bitmapToI420(Bitmap src, JavaI420Buffer dest) {
        int width = src.getWidth();
        int height = src.getHeight();

        if(width != dest.getWidth() || height != dest.getHeight())
            return;

        int strideY = dest.getStrideY();
        int strideU = dest.getStrideU();
        int strideV = dest.getStrideV();
        ByteBuffer dataY = dest.getDataY();
        ByteBuffer dataU = dest.getDataU();
        ByteBuffer dataV = dest.getDataV();

        for(int line = 0; line < height; line++) {
            if(line % 2 == 0) {
                for (int x = 0; x < width; x += 2) {
                    int px = src.getPixel(x, line);
                    byte r = (byte) ((px >> 16) & 0xff);
                    byte g = (byte) ((px >> 8) & 0xff);
                    byte b = (byte) (px & 0xff);

                    dataY.put(line * strideY + x, (byte) (((66 * r + 129 * g + 25 * b) >> 8) + 16));
                    dataU.put(line / 2 * strideU + x / 2, (byte) (((-38 * r + -74 * g + 112 * b) >> 8) + 128));
                    dataV.put(line / 2 * strideV + x / 2, (byte) (((112 * r + -94 * g + -18 * b) >> 8) + 128));

                    px = src.getPixel(x + 1, line);
                    r = (byte) ((px >> 16) & 0xff);
                    g = (byte) ((px >> 8) & 0xff);
                    b = (byte) (px & 0xff);

                    dataY.put(line * strideY + x, (byte) (((66 * r + 129 * g + 25 * b) >> 8) + 16));
                }
            } else {
                for (int x = 0; x < width; x += 1) {
                    int px = src.getPixel(x, line);
                    byte r = (byte) ((px >> 16) & 0xff);
                    byte g = (byte) ((px >> 8) & 0xff);
                    byte b = (byte) (px & 0xff);

                    dataY.put(line * strideY + x, (byte) (((66 * r + 129 * g + 25 * b) >> 8) + 16));
                }
            }
        }
    }

    private void saveImg(Bitmap mBitmap, String fileName) {

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(AppMaster.getInstance().getAppContext().getFilesDir()+"/"+fileName);
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

}