package t20220049.sw_vision.arm_controller.uitls;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;

/**
 * Created by lucas on 2022/4/12.
 */
public class ColorUtil {

    /**
     * get application's accent color
     *
     * @return accent color
     */
    public static int getAccentColor(Context context) {
        int accentColor = 0;
        TypedValue accentColorTypedValue = new TypedValue();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.getTheme().resolveAttribute(android.R.attr.colorAccent, accentColorTypedValue, true);
                accentColor = accentColorTypedValue.data;
            } else {
                throw new RuntimeException("SDK_INT less than LOLLIPOP");
            }
        } catch (Exception e) {
            try {
                int colorPrimaryId = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
                if (colorPrimaryId != 0) {
                    context.getTheme().resolveAttribute(colorPrimaryId, accentColorTypedValue, true);
                    accentColor = accentColorTypedValue.data;
                } else {
                    throw new RuntimeException("colorAccent not found");
                }
            } catch (Exception e1) {
                accentColor = Color.BLUE;
            }
        }
        return accentColor;
    }

    /**
     * get application's primary color
     *
     * @param context
     * @return
     */
    public static int getPrimaryColor(Context context) {
        int primaryColor = 0;
        TypedValue primaryColorTypedValue = new TypedValue();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.getTheme().resolveAttribute(android.R.attr.colorPrimary, primaryColorTypedValue, true);
                primaryColor = primaryColorTypedValue.data;
            } else {
                throw new RuntimeException("SDK_INT less than LOLLIPOP");
            }
        } catch (Exception e) {
            try {
                int colorPrimaryId = context.getResources().getIdentifier("colorPrimary", "attr", context.getPackageName());
                if (colorPrimaryId != 0) {
                    context.getTheme().resolveAttribute(colorPrimaryId, primaryColorTypedValue, true);
                    primaryColor = primaryColorTypedValue.data;
                } else {
                    throw new RuntimeException("colorPrimary not found");
                }
            } catch (Exception e1) {
                primaryColor = Color.BLUE;
            }
        }
        return primaryColor;
    }


    /**
     * get application's primary dark color
     *
     * @param context
     * @return
     */
    public static int getPrimaryDarkColor(Context context) {
        int primaryDarkColor = 0;
        TypedValue primaryColorDarkTypedValue = new TypedValue();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.getTheme().resolveAttribute(android.R.attr.colorPrimaryDark, primaryColorDarkTypedValue, true);
                primaryDarkColor = primaryColorDarkTypedValue.data;
            } else {
                throw new RuntimeException("SDK_INT less than LOLLIPOP");
            }
        } catch (Exception e) {
            try {
                int colorPrimaryId = context.getResources().getIdentifier("colorPrimaryDark", "attr", context.getPackageName());
                if (colorPrimaryId != 0) {
                    context.getTheme().resolveAttribute(colorPrimaryId, primaryColorDarkTypedValue, true);
                    primaryDarkColor = primaryColorDarkTypedValue.data;
                } else {
                    throw new RuntimeException("colorPrimaryDark not found");
                }
            } catch (Exception e1) {
                primaryDarkColor = Color.BLUE;
            }
        }
        return primaryDarkColor;
    }

    /**
     * judge the color's light and shade
     * @param color the target color
     * @return true if is light color,else false if dark color
     */
    public static boolean isLight(int color) {
        return Math.sqrt(
                Color.red(color) * Color.red(color) * .241 +
                        Color.green(color) * Color.green(color) * .691 +
                        Color.blue(color) * Color.blue(color) * .068) > 130;
    }

    public static int getBaseColor(int color) {
        if (isLight(color)) {
            return Color.BLACK;
        }
        return Color.WHITE;
    }
}
