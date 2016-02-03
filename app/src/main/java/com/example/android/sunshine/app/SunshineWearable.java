package com.example.android.sunshine.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by ebal on 11/11/15.
 */
public class SunshineWearable implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult>{

    private static String LOG_TAG = SunshineWearable.class.getSimpleName();
    private  GoogleApiClient mGoogleApiClient;
    private  Context mContext;
    Bitmap mWeatherBitmap;
    int mWeatherId;
    Double mHighTemp;
    Double mLowTemp;

    public void connectWearable(final Context context) {
        Log.e(LOG_TAG, "Entering connectWearable");
//        mGoogleApiClient = new GoogleApiClient.Builder(context)
//                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
//                    @Override
//                    public void onConnected(Bundle bundle) {
//                        Log.e(LOG_TAG, "Connection to the wearable succeeded");
//                    }
//
//                    @Override
//                    public void onConnectionSuspended(int i) {
//                        Log.e(LOG_TAG, "Connection to the wearable suspended");
//                    }
//                })
//                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
//                    @Override
//                    public void onConnectionFailed(ConnectionResult connectionResult) {
//                        Log.e(LOG_TAG, "Connection to the wearable failed");
//                    }
//                })
//                .addApi(Wearable.API)
//                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
//        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Log.e(LOG_TAG, "Connection to the wearable succeeded");
        updateWearable(mHighTemp, mLowTemp, mWeatherId);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG, "Connection to the wearable suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Connection to the wearable failed");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.e(LOG_TAG, "on data changed");

    }

    public void updateWearable(Double high, Double low, Integer weatherId){
        Log.e(LOG_TAG, "Entering updateWearable");
//        connectWearable(context);
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(mContext.getResources().getString(R.string.wearable_data_item_path));
        putDataMapRequest.getDataMap().putDouble(mContext.getResources().getString(R.string.wearable_data_item_high_temp), high);
        putDataMapRequest.getDataMap().putDouble(mContext.getResources().getString(R.string.wearable_data_item_low_temp), low);
        putDataMapRequest.getDataMap().putInt(mContext.getResources().getString(R.string.wearable_data_item_weather_id), weatherId);
        Log.e(LOG_TAG, "put high temp : " + high.toString() + " - low temp : " + low.toString());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        // non blocking request on the UI thread
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(this);
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess()) {
            Log.i("SUNSHINE", "Data update successfully sync with the wearable");
        } else {
            Log.i("SUNSHINE", "Syncing with the wearable failed");
        }
                mGoogleApiClient.disconnect();

    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    public SunshineWearable(Context context, Double high, Double low, Integer weatherId){
        mContext = context;
        mHighTemp =  high;
        mLowTemp = low;
        mWeatherId = weatherId;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
//        mGoogleApiClient.connect();

        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context.getApplicationContext());
        if (ConnectionResult.SUCCESS == result) {
            mGoogleApiClient.connect();
            Log.e(LOG_TAG, "Jerem Google Play service installed " + result);
        } else {
            //Show appropriate dialog
            if (ConnectionResult.SERVICE_MISSING == result) {
                Log.e(LOG_TAG, "Jerem Google Play service not installed : service missing");
            }
            if (ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED == result) {
                Log.e(LOG_TAG, "Jerem Google Play service not installed : service update required");
            }
        }
        Log.e(LOG_TAG, "Connection to the wearable launched");
    }

}
