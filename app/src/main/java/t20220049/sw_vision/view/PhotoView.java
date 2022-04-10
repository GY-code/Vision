package t20220049.sw_vision.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import t20220049.sw_vision.R;
import t20220049.sw_vision.app.AnimationListener;
import t20220049.sw_vision.utils.ResUtil;


public class PhotoView extends FrameLayout {

    private final static String TAG = PhotoView.class.getSimpleName();

    private Context mContext;
    private Animation[] animPhotos;
    private LayoutInflater mInflater;
    private boolean isHidden;
    private final static int showNum = 5;
    private static long delayMillis = 4000;
    /**
     * 自定义的动画开始与结束接口
     */
    private AnimationListener mAnimationProgressListener;
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
    private TextView[] textViews;
    private ObjectAnimator[] animator;

    public PhotoView(Context context) {
        this(context, null);
    }

    public PhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        isHidden = false;
        init();
    }

    @SuppressLint("ResourceType")
    private void init() {
        mInflater = LayoutInflater.from(mContext);

        this.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                PhotoView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                //RippleView宽
                int rippleW = PhotoView.this.getWidth();
                //RippleView高
                int rippleH = PhotoView.this.getHeight();
                radiusP = ResUtil.getDimens(mContext, R.dimen.common_padding_16);
                radiusC = ResUtil.getDimens(mContext, R.dimen.common_padding_36);
                int padding = ResUtil.getDimens(mContext, R.dimen.common_padding_20);
                radiusB = rippleW / 2 - radiusP - padding;
                pointCX = rippleW / 2;
                pointCY = rippleH / 2;
                return true;
            }
        });
    }


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
                    PhotoView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    //RippleView宽
                    int rippleW = PhotoView.this.getWidth();
                    //RippleView高
                    int rippleH = PhotoView.this.getHeight();
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
                    PhotoView.this.removeView(imageViews[i]);
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
        textViews = new TextView[size];
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
        showNextUserList();
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
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (null != imageViews && null != imageViews[k]) {
                        animator[k] = ObjectAnimator.ofFloat(imageViews[k], "alpha", 1.0f, 0.0f).setDuration(150);
                        animator[k].start();
                        animator[k].addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (null != imageViews && null != imageViews[k]) {
                                    imageViews[k].setVisibility(View.GONE);
                                }
                                if (null != animator && null != animator[k]) {
                                    animator[k].cancel();
                                }
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
                        animPhotos[k].cancel();
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
        isAddShowing = false;
        mHandler.removeCallbacksAndMessages(null);

        if (imageViews != null) {
            for (int i = 0; i < imageViews.length; i++) {
                if (null != imageViews[i]) {
                    imageViews[i].setVisibility(GONE);
                    imageViews[i] = null;
                }
            }
            imageViews = null;
        }

        if (animator != null) {
            for (int i = 0; i < animator.length; i++) {
                if (null != animator[i]) {
                    animator[i].cancel();
                }
            }
            animator = null;
        }

        if (animPhotos != null) {
            for (int i = 0; i < animPhotos.length; i++) {
                if (null != animPhotos[i]) {
                    animPhotos[i].reset();
                }
            }
            animPhotos = null;
        }

    }


    //生成随机数 范围 [0,max)
    private int getRandomInt(int max) {
        Random random = new Random();
        return random.nextInt(max);
    }


    public void onHiddenChanged(boolean hidden) {
        this.isHidden = hidden;
        if (isHidden) {
            stopShowUserList();
        } else {
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
        animPhotos = null;
        animator = null;
    }

}