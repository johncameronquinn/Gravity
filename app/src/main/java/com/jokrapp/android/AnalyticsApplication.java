/*
 * Copyright (c) 2015. John C Quinn, All Rights Reserved.
 */

package com.jokrapp.android;

import android.app.Application;
import android.util.Log;

import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;


/**
 * Created by ev0x on 9/29/15.
 */
public class AnalyticsApplication extends Application {
    private MobileAnalyticsManager mTracker;

    /**
     * Gets the default for this {@link Application}
     * @return tracker
     */
    synchronized public MobileAnalyticsManager getAnalyticsManager() {

        if (mTracker ==null) {
            try {
                mTracker = MobileAnalyticsManager.getOrCreateInstance(
                        this.getApplicationContext(),
                        "ce40f7ab230f4e75a16e5fcbbf372515", //Amazon Mobile Analytics App ID
                        "us-east-1:4a6fba12-a772-4939-b0c2-bd11d95f9f5c" //Amazon Cognito Identity Pool ID

                );

                if (Constants.LOGV) {
                    Log.v("AnalyticsApplication","Successfully initialized analytics manager");
                }
            } catch (InitializationException ex) {
                Log.e(this.getClass().getName(), "Failed to initialize Amazon Mobile Analytics", ex);
                throw new RuntimeException("Failed to initialize analytics...");
            }
        }

        return mTracker;
    }


}
