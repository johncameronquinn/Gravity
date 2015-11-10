package com.jokrapp.android;

import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokrapp.android.util.LogUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;



/**
 * Created by ev0x on 11/9/15.
 */
public class ServerTask {

    Bundle dataBundle;

    protected DataHandlingService mService;

    private boolean VERBOSE = true;
    private final String TAG = "ServerTask";

    public ServerTask(DataHandlingService mService, Bundle dataBundle) {
        this.dataBundle = dataBundle;
        this.mService = mService;

    }

    public Bundle getDataBundle() {
        return dataBundle;
    }

       public HttpURLConnection connectToServer(String ServerPath) throws ConnectException {
        return mService.connectToServer(ServerPath);
    }



}
