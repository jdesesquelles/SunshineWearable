package com.example.android.sunshine.app;

import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.example.android.sunshine.app.data.WeatherContract;
import android.net.Uri;
import android.database.Cursor;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.common.api.ResultCallback;
import android.text.format.Time;

/**
 * Created by ebal on 05/02/16.
 */
public class DataLayerListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public int mWeatherId;
    public Bitmap mWeatherBitmap;
    public String mHighTempText = "00.0";
    public String mLowTempText = "00.0";
    GoogleApiClient mGoogleApiClient;
    ContentObserver mContentObserver;
    Uri mWeatherForLocationUri;
    Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.e("jerem", "onPeerConnected: ");
        // Connect GoogleApiClient whenever a watch is paired with the device
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("jerem", "GoogleApiClient CONNECTED");
        // Registering listener, retrieving the current weather Data and updating the wearable
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        getCurrentWeatherData();
        mContentObserver = new weatherContentObserver(mHandler);
        RegisterWeatherContentObserver();
    }

    private void getCurrentWeatherData() {
        final String[] WATCHFACE_COLUMNS = {
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
        };

        final int INDEX_WEATHER_ID = 0;
        final int INDEX_MAX_TEMP = 1;
        final int INDEX_MIN_TEMP = 2;
        String location = Utility.getPreferredLocation(getApplicationContext());
        mWeatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                location, System.currentTimeMillis());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Cursor cursor = getApplicationContext().getContentResolver().query(mWeatherForLocationUri, WATCHFACE_COLUMNS, null,
                null, sortOrder);

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);
            updateWearable(high, low, weatherId);
        }
        cursor.close();

    }

    private void RegisterWeatherContentObserver() {
        getApplicationContext().getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, mContentObserver);
    }

    private void UnregisterWeatherContentObserver() {
        getApplicationContext().getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("jerem", "GoogleApiClient Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("jerem", "GoogleApiClient Connection Failed");
    }

    // Syncing data with the wearable
    private void updateWearable(double high, double low, int weatherId) {
        Log.e("jerem", "Entering updateWeather");
        Time timeStamp = new Time();
        timeStamp.setToNow();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(getApplicationContext().getResources().getString(R.string.wearable_data_item_path));
        putDataMapRequest.getDataMap().putDouble(getApplicationContext().getResources().getString(R.string.wearable_data_item_high_temp), high);
        putDataMapRequest.getDataMap().putDouble(getApplicationContext().getResources().getString(R.string.wearable_data_item_low_temp), low);
        putDataMapRequest.getDataMap().putInt(getApplicationContext().getResources().getString(R.string.wearable_data_item_weather_id), weatherId);
        putDataMapRequest.getDataMap().putString("TimeStamp", timeStamp.toString());

        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        //TODO
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
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.e("jerem", "onPeerDisconnected: ");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        UnregisterWeatherContentObserver();
        mGoogleApiClient.disconnect();
    }

    // Inner class for Weather Content Observer
    private class weatherContentObserver extends ContentObserver {

        weatherContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            getCurrentWeatherData();
            // do s.th.
            // depending on the handler you might be on the UI
            // thread, so be cautious!
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        getCurrentWeatherData();
    }
}
