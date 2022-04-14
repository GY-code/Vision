package t20220049.sw_vision.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

/**
 * 将多张图片拼接在一起
 * @author yw-tony
 *
 */
public class JointBitmapView extends View{
    private Bitmap bitmap;

    public JointBitmapView(Context context){
        super(context);
    }
    public JointBitmapView(Context context, Bitmap bit1, Bitmap bit2, int mode) {
        super(context);
        if(mode == 0){
            bitmap = newBitmapHorizontal(bit1,bit2);
        } else{
            bitmap = newBitmapVertical(bit1,bit2);
        }

    }
    /**
     * 拼接图片
     * @param bit1
     * @param bit2
     * @return 返回拼接后的Bitmap
     */
    Bitmap newBitmapVertical(Bitmap bit1,Bitmap bit2){
        int width = bit1.getWidth();
        int height = bit1.getHeight() + bit2.getHeight();
        float drawWidth = (float) ((bit1.getWidth() - bit2.getWidth()) / 2.0);
        //创建一个空的Bitmap(内存区域),宽度等于第一张图片的宽度，高度等于两张图片高度总和
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //将bitmap放置到绘制区域,并将要拼接的图片绘制到指定内存区域
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bit1, 0, 0, null);
        canvas.drawBitmap(bit2, drawWidth, bit1.getHeight(), null);
        return bitmap;
    }

    Bitmap newBitmapHorizontal(Bitmap bit1,Bitmap bit2){
        int width = bit1.getWidth() + bit2.getWidth();
        int height = bit1.getHeight();
        //创建一个空的Bitmap(内存区域),高度等于第一张图片的高度，宽度等于两张图片宽度总和
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //将bitmap放置到绘制区域,并将要拼接的图片绘制到指定内存区域
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bit1, 0, 0, null);
        canvas.drawBitmap(bit2, bit1.getWidth(), 0, null);
        return bitmap;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, null);
        bitmap.recycle();
    }

}
