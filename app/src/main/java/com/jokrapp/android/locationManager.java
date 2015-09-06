package com.jokrapp.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;


/**
 * Author/Copyright John C. Quinn All Rights Reserved.
 * date 4/26/2015
 *
 * service 'locationManager'
 *
 * this ongoing service manages the user's current location based on all factors
 * it is binded to the imageRequest service, and will continually update the location
 * variable which will be stored there
 *
 * it is a service that immediately launches when the app is launched, and will cause a small
 * wait until it successfully establishes connection and recieves a location value
 * this request will come in the form of an intent
 */
public class locationManager extends IntentService {

    public locationManager() {
        super("locationManager");
    }

    /**
     * this method represents the initial request to receive the current location that will be
     * made as soon as the app launches.
     *
     * @param intent
     */
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
