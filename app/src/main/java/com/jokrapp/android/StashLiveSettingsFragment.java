/*
 * Copyright (c) 2015. John C Quinn, All Rights Reserved.
 */

package com.jokrapp.android;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Map;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StashLiveSettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StashLiveSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StashLiveSettingsFragment extends Fragment implements TextView.OnEditorActionListener{

    private final String TAG = "StashLiveFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    static final String LIVE_NAME_KEY = "namekey";
    private Set<String> settings;
    private final boolean VERBOSE = StashActivity.VERBOSE;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StashLiveSettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StashLiveSettingsFragment newInstance(String param1, String param2) {
        StashLiveSettingsFragment fragment = new StashLiveSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public StashLiveSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERBOSE) Log.v(TAG,"entering onCreate...");

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        if (VERBOSE) Log.v(TAG,"exiting onCreate...");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.v(TAG,"entering onCreateView...");
        // Inflate the layout for this fragment

        if (VERBOSE) Log.v(TAG,"exiting onCreateView...");
        return inflater.inflate(R.layout.fragment_stash_live_settings, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView liveNameView = (TextView)view.findViewById(R.id.stash_live_settings_name);
        liveNameView.setText(mListener.loadSetting(LIVE_NAME_KEY));
        liveNameView.setOnEditorActionListener(this);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (StashActivity.VERBOSE) Log.v(TAG,"entering onEditorAction...");


        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (StashActivity.VERBOSE) Log.v(TAG, v.toString());

            switch (v.getId()) {
                case R.id.stash_live_settings_name:
                    mListener.saveSetting(LIVE_NAME_KEY,v.getText().toString());
                    break;
            }
            if (StashActivity.VERBOSE) Log.v(TAG,"exiting onEditorAction...");

            return true;
        }

        if (StashActivity.VERBOSE) Log.v(TAG,"exiting onEditorAction...");
        return false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
        public void saveSetting(String key, String value);
        public String loadSetting(String key);
    }

}
