//
// Copyright 2015 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.4
//
package com.amazonaws.mobile;

import com.amazonaws.regions.Regions;

/**
 * This class defines constants for the developer's resource
 * identifiers and API keys. It should be kept private.
 */
public class AWSConfiguration {

    // AWS MobileHub user agent string
    public static final String AWS_MOBILEHUB_USER_AGENT =
        "MobileHub 3f9cd5b8-f442-43ef-b39a-fc9388479351 aws-my-sample-app-android-v0.4";
    // AMAZON COGNITO
    public static final Regions AMAZON_COGNITO_REGION =
        Regions.US_EAST_1;
    public static final String  AMAZON_COGNITO_IDENTITY_POOL_ID =
        "us-east-1:7e122fe9-aee8-4536-b675-f2f4e882b724";
    // Custom Developer Provided Authentication ID
    public static final String DEVELOPER_AUTHENTICATION_PROVIDER_ID =
        "gravity.johnquinn.com";
    // Developer Authentication - URL for Create New Account
    public static final String DEVELOPER_AUTHENTICATION_CREATE_ACCOUNT_URL =
        "aws.amazon.com";
    // Developer Authentication - URL for Forgot Password
    public static final String DEVELOPER_AUTHENTICATION_FORGOT_PASSWORD_URL =
        "aws.amazon.com";
    // Account ID
    public static final String DEVELOPER_AUTHENTICATION_ACCOUNT_ID =
        "581398785260";
    public static String DEVELOPER_AUTHENTICATION_DISPLAY_NAME = "Custom";
    // AMAZON MOBILE ANALYTICS
    public static final String  AMAZON_MOBILE_ANALYTICS_APP_ID =
        "e012c9fd5899442dbfe80b4bc0816405";
    // Amazon Mobile Analytics region
    public static final Regions AMAZON_MOBILE_ANALYTICS_REGION = Regions.US_EAST_1;
    // GOOGLE CLOUD MESSAGING API KEY
    public static final String GOOGLE_CLOUD_MESSAGING_API_KEY =
        "AIzaSyDbUSb5tkJZCKD2_S34V6-V_Ja67G9B0-Y";
    // GOOGLE CLOUD MESSAGING SENDER ID
    public static final String GOOGLE_CLOUD_MESSAGING_SENDER_ID =
        "625139449339";

    // SNS PLATFORM APPLICATION ARN
    public static final String AMAZON_SNS_PLATFORM_APPLICATION_ARN =
        "arn:aws:sns:us-east-1:581398785260:app/GCM/gravity_MOBILEHUB_735100335";
    // SNS DEFAULT TOPIC ARN
    public static final String AMAZON_SNS_DEFAULT_TOPIC_ARN =
        "arn:aws:sns:us-east-1:581398785260:gravity_alldevices_MOBILEHUB_735100335";
    // SNS PLATFORM TOPIC ARNS
    public static final String[] AMAZON_SNS_TOPIC_ARNS =
        {};
    public static final String AMAZON_CONTENT_DELIVERY_S3_BUCKET =
        "gravity-contentdelivery-mobilehub-735100335";
}
