package com.jokrapp.android.dev;

import android.app.Activity;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.jokrapp.android.FireFlyContentProvider;
import com.jokrapp.android.R;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeveloperFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class DeveloperFragment extends Fragment implements View.OnClickListener {

    private final String TAG = "DeveloperFragment";

    AmazonS3Client s3Client;

    HandlerThread handlerThread = new HandlerThread("DevpanelHandler");
    Handler networkHandler;


    private OnFragmentInteractionListener mListener;

    public DeveloperFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handlerThread.start();
        networkHandler = new Handler(handlerThread.getLooper());
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials("AKIAIZ42NH277ZC764XQ","pMYCGMq+boy6858OfITL4CTXWgdkVbVreyROHckG"));
        s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));

        networkHandler.post(new ListBucketsRunnable());
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

        view.findViewById(R.id.devupload).setOnClickListener(this);
        view.findViewById(R.id.devdownload).setOnClickListener(this);
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
                Log.d(TAG,"Activity resulted in picture");

                networkHandler.post(new UploadImageRunnable(data.getData()));

            }
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.devupload:
                Log.i(TAG, "opening gallery to upload image");
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
                break;

            case R.id.devdownload:
                Log.i(TAG, "downloading image");
                networkHandler.post(new DownloadImageRunnable());
                break;
        }
    }

    public class ListBucketsRunnable implements Runnable {
        public void run() {
            List<Bucket> buckets = s3Client.listBuckets();
            for (Bucket b : buckets) {
                Log.i(TAG,b.getName());
            }
        }
    }

    private class UploadImageRunnable implements Runnable {
        Uri selectedImageUri;
        private UploadImageRunnable(Uri uri){
            selectedImageUri = uri;
        }
        @Override
        public void run() {
            Log.d(TAG,"entering UploadImageRunnable...");

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            String[] proj = { MediaStore.Images.Media.DATA };
            CursorLoader loader = new CursorLoader(getActivity(), selectedImageUri, proj, null, null, null);
            Cursor cursor = loader.loadInBackground();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String result = cursor.getString(column_index);
            cursor.close();


            TransferUtility transferUtility = new TransferUtility(s3Client, getActivity());

            TransferObserver observer = transferUtility.upload(
                    "launch-zone",     /* The bucket to upload to */
                    "local/2",
                    new File(result)/* The key for the uploaded object */
                    /* The file where the data to upload exists */
            );

            final ProgressBar progressBar = (ProgressBar)getActivity().findViewById(R.id.devprogress);

            observer.setTransferListener(new TransferListener(){

                @Override
                public void onStateChanged(int id, TransferState state) {
                    Log.d(TAG,"entering onStateChanged...");
                    switch (state) {

                        case COMPLETED:

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(),"Yay!",Toast.LENGTH_SHORT).show();
                                    progressBar.setProgress(0);
                                }
                            });
                            break;
                    }

                    // do something
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    Log.d(TAG,"entering onProgressChanged...");
                    int percentage = (int) (bytesCurrent/bytesTotal * 100);
                    progressBar.setProgress(percentage);
                    //Display percentage transfered to user
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG,"entering onError...",ex);
                    // do something
                }

            });


        }
    }

    private class DownloadImageRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "entering DownloadImageRunnable...");

            S3Object object;

            GetObjectRequest getObjectRequest = new GetObjectRequest("launch-zone","image.jpg");

            try {
                object = s3Client.getObject(getObjectRequest);
            } catch (AmazonServiceException ase) {
                System.out.println("Error Message:    " + ase.getMessage());
                System.out.println("HTTP Status Code: " + ase.getStatusCode());
                System.out.println("AWS Error Code:   " + ase.getErrorCode());
                System.out.println("Error Type:       " + ase.getErrorType());
                System.out.println("Request ID:       " + ase.getRequestId());
                Log.e(TAG, "error, quitting... ", ase);


                return;
            } catch (AmazonClientException ace) {
                System.out.println("Error Message: " + ace.getMessage());
                Log.e(TAG,"error, quitting...",ace);

                return;
            }

            Log.d(TAG, "S3 Object received: " + object.getObjectMetadata().getETag());

            OutputStream os;
            S3ObjectInputStream is;
            try {
                /*
                 * Lets the ContentProvider handle where to store the image,
                 * opens a stream from the amazon object to the output provided
                 */
                Uri uri = Uri.withAppendedPath(FireFlyContentProvider
                        .CONTENT_URI_LIVE, "1");
                is = object.getObjectContent();
                os = getActivity().getContentResolver().openOutputStream(uri);
                org.apache.commons.compress.utils.IOUtils.copy(is,os);

            } catch (IOException e) {
                Log.e(TAG,"error retrieving content and saving to file path",e);
            }



            Log.d(TAG,"exiting DownloadImageRunnable...");
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
        void onDeveloperInteraction(int request, Uri uri);
    }

}
