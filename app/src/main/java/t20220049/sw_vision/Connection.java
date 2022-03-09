package t20220049.sw_vision;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Wangrx on 2017/11/2.
 */

public class Connection extends View {
    public float currentX = 70;
    public float currentY = 70;
    //定义。创建画笔
    Paint p = new Paint();

    public Connection(Context context) {
        super(context);
    }

    public Connection(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Connection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
// TODO Auto-generated method stub
        super.onDraw(canvas);
//设置画笔的颜色
        p.setColor(Color.RED);
//绘制一个小球
//参数分别是：圆心坐标，半径 ，所使用的画笔
        canvas.drawCircle(currentX, currentY, 30, p);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
// TODO Auto-generated method stub
    //修改当前的坐标
        this.currentX = event.getX();
        this.currentY = event.getY();
    //重绘小球
        this.invalidate();

        return true;
    }
}