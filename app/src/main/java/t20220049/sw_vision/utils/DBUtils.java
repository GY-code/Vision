package t20220049.sw_vision.utils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.webrtc.ContextUtils;

import java.util.Date;

public class DBUtils {

    static DBHelper dbHelper = new DBHelper(ContextUtils.getApplicationContext(), "info.db", null, 1);
    static SQLiteDatabase db = dbHelper.getWritableDatabase();

    public static void insertControl(String userid, String operate, Date time, int stat) {
        // 创建ContentValue设置参数
        ContentValues contentValues = new ContentValues();
        contentValues.put("userid", userid);
        contentValues.put("operate", operate);
        contentValues.put("time", time.getTime());
        contentValues.put("stat", stat);
        insert("control_table", contentValues);
    }

    public static void insertLocation(String userid, String location, Date time, int stat) {
        // 创建ContentValue设置参数
        ContentValues contentValues = new ContentValues();
        contentValues.put("userid", userid);
        contentValues.put("location", location);
        contentValues.put("time", time.getTime());
        contentValues.put("stat", stat);
        insert("location_table", contentValues);
    }

    public static void insertException(int excepid, String location, String type, String msg) {
        // 创建ContentValue设置参数
        ContentValues contentValues = new ContentValues();
        contentValues.put("excepid", excepid);
        contentValues.put("location", location);
        contentValues.put("type", type);
        contentValues.put("msg", msg);
        insert("exception_table", contentValues);
    }

    public static void insertDevice(String userid, String name, String ip, int stat) {
        // 创建ContentValue设置参数
        ContentValues contentValues = new ContentValues();
        contentValues.put("userid", userid);
        contentValues.put("name", name);
        contentValues.put("ip", ip);
        contentValues.put("stat", stat);
        insert("device_table", contentValues);
    }

    public static void insert(String table, ContentValues contentValues) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 插入数据
        // insert方法参数1：要插入的表名
        // insert方法参数2：如果发现将要插入的行为空时，会将这个列名的值设为null
        // insert方法参数3：contentValue
        long i = db.insert(table, null, contentValues);
        // 释放连接
        db.close();
    }

}
