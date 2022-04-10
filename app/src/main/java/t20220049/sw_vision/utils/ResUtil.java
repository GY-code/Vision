package t20220049.sw_vision.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import t20220049.sw_vision.app.AppMaster;

public class ResUtil {

    public static String getString(@StringRes int resId) {
        return AppMaster.getInstance().getAppContext().getString(resId);
    }

    public static String getString(Context context, @StringRes int resId) {
        return context.getApplicationContext().getString(resId);
    }

    public static String getString(Context context, @StringRes int resId, Object... obj) {
        return context.getApplicationContext().getString(resId, obj);
    }

    public static String getString(@StringRes int resId, Object... obj) {
        return AppMaster.getInstance().getAppContext().getString(resId, obj);
    }


    public static int getColor(Context context, @ColorRes int resId) {
        return ContextCompat.getColor(context.getApplicationContext(), resId);
    }

    public static int getColor(@ColorRes int resId) {
        AppMaster a = AppMaster.getInstance();
        Context c = AppMaster.getInstance().getAppContext();
        if(c!=null)
            return ContextCompat.getColor(c, resId);
        else
            return -1;
    }


    public static Drawable getDrawable(Context context, @DrawableRes int resId) {
        return ContextCompat.getDrawable(context.getApplicationContext(), resId);
    }

    public static Drawable getDrawable(@DrawableRes int resId) {
        Context c = AppMaster.getInstance().getAppContext();
        if(c!=null)
            return ContextCompat.getDrawable(AppMaster.getInstance().getAppContext(), resId);
        else
            return null;
    }

    public static int getDimens(Context context, @DimenRes int resId) {
        return context.getApplicationContext().getResources().getDimensionPixelSize(resId);
    }

}
