package com.example.zyl.stickytablayout.view;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.example.zyl.stickytablayout.R;
import com.example.zyl.stickytablayout.Utils;


/**
 * 粘性导航栏布局
 */
public class StickyView extends LinearLayout {
    private static final String TAG = "StickyNavLayout";

    /**
     * 标题栏
     */
    private View mTop;
    /**
     * 指示器
     */
    private View mNav;
    private ViewPager mViewPager;

    private int mTopViewHeight;
    private RecyclerView mInnerScrollView;
    private boolean isTopHidden = false;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;

    private float mLastY, mLastX;
    private boolean mDragging;
    private int mMaximumVelocity, mMinimumVelocity;
    /**
     * 标题栏高度
     */
    private int TOP_H = 56;

    /**
     * 当前布局抬起手时瞬时速度可以滑动的距离
     */
    private double thisUpCould_Distance;

    private ValueAnimator flingAnimation;
    /**
     * 不能滚到上面了
     */
    private boolean is_NoCallScrollInFling;
    /**
     * 已经惯性滚动过了
     */
    private boolean alreadyFlingFirst = false;
    float pullDownDistance;

    public StickyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
        TOP_H= (int) (48*context.getResources().getDisplayMetrics().density+0.5f);
        TOP_H = TOP_H + (Build.VERSION.SDK_INT > 19 ? 72 : 0);
        mScroller = new OverScroller(context);
//        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTouchSlop = 8;
        pullDownDistance = mTouchSlop;
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    int mTopFirstHeight = 0;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTop = findViewById(R.id.id_stickyNavLayout_topView);
        mNav = findViewById(R.id.id_stickyNavLayout_indicator);
        View view = findViewById(R.id.id_stickyNavLayout_viewpager);
        if (!(view instanceof ViewPager)) {
            throw new RuntimeException(
                    "id_stickyNavLayout_viewpager show used by ViewPager !");
        }
        mViewPager = (ViewPager) view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        params.height = getMeasuredHeight() - mNav.getMeasuredHeight() - TOP_H;//减相同高度防止listview高度超出显示范围
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 具体可以通过分辨率来获取值丢进去，暂时为144。这里修改高度，为状态栏的高度
        mTopViewHeight = mTop.getMeasuredHeight() - TOP_H;
        mTopFirstHeight = mTop.getMeasuredHeight();
    }

    /**
     * public boolean canScrollVertically(View view, int direction)
     * 用来判断view在竖直方向上能不能向上或者向下滑动
     * view      v
     * direction 方向    负数代表向上滑动 ，正数则反之
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        float y = ev.getY();
        float x = ev.getX();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                mLastX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                float dx = x - mLastX;
                getCurrentScrollView();

                if (Math.abs(dy) > mTouchSlop && Math.abs(dy) > Math.abs(dx)) {
                    mDragging = true;
                    if (mInnerScrollView instanceof RecyclerView) {
                        RecyclerView rv = mInnerScrollView;
                        //1，头部显示且滚动view不能下滑，拦截事件
                        //2，头部隐藏且滚动view不能下滑且方向向下，拦截事件
                        //3，如果触摸的是选项卡，拦截事件
                        int[] mNavScreen = new int[2];
                        mNav.getLocationOnScreen(mNavScreen);

                        if ((!isTopHidden && !ViewCompat.canScrollVertically(rv, -1))
                                || (!ViewCompat.canScrollVertically(rv, -1) && isTopHidden && dy > 0)
                                || (mLastY > TOP_H && mLastY < (mNavScreen[1] + mNav.getHeight()))) {
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDragging = false;
                recycleVelocityTracker();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }


    /**
     * 是不是想下拉
     */
    private boolean is_canPull_up;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        int action = event.getAction();
        float y = event.getY();
        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) mScroller.abortAnimation();//停止动画
                mLastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging && pullDownDistance == mTouchSlop) {
                    scrollBy(0, (int) -dy);
                    if (getScrollY() == mTopViewHeight && dy < 0) {//拖拽到顶部了，继续上托，重新分发事件
                        recycleVelocityTracker();
                        event.setAction(MotionEvent.ACTION_DOWN);
                        dispatchTouchEvent(event);
                    }
                }
                if (getScrollY() == 0) {//当顶部完全显示时：记录下拉距离
//                    if (dy >0){
                        recycleVelocityTracker();
//                    }
                    is_canPull_up = true;
                    pullDownDistance = pullDownDistance + dy;
                    if (pullDownDistance < mTouchSlop) {
                        pullDownDistance = mTouchSlop;
                    }
                } else {
                    is_canPull_up = false;
                }
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                pullDownDistance = mTouchSlop;
                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityY = (int) mVelocityTracker.getYVelocity();
//                Log.i(TAG, "velocityY2: " + velocityY);
//                Log.i(TAG, "mMinimumVelocity: "+mMinimumVelocity);
                if (Math.abs(velocityY) > mMinimumVelocity) {
                    thisUpCould_Distance = Utils.getSplineFlingDistance(getContext(), Math.abs(velocityY));
                    fling(-velocityY);
                }
                recycleVelocityTracker();
                break;
        }
        if (pullDownListener != null) {
            pullDownListener.pullDownDistance(pullDownDistance - mTouchSlop, action == MotionEvent.ACTION_UP && is_canPull_up);
        }
        return super.onTouchEvent(event);
    }

    public interface PullDownWithUpListener {
        void pullDownDistance(float distance, boolean isUP);

        void pullUp(float ratio);
    }

    private PullDownWithUpListener pullDownListener;

    public void setPullDownListener(PullDownWithUpListener pullDownListener) {
        this.pullDownListener = pullDownListener;
    }

    /**
     * 获取当前滚动的View
     */
    private void getCurrentScrollView() {
        int currentItem = mViewPager.getCurrentItem();
        PagerAdapter a = mViewPager.getAdapter();
        if (a instanceof FragmentPagerAdapter) {
            final FragmentPagerAdapter fadapter = (FragmentPagerAdapter) a;
            Fragment item = (Fragment) fadapter.instantiateItem(mViewPager, currentItem);
            mInnerScrollView = (RecyclerView) (item.getView().findViewById(R.id.id_stickyNavLayout_innerScrollview));
            //处理recyclerView下拉结束后parent滚动(先惯性滚动，然后rv到顶停止，处理后续自由滚动)
            mInnerScrollView.setOnFlingListener(new RecyclerView.OnFlingListener() {
                @Override
                public boolean onFling(int velocityX, final int velocityY) {
//                    Log.i(TAG, "ACTION_UP_onFling: "+velocityY);
                    //这个速度滑到速度为0用时，
                    int splineFlingDuration = Utils.getSplineFlingDuration(getContext(), velocityY);
//                    Log.i(TAG, "Duration: " + splineFlingDuration);
                    //这个速度将要滑动的总距离
                    final double splineFlingDistance = Utils.getSplineFlingDistance(getContext(), velocityY);
                    if (flingAnimation != null) {
                        if (flingAnimation.isRunning()) flingAnimation.end();
                        flingAnimation = null;
                    }
                    flingAnimation = ValueAnimator.ofFloat(0, (float) splineFlingDistance).setDuration(splineFlingDuration);
                    flingAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            if (is_NoCallScrollInFling && !alreadyFlingFirst && velocityY < 0) {
                                is_NoCallScrollInFling = false;
                                alreadyFlingFirst = true;
                                float alreadyFlingDistance = (float) (splineFlingDistance * (animation.getAnimatedFraction()));
//                                Log.i(TAG, "已经滑动的距离: " + alreadyFlingDistance);
                                //剩余距离=总距离-已经滑动的距离
                                double distance = splineFlingDistance - alreadyFlingDistance;
                                //根据剩余距离获得滑动的初速度
                                int startV = Utils.getVelocityByDistance(getContext(), distance);
//                                Log.i(TAG, "ACTION_UP_startV: "+startV);
                                //让布局滚动
                                fling(-startV);
                            }
                        }
                    });
                    flingAnimation.start();

                    return false;
                }
            });
            mInnerScrollView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
//                    Log.i("onScrolled", "getScrollState: " + recyclerView.getScrollState());
                    if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                        boolean b = ViewCompat.canScrollVertically(recyclerView, -1);//能滚向上面
                        if (!b) {//不能滑向上方了，
                            is_NoCallScrollInFling = true;
                        } else {
                            is_NoCallScrollInFling = false;
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    boolean b = ViewCompat.canScrollVertically(recyclerView, -1);//能滚向上面
                    requestDisallowInterceptTouchEvent(b);
                }
            });
        }
    }

    public void fling(int velocityY) {
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) y = 0;

        if (y > mTopViewHeight) y = mTopViewHeight;

        if (y != getScrollY()) super.scrollTo(x, y);

        isTopHidden = getScrollY() == mTopViewHeight;
        if (pullDownListener != null) {
            pullDownListener.pullUp((1.0f * getScrollY()) / mTopViewHeight);
        }
    }

    /**
     * 由父视图调用用来请求子视图根据偏移值 mScrollX,mScrollY重新绘制
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {//true说明滚动尚未完成，false说明滚动已经完成。
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        } else {
            alreadyFlingFirst = false;
            if (isTopHidden && !mDragging) {//向上滑时，滑倒头了而且没有拖拽的情况下，让recyclerView滑
                //可以滑动的总距离，减去已滑动的距离，即剩余可滑动距离
                double distance = thisUpCould_Distance - getScrollY();
                //根据距离拿到初始速度
                int startV = Utils.getVelocityByDistance(getContext(), distance);
//                Log.i(TAG, "startV: " + startV);
                //根据这个速度让滚动布局惯性滑动
//                if (!ViewCompat.canScrollVertically(mInnerScrollView, -1)) {
                mInnerScrollView.fling(0, startV+mMinimumVelocity);
//                }
            }
        }
    }

    /**
     * 初始化速度跟踪器
     */
    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    /**
     * 回收速度跟踪器
     */
    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

}