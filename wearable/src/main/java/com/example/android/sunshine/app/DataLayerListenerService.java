//package com.example.android.sunshine.app;
//
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.api.ResultCallback;
//import com.google.android.gms.wearable.DataApi;
//import com.google.android.gms.wearable.DataItemBuffer;
//import com.google.android.gms.wearable.Node;
//import com.google.android.gms.wearable.NodeApi;
//import com.google.android.gms.wearable.Wearable;
//import com.google.android.gms.wearable.DataItem;
//import com.google.android.gms.wearable.DataMap;
//import com.google.android.gms.wearable.DataMapItem;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//
//import android.graphics.Bitmap;
//import android.os.Bundle;
//import android.util.Log;
//
//import com.google.android.gms.wearable.WearableListenerService;
//import com.google.android.gms.wearable.DataEventBuffer;
//import com.google.android.gms.wearable.DataEvent;
//
//import android.graphics.BitmapFactory;
//
//import com.google.android.gms.common.ConnectionResult;
//
///**
// * Created by ebal on 05/02/16.
// */
//public class DataLayerListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
//
//    public int mWeatherId;
//    public Bitmap mWeatherBitmap;
//    public String mHighTempText = "00.0";
//    public String mLowTempText = "00.0";
//    GoogleApiClient mGoogleApiClient;
//
////                mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
////                    .addConnectionCallbacks(this)
////                    .addOnConnectionFailedListener(this)
////                    .addApi(Wearable.API)
////                    .build();
////
////            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
////            if (ConnectionResult.SUCCESS == result) {
////                mGoogleApiClient.connect();
////                Log.e(LOG_TAG, "Google Play service installed " + result);
////            } else {
////                if (ConnectionResult.SERVICE_MISSING == result) {
////                    Log.e(LOG_TAG, "Google Play service not installed : service missing");
////                }
////                if (ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED == result) {
////                    Log.e(LOG_TAG, "Google Play service not installed : service update required");
////                }
////            }
//
////    @Override
////    public void onResult(DataItemBuffer dataItems) {
////        Log.e("Jerem", "onResult: ");
////        for (DataItem dataItem : dataItems) {
////            if (dataItem.getUri().getPath().compareTo(getResources().getString(R.string.wearable_data_item_path)) == 0) {
////                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
////                Double HighTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_high_temp));
////                mHighTempText = HighTemp.toString();
////                Double LowTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_low_temp));
////                mLowTempText = LowTemp.toString();
////            }
////        }
////        dataItems.release();
//////            if (isVisible() && !isInAmbientMode()) {
//////                invalidate();
//////            }
////    }
//
//    @Override
//    public void onDataChanged(DataEventBuffer dataEventBuffer) {
//        Log.e("jerem", "onDataChanged: ");
//        if (mGoogleApiClient == null) {
//
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(Wearable.API)
//                    .build();
//        }
//        if (!mGoogleApiClient.isConnected()) {
//
//            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
//            if (ConnectionResult.SUCCESS == result) {
//                mGoogleApiClient.connect();
//                Log.e("jerem", "Google Play service installed " + result);
//            } else {
//                if (ConnectionResult.SERVICE_MISSING == result) {
//                    Log.e("jerem", "Google Play service not installed : service missing");
//                }
//                if (ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED == result) {
//                    Log.e("jerem", "Google Play service not installed : service update required");
//                }
//            }
//        }
//        for (DataEvent dataEvent : dataEventBuffer) {
//            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
//                continue;
//            }
//            DataItem dataItem = dataEvent.getDataItem();
//            String path = dataItem.getUri().getPath();
//            if (!path.equals(getResources().getString(R.string.wearable_data_item_path))) {
//                continue;
//            }
//            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
//            DataMap dataMap = dataMapItem.getDataMap();
//            Double HighTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_high_temp));
//            mHighTempText = HighTemp.toString();
//            Double LowTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_low_temp));
//            mLowTempText = LowTemp.toString();
//            mWeatherId = dataMap.getInt(getResources().getString(R.string.wearable_data_item_weather_id));
//            if (mWeatherId != -1) {
//                mWeatherBitmap = BitmapFactory.decodeResource(SunshineWatchFaceService.this.getResources()
//                        , Utility.getArtResourceForWeatherCondition(mWeatherId));
//            }
////                invalidate();
//    }
//
//    }
//
//    @Override
//    public void onConnected(Bundle bundle) {
//        Wearable.DataApi.addListener(mGoogleApiClient, this);
//        Log.e("jerem", "GoogleApiClient CONNECTED");
//    }
//
//
//    @Override
//    public void onConnectionSuspended(int i) {
//        Log.e("jerem", "GoogleApiClient Connection Suspended");
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Log.e("jerem", "GoogleApiClient Connection Failed");
//    }
//
//    @Override
//    public void onPeerConnected(Node node) {
//        Log.e("jerem", "onPeerConnected: ");
//
//    }
//
//    @Override
//    public void onPeerDisconnected(Node node) {
//        Log.e("jerem", "onPeerDisconnected: ");
//
//    }
//}
