package t20220049.sw_vision.ui_utils;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import t20220049.sw_vision.R;
import org.webrtc.ContextUtils;

public class MyNotification extends AppCompatActivity {
    private Context mContext = MyNotification.this;
    private NotificationManager mNManager;
    private Notification notify1;
    private static final int NOTIFYID_1 = 1;
    Notification.Builder mBuilder;

    public void buildNotification(String title, String text, String ticker){
        mNManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new Notification.Builder(mContext);
        mBuilder.setContentTitle(title)                        //标题
                .setContentText(text)      //内容
                .setTicker(ticker)             //收到信息后状态栏显示的文字信息
                .setSmallIcon(R.drawable.ic_end_call)
                .setWhen(System.currentTimeMillis())           //设置通知时间
                .setAutoCancel(true);                          //设置点击后取消Notification

        notify1 = mBuilder.build();
        mNManager.notify(NOTIFYID_1, notify1);
    }

    public void setProgress2(int progress){
        if(progress == 100){
            mNManager.cancel(NOTIFYID_1);
        }else{
            mBuilder.setProgress(100, progress, false);
        }

    }
}
