package com.jokrapp.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;

/***************************************************************************************************
 * class "ImageAdapterView"
 *
 * this class serves as an Adapter view for {@link ImageStackCursorAdapter}
 */
public class ImageCursorAdapterView extends AdapterView<CursorAdapter> {
    private final boolean VERBOSE = false;
    private final String TAG = "ImageAdapterView";

    public static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mNumberOfCards = -1;
    private final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (VERBOSE) {
                Log.v(TAG,"DataSetObserver has been notified of a change, rebuilding...");
            }
            ensureFull();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            clearStack();
        }
    };
    private final Rect boundsRect = new Rect();
    private final Rect childRect = new Rect();
    private final Matrix mMatrix = new Matrix();



    //TODO: determine max dynamically based on device speed
    private int mMaxVisible = 2;
    private int mOffset = 0;
    private GestureDetector mGestureDetector;
    private int mFlingSlop;
    private CursorAdapter mCursorAdapter;
    private float mLastTouchX;
    private float mLastTouchY;
    private View mTopCard;
    private int mTouchSlop;
    private int mGravity;
    private int mNextAdapterPosition;
    private boolean mDragging;



    public ImageCursorAdapterView(Context context) {
        super(context);

        Log.d(TAG, "created not from xml...");

        setGravity(Gravity.CENTER);
        init();

    }

    public ImageCursorAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromXml(attrs);
        init();
    }


    public ImageCursorAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initFromXml(attrs);
        init();
    }

    private void init() {

        setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        mFlingSlop = viewConfiguration.getScaledMinimumFlingVelocity();
        mTouchSlop = viewConfiguration.getScaledTouchSlop();
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    private void initFromXml(AttributeSet attr) {
        setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public CursorAdapter getAdapter() {
        return mCursorAdapter;
    }

    @Override
    public void setAdapter(CursorAdapter adapter) {
        if (VERBOSE) {
            Log.v(TAG, "entering setAdapter...");
        }

        if (mCursorAdapter != null)
            mCursorAdapter.unregisterDataSetObserver(mDataSetObserver);

        clearStack();
        mTopCard = null;
        mCursorAdapter = adapter;
        mNextAdapterPosition = 0;
        adapter.registerDataSetObserver(mDataSetObserver);

        ensureFull();

        if (getChildCount() != 0) {

            if (VERBOSE) {
                Log.v(TAG,"mTopCard is set to: " + (getChildCount() - 1));
            }
            mTopCard = getChildAt(getChildCount() - 1);
            mTopCard.setLayerType(LAYER_TYPE_HARDWARE, null);
        }
        mNumberOfCards = getAdapter().getCount();
        requestLayout();

        if (VERBOSE) {
            Log.v(TAG, "exiting setAdapter...");
        }
    }

    private void ensureFull() {
        if (VERBOSE) {
            Log.v(TAG, "entering ensureFull...");

            Log.v(TAG,"while ( " + mNextAdapterPosition + " < " + mCursorAdapter.getCount() + " && " +
                    getChildCount() + " < " + mMaxVisible + " )");
        }


     //   Cursor data = mCursorAdapter.getCursor();

        while (mNextAdapterPosition < mCursorAdapter.getCount() && getChildCount() < mMaxVisible) {
            View view = mCursorAdapter.getView(mNextAdapterPosition, null, this);
            view.setLayerType(LAYER_TYPE_SOFTWARE, null);

         /*   View btnView = view.findViewById(R.id.messageButton);
            btnView.setTag(data.getInt(0));

            btnView = view.findViewById(R.id.blockButton);
            btnView.setTag(data.getInt(0));
            data.moveToNext();*/


            addViewInLayout(view, 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                    mCursorAdapter.getItemViewType(mNextAdapterPosition)), false);

           requestLayout();

            mNextAdapterPosition += 1;
        }

        if (getChildCount() != 0) {

            if (VERBOSE) {
                Log.v(TAG,"mTopCard is set to: " + (getChildCount() - 1));
            }
            mTopCard = getChildAt(getChildCount() - 1);
            mTopCard.setLayerType(LAYER_TYPE_HARDWARE, null);
        }
        mNumberOfCards = getAdapter().getCount();
        //requestLayout();

        if (VERBOSE) {
            Log.v(TAG, "exiting ensureFull...");
        }
    }

    private void clearStack() {
        if (VERBOSE) {
            Log.v(TAG, "entering clearStack, resetting everything...");
        }

        removeAllViewsInLayout();
        mNextAdapterPosition = 0;
        mOffset = 0;
        mTopCard = null;

        if (VERBOSE) {
            Log.v(TAG, "exiting clearStack...");
        }
    }

    public void addViewToStack() {

    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int requestedWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int requestedHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int childWidth, childHeight;

            childWidth = requestedWidth;
            childHeight = requestedHeight;


        int childWidthMeasureSpec, childHeightMeasureSpec;
        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST);
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            assert child != null;
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        for (int i = 0; i < getChildCount(); i++) {
            boundsRect.set(0, 0, getWidth(), getHeight());

            View view = getChildAt(i);
            int w, h;
            w = view.getMeasuredWidth();
            h = view.getMeasuredHeight();

            Gravity.apply(mGravity, w, h, boundsRect, childRect);
            view.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);

        }
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (VERBOSE) {
            //Log.v(TAG, "entering onTouchEvent...");
        }

        if (mTopCard == null) {
            //if (VERBOSE) {
//                Log.v(TAG, "mTopcard was null, exiting...");
//            }
            return false;
        }
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        //Log.d("Touch Event", MotionEvent.actionToString(event.getActionMasked()) + " ");
        final int pointerIndex;
        final float x, y;
        final float dx, dy;
        switch (event.getActionMasked()) {


            case MotionEvent.ACTION_DOWN:
                mTopCard.getHitRect(childRect);

                pointerIndex = event.getActionIndex();
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                if (!childRect.contains((int) x, (int) y)) {
                    return false;
                }
                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = event.getPointerId(pointerIndex);


                float[] points = new float[]{x - mTopCard.getLeft(), y - mTopCard.getTop()};
                mTopCard.getMatrix().invert(mMatrix);
                mMatrix.mapPoints(points);
                mTopCard.setPivotX(points[0]);
                mTopCard.setPivotY(points[1]);

                break;
            case MotionEvent.ACTION_MOVE:

                pointerIndex = event.findPointerIndex(mActivePointerId);
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);
                dx = x - mLastTouchX;
                dy = y - mLastTouchY;

                if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }

                if(!mDragging) {
                    return true;
                }

                //mTopCard.setTranslationX(mTopCard.getTranslationX() + dx);
                mTopCard.setTranslationY(mTopCard.getTranslationY() + dy);

                //mTopCard.setRotation(40 * mTopCard.getTranslationX() / (getWidth() / 2.f));

                mLastTouchX = x;
                mLastTouchY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mDragging) {
                    return true;
                }

                pointerIndex = event.findPointerIndex(mActivePointerId);
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                Log.d(TAG,"pointer UP at: " + x + ", " + y);

                mDragging = false;
                mActivePointerId = INVALID_POINTER_ID;

                ValueAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mTopCard,
                        PropertyValuesHolder.ofFloat("translationX", 0),
                        PropertyValuesHolder.ofFloat("translationY", 0),
                        PropertyValuesHolder.ofFloat("pivotX", mTopCard.getWidth() / 2.f),
                        PropertyValuesHolder.ofFloat("pivotY", mTopCard.getHeight() / 2.f)
                ).setDuration(250);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.start();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);

                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = event.getX(newPointerIndex);
                    mLastTouchY = event.getY(newPointerIndex);

                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
        }

        if (VERBOSE) {
            //Log.v(TAG, "exiting onTouchEvent...");
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (VERBOSE) {
            Log.d(TAG, "entering onInterceptTouchEvent...");
        }

        if (mTopCard == null) {
            if (VERBOSE) {
                Log.v(TAG, "mTopcard was null, exiting...");
            }
            return false;
        }
        if (mGestureDetector.onTouchEvent(event)) {

            Log.d(TAG, "gestureDetector handled fling, now passing to child...");
            return true;
        }
        final int pointerIndex;
        final float x, y;
        final float dx, dy;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTopCard.getHitRect(childRect);

                pointerIndex = event.getActionIndex();
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                if (!childRect.contains((int) x, (int) y)) {
                    if (VERBOSE) {
                        Log.v(TAG, "pointer wasn't touching the card, exiting...");
                    }
                    return false;
                }

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = event.getPointerId(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = event.findPointerIndex(mActivePointerId);
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);
                if (Math.abs(x - mLastTouchX) > mTouchSlop || Math.abs(y - mLastTouchY) > mTouchSlop) {
                    float[] points = new float[]{x - mTopCard.getLeft(), y - mTopCard.getTop()};
                    mTopCard.getMatrix().invert(mMatrix);
                    mMatrix.mapPoints(points);
                    mTopCard.setPivotX(points[0]);
                    mTopCard.setPivotY(points[1]);
                    if (VERBOSE) {
                        Log.v(TAG, "action move, so passing to child, exiting onInterceptTouchEvent...");
                    }
                    return true;
                }
        }


        if (VERBOSE) {
            Log.v(TAG, "exit onInterceptTouchEvent...");
        }
        return false;
    }

    @Override
    public View getSelectedView() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSelection(int position) {
        throw new UnsupportedOperationException();
    }

    public int getGravity() {
        return mGravity;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        int viewType;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }
    }

    /**
     * class 'GestureListener'
     *
     * listener class that detects flings and handles them appropriately
     * fling left to delete
     */
    private class GestureListener extends SimpleOnGestureListener {
        private String TAG = "FlingGestureListener";
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (VERBOSE) {
                Log.v(TAG, "entering onFling...");
                Log.v("Fling", "Fling with " + velocityX + ", " + velocityY);
            }
            final View topCard = mTopCard;
            float dx = e2.getX() - e1.getX();

            /**
             * Tests for fling to the left
             *
             * does so, by testing  that both the fling and touch are above the minimum, AND
             * the X velocity is greater than the Y
             */
            if (Math.abs(dx) > mTouchSlop &&
                    Math.abs(velocityY) > Math.abs(velocityX) &&
                    Math.abs(velocityY) > mFlingSlop * 1.5) {
                float targetX = topCard.getX();
                float targetY = topCard.getY();
                long duration = 0;


                //todo: magic numbers, clean them up
                boundsRect.set(0 - topCard.getWidth() - 100, 0 - topCard.getHeight() - 100, getWidth() + 100, getHeight() + 100);



                while (boundsRect.contains((int) targetX, (int) targetY)) {
                    targetX += velocityX / 10;
                    targetY += velocityY / 10;
                    duration += 100;
                }
                duration = Math.min(500, duration); //duration is at least 500


                if (VERBOSE) {
                    Log.v(TAG,"new mTopCard at position: " + (getChildCount()-2));
                }
                mTopCard = getChildAt(getChildCount() - 2);

                if (mOffset >= mNumberOfCards) {
                    mOffset = mNumberOfCards-1;
                }

                if (VERBOSE) {
                    Log.v(TAG, "current imageModel is at position: " + mOffset);
                }
                if(mTopCard != null)
                    mTopCard.setLayerType(LAYER_TYPE_HARDWARE, null);

                if (VERBOSE) {
                    Log.v(TAG, "animating fling...");
                }
                topCard.animate()
                        .setDuration(duration)
                        .alpha(.75f)
                        .setInterpolator(new LinearInterpolator())
                        .y(targetY)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (VERBOSE) {
                                    Log.v(TAG,"animation ended, removing topCard view...");
                                }

                                ((ImageStackCursorAdapter)getAdapter()).pop();
                                removeViewInLayout(topCard);
                                mOffset++;

                                //refill ->>this may be unnecessary
                                ensureFull();
                                //mNextAdapterPosition = mNextAdapterPosition+1;


                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                onAnimationEnd(animation);
                            }
                        });



                if (VERBOSE) {
                    Log.v(TAG, "success, exiting...");
                }
                return true;
            }
                if (VERBOSE) {
                    Log.v(TAG, "fling had invalid x, y, or didn't meet the touchslop");
                }
                return false;
        }
    }

}