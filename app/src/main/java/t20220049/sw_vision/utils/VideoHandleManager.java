package t20220049.sw_vision.utils;

import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import io.microshow.rxffmpeg.RxFFmpegSubscriber;
import t20220049.sw_vision.ui.ControlActivity;
import t20220049.sw_vision.ui_utils.MyNotification;
//import t20220049.sw_vision.view.VideoTestView;

public class VideoHandleManager {
    private static VideoHandleManager instance;
    static MyNotification notification = new MyNotification();

    public static WeakReference<ControlActivity> ControlActivityWeakRef;

    public static void setControlActivityWeakRef(ControlActivity activity) {
        ControlActivityWeakRef = new WeakReference<>(activity);
    }

    private VideoHandleManager() {
    }

    public static VideoHandleManager getInstance() {
        if (instance == null) {
            synchronized (VideoHandleManager.class) {
                if (instance == null) {
                    instance = new VideoHandleManager();
                }
            }
        }

        return instance;
    }

    private void deleteNotExisted(ArrayList<VideoFragment> fragments, String srcDir) {
        ArrayList<String> filePaths = new ArrayList<>();
        int i = 0;
        while (i < fragments.size()) {
            File file = new File(srcDir + fragments.get(i).filename);
            if (!file.exists()) {
                Log.e("zsy", "Delete Not existed file " + fragments.get(i).filename);
                fragments.remove(i);
            } else {
                i++;
            }
        }

    }

    public void cutVideosAndCombine(ArrayList<VideoFragment> fragments, String outputFile, String srcDir) {
        myRxFFmpegSubscriber subscriber = new myRxFFmpegSubscriber();
        deleteNotExisted(fragments, srcDir);

        String outputPath = srcDir + outputFile;

        notification.sendNotification(ControlActivityWeakRef.get().getApplicationContext(), 5, "融合视频", "融合视频进度");
        Log.e("zsy", "FFMPEG start working!");

        ArrayList<String> midMp4Paths = new ArrayList<>();
        ArrayList<String> midTsPaths = new ArrayList<>();

        for (int i = 0; i < fragments.size(); i++) {
            midMp4Paths.add(srcDir + Integer.valueOf(i).toString() + ".mp4");
            midTsPaths.add(srcDir + Integer.valueOf(i).toString() + ".ts");
        }

        File file = new File(outputPath);
        if (file.exists() && file.isFile()) file.delete();

        for (int i = 0; i < midMp4Paths.size(); i++) {  // delete exist files have the same name
            file = new File(midMp4Paths.get(i));
            if (file.exists() && file.isFile()) file.delete();
            file = new File(midTsPaths.get(i));
            if (file.exists() && file.isFile()) file.delete();
        }

        for (int i = 0; i < fragments.size(); i++) {    // cut video and convert mp4 to ts
            VideoFragment fragment = fragments.get(i);
            String sourcePath = srcDir + fragment.filename;
            String mp4Path = midMp4Paths.get(i);
            String tsPath = midTsPaths.get(i);
            String[] command = FFmpegCommandFactory.cutMp4Command(sourcePath, mp4Path, fragment.startTime, fragment.durance).split(" ");
            RxFFmpegInvoke.getInstance().runCommand(command, subscriber);
            command = FFmpegCommandFactory.mp4ToTsCommand(mp4Path, tsPath).split(" ");
            RxFFmpegInvoke.getInstance().runCommand(command, subscriber);
        }

        String[] command = FFmpegCommandFactory.combineTsCommand(
                (String[]) midTsPaths.toArray(new String[(int)midTsPaths.size()]),
                outputPath)
                .split(" ");
        RxFFmpegInvoke.getInstance().runCommand(command, subscriber);


        for (int i = 0; i < midMp4Paths.size(); i++) {  // delete mid files
            file = new File(midMp4Paths.get(i));
            if (file.exists() && file.isFile()) file.delete();
            file = new File(midTsPaths.get(i));
            if (file.exists() && file.isFile()) file.delete();
        }

        Log.e("zsy", "FFMPEG finish working!");

    }


    private static class myRxFFmpegSubscriber extends RxFFmpegSubscriber {
        @Override
        public void onFinish() {
            Log.e("zsy", "output success");
        }

        @Override
        public void onProgress(int progress, long progressTime) {
            Log.d("zsy", "onProgress: " + progress);
            if(progress <= 100){
//                new Thread(() -> {
//                    notification.updateNotification(5, progress);
//                }).start();
                notification.updateNotification(5, progress);
            }else{
//                new Thread(() -> {
//                    notification.updateNotification(5, progress);
//                }).start();
                notification.updateNotification(5, progress);
            }
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onError(String message) {
            System.out.println("error: " + message);
        }
    }
}
