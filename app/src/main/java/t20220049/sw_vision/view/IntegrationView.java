package t20220049.sw_vision.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

//将这些view合起来
public class IntegrationView extends FrameLayout {
    private PointsView mPointsView;
    private PhotoView mPhotoView;
    private RippleRoundView mRippleRoundView;

    private Context mContext;

    public IntegrationView(Context context) {
        this(context, null);
    }

    public IntegrationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IntegrationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
    }

    private void initView() {
        mPointsView = new PointsView(mContext);
        addView(mPointsView);
        mPointsView.startRippleAnimation();

        mRippleRoundView = new RippleRoundView(mContext);
        mRippleRoundView.setAlpha((float) 0.5);
        addView(mRippleRoundView);
        mRippleRoundView.startRippleAnimation();


        List<String> godHeadPhoto = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            godHeadPhoto.add("" + i);
        }
        mPhotoView = new PhotoView(mContext);
        mPhotoView.setAlpha((float) 0.9);
        addView(mPhotoView);
        mPhotoView.setUserPhoto(godHeadPhoto);
    }

}
