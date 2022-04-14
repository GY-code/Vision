package t20220049.sw_vision.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import t20220049.sw_vision.webRTC_utils.PeerConnectionHelper;


public class RecordUtil {
    //0表示采集端  1 表示控制端
    Context context;
    String srcPath;
    File y4mfile;
    private static final String VIDEO_BASE_URI = "content://media/external/video/media";
    private static final String TAG = "RecordUtil";
    private long stime;
    private long ltime;

    public RecordUtil(Context c) {
        context = c;
        srcPath = context.getFilesDir().getAbsolutePath() + "/";
        y4mfile=new File(srcPath + "local.y4m");
        RxFFmpegInvoke.getInstance().setDebug(true);
    }

    public void cleary4m() {
        if (y4mfile.isFile() && y4mfile.exists()) {
            y4mfile.delete();
        }
    }

    public void setVideoStart(VideoFileRenderer vfr, VideoTrack localTrack, EglBase rootEglBase) {
        stime=new Date().getTime();
        try {
            vfr = new VideoFileRenderer(y4mfile.toString(),
                    PeerConnectionHelper.VIDEO_RESOLUTION_WIDTH, PeerConnectionHelper.VIDEO_RESOLUTION_HEIGHT, rootEglBase.getEglBaseContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        localTrack.addSink(vfr);
    }

    public void  terminateVideo(VideoFileRenderer vfr, VideoTrack localTrack, EglBase rootEglBase, Activity activity) {
        ltime=(new Date().getTime()-stime)/1000;
        if (vfr != null) {
            localTrack.removeSink(vfr);
            vfr.release();
        }
        activity.runOnUiThread(() -> {
            Toast.makeText(context, "结束录制", Toast.LENGTH_SHORT).show();
        });
        new Thread(() -> {
            String text = "ffmpeg -t "+ltime+" -accurate_seek -i " + y4mfile.toString()+" " + srcPath + "local" + ".mp4";
            Log.d(TAG, "terminateVideo: " + text);
            String[] commands = text.split(" ");
            RxFFmpegInvoke.getInstance().runCommand(commands, new RxFFmpegInvoke.IFFmpegListener() {
                @Override
                public void onFinish() {
                    Log.e(TAG, "onFinish: " + text);
                    insertVideo(srcPath + "local" + ".mp4", context);
                    activity.runOnUiThread(() -> {
                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show();
                    });
                    cleary4m();
                }

                @Override
                public void onProgress(int progress, long progressTime) {
                    Log.d(TAG, "onProgress: "+progress);
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

    public void insertVideo(String videoPath, Context context) {
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
        mCurrentVideoValues.put(MediaStore.Video.Media.DATA, videoPath);
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
            new File(imagePath).delete();
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
    public void havePhoto(Activity activity, SurfaceViewRenderer mySurfaceViewRenderer) {
        if (mySurfaceViewRenderer != null)
            mySurfaceViewRenderer.addFrameListener(new EglRenderer.FrameListener() {
                @Override
                public void onFrame(Bitmap bitmap) {
                    activity.runOnUiThread(() -> {
                        savePhoto(activity, bitmap);
                        mySurfaceViewRenderer.removeFrameListener(this);
                    });
                }
            }, 1);
    }
    //控制端存储所有缩略画面
    public void haveAllPhoto(Activity activity, Map<String, SurfaceViewRenderer> _videoViews) {
        for (String userId : _videoViews.keySet()) {
            SurfaceViewRenderer svr = _videoViews.get(userId);
            if (svr != null)
                svr.addFrameListener(new EglRenderer.FrameListener() {
                    @Override
                    public void onFrame(Bitmap bitmap) {
                        activity.runOnUiThread(() -> {
                            savePhoto(activity, bitmap);
                            svr.removeFrameListener(this);
                        });
                    }
                }, 1);
        }
    }
    //存到本地photo
    public void savePhoto(Activity activity, Bitmap bitmap) {
        long curTime = new Date().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "record_photo "+sdf.format(curTime));
        String fileName = "record_photo " + sdf.format(curTime) + ".png";
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, fileName, fileName);
        activity.runOnUiThread(() -> {
            Toast.makeText(context, "已保存图片到相册", Toast.LENGTH_SHORT).show();
        });
        File appDir = new File(context.getFilesDir() + "");
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
    private String getBetweenStr(long between) {
        long hour = (between / (60 * 60 * 1000));
        long min = ((between / (60 * 1000)) - hour * 60);
        long s = (between / 1000 - hour * 60 * 60 - min * 60);
        long ms = (between - hour * 60 * 60 * 1000 - min * 60 * 1000 - s * 1000);
        return String.format("%02d:%02d:%02d.%03d", hour, min, s, ms);
    }


}
