package com.example.zyl.stickytablayout;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.ViewConfiguration;

/**
 * Created by TFHR02 on 2018/3/2.
 *  //计算惯性滚动需要的变量,代码来源于scroller
 */
public class Utils {
    private static final float INFLEXION = 0.35f;
    private static float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static float mPhysicalCoeff;
    public static float ppi=0f;
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));

    private static void getPPi(Context context) {
        if (ppi==0){
            ppi = context.getResources().getDisplayMetrics().density * 160.0f;
            mPhysicalCoeff = SensorManager.GRAVITY_EARTH * 39.37f * ppi * 0.84f;
        }
    }

    private static double getSplineDeceleration(Context context, int velocity) {
        getPPi(context);
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private static double getSplineDecelerationByDistance(Context context, double distance) {
        getPPi(context);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return decelMinusOne * (Math.log(distance / (mFlingFriction * mPhysicalCoeff))) / DECELERATION_RATE;
    }

    /**
     * 根据速度获取滑动的时间
     */
    public static int getSplineFlingDuration(Context context, int velocity) {
        final double l = getSplineDeceleration(context,velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }

    /**
     * 通过初始速度获取最终滑动距离
     * @param context
     * @param velocity
     * @return
     */
    public static double getSplineFlingDistance(Context context, int velocity) {
        final double l = getSplineDeceleration(context,velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }


    /**
     * 通过需要滑动的距离获取初始速度
     * @param context
     * @param distance
     * @return
     */
    public static int getVelocityByDistance(Context context, double distance) {
        final double l = getSplineDecelerationByDistance(context,distance);
        int velocity = (int) (Math.exp(l) * mFlingFriction * mPhysicalCoeff / INFLEXION);
        return Math.abs(velocity);
    }
}
