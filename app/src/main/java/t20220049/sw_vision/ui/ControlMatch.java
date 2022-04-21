package t20220049.sw_vision.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import t20220049.sw_vision.R;

public class ControlMatch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_match);
//        开始按钮初始化
        ImageView playButton=findViewById(R.id.refreshButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ControlMatch.this, ControlVideo.class);
                startActivity(intent);
            }
        });

    }
}