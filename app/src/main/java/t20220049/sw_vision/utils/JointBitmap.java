package t20220049.sw_vision.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JointBitmap extends AppCompatActivity {
    private JointBitmapView view;
    int picNum = 0;
    private Bitmap[] bitmaps;
    Bitmap result;

    public void receiveFile(String[] dir, String[] name){
        picNum = dir.length;
        bitmaps = new Bitmap[picNum];
        for(int i = 0; i < dir.length; i++){
            File appDir = new File(dir[i]);
            if (!appDir.exists()) appDir.mkdir();
            File file = new File(appDir, name[i]);
            try {
                FileInputStream fis = new FileInputStream(file);
                bitmaps[i] = BitmapFactory.decodeStream(fis);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void jointPhoto(){
        JointBitmapView view1 = new JointBitmapView(this);
        Bitmap temp = bitmaps[0];
        Bitmap temp2 = bitmaps[picNum-1];

        if(picNum == 9){
            Bitmap temp3 = bitmaps[picNum-6];
            for(int i = 0; i < 2; i++){
                temp = view1.newBitmapHorizontal(temp, bitmaps[i+1]);
                temp2 = view1.newBitmapHorizontal(bitmaps[picNum-i-2], temp2);
                temp3 = view1.newBitmapHorizontal(temp3, bitmaps[i+4]);
            }
            Bitmap temp4 = view1.newBitmapVertical(temp, temp2);
            view = new JointBitmapView(this, temp4, temp3,1);
            result = view1.newBitmapVertical(temp4, temp3);
        } else if(picNum == 3) {
            temp = view1.newBitmapHorizontal(bitmaps[0], bitmaps[1]);
            view = new JointBitmapView(this, temp, bitmaps[2],0);
            result = view1.newBitmapHorizontal(temp, bitmaps[2]);
        } else if(picNum == 2){
            view = new JointBitmapView(this, bitmaps[0], bitmaps[1],0);
            result = view1.newBitmapHorizontal(bitmaps[0], bitmaps[1]);
        } else if(picNum == 1){
            Log.e("error", "error");
        } else {
            for(int i = 0; i < ((picNum - (picNum / 2)) - 1); i++){
                temp = view1.newBitmapHorizontal(temp, bitmaps[i+1]);
            }

            for(int j = 0; j < ((picNum / 2) - 1); j++){
                temp2 = view1.newBitmapHorizontal(bitmaps[picNum-j-2], temp2);
            }
            view = new JointBitmapView(this, temp, temp2,1);
            result = view1.newBitmapVertical(temp, temp2);
        }
        savePhoto(result);
    }

    private void savePhoto(Bitmap bitmap) {
        long curTime = new Date().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "record_photo "+sdf.format(curTime));
        String fileName = "photo-" + sdf.format(curTime) + ".png";
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, fileName, fileName);
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "已保存图片到相册", Toast.LENGTH_SHORT).show();
        });

        File appDir = new File(getApplicationContext().getFilesDir() + "");
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
}
