package t20220049.sw_vision;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import t20220049.sw_vision.nodejs.NodejsActivity;

public class HomePage extends AppCompatActivity {
    ImageView controlButton;
    ImageView collectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        controlButton = findViewById(R.id.controlButton);
        collectButton = findViewById(R.id.collectButton);

        controlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, ControlMatch.class);
                startActivity(intent);

            }
        });

        collectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(HomePage.this, CollectMatch.class);
                Intent intent = new Intent(HomePage.this, NodejsActivity.class);
                startActivity(intent);
            }
        });
    }
}
