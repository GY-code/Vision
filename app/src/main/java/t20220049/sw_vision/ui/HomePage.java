package t20220049.sw_vision.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import t20220049.sw_vision.R;
import t20220049.sw_vision.entrance.WebrtcUtil;
import t20220049.sw_vision.transfer.SearchActivity;

public class HomePage extends AppCompatActivity {
    ImageView controlButton;
    ImageView collectButton;

    private static final int CODE_REQ_PERMISSIONS = 665;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQ_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showToast("缺少权限，请先授予权限: " + permissions[i]);
                    return;
                }
            }
            showToast("已获得权限");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        controlButton = findViewById(R.id.controlButton);
        collectButton = findViewById(R.id.collectButton);

        ActivityCompat.requestPermissions(HomePage.this,
                new String[]{Manifest.permission.CHANGE_NETWORK_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION}, CODE_REQ_PERMISSIONS);

        controlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, ReceiveFileActivity.class);
                startActivity(intent);
//                WebrtcUtil.call(HomePage.this, "ws://106.13.236.207:3000", "123456");
            }
        });

        collectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, SendFileActivity.class);
                startActivity(intent);
//                Intent intent = new Intent(HomePage.this, CollectMatch.class);
//                Intent intent = new Intent(HomePage.this, NodejsActivity.class);
//                startActivity(intent);
//                WebrtcUtil.callSingle(HomePage.this, "ws://106.13.236.207:3000",
//                        "123456", true);
            }
        });
    }
}
