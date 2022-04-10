package t20220049.sw_vision.view;

import static java.lang.Math.abs;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import t20220049.sw_vision.R;
import t20220049.sw_vision.app.AnimationListener;
import t20220049.sw_vision.utils.ResUtil;

public class RippleView extends FrameLayout {

    private boolean isRiskMove;
    private int mRiskLastX;
    private int mRiskLastY;

    int activateDevice = 0;
    boolean isCollision = false;
    int deviceWidth, deviceHeight;

    private final static String TAG = RippleView.class.getSimpleName();
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
    private Animation[] animPoints;
    private Animation[] animPhotos;

    private int[] mList = new int[]{R.mipmap.icon_secect_points_1, R.mipmap.icon_secect_2, R.mipmap.icon_secect_3, R.mipmap.icon_secect_4, R.mipmap.icon_secect_5};
    private LayoutInflater mInflater;
    private boolean isHidden;

    public RippleView(Context context) {
        this(context, null);
    }

    public RippleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        isHidden = false;
        init();
    }

    @SuppressLint("ResourceType")
    private void init() {
        mInflater = LayoutInflater.from(mContext);
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
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_580);
            } else if (j == 1) {
                mSmallCircleParams.rightMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_45);
                mSmallCircleParams.gravity = Gravity.RIGHT;
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_189);
            } else if (j == 2) {
                mSmallCircleParams.leftMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_17);
                mSmallCircleParams.topMargin = ResUtil.getDimens(mContext, R.dimen.common_padding_450);
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
                    if (mAnimationProgressListener != null) {
                        mAnimationProgressListener.startAnimation();
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mAnimationProgressListener != null) {
                        mAnimationProgressListener.endAnimation();
                    }

                    if (null != animPoints && null != animPoints[j]) {
                        animPoints[j].cancel();
                    }

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

        }


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

        this.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                RippleView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                //RippleView宽
                int rippleW = RippleView.this.getWidth();
                //RippleView高
                int rippleH = RippleView.this.getHeight();
                radiusP = ResUtil.getDimens(mContext, R.dimen.common_padding_16);
                radiusC = ResUtil.getDimens(mContext, R.dimen.common_padding_36);
                int padding = ResUtil.getDimens(mContext, R.dimen.common_padding_20);
                radiusB = rippleW / 2 - radiusP - padding;
                pointCX = rippleW / 2;
                pointCY = rippleH / 2;
                return true;
            }
        });
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        deviceWidth = wm.getDefaultDisplay().getWidth();
        deviceHeight = wm.getDefaultDisplay().getHeight();
//        setOnTouchListener(new OnTouchListener() {
//            @Override
////            public boolean onTouch(View view, MotionEvent motionEvent) {
////                if(imageViews==null)
////                    return false;
////                int currentX = (int) motionEvent.getRawX();
////                int currentY = (int) motionEvent.getRawY();
////                if (activateDevice == 0) {
////                    for (int i=0;i<imageViews.length;i++) {
////                        CircleImageView img = imageViews[i];
////                        if(img == null)
////                            continue;
////                        float dx = img.currentX;
////                        float dy = img.currentY;
////                        float detax = abs(currentX - dx);
////                        float detay = abs(currentY - dy);
////                        Log.d(TAG, "deta" + detax + "," + detay);
////                        if (detax < img.getWidth() && detay < img.getHeight()) {
////                            activateDevice = i;
////                            break;
////                        }
////                    }
////                } else {
////                    CircleImageView img = imageViews[activateDevice];
////                    if (img != null) {
////                        if(!isCollision) {
////                            int centerX = deviceWidth / 2;
////                            int centerY = deviceHeight / 2;
////                            double distanceFromCenter = Math.sqrt((currentX - centerX) * (currentX - centerX) + (currentY - centerY) * (currentY - centerY));
////                            int clientWidth = img.getWidth();
////                            int clientHeight = img.getHeight();
//////                            if ( (distanceFromCenter <= clientWidth + clientWidth / 2) ) {
//////                                //移动到与中心球碰撞
//////                                img.status = 1;
//////                                isCollision = true;
//////                            } else {
////                                img.currentX = currentX ;
////                                img.currentY = currentY ;
//////                                img.setTranslationX((int)(currentX - mRiskLastX));
//////                                img.setTranslationY((int)(currentY - mRiskLastY));
//////                                img.layout();
////                                mRiskLastX = currentX;
////                                mRiskLastY = currentY;
////                                int status = img.status;
////                                if (status == 0 && distanceFromCenter <= 200) {
////                                    img.status = 1;
////                                    tryConnect(img);
////                                }
////                                if (status != 0 && distanceFromCenter > 200) {
////                                    img.status = 0;
////                                }
////
//////                            }
////                        }
////                        invalidate();
////                    }
////                }
//////                Log.d(TAG, "activate:" + activateDevice);
////                return false;
////            }
//        });
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick ");
                activateDevice = 0;
                isCollision = false;
            }
        });
    }
    private int tryConnect(CircleImageView device) {
        device.status = 2;
        return device.status;
    }
    private final static int showNum = 5;
    private static long delayMillis = 4000;

    private int radiusP;
    private int radiusC;
    private int radiusB;
    private int pointCX;
    private int pointCY;
    private int page = 0;
    private int lastNum = 0;
    private boolean isAddShowing = false;
    private List<String> mGodHeadPhoto = new ArrayList<>();
    private Handler mHandler = new Handler();
    private UserRunnable userRunnable;
    private CircleImageView[] imageViews;
    private ObjectAnimator[] animator;

    /**
     * 设置大神头像
     */
    public void setUserPhoto(List<String> godHeadPhoto) {
        Log.e(TAG, "setUserPhoto==11==" + (null == godHeadPhoto ? 0 : godHeadPhoto.size()));
        if (null == godHeadPhoto || godHeadPhoto.isEmpty()) {
            return;
        }
        mGodHeadPhoto.addAll(godHeadPhoto);
        Log.e(TAG, "setUserPhoto==22==" + mGodHeadPhoto.size() + "==isAddShowing==" + isAddShowing);
        if (isAddShowing) {
            return;
        }
        page = 0;
        lastNum = 0;
        isAddShowing = true;
        if (radiusP <= 0 || radiusC <= 0 || radiusB <= 0 || pointCX <= 0 || pointCY <= 0) {
            this.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    RippleView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    //RippleView宽
                    int rippleW = RippleView.this.getWidth();
                    //RippleView高
                    int rippleH = RippleView.this.getHeight();
                    radiusP = ResUtil.getDimens(mContext, R.dimen.common_padding_16);
                    radiusC = ResUtil.getDimens(mContext, R.dimen.common_padding_36);
                    int padding = ResUtil.getDimens(mContext, R.dimen.common_padding_20);
                    radiusB = rippleW / 2 - radiusP - padding;
                    pointCX = rippleW / 2;
                    pointCY = rippleH / 2;

                    startShowUserList(true);
                    return true;
                }
            });
        } else {
            startShowUserList(true);
        }
    }

    /**
     * 开始显示
     *
     * @param isFirst 是否第一次显示
     */
    private void startShowUserList(boolean isFirst) {
        if (isHidden || (!isFirst && isAddShowing)) {
            return;
        }
        isAddShowing = true;
        showNextUserList();
    }

    /**
     * 获得当前显示大神list
     *
     * @return
     */
    private List<String> getCurShowList() {
        if (null == mGodHeadPhoto || mGodHeadPhoto.isEmpty()) {
            return null;
        }
        int size = mGodHeadPhoto.size();
        if (showNum >= size) {
            page = 0;
            lastNum = size;
            return mGodHeadPhoto;
        }
        int start = lastNum + page * showNum;
        if (start >= size) {
            return null;
        }
        int end = lastNum + (page + 1) * showNum;
        if (end >= size) {
            //列表已经展示结束
            page = 0;
            if (end == size) {
                lastNum = 0;
                return mGodHeadPhoto.subList(start, size);
            } else {
                lastNum = end - size;
                List<String> list = new ArrayList<>();
                list.addAll(mGodHeadPhoto.subList(start, size));
                list.addAll(mGodHeadPhoto.subList(0, lastNum));
                return list;
            }
        } else {
            page++;
            return mGodHeadPhoto.subList(start, end);
        }
    }

    /**
     * @param radiusP 五个随机大神头像半径
     * @param radiusC 中心点圆半径
     * @param radiusB 显示区域半径
     * @param pointCX 中心点x坐标
     * @param pointCY 中心点y坐标
     */
    private void addViewPhoto(int radiusP, int radiusC, int radiusB, int pointCX, int pointCY, List<String> list) {
        Log.e(TAG, "addViewPhoto==" + (null == list ? 0 : list.size()) + "==lastNum==" + lastNum + "==page==" + page + "==mGodHeadPhoto.size==" + mGodHeadPhoto.size());
        if (null != imageViews) {
            for (int i = 0; i < imageViews.length; i++) {
                if (null != imageViews[i]) {
                    RippleView.this.removeView(imageViews[i]);
                }
            }
        }
        if (userRunnable != null) {
            mHandler.removeCallbacks(userRunnable);
        }
        int size = null == list ? 0 : list.size();
        if (size == 0) {
            isAddShowing = false;
            animPhotos = null;
            imageViews = null;
            return;
        }
        animPhotos = new Animation[size];
        imageViews = new CircleImageView[size];
        animator = new ObjectAnimator[size];

        //动画控件的宽高
        for (int i = 0; i < size; i++) {
            final int k = i;
            int x1;
            int y1;
            int x2;
            int y2;
            if (k == 0) {
                /**随机按钮分四片区域显示
                 * 左上角区域
                 */
                x1 = pointCX - radiusB;
                y1 = pointCY - radiusB;

                x2 = pointCX - radiusP;
                y2 = pointCY - radiusC - radiusP;
            } else if (k == 1) {
                /**随机按钮分四片区域显示
                 * 右上角区域
                 */
                x1 = pointCX + radiusP;
                y1 = pointCY - radiusB;

                x2 = pointCX + radiusB;
                y2 = pointCY - radiusC;
            } else if (k == 2) {
                /**随机按钮分四片区域显示
                 * 左下角区域
                 */
                x1 = pointCX - radiusB;
                y1 = pointCY + radiusC + radiusP;

                x2 = pointCX - radiusP;
                y2 = pointCY + radiusB;
            } else if (k == 3) {
                /**随机按钮分四片区域显示
                 * 右下角区域
                 */
                x1 = pointCX + radiusP;
                y1 = pointCY + radiusC + radiusP;

                x2 = pointCX + radiusB;
                y2 = pointCY + radiusB;
            } else {
                int yshu = getRandomInt(Integer.MAX_VALUE) % 2;
                if (yshu == 0) {
                    /**随机按钮分四片区域显示
                     * 左中区域
                     */
                    x1 = pointCX - radiusB;
                    y1 = pointCY - radiusC + radiusP;

                    x2 = pointCX - radiusC - radiusP;
                    y2 = pointCY + radiusC - radiusP;
                } else {
                    /**随机按钮分四片区域显示 j
                     * 右中区域
                     */
                    x1 = pointCX + radiusC + radiusP;
                    y1 = pointCY - radiusC + radiusP;

                    x2 = pointCX + radiusB;
                    y2 = pointCY + radiusC - radiusP;
                }
            }
//            Log.e("addView", "addView x ==== == " + ((x2 - x1) / 2 + x1));
//            Log.e("addView", "addView y ==== == " + ((y2 - y1) / 2 + y1));
            //随机生成一个屏内的位置来显示动画

            int xs = 0;
            int ys = 0;
            boolean flag = true;
            while (flag) {
                xs = getRandomInt(Integer.MAX_VALUE) % (x2 - x1) + x1;
                ys = getRandomInt(Integer.MAX_VALUE) % (y2 - y1) + y1;
                if (isCricle(xs, ys)) {
                    flag = false;
                }
            }

            final int x = xs;
            final int y = ys;
            final String pathPhoto = list.get(k);

//            Log.e("addView", "addView x   === " + x);
//            Log.e("addView", "addView y   === " + y);
            mHandler.postDelayed(new WaitPhotoRunnable(pathPhoto, k, x, y), k * 180);

        }
//        showNextUserList();
    }

    //判断点与圆心之间的距离和圆半径的关系
    public boolean isCricle(int x, int y) {
        double d2 = Math.hypot((x - pointCX), (y - pointCY));
        if (d2 < radiusB) {
            return true;
        }
        return false;
    }

    boolean flag = true;

    /**
     * 继续显示
     */
    private void showNextUserList() {
        if (null == userRunnable) {
            userRunnable = new UserRunnable();
        }

        if (flag) {
            delayMillis = 2000;
            flag = false;
        } else {
            delayMillis = 4000;
        }
        mHandler.postDelayed(userRunnable, delayMillis);
    }

    class UserRunnable implements Runnable {
        @Override
        public void run() {
            addViewPhoto(radiusP, radiusC, radiusB, pointCX, pointCY, getCurShowList());
        }
    }

    public class WaitPhotoRunnable implements Runnable {
        private String pathPhoto;
        private int k;
        private int x;
        private int y;

        public WaitPhotoRunnable(String pathPhoto, int k, int x, int y) {
            this.pathPhoto = pathPhoto;
            this.k = k;
            this.x = x;
            this.y = y;
        }

        @Override
        public void run() {
            LayoutParams mCircleParams = new LayoutParams(radiusP * 2, radiusP * 2);
            mCircleParams.leftMargin = x - radiusP;
            mCircleParams.topMargin = y - radiusP;
            imageViews[k] = (CircleImageView) mInflater.inflate(R.layout.view_comment_circleimageview, null);
            addView(imageViews[k], mCircleParams);
            imageViews[k].currentX = x;
            imageViews[k].currentY = y;
            imageViews[k].status = 0;
//            Log.e("resId","resId   ==  "+resId);
//            Log.e("resId","resId x  ==  "+x);
//            ImageLoaderUtils.getInstance().loadPicture(pathPhoto, imageViews[k],
//                    resId, resId);
            imageViews[k].setImageDrawable(ResUtil.getDrawable(R.drawable.ic_client));
            animPhotos[k] = AnimationUtils.loadAnimation(getContext(), R.anim.player_double_click_animation);
            imageViews[k].startAnimation(animPhotos[k]);
            animPhotos[k].setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (mAnimationProgressListener != null) {
                        mAnimationProgressListener.startAnimation();
                    }
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mAnimationProgressListener != null) {
//                        mAnimationProgressListener.endAnimation();
                    }
                    if (null != imageViews && null != imageViews[k]) {
                        animator[k] = ObjectAnimator.ofFloat(imageViews[k], "alpha", 1.0f, 1.0f).setDuration(150);
                        animator[k].start();
                        animator[k].addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
//                                if (null != imageViews && null != imageViews[k]) {
//                                    imageViews[k].setVisibility(View.GONE);
//                                }
//                                if (null != animator && null != animator[k]) {
//                                    animator[k].cancel();
//                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                    }
                    if (null != animPhotos && null != animPhotos[k]) {
//                        animPhotos[k].cancel();
                    }
                }
                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }

    /**
     * 停止显示
     */
    private void stopShowUserList() {
////        if (null == userRunnable) {
////            return;
////        }
//        isAddShowing = false;
////        mHandler.removeCallbacks(userRunnable);
//        mHandler.removeCallbacksAndMessages(null);
//
//        if (imageViews != null) {
//            for (int i = 0; i < imageViews.length; i++) {
//                if (null != imageViews[i]) {
//                    imageViews[i].setVisibility(GONE);
//                    imageViews[i] = null;
//                }
//            }
//            imageViews = null;
//        }
//
//        if (animator != null) {
//            for (int i = 0; i < animator.length; i++) {
//                if (null != animator[i]) {
//                    animator[i].cancel();
//                }
//            }
//            animator = null;
//        }
//
//        if (animPhotos != null) {
//            for (int i = 0; i < animPhotos.length; i++) {
//                if (null != animPhotos[i]) {
//                    animPhotos[i].reset();
//                }
//            }
//            animPhotos = null;
//        }

    }

    //生成随机数 范围 [0,max)
    private int getRandomInt(int max) {
        Random random = new Random();
        return random.nextInt(max);
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
        if (animPoints != null) {
            for (int i = 0; i < animPoints.length; i++) {
                if (null != animPoints[i]) {
                    animPoints[i].reset();
                }
            }
            animPoints = null;
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
        page = 0;
        lastNum = 0;
        isAddShowing = false;
        mHandler.removeCallbacksAndMessages(null);
        userRunnable = null;
        isHidden = true;
        if (null != mGodHeadPhoto) {
            mGodHeadPhoto.clear();
        }
        imageViews = null;
        animatorSet = null;
        animPoints = null;
        animPhotos = null;
        animator = null;
    }

    public void setAnimationProgressListener(AnimationListener mAnimationProgressListener) {
        this.mAnimationProgressListener = mAnimationProgressListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint devicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        devicePaint.setColor(Color.WHITE);
        devicePaint.setTextSize(30f);
        statusPaint.setColor(Color.GREEN);
        statusPaint.setTextSize(35f);

        //绘制中心服务设备
//        canvas.drawBitmap(serverBitmap, (float) (deviceWidth - serverWidth) / 2, (float) (deviceHeight - serverHeight) / 2, null);
        // 绘制客户设备
        if(imageViews==null)
            return;
        for (CircleImageView img : imageViews) {
            if (img == null)
                continue;
            String deviceText = img.deviceName + " (" + img.nickName + ")";
            canvas.drawText(deviceText, img.currentX - 120, img.currentY - 70, devicePaint);
            if (img.status == 1){
                canvas.drawText("（连接中...）", img.currentX -5, img.currentY - 80, statusPaint);
            }else if(img.status==2){
                canvas.drawText("（已连接）", img.currentX - 5, img.currentY - 80, statusPaint);
            }

        }
    }
}