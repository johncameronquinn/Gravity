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


public class CursorPagerAdapter<F extends Fragment> extends FragmentStatePagerAdapter {
    private final Class<F> fragmentClass;
    private final String[] projection;
    private Cursor cursor;
    private LiveThreadFragment mThreadFragment;

    public CursorPagerAdapter(FragmentManager fm, Class<F> fragmentClass, String[] projection, Cursor cursor) {
        super(fm);
        this.fragmentClass = fragmentClass;
        this.projection = projection;
        this.cursor = cursor;
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