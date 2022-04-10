package t20220049.sw_vision.entrance;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import t20220049.sw_vision.R;


public class NodejsActivity extends AppCompatActivity {
    private EditText et_signal;
    private EditText et_room;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nodejs);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        initView();
        initVar();

    }

    private void initView() {
        et_signal = findViewById(R.id.et_signal);
        et_room = findViewById(R.id.et_room);
    }

    private void initVar() {
        et_signal.setText("ws://106.13.236.207:3000");
        et_room.setText("123456");
    }

    /*-------------------------- nodejs版本服务器测试--------------------------------------------*/
    public void JoinRoomSingleVideo(View view) {
//        WebrtcUtil.callSingle(this,
//                et_signal.getText().toString(),
//                et_room.getText().toString().trim(),
//                true);
        WebrtcUtil.callSingle(this, "ws://106.13.236.207:3000",
                "123456", true);

    }

    public void JoinRoom(View view) {
//        WebrtcUtil.call(this, et_signal.getText().toString(), et_room.getText().toString().trim());
        WebrtcUtil.call(this, "ws://106.13.236.207:3000", "123456");
    }


}
