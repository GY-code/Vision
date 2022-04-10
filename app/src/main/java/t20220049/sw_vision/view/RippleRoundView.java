package t20220049.sw_vision.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;

import t20220049.sw_vision.R;
import t20220049.sw_vision.transfer.app.AnimationListener;
import t20220049.sw_vision.transfer.utils.ResUtil;

//波纹
public class RippleRoundView extends FrameLayout {

    private final static String TAG = RippleRoundView.class.getSimpleName();
    /**
     * 涟漪的颜色
     */
    private int rippleColor = ResUtil.getColor(R.color.white);
    /**
     * 最里面涟漪的实心圆
     */
    private float rippleStrokeWidth = 5;
    /**
     * 涟漪的半径
     */
    private float rippleRadius = getResources().getDimension(R.dimen.common_padding_26);
    /**
     * 自定义的动画开始与结束接口
     */
    private AnimationListener mAnimationProgressListener;
    /**
     * 画笔
     */
    private Paint paint;
    /**
     * 动画集合
     */
    private AnimatorSet animatorSet;
    /**
     * 自定义view集合
     */
    private ArrayList<RipplView> rippleViewList = new ArrayList<>();
    /**
     * 每次动画的时间
     */
    private int rippleDurationTime = 4000;
    /**
     * 涟漪条目
     */
    private int rippleAmount = 4;
    /**
     * 每条涟漪依次出现的延迟
     */
    private int rippleDelay;

    private Context mContext;
    private boolean isHidden;

    public RippleRoundView(Context context) {
        this(context, null);
    }

    public RippleRoundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleRoundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        isHidden = false;
        init();
    }

    @SuppressLint("ResourceType")
    private void init() {
        //画每个圆的时间间隔为一个圆的动画时间除以总共出现圆的个数，达到每个圆出现的时间间隔一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(ResUtil.getDrawable(R.drawable.bg_timely_manner));
        }
        rippleDelay = rippleDurationTime / rippleAmount;
        //初始化画笔
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(ResUtil.getDimens(mContext, R.dimen.common_padding_1));
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(rippleColor);
        paint.setAlpha(100);

        animatorSet = new AnimatorSet();
        animatorSet.setDuration(rippleDurationTime);

        //加速插值器
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());

        //布局 管理器，让圆剧中显示
        LayoutParams rippleParams = new LayoutParams((int) (2 * (rippleRadius + rippleStrokeWidth * 3)), (int) (2 * (rippleRadius + rippleStrokeWidth * 3)));
        rippleParams.gravity = Gravity.CENTER;

        //动画的集合
        ArrayList<Animator> animatorList = new ArrayList<>();

        //水波纹 缩放、渐变动画
        for (int i = 0; i < rippleAmount; i++) {

            RipplView rippleView = new RipplView(getContext());
            addView(rippleView, rippleParams);
            rippleViewList.add(rippleView);

            //伸缩动画
            float rippleScale = 6.0f;
            final ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(rippleView, "ScaleX", 1.0f, rippleScale);
            scaleXAnimator.setRepeatCount(ValueAnimator.INFINITE);
            scaleXAnimator.setRepeatMode(ObjectAnimator.RESTART);
            scaleXAnimator.setStartDelay(i * rippleDelay);
            animatorList.add(scaleXAnimator);

            final ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(rippleView, "ScaleY", 1.0f, rippleScale);
            scaleYAnimator.setRepeatCount(ValueAnimator.INFINITE);
            scaleYAnimator.setRepeatMode(ObjectAnimator.RESTART);
            scaleYAnimator.setStartDelay(i * rippleDelay);
            animatorList.add(scaleYAnimator);

            //透明度动画
            final ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(rippleView, "Alpha", 1.0f, 0f);
            alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
            alphaAnimator.setRepeatMode(ObjectAnimator.RESTART);
            alphaAnimator.setStartDelay(i * rippleDelay);
            animatorList.add(alphaAnimator);
        }

        //开始动画
        animatorSet.playTogether(animatorList);
        //动画的监听
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (mAnimationProgressListener != null) {
                    mAnimationProgressListener.startAnimation();
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (mAnimationProgressListener != null) {
                    mAnimationProgressListener.endAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });


    }

    /**
     * 开始显示
     *
     * @param isFirst 是否第一次显示
     */
    private void startShowUserList(boolean isFirst) {
        if (isHidden || (!isFirst)) {
            return;
        }
//        isAddShowing = true;
//        showNextUserList();
    }

    /**
     * 停止显示
     */
    private void stopShowUserList() {


    }

    //画一个圆
    private class RipplView extends View {

        RipplView(Context context) {
            super(context);
            this.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //圆的半径，也就是它的父布局宽或高（取最小）的一半
            int radius = (Math.min(getWidth(), getHeight())) / 2;
            /**
             * 参数解析：
             * 圆心的x坐标。
             * 圆心的y坐标。
             * 圆的半径。
             * 绘制时所使用的画笔。
             */
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(radius, radius, radius - rippleStrokeWidth, paint);

        }
    }

    /**
     * 对外的开始动画v
     */
    public void startRippleAnimation() {
        for (RipplView rippleView : rippleViewList) {
            rippleView.setVisibility(VISIBLE);
        }
        if (null != animatorSet && !animatorSet.isRunning()) {
            animatorSet.start();
        }
    }

    /**
     * 对面外的结束动画
     */
    public void stopRippleAnimation() {

        for (RipplView rippleView : rippleViewList) {
            rippleView.setVisibility(INVISIBLE);
        }
        if (null != animatorSet) {
            animatorSet.end();
        }

    }

    public void onHiddenChanged(boolean hidden) {
        this.isHidden = hidden;
        if (isHidden) {
            stopRippleAnimation();
            stopShowUserList();
        } else {
            startRippleAnimation();
            startShowUserList(false);
        }
    }

    public void destroy() {
        //结束动画
        isHidden = true;

        animatorSet = null;
    }

    public void setAnimationProgressListener(AnimationListener mAnimationProgressListener) {
        this.mAnimationProgressListener = mAnimationProgressListener;
    }

}