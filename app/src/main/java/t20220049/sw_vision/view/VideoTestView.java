package t20220049.sw_vision.view;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.util.ArrayList;

import io.microshow.rxffmpeg.RxFFmpegInvoke;
import io.microshow.rxffmpeg.RxFFmpegSubscriber;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import t20220049.sw_vision.R;
import t20220049.sw_vision.utils.FFmpegCommandFactory;
import t20220049.sw_vision.utils.VideoFragment;

public class VideoTestView extends AppCompatActivity {

    private RxPermissions rxPermissions = null;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);
        RxFFmpegInvoke.getInstance().setDebug(true);

        Button btn = findViewById(R.id.button3);

        rxPermissions = new RxPermissions(this);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    mCompositeDisposable.add(rxPermissions.request(PERMISSIONS_STORAGE).subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) throws Exception {
                            if (aBoolean) {// 用户同意了权限
                                String filename1 = "VID_20220331_141630.mp4";
                                String filename2 = "VID_20220331_141614.mp4";
                                String filename3 = "VID_20220331_141601.mp4";

                                ArrayList<VideoFragment> fragments = new ArrayList<>();

                                fragments.add(new VideoFragment(1, 2, filename1));
                                fragments.add(new VideoFragment(3, 1, filename2));
                                fragments.add(new VideoFragment(4, 3, filename3));
                                fragments.add(new VideoFragment(7, 1, filename1));
                                fragments.add(new VideoFragment(2, 5, filename3));
                                fragments.add(new VideoFragment(2, 6, filename1));


//                                new Thread(VideoView.this::testFile).start();
                                new Thread(() -> {cutVideosAndCombine(fragments, "output.mp4");}).start();
                            } else {//用户拒绝了权限
                                Toast.makeText(VideoTestView.this, "您拒绝了权限，请往设置里开启权限", Toast.LENGTH_LONG).show();
                            }
                        }
                    }));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void cutVideosAndCombine(ArrayList<VideoFragment> fragments, String outputFile) {
        myRxFFmpegSubscriber subscriber = new myRxFFmpegSubscriber();

        String srcDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/";

        String outputPath = srcDir + outputFile;

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

    }

    private static class myRxFFmpegSubscriber extends RxFFmpegSubscriber {
        @Override
        public void onFinish() {
            Log.e("zsy", "output success");
        }

        @Override
        public void onProgress(int progress, long progressTime) {

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