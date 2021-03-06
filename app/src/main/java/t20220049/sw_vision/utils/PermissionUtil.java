package t20220049.sw_vision.utils;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuYi on 2022/1/2.
 * android_shuai@163.com
 */
public class PermissionUtil {

    private static final String TAG = "PermissionUtil";
    // 檢查是否有權限
    public static boolean isNeedRequestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        return isNeedRequestPermission(activity,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.FOREGROUND_SERVICE);
        // Manifest.permission.SYSTEM_ALERT_WINDOW
    }

    private static boolean isNeedRequestPermission(Activity activity, String... permissions) {
        List<String> mPermissionListDenied = new ArrayList<>();
        for (String permission : permissions) {
            int result = checkPermission(activity, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                mPermissionListDenied.add(permission);
            }
        }
                if (mPermissionListDenied.size() > 0) {
            String[] pears = new String[mPermissionListDenied.size()];
            pears = mPermissionListDenied.toArray(pears);
            ActivityCompat.requestPermissions(activity, pears, 0);
            return true;
        } else {
            return false;
        }
    }

    private static int checkPermission(Activity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission);
    }

    public static boolean isNeedOverLay(Activity activity){
        if (!Settings.canDrawOverlays(activity.getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:t20220049.sw_vision"));
            startActivityForResult(activity, intent, 0, null);
            return true;
        }else
            return false;
    }


}
