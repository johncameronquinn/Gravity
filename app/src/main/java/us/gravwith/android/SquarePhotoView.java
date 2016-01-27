package us.gravwith.android;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by caliamara on 1/26/16.
 */
public class SquarePhotoView extends PhotoView {

    public SquarePhotoView(Context context) {
        super(context);
    }

    public SquarePhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquarePhotoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getMeasuredHeight();
        setMeasuredDimension(height, height);
    }


}