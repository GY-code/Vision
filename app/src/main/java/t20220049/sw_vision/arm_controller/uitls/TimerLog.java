package t20220049.sw_vision.arm_controller.uitls;

import android.util.Log;

/**
 * 计时的日志工具
 * Created by lucas on 2022/4/19.
 */
public class TimerLog {

    private static long currentTime = System.currentTimeMillis();

    public static void logTime(String tag){
        long millis = System.currentTimeMillis();
        Log.i("TimerLog", tag +  " cost " + (millis - currentTime) + " ms");
        currentTime = millis;
    }
}
