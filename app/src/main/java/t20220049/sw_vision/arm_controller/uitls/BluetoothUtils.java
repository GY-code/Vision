package t20220049.sw_vision.arm_controller.uitls;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 蓝牙相关工具类
 * Created by lucas on 2022/4/24.
 */
public class BluetoothUtils {

    private static final String TAG = BluetoothUtils.class.getSimpleName();

    public static boolean isSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            LogUtil.w(TAG, "device do not support bluetooth!");
            return false;
        }
        return true;
    }

    public static boolean removePaired(BluetoothDevice device) {
        try {
            Method removeMethod = device.getClass().getMethod("removeBond");
            return (Boolean) removeMethod.invoke(device);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setPin(BluetoothDevice device, String pin) {
        try {
            Method setPin = device.getClass().getMethod("setPin", new Class[]{byte[].class});
            return (Boolean) setPin.invoke(device, new Object[]{pin.getBytes()});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createBound(BluetoothDevice device){
        try {
            Method createBond = device.getClass().getMethod("createBond");
            return (Boolean) createBond.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean cancelPairingUserInput (BluetoothDevice device){
        try {
            Method cancelPairingUserInput = device.getClass().getMethod("cancelPairingUserInput");
            return (Boolean) cancelPairingUserInput.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
