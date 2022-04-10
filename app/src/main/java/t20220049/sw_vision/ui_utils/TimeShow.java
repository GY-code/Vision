package t20220049.sw_vision.ui_utils;

import android.os.Bundle;
import android.widget.Chronometer;

import androidx.appcompat.app.AppCompatActivity;

import t20220049.sw_vision.R;

public class TimeShow extends AppCompatActivity {
    private Chronometer mChronometer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);
        initView();
    }

    private void initView() {
//        mChronometer = (Chronometer) findViewById(R.id.record_chronometer);
//        //setFormat设置用于显示的格式化字符串。
//        //替换字符串中第一个“%s”为当前"MM:SS"或 "H:MM:SS"格式的时间显示。
//        mChronometer.setFormat("%s");
//        mChronometer.start();
    }

}
