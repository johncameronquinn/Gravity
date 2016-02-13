package us.gravwith.android.view;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * View pager used for a finite, low number of pages, where there is no need for
 * optimization.
 */
public class StaticViewPager extends ViewPager {

    /**
     * Initialize the view.
     *
     * @param context
     *            The application context.
     */
    public StaticViewPager(final Context context) {
        super(context);
    }

    /**
     * Initialize the view.
     *
     * @param context
     *            The application context.
     * @param attrs
     *            The requested attributes.
     */
    public StaticViewPager(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Make sure all are loaded at once
        final int childrenCount = getChildCount();
        setOffscreenPageLimit(childrenCount - 1);

        // Attach the adapter
        setAdapter(new PagerAdapter() {

            @Override
            public Object instantiateItem(final ViewGroup container, final int position) {
                return container.getChildAt(position);
            }

            @Override
            public boolean isViewFromObject(final View arg0, final Object arg1) {
                return arg0 == arg1;

            }

            @Override
            public int getCount() {
                return childrenCount;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                //super.destroyItem(container, position, object);
            }
        });
    }

}
