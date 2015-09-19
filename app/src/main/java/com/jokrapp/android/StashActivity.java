/*
 * Copyright (c) 2015. John C Quinn, All Rights Reserved.
 */

package com.jokrapp.android;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;

public class StashActivity extends Activity implements StashGalleryFragment.OnFragmentInteractionListener,
        StashLiveSettingsFragment.OnFragmentInteractionListener,
        StashLocalSettingsFragment.OnFragmentInteractionListener {

    static final String STARTING_PAGE_POSITION_KEY = "startkey";
    private CustomViewPager pager;
    private  StashAdapter adapter;
    private final String TAG = "StashActivity";
    static final boolean VERBOSE = true;


    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onCreate...");

        setContentView(R.layout.activity_main);
        int startingPosition = getIntent().getExtras().getInt(STARTING_PAGE_POSITION_KEY, 1);
        if (VERBOSE) Log.v(TAG,"StashActivity created with postion" + startingPosition + " provided.");
        adapter = new StashAdapter(getFragmentManager());
        pager = (CustomViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setCurrentItem(startingPosition);

        preferences = getSharedPreferences("Settings",MODE_PRIVATE);
        editor = preferences.edit();
        if (VERBOSE) Log.v(TAG,"exiting onCreate...");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void saveSetting(String key, String value) {
        if (VERBOSE) {
            Log.v(TAG, "entering saveSettings...");
            Log.v(TAG, "printing setting: " + key + ", " + value);
            Toast.makeText(this,"Saving setting: " + key,Toast.LENGTH_SHORT).show();
        }
        editor.putString(key,value).apply();
        if (VERBOSE) Log.v(TAG,"exiting saveSettings...");
    }

    public String loadSetting(String key) {
        return preferences.getString(key,"");
    }





    private class StashAdapter extends FragmentPagerAdapter {
        private final int STASHACTIVITY_PAGE_COUNT = 3;
        private final int LOCAL_SETTINGS_POSTION = 0;
        private final int GALLERY_POSTION = 1;
        private final int LIVE_SETTINGS_POSTION = 2;

        public StashAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case LOCAL_SETTINGS_POSTION:
                    return StashLocalSettingsFragment.newInstance(" "," ");
                case GALLERY_POSTION:
                    return StashGalleryFragment.newInstance(" "," ");
                case LIVE_SETTINGS_POSTION:
                    return StashLiveSettingsFragment.newInstance(" "," ");
                default:
                    Log.e(TAG,"invalid fragment position loaded...");
                    return null;
            }
        }

        @Override
        public int getCount() {
            return STASHACTIVITY_PAGE_COUNT;
        }

        /**
         * method 'getPageTitle'
         *
         * called by the PagerTitleStrip within the ViewPager
         * used to title the various tabs of the ViewPager
         *
         * @param position of the page who's title needs to be returned
         * @return String of the title
         */
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case LOCAL_SETTINGS_POSTION:
                    return "Local Settings";
                case GALLERY_POSTION:
                    return "Stash";
                case LIVE_SETTINGS_POSTION:
                    return "Live Settings";
                default:
                    return null;
            }
        }
    }


    }

