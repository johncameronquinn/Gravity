package us.gravwith.android;

/**
 * Created by ev0xon 10 /3/15.
 */

import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;
import android.widget.TextView;


public class CursorPagerAdapter<F extends Fragment> extends FragmentStatePagerAdapter {
    private final Class<F> fragmentClass;
    private final String[] projection;
    private Cursor cursor;
    private LiveThreadFragment mThreadFragment;

    private TextView uniqueView;
    private TextView replyView;
    private TextView timeView;
    private TextView titleView;

    public CursorPagerAdapter(FragmentManager fm, Class<F> fragmentClass, String[] projection, Cursor cursor) {
        super(fm);
        this.fragmentClass = fragmentClass;
        this.projection = projection;
        this.cursor = cursor;
    }

    public void setDisplayViews(TextView titleView, TextView uniqView, TextView replyView, TextView timeView) {
        this.titleView = titleView;
        uniqueView = uniqView;
        this.replyView = replyView;
        this.timeView = timeView;
    }

    public void updateViews() {
        if (uniqueView != null) {

            if (mThreadFragment == null) {
                titleView.setText("");
                uniqueView.setText("");
                timeView.setText("");
                replyView.setText("");
            } else {
                titleView.setText(mThreadFragment.getTitle());
                uniqueView.setText(mThreadFragment.getUniqueCount());
                timeView.setText(mThreadFragment.getRelativeTime());
                replyView.setText(mThreadFragment.getReplyCount());
            }
        }
    }


    @Override
    public F getItem(int position) {
        if (cursor == null) // shouldn't happen
            return null;

        if (cursor.getCount()==0)
            return null;

        cursor.moveToPosition(position);
        F frag;
        try {
            frag = fragmentClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        Bundle args = new Bundle();
        for (int i = 0; i < projection.length; ++i) {
                args.putString(projection[i], cursor.getString(i));
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public int getCount() {
        if (cursor == null)
            return 0;
        else
            return cursor.getCount();
    }

    public void swapCursor(Cursor c) {
        if (cursor == c)
            return;

        this.cursor = c;
        notifyDataSetChanged();
    }

    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        mThreadFragment = (LiveThreadFragment)object;
    }

    public LiveThreadFragment getCurrentFragment() {
        return mThreadFragment;
    }

}