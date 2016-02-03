package com.example.android.sunshine.app.wearable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import android.content.Context;

import java.io.ByteArrayOutputStream;

/**
 * Created by ebal on 16/01/16.
 */
public class SunshineWearable implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = "SunshineWearable";
    private final GoogleApiClient mGoogleApiClient;
    private double mWearableHighTemp;
    private double mWearableLowTemp;
    private int mWearableWeatherId;
    private Context mContext;

    public SunshineWearable(Context context) {
        mContext = context;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.e(LOG_TAG, "Connection to googleApiClient succeeded");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.e(LOG_TAG, "Connection to the wearable suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.e(LOG_TAG, "Connection to the wearable failed");
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("jerem", "onConnected: ");
        putData();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void updateWearable(double high, double low, int weatherId) {
        Log.e(LOG_TAG, "Entering updateWeather");
//        mGoogleApiClient.connect();
        mWearableHighTemp = high;
        mWearableLowTemp = low;
        mWearableWeatherId = weatherId;
    }
    private void putData() {
        Log.e("jerem", "putData: ");
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(mContext.getResources().getString(R.string.wearable_data_item_path));
        putDataMapRequest.getDataMap().putDouble(mContext.getResources().getString(R.string.wearable_data_item_high_temp), mWearableHighTemp);
        putDataMapRequest.getDataMap().putDouble(mContext.getResources().getString(R.string.wearable_data_item_low_temp), mWearableLowTemp);

        int artResourceForWeatherCondition = Utility.getArtResourceForWeatherCondition(mWearableWeatherId);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), artResourceForWeatherCondition);
        Asset asset = createAssetFromBitmap(bitmap);
        if (asset != null) {
            putDataMapRequest.getDataMap().putAsset(mContext.getResources().getString(R.string.wearable_data_item_asset), asset);
        }

        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.i("SUNSHINE", "Data update successfully sync with the wearable");
                } else {
                    Log.i("SUNSHINE", "Syncing with the wearable failed");
                }
            }
        });

//        mGoogleApiClient.disconnect();

    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
