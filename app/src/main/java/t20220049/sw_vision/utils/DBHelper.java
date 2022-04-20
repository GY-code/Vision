package t20220049.sw_vision.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String[] sql = new String[4];
        sql[0] = "create table device_table\n" +
                "(\n" +
                "    userid varchar(50)\n" +
                "        constraint device_table_pk\n" +
                "            primary key,\n" +
                "    name   varchar(50),\n" +
                "    ip     varchar(50),\n" +
                "    stat   smallint\n" +
                ");";
        sql[1] = "create table control_table\n" +
                "(\n" +
                "    userid  varchar(50)\n" +
                "        references device_table,\n" +
                "    operate varchar(50),\n" +
                "    time    datetime,\n" +
                "    stat    smallint\n" +
                ");\n";
        sql[2] = "create table location_table\n" +
                "(\n" +
                "    userid   varchar(50)\n" +
                "        constraint location_table_pk\n" +
                "            primary key\n" +
                "        references device_table,\n" +
                "    location varchar(100),\n" +
                "    time     datetime,\n" +
                "    stat     smallint\n" +
                ");";
        sql[3] = "create table exception_table\n" +
                "(\n" +
                "    excepid  integer\n" +
                "        constraint exception_table_pk\n" +
                "            primary key autoincrement,\n" +
                "    location varchar(50),\n" +
                "    type     varchar(50),\n" +
                "    msg      varchar(200)\n" +
                ");";
        //执行sql语句
        for (String s : sql) {
            sqLiteDatabase.execSQL(s);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
