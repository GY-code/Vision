package t20220049.sw_vision.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import t20220049.sw_vision.ui.CollectActivity;
import t20220049.sw_vision.ui.ControlActivity;
import t20220049.sw_vision.ui_utils.MyNotification;
import t20220049.sw_vision.webRTC_utils.PeerConnectionHelper;


public class RecordUtil {
    //0表示采集端  1 表示控制端
    Context context;
    static Context backupContext;
    String filePath;
    static String localPath;
    String remotePath;
    String localy4m;
    static String localmp4;
    public static String localPhoto;
    public static String remotePhotoPath;
    public static String remoteVideoPath;
    private static String VIDEO_BASE_URI;
    private static final String TAG = "RecordUtil";
    private long stime;
    private long ltime;
    public static boolean isFullDefinition = true;

    public static WeakReference<ControlActivity> ControlActivityWeakRef;

    public static void setControlActivityWeakRef(ControlActivity activity) {
        ControlActivityWeakRef = new WeakReference<>(activity);
    }

    public static WeakReference<CollectActivity> CollectActivityWeakRef;

    public static void setCollectActivityWeakRef(CollectActivity activity) {
        CollectActivityWeakRef = new WeakReference<>(activity);
    }

    public static void setMyId(String myId) {
        RecordUtil.myId = myId;
        localPhoto = localPath + myId + ".png";
        localmp4 = localPath + myId + ".mp4";
    }

    public static String getMyId() {
        return myId;
    }

    private static String myId;


    public RecordUtil(Context c) {
        context = c;
        backupContext = c;
        filePath = context.getFilesDir().getAbsolutePath() + "/";
        localPath = filePath + "local/";
        remotePath = filePath + "remote/";
        localy4m = localPath + "local.y4m";
        remotePhotoPath = remotePath + "photo/";
        remoteVideoPath = remotePath + "video/";
        RxFFmpegInvoke.getInstance().setDebug(true);
        mkDir(localPath);
        mkDir(remotePath);
        mkDir(remotePhotoPath);
        mkDir(remoteVideoPath);
        if (Build.VERSION.SDK_INT >= 29) {
            VIDEO_BASE_URI = String.valueOf(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY));
        } else {
            VIDEO_BASE_URI = String.valueOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        }
    }

    public static void setFullDefinition(boolean b) {
        isFullDefinition = b;
    }

    public void mkDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public static void clearFile(String path) {
        File file = new File(path);
        if (file.isFile() && file.exists()) {
            file.delete();
            Log.e(TAG, "terminateVideo: release0");
        }
    }


    public void setVideoStart(VideoFileRenderer vfr, VideoTrack localTrack, EglBase rootEglBase) {
        stime = new Date().getTime();
        try {
            vfr = new VideoFileRenderer(localy4m, PeerConnectionHelper.VIDEO_RESOLUTION_WIDTH, PeerConnectionHelper.VIDEO_RESOLUTION_HEIGHT, rootEglBase.getEglBaseContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        localTrack.addSink(vfr);
    }

    public void terminateVideo(VideoFileRenderer vfr, VideoTrack localTrack, EglBase rootEglBase, Activity activity, boolean isCollect, boolean isSend, int flag) {
        ltime = (new Date().getTime() - stime) / 1000;
        if (vfr != null) {
            localTrack.removeSink(vfr);
            vfr.release();
            Log.e(TAG, "terminateVideo: release1");
        }
        activity.runOnUiThread(() -> {
            Toast.makeText(context, "结束录制", Toast.LENGTH_SHORT).show();
        });

        // 通知
        MyNotification notification = new MyNotification();
        if(flag == 0){
            notification.sendNotification(CollectActivityWeakRef.get().getApplicationContext(), 4, "本地视频处理", "本地视频处理进度");
        }else{
            notification.sendNotification(ControlActivityWeakRef.get().getApplicationContext(), 3, "本地视频处理", "本地视频处理进度");
        }

        clearFile(localmp4);
        clearFile(remoteVideoPath + myId + ".mp4");
        new Thread(() -> {
            String text;
            if (isCollect) {
                text = "ffmpeg -t " + ltime + " -accurate_seek -i " + localy4m + " " + localmp4;
            } else {
                text = "ffmpeg -t " + ltime + " -accurate_seek -i " + localy4m + " " + remoteVideoPath + myId + ".mp4";
            }
            Log.e(TAG, "terminateVideo: " + text);
            String[] commands = text.split(" ");
            RxFFmpegInvoke.getInstance().runCommand(commands, new RxFFmpegInvoke.IFFmpegListener() {
                @Override
                public void onFinish() {
                    Log.e(TAG, "onFinish: " + text);
                    if (isCollect) {
                        saveVideo2Gallery(localmp4, context);
                    } else {
                        saveVideo2Gallery(remoteVideoPath + myId + ".mp4", context);
                    }
                    activity.runOnUiThread(() -> {
                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show();
                    });
                    clearFile(localy4m);
                    if (isCollect && isSend) {
                        TransferUtil.C2S_Video(localmp4, context);
                    }
                }

                @Override
                public void onProgress(int progress, long progressTime) {
                    Log.d(TAG, "onProgress: " + progress);
                    if(flag==1){
                        new Thread(() -> {
                            notification.updateNotification(3, progress);
                        }).start();
                    }else{
                        new Thread(() -> {
                            notification.updateNotification(4, progress);
                        }).start();
                    }
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, message);
                }
            });
        }).start();

    }

    // reference：https://developer.android.com/training/data-storage/shared/media#java
    public void saveVideo2Gallery(String videoPath, Context context) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        Uri newUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", new File(videoPath));
//        retriever.setDataSource(getApplicationContext(), newUri);// videoPath 本地视频的路径
//
        retriever.setDataSource(videoPath);
        int nVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int nVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        int duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        long dateTaken = System.currentTimeMillis();
        File file = new File(videoPath);
        String title = file.getName();
        String filename = file.getName();
        String mime = "video/mp4";
        ContentValues mCurrentVideoValues = new ContentValues(9);
        mCurrentVideoValues.put(MediaStore.Video.Media.TITLE, title);
        mCurrentVideoValues.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        mCurrentVideoValues.put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        mCurrentVideoValues.put(MediaStore.Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        mCurrentVideoValues.put(MediaStore.Video.Media.WIDTH, nVideoWidth);
        mCurrentVideoValues.put(MediaStore.Video.Media.HEIGHT, nVideoHeight);
        mCurrentVideoValues.put(MediaStore.Video.Media.RESOLUTION, Integer.toString(nVideoWidth) + "x" + Integer.toString(nVideoHeight));
        mCurrentVideoValues.put(MediaStore.Video.Media.SIZE, new File(videoPath).length());
        mCurrentVideoValues.put(MediaStore.Video.Media.DURATION, duration);
        ContentResolver contentResolver = context.getContentResolver();
        Uri videoTable = Uri.parse(VIDEO_BASE_URI);
        Uri uri = contentResolver.insert(videoTable, mCurrentVideoValues);
        writeFile(videoPath, mCurrentVideoValues, contentResolver, uri);
    }

    private void writeFile(String imagePath, ContentValues values, ContentResolver contentResolver, Uri item) {
        try (OutputStream rw = contentResolver.openOutputStream(item, "rw")) {
            // Write data into the pending image.
            Sink sink = Okio.sink(rw);
            BufferedSource buffer = Okio.buffer(Okio.source(new File(imagePath)));
            buffer.readAll(sink);
            values.put(MediaStore.Video.Media.IS_PRIVATE, 0);
            contentResolver.update(item, values, null, null);
//            new File(imagePath).delete();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Cursor query = context.getContentResolver().query(item, null, null, null);
                if (query != null) {
                    int count = query.getCount();
                    Log.d("writeFile", "writeFile result :" + count);
                    query.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //照一张本地缩略图片
    public void catchPhoto(Activity activity, SurfaceViewRenderer mySurfaceViewRenderer) {
        Log.e(TAG, "havePhoto");
        if (mySurfaceViewRenderer != null)
            Log.e(TAG, "add");
        mySurfaceViewRenderer.addFrameListener(new EglRenderer.FrameListener() {
            @Override
            public void onFrame(Bitmap bitmap) {
                activity.runOnUiThread(() -> {
                    savePhotoInLocal(bitmap);
                    mySurfaceViewRenderer.removeFrameListener(this);
                });
            }
        }, 1);
    }

    //控制端存储所有缩略画面
    public void catchAllPhoto(Activity activity, Map<String, SurfaceViewRenderer> _videoViews) {
        for (String userId : _videoViews.keySet()) {
            SurfaceViewRenderer svr = _videoViews.get(userId);
            if (svr != null)
                svr.addFrameListener(new EglRenderer.FrameListener() {
                    @Override
                    public void onFrame(Bitmap bitmap) {
                        activity.runOnUiThread(() -> {
                            savePhotoInLocal(bitmap);
                            svr.removeFrameListener(this);
                        });
                    }
                }, 1);
        }
    }

    public String getCurTimeStr() {
        long curTime = new Date().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(curTime);
    }

    //存photo到文件系统
    public void savePhotoInLocal(Bitmap bitmap) {
        Log.e(TAG, "save");
        clearFile(localPhoto);
        File file = new File(localPhoto);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //存photo到文件系统
    public void savePhotoInRemote(Bitmap bitmap) {
        Log.e(TAG, "save");
        clearFile(localPhoto);
        File file = new File(remotePhotoPath + myId + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePhoto2Gallery(Activity activity, Bitmap bitmap, String fileName) {
        activity.runOnUiThread(() -> {
            Toast.makeText(context, "已保存图片到相册", Toast.LENGTH_SHORT).show();
        });
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, fileName, fileName);
    }

    public void savePhoto2Gallery(Bitmap bitmap) {
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, getCurTimeStr(), getCurTimeStr());
    }

    private String getBetweenStr(long between) {
        long hour = (between / (60 * 60 * 1000));
        long min = ((between / (60 * 1000)) - hour * 60);
        long s = (between / 1000 - hour * 60 * 60 - min * 60);
        long ms = (between - hour * 60 * 60 * 1000 - min * 60 * 1000 - s * 1000);
        return String.format("%02d:%02d:%02d.%03d", hour, min, s, ms);
    }


}
