package t20220049.sw_vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.util.HashMap;


public class Connection extends View {

    Bitmap clientBitmap;
    Bitmap serverBitmap;
    int clientWidth, clientHeight;
    int serverWidth, serverHeight;
    int deviceWidth, deviceHeight;
    HashMap<Integer, Device> deviceMap = new HashMap<>();
    int nextId = 1;
    int activateDevice = 0;
    private static final String TAG = "Connection";


    private void initDevice() {
        deviceMap.put(1, new Device(500, 100));
        deviceMap.put(2, new Device(200, 100));
        deviceMap.put(3, new Device(800, 100));
        deviceMap.put(4, new Device(400, 200));

    }

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
        initDevice();
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float currentX = (int) motionEvent.getX();
                float currentY = (int) motionEvent.getY();
                if (activateDevice == 0) {
                    for (int key : deviceMap.keySet()) {
                        Device device = deviceMap.get(key);
                        float dx = device.currentX;
                        float dy = device.currentY;
                        float detax = currentX - dx;
                        float detay = currentY - dy;
                        Log.d(TAG, "deta" + detax + "," + detay);
                        if (detax < clientWidth && detay < clientHeight && detax > 0 && detay > 0) {
                            activateDevice = key;
                            break;
                        }
                    }
                } else {
                    Device device = deviceMap.get(activateDevice);
                    if (device != null) {
                        device.currentX = currentX - (float) clientWidth / 2;
                        device.currentY = currentY - (float) clientHeight / 2;
                        invalidate();
                    }
                }
//                Log.d(TAG, "activate:" + activateDevice);
                return false;
            }
        });
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick ");
                activateDevice = 0;
            }
        });

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
        canvas.drawBitmap(serverBitmap, (float) (deviceWidth - serverWidth) / 2, (float) (deviceHeight - serverHeight) / 2, null);
        // 绘制客户设备
        for (int key : deviceMap.keySet()) {
            Device device = deviceMap.get(key);
            canvas.drawBitmap(clientBitmap, device.currentX, device.currentY, null);
        }

    }


}