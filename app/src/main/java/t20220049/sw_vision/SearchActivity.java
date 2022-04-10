package t20220049.sw_vision;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import t20220049.sw_vision.app.AnimationListener;
import t20220049.sw_vision.view.CircleImageView;
import t20220049.sw_vision.view.RippleView;

public class SearchActivity extends AppCompatActivity {

    private RippleView mRippleView;
    private CircleImageView mAccountPhoto;
    private Animation animAccountPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initView();
    }

    private void initView() {
        ImageView playButton=findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SearchActivity.this, ControlVideo.class);
                startActivity(intent);
            }
        });
        mRippleView = (RippleView) findViewById(R.id.RippleView);
        mAccountPhoto = (CircleImageView) findViewById(R.id.mAccountPhoto);
        animAccountPhoto = AnimationUtils.loadAnimation(this, R.anim.anim_the_heartbeatsmall);
        mAccountPhoto.startAnimation(animAccountPhoto);

        mRippleView.startRippleAnimation();
        mRippleView.setAnimationProgressListener(new AnimationListener() {
            @Override
            public void startAnimation() {
                //开始动画了
            }

            @Override
            public void endAnimation() {
                //结束动画了
            }
        });

        List<String> godHeadPhoto = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            godHeadPhoto.add("" + i);
        }
        mRippleView.setUserPhoto(godHeadPhoto);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animAccountPhoto != null) {
            animAccountPhoto.start();
        }
        mRippleView.onHiddenChanged(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (animAccountPhoto != null) {
            animAccountPhoto.reset();
            animAccountPhoto = null;
        }
        mRippleView.onHiddenChanged(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRippleView != null) {
            mRippleView.destroy();
            mRippleView = null;
        }
    }
}
