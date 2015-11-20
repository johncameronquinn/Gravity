package com.jokrapp.android;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Author/Copyright John C. Quinn All Rights Reserved
 * Date last modified: 2015-06-17
 *
 * class 'CustomViewPager'
 *
 * a custom implementation of the viewpager to allow the ability to disable paging on command
 */
public class CustomViewPager extends ViewPager  {

    private boolean isPagingEnabled = true;


    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* if true, stops swipes, passes view to child
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.isPagingEnabled &&  super.onTouchEvent(event);
        //boolean out
       // Log.d("ViewPager","onTouchEvent:  " + out);
                //return out;
    }



    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onInterceptTouchEvent(event);


        //boolean out =
     //   Log.d("ViewPager","onInterceptTouchEvent:  " + out);
               // return out;
    }

    public void setPagingEnabled(boolean b) {
        this.isPagingEnabled = b;
    }

    public boolean isPagingEnabled() {
        return isPagingEnabled;
    }
}