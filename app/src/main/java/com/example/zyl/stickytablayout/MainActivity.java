package com.example.zyl.stickytablayout;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.zyl.stickytablayout.view.StickyNavLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private String[] mTitles = new String[]{"简介", "评价", "相关"};
    private TabLayout mIndicator;
    private ViewPager mViewPager;
    private StickyNavLayout snl;
    private FragmentPagerAdapter mAdapter;
    private float imgFirstHeight;
    private TabFragment[] mFragments = new TabFragment[mTitles.length];
    private List<ImageView> titleIvs = new ArrayList<>();
    private int[] titleImg = {R.drawable.bg, R.drawable.bg2};
    private RelativeLayout topView;
    private Toolbar toolbar;
    private Window mwindow;
    private ViewPager vp_titleImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        StatusBarUtil.setTransparentForImageView(this, toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 初始化视图
        mwindow = getWindow();
        initData();
        snl.setPullDownListener(new MyPullDownListener());
    }

    private class MyPullDownListener implements StickyNavLayout.PullDownWithUpListener {
        /**
         * 是否达到刷新条件
         */
        private boolean is_canRefresh;
        private float last_distance;

        @Override
        public void pullDownDistance(float distance, boolean isUP) {
            if (imgFirstHeight == 0) {
                imgFirstHeight = topView.getLayoutParams().height;
            }

            distance = distance / 2.4f;//（2.2是阻尼大小）
            if (distance > 1 * imgFirstHeight) return;//下拉边界值

            if (!is_canRefresh && distance > 0.25 * imgFirstHeight) {//达到最小刷新距离，
                is_canRefresh = true;
                Toast.makeText(MainActivity.this, "回弹可刷新！", Toast.LENGTH_SHORT).show();
            }
            if (distance == 0) {//已经回弹
                if (is_canRefresh && isUP) {
                    Toast.makeText(MainActivity.this, "调用刷新！", Toast.LENGTH_SHORT).show();
                }
                is_canRefresh = false;
            }

            if (isUP) {
                //渐渐恢复原来的高度----渐变动画：height
                SmoothAnimation resetAnmiation = new SmoothAnimation(topView, (int) imgFirstHeight);

                float ratio = last_distance / imgFirstHeight;
//                Log.i(TAG, "pullDownDistance: "+ratio);

                resetAnmiation.setDuration((long) (ratio*300));
                topView.startAnimation(resetAnmiation);
            } else {
                //当前需要设置的高度
                float h = imgFirstHeight + distance;
                topView.getLayoutParams().height = (int) h;
                topView.requestLayout();
            }
            last_distance=distance;
        }

        @Override
        public void pullUp(float ratio) {
//            Log.i(TAG, "pullUp: " + ratio);
            setNavColor(ratio);
        }
    }

    private void setNavColor(float expandedPercentage) {
        int toolThemeColor = Color.WHITE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (expandedPercentage == 1) {
                mwindow.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                mwindow.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
            toolThemeColor= ColorUtils.blendARGB(Color.WHITE, Color.DKGRAY, expandedPercentage);
            mwindow.setStatusBarColor(ColorUtils.blendARGB(Color.TRANSPARENT, Color.WHITE, expandedPercentage));
        }

        Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
        //以下三行是修改回退按钮为白色的逻辑
        upArrow.setColorFilter(toolThemeColor, PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        toolbar.setTitleTextColor(toolThemeColor);


        toolbar.setBackgroundColor(ColorUtils.blendARGB(Color.TRANSPARENT, Color.WHITE, expandedPercentage));
    }

    private class SmoothAnimation extends Animation {
        private View iv;
        private int vHeight;
        private int extraHeight;//高度差

        public SmoothAnimation(View iv, int targetHeight) {
            this.iv = iv;
            this.vHeight = iv.getHeight();
            extraHeight = vHeight - targetHeight;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            iv.getLayoutParams().height = (int) (vHeight - extraHeight * interpolatedTime);
            iv.requestLayout();

            super.applyTransformation(interpolatedTime, t);
        }
    }


    private void initViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        snl = (StickyNavLayout) findViewById(R.id.snl);
        topView = (RelativeLayout) findViewById(R.id.id_stickyNavLayout_topView);
        mIndicator = (TabLayout) findViewById(R.id.id_stickyNavLayout_indicator);
        mViewPager = (ViewPager) findViewById(R.id.id_stickyNavLayout_viewpager);
        vp_titleImg = (ViewPager) findViewById(R.id.vp_titleImg);
    }

    private void initData() {
        //内容区域
        for (int i = 0; i < mTitles.length; i++) {
            mFragments[i] = TabFragment.newInstance(mTitles[i]);
        }

        mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return mTitles.length;
            }

            @Override
            public Fragment getItem(int position) {
                return mFragments[position];
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return mTitles[position];
            }
        };

        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mIndicator.setupWithViewPager(mViewPager);

        //标题栏
        vp_titleImg.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ImageView imageView;
                if (titleIvs.size() == 0) {
                    imageView = new ImageView(MainActivity.this);
                    imageView.setAdjustViewBounds(true);
                    imageView.setMaxHeight(400 * 3);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    imageView = titleIvs.remove(titleIvs.size() - 1);
                }
                imageView.setImageResource(titleImg[position % titleImg.length]);

                container.addView(imageView);
                return imageView;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((ImageView) object);
                titleIvs.add((ImageView) object);
            }

        });
    }


}
