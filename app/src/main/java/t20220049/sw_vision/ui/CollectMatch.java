package t20220049.sw_vision.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import t20220049.sw_vision.R;

public class CollectMatch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_match);
        TextView deviceName = findViewById(R.id.deviceNameText);
        TextView nickName = findViewById(R.id.nickNameText);
        deviceName.setText("Oneplus 9 RT");
        nickName.setText("本机" + "(Lily)");
        ImageView clientIcon = findViewById(R.id.clienticon);
        clientIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog connecting = new AlertDialog.Builder(CollectMatch.this).setMessage("正在连接...").create();
                new AlertDialog.Builder(CollectMatch.this)
                        .setTitle("确认连接")
                        .setMessage("Oneplus 9 RT 希望连接")
                        .setNegativeButton("否", null)
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                connecting.show();
//                              加载完毕
                                connecting.cancel();
                                Intent intent = new Intent(CollectMatch.this, ControlVideo.class);
                                startActivity(intent);
                            }
                        }).show();
            }
        });
    }
}