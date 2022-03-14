package t20220049.sw_vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;


public class Connection extends View {
    public float currentX = 70;
    public float currentY = 70;
    Bitmap clientBitmap;
    Bitmap serverBitmap;
    int clientWidth, clientHeight;
    int serverWidth, serverHeight;
    int deviceWidth, deviceHeight;
    Paint p = new Paint();
    private static final String TAG = "Connection";

    private void initialize() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        clientBitmap = getBitmap(this.getContext(), R.drawable.ic_client);
        clientWidth = clientBitmap.getWidth();
        clientHeight = clientBitmap.getHeight();
        serverBitmap = getBitmap(this.getContext(), R.drawable.ic_server);
        serverWidth = serverBitmap.getWidth();
        serverHeight = serverBitmap.getHeight();
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        deviceWidth = wm.getDefaultDisplay().getWidth();
        deviceHeight = wm.getDefaultDisplay().getHeight();

    }

    private static Bitmap getBitmap(Context context, int vectorDrawableId) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = context.getDrawable(vectorDrawableId);
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        } else {
            bitmap = BitmapFactory.decodeResource(context.getResources(), vectorDrawableId);
        }
        return bitmap;
    }

    public Connection(Context context) {
        super(context);
        initialize();
    }

    public Connection(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public Connection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制中心服务设备
        canvas.drawBitmap(serverBitmap, (deviceWidth - serverWidth) / 2, (deviceHeight - serverHeight) / 2, null);
        // 绘制当前触发设备
        canvas.drawBitmap(clientBitmap, currentX - clientWidth / 2, currentY - clientHeight / 2, null);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //修改当前的坐标
        this.currentX = event.getX();
        this.currentY = event.getY();
        //重绘
        this.invalidate();
        return true;
    }
}