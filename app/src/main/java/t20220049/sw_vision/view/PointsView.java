package t20220049.sw_vision.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import t20220049.sw_vision.R;
import t20220049.sw_vision.transfer.app.AnimationListener;
import t20220049.sw_vision.transfer.utils.ResUtil;

//背景中若干个动态圆圈
public class PointsView extends FrameLayout {
    private Context mContext;
    private Animation[] animPoints;
    /**
     * 自定义的动画开始与结束接口
     */
    private AnimationListener mAnimationProgressListener;
    private int[] mList = new int[]{R.mipmap.icon_secect_points_1, R.mipmap.icon_secect_2, R.mipmap.icon_secect_3, R.mipmap.icon_secect_4, R.mipmap.icon_secect_5};

    public PointsView(@NonNull Context context) {
        this(context,null);
    }

    public PointsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public PointsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init(){
        int size = mList.length;
        animPoints = new Animation[size];
        /**
         * 五个小点 动画显示
         * */
        for (int i = 0; i < size; i++) {
            final int j = i;
            LayoutParams mSmallCircleParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            if (j == 0) {
                mSmallCircleParams.rightMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_140);
                mSmallCircleParams.gravity = Gravity.RIGHT;
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_380);
            } else if (j == 1) {
                mSmallCircleParams.rightMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_75);
                mSmallCircleParams.gravity = Gravity.RIGHT;
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_89);
            } else if (j == 2) {
                mSmallCircleParams.leftMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_17);
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_150);
            } else if (j == 3) {
                mSmallCircleParams.leftMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_39);
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_240);
            } else if (j == 4) {
                mSmallCircleParams.rightMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_112);
                mSmallCircleParams.gravity = Gravity.RIGHT;
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_224);
            }

            ImageView mSmallCircleView = new ImageView(getContext());
            mSmallCircleView.setImageResource(mList[j]);
            addView(mSmallCircleView, mSmallCircleParams);

            /**加载动画*/
            animPoints[j] = AnimationUtils.loadAnimation(getContext(), R.anim.anim_the_heartbeat);
            mSmallCircleView.startAnimation(animPoints[j]);

            animPoints[j].setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {

                    if (null != animPoints && null != animPoints[j]) {
                        animPoints[j].cancel();
                    }

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

        }
    }

    /**
     * 对外的开始动画v
     */
    public void startRippleAnimation() {

        if (animPoints != null) {
            for (int i = 0; i < animPoints.length; i++) {
                if (null != animPoints[i]) {
                    animPoints[i].start();
                }
            }
        }
    }

    /**
     * 对面外的结束动画
     */
    public void stopRippleAnimation() {

        if (animPoints != null) {
            for (int i = 0; i < animPoints.length; i++) {
                if (null != animPoints[i]) {
                    animPoints[i].reset();
                }
            }
            animPoints = null;
        }


    }

    public void destroy() {
        //结束动画

        animPoints = null;

    }

}
