package t20220049.sw_vision.utils;
import t20220049.sw_vision.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class Pano {
    /**
     * 图片拼接成功的标识位
     */
    public final static int OK = 0;

    /**
     * 需要更多图片进行拼接的标识位
     */
    public final static int ERR_NEED_MORE_IMGS = 1;

    /**
     * 图片不符合拼接标准的标识位
     */
    public final static int ERR_HOMOGRAPHY_EST_FAIL = 2;

    /**
     * 图片参数处理失败的标识位
     */
    public final static int ERR_CAMERA_PARAMS_ADJUST_FAIL = 3;

//    private String[] mImagePath = new String[]{"123","123"};

    static {
        System.loadLibrary("native-lib");
    }

//    mergeBitmap(mImagePath,new Pano.onStitchResultListener(){
//
//        @Override
//        public void onSuccess(Bitmap bitmap) {
//            Toast.makeText(Pano.this,"图片拼接成功！",Toast.LENGTH_LONG).show();
//            replaceImage(bitmap);
//        }
//
//        @Override
//        public void onError(String errorMsg) {
//            Toast.makeText(Pano.this,"图片拼接失败！",Toast.LENGTH_LONG).show();
//            System.out.println(errorMsg);
//        }
//    });

    /**
     * 拼接图片的方法
     * @param paths 图像URL的集合
     * @param listener 监听器回调
     * @return
     */
    public void mergeBitmap(String paths[], @NonNull Pano.onStitchResultListener listener) {
        for (String path : paths) {
            if (!new File(path).exists()) {
                listener.onError("无法读取文件或文件不存在:" + path);
                return;
            }
        }
        int wh[] = stitchImages(paths);
        switch (wh[0]) {
            case OK: {
                Bitmap bitmap = Bitmap.createBitmap(wh[1], wh[2], Bitmap.Config.ARGB_8888);
                int result = getBitmap(bitmap);
                if (result == OK && bitmap != null){
                    listener.onSuccess(bitmap);
                }else{
                    listener.onError("图片合成失败");
                }
            }
            break;
            case ERR_NEED_MORE_IMGS: {
                listener.onError("需要更多图片");
                return;
            }
            case ERR_HOMOGRAPHY_EST_FAIL: {
                listener.onError("图片对应不上");
                return;
            }
            case ERR_CAMERA_PARAMS_ADJUST_FAIL: {
                listener.onError("图片参数处理失败");
                return;
            }
        }
    }


    /**
     * 拼接监听回调的接口
     */
    public interface onStitchResultListener {

        void onSuccess(Bitmap bitmap);

        void onError(String errorMsg);
    }

    /**
     * 调用底层的JNI方法（示例）
     * @return
     */
    // public native String stringFromJNI();
    private native static int[] stitchImages(String path[]);

    private native static void getMat(long mat);

    private native static int getBitmap(Bitmap bitmap);
}

