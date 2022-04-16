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
import java.io.FileInputStream;
import java.io.IOException;

public class Pano extends AppCompatActivity {

    /**
     * “选择图片”时的标识位
     */
    private static final int CHOOSE_PHOTO = 1;

    /**
     * 显示图片位置的标识位
     */
    private int DISPLAY_IMAGE = 1;

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

    /**
     * “选择图片1”的按钮实例
     */
    private Button mBtnSelect;

    /**
     * “选择图片2”的按钮实例
     */
    private Button mBtnSelect2;

    /**
     * “拼接图像”的按钮实例
     */
    private Button mMerge;

    /**
     * 显示图像1的实例
     */
    private ImageView mImageView;

    /**
     * 显示图像2的实例
     */
    private ImageView mImageView2;

    /**
     * 图像1的实例
     */
    private Bitmap mBitmap;

    /**
     * 图像2的实例
     */
    private Bitmap mBitmap2;

    /**
     * 存储待拼接的图像集合
     */
    private String[] mImagePath = new String[]{"abc","abc"};

    /**
     * 存储待拼接的图像集合的索引
     */
    private static int i = 0;

    // 引用native方法
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 初始化布局
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panorama);

        // 初始化控件实例
        mBtnSelect = findViewById(R.id.btn_select);
        mBtnSelect2 = findViewById(R.id.btn_select2);
        mMerge = findViewById(R.id.btn_merge);
        mImageView = findViewById(R.id.imageView);
        mImageView2 = findViewById(R.id.imageView2);

        // “选择图片1”的点击方法
        mBtnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(Pano.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(Pano.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else{
                    DISPLAY_IMAGE = 1;
                    openAlbum();
                }
            }
        });

        // “选择图片2”的点击方法
        mBtnSelect2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(Pano.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(Pano.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else{
                    DISPLAY_IMAGE = 2;
                    openAlbum();
                }
            }
        });

        // “拼接图像”的点击事件
        mMerge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Pano.this,mImagePath[0],Toast.LENGTH_LONG).show();
                mergeBitmap(mImagePath,new onStitchResultListener(){

                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        Toast.makeText(Pano.this,"图片拼接成功！",Toast.LENGTH_LONG).show();
                        replaceImage(bitmap);
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Toast.makeText(Pano.this,"图片拼接失败！",Toast.LENGTH_LONG).show();
                        System.out.println(errorMsg);
                    }
                });
            }
        });

        // 调用Native程序的示例
        /*
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
         */
    }

    /**
     * 动态申请权限的处理方法
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "拒绝授权将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 打开相册
     */
    private void openAlbum(){
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_PHOTO);
    }

    /* OpenCV的测试方法
    private Bitmap hivePic() {
        Log.e(TAG, "hivePic: HHHA:0====>");
        Mat des = new Mat();
        Mat src = new Mat();
        Bitmap srcBit = BitmapFactory.decodeResource(getResources(), R.drawable.test2);
        Utils.bitmapToMat(srcBit, src);
        Bitmap grayBit = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(src, des, Imgproc.COLOR_BGR2GRAY);
        Utils.matToBitmap(des, grayBit);
        return grayBit;
    }
     */

    /**
     * Activity的回调处理
     * @param requestCode 请求参数
     * @param resultCode 结果参数
     * @param data 数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode  == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data); /* 4.4及以上系统使用这个方法处理图片 */
                    } else {
                        handleImageBeforeKitKat(data); /* 4.4及以下系统使用这个方法处理图片 */
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 4.4及以上系统处理图片的方法
     * @param data 数据
     */
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data){
        String imagePath = null;
        Uri uri = data.getData();
        if(DocumentsContract.isDocumentUri(this,uri)){
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if ("com.android.provideres.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);
            }
        }else if ("content".equalsIgnoreCase(uri.getScheme())){
            imagePath = getImagePath(uri,null);
        }else if ("file".equalsIgnoreCase(uri.getScheme())){
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    /**
     * 4.4以下系统处理图片的方法
     * @param data 数据
     */
    private void handleImageBeforeKitKat(Intent data){
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        displayImage(imagePath);
    }

    /**
     * 获取图片路径
     * @param uri 图片Url
     * @param selection 默认为空值
     * @return
     */
    private String getImagePath(Uri uri,String selection){
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if(cursor != null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        mImagePath[i++] = path;
        return path;
    }

    /**
     * 图片显示的方法
     * @param imagePath 图片路径
     */
    private void displayImage(String imagePath){
        if(imagePath != null){
            Log.i("1", "1");
//            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

            File file = new File(imagePath);
            Bitmap bitmap = null;
            try {
                FileInputStream fis = new FileInputStream(file);
                bitmap = BitmapFactory.decodeStream(fis);
            } catch (IOException e){
                e.printStackTrace();
            }
            
            Log.i("2", "2");
            if (DISPLAY_IMAGE == 1){
                mImageView.setImageBitmap(bitmap);
                mBitmap = bitmap;
            }
            else {
                mImageView2.setImageBitmap(bitmap);
                mBitmap2 = bitmap;
            }
        }else{
            Toast.makeText(this,"获取图片失败",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 拼接图片的方法
     * @param paths 图像URL的集合
     * @param listener 监听器回调
     * @return
     */
    private void mergeBitmap(String paths[], @NonNull onStitchResultListener listener) {
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
     * 替换图片的方法
     * @param bitmap 拼接后的图像
     */
    private void replaceImage(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
        mImageView2.setVisibility(View.GONE);
        mBtnSelect.setVisibility(View.GONE);
        mBtnSelect2.setVisibility(View.GONE);
        mMerge.setVisibility(View.GONE);
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

