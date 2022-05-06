package t20220049.sw_vision.arm_controller.uitls;

import android.content.Context;
import android.widget.Toast;

/**
 * Log工具类
 * Created by lucas on 2022/4/11.
 */
public class LogUtil {

    private static boolean isDebug = true;

    public static void showToast(Context context, String msg) {
        if (isDebug)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static int i(String tag, String msg) {
        if (isDebug)
            return android.util.Log.i(tag, msg);
        return 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        if (isDebug)
            return android.util.Log.i(tag, msg, tr);
        return 0;
    }

    public static int d(String tag, String msg) {
        if (isDebug)
            android.util.Log.d(tag, msg);
        return 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        if (isDebug)
            return android.util.Log.d(tag, msg, tr);
        return 0;
    }

    public static int w(String tag, String msg) {
        if (isDebug)
            android.util.Log.w(tag, msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        if (isDebug)
            return android.util.Log.w(tag, msg, tr);
        return 0;
    }

    public static int e(String tag, String msg) {
        if (isDebug)
            android.util.Log.e(tag, msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (isDebug)
            return android.util.Log.e(tag, msg, tr);
        return 0;
    }

    public static int v(String tag, String msg) {
        if (isDebug)
            android.util.Log.v(tag, msg);
        return 0;
    }

    public static int v(String tag, String msg, Throwable tr) {
        if (isDebug)
            return android.util.Log.v(tag, msg, tr);
        return 0;
    }
}
