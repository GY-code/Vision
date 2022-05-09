package t20220049.sw_vision.ui_utils;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import t20220049.sw_vision.R;
import t20220049.sw_vision.ui.ControlActivity;

import org.webrtc.ContextUtils;

public class MyNotification {
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    /**
     * 发送通知
     */
    public void sendNotification(Context context, int ID, String title, String text){

        final String channelID = "message";
        Intent intent = new Intent(context, ControlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //获取系统通知服务
        mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Android 8.0开始要设置通知渠道
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(channelID,
                    channelID,NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }

        //创建通知
        mBuilder = new NotificationCompat.Builder(context,channelID)
                .setContentTitle(title)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
                .setSmallIcon(R.drawable.vision)
                .setProgress(100, 0, false)
                .setAutoCancel(false);

        //发送通知( id唯一,可用于更新通知时对应旧通知; 通过mBuilder.build()拿到notification对象 )
        mNotificationManager.notify(ID, mBuilder.build());
    }

    /**
     * 更新通知
     */
    public void updateNotification(int ID, int progress){
        if(progress == 100){
            mBuilder.setContentTitle("已完成(" + progress + "%)");
            mBuilder.setProgress(100, progress, false);
            mNotificationManager.notify(ID,mBuilder.build());
        } else{
            mBuilder.setContentTitle("进行中(" + progress + "%)");
            mBuilder.setProgress(100, progress, false);
            mNotificationManager.notify(ID,mBuilder.build());
        }
    }
}
