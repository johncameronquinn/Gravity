package com.jokrapp.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.FileNotFoundException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeveloperFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class DeveloperFragment extends Fragment implements View.OnClickListener {

    private final String TAG = "DeveloperFragment";

    private OnFragmentInteractionListener mListener;

    public DeveloperFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_developer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.devbutton).setOnClickListener(this);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * method 'onActivityResult'
     *
     * called when the user selects an image to be loaded into live, or cancels
     *
     * @param requestCode code supplied to the activity
     * @param resultCode result be it cancel or otherwise
     * @param data the uri returned
     */
    private final int SELECT_PICTURE = 1;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                Bitmap b = null;
                try {
                    b = BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(selectedImageUri));
                    ((ImageView)getView().findViewById(R.id.imageView)).setImageBitmap(b);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "error decoding stream from returned URI...");
                }

            } else {

            }
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.devbutton:
                Log.i(TAG, "opening gallery");
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
                break;
        }
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
        public void onDeveloperInteraction(int request,Uri uri);
    }

}
