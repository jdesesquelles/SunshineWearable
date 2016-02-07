/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.wearable.DataItemBuffer;
import android.util.Log;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {
    private String LOG_TAG = "jerem";
    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements NodeApi.NodeListener, DataApi.DataListener, ResultCallback<DataItemBuffer>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        GoogleApiClient mGoogleApiClient;
        public Bitmap mWeatherBitmap;
        public int mWeatherId;
        public String mHighTempText = "00.0";
        public String mLowTempText = "00.0";
        DataEventBuffer mDataEventBuffer;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
                mDate = Calendar.getInstance().getTime();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        private static final float STROKE_WIDTH = 0.75f;
        private float mTextSpacingHeight;

        Paint mBackgroundPaint;
        Paint mTextTimePaint;
        Paint mTextDatePaint;
        Paint mTextLowTempPaint;
        Paint mTextHighTempPaint;
        Paint mStrikePaint;
        Paint mWeatherPaint;

        boolean mAmbient;
        DateFormat mDateFormat = new SimpleDateFormat("EEE, MMM d yyyy ");
        Time mTime;
        Date mDate;
        int mTapCount;

        float mXOffset;
        float mYOffset;

        String textTime;
        String textDate;
        /**
         * Positioning
         **/
        // Time
        Float textTimeX;
        // Date
        Float textDateXRound;
        Float textDateXSquare;
        Float textDateX;
        Float textDateY;
        // Stroke
        Float strokeStartX;
        Float strokeStartY;
        Float strokeStopX;
        Float strokeStopY;
        // Bitmap
        Float bitmapLeft;
        Float bitmapTop;
        // High temperature
        Float textHighX;
        Float textHighY;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
                canvas.drawBitmap(mWeatherBitmap, bitmapLeft, bitmapTop, null);
            }
            mTime.setToNow();
            textTime = String.format("%d:%02d", mTime.hour, mTime.minute);
            if (textTime.length() == 4) {
                // Ensuring hour is two caracters long
                textTime = "0" + textTime;
            }
            textDate = mDateFormat.format(mDate);
            canvas.drawText(textTime, textTimeX, mYOffset, mTextTimePaint);
            canvas.drawText(textDate.toUpperCase(), textDateX, textDateY, mTextDatePaint);
            canvas.drawLine(strokeStartX, strokeStartY, strokeStopX, strokeStopY, mStrikePaint);
            canvas.drawText(mHighTempText + " - " + mLowTempText, textHighX, textHighY, mTextHighTempPaint);
        }

        private void setGoogleApiClient() {
            //TODO
            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
            if (ConnectionResult.SUCCESS == result) {
                mGoogleApiClient.connect();
//                onDataChanged(mDataEventBuffer);
                Log.e(LOG_TAG, "Google Play service installed " + result);
            } else {
                if (ConnectionResult.SERVICE_MISSING == result) {
                    Log.e(LOG_TAG, "Google Play service not installed : service missing");
                }
                if (ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED == result) {
                    Log.e(LOG_TAG, "Google Play service not installed : service update required");
                }
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setGoogleApiClient();
            mTime = new Time();
            mDate = Calendar.getInstance().getTime();
            mWeatherPaint = new Paint();
            mWeatherBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_clear);
            textTime = String.format("%d:%02d", mTime.hour, mTime.minute);
            textDate = mDateFormat.format(mDate);
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFaceService.this.getResources();
            mTextSpacingHeight = resources.getDimension(R.dimen.interactive_text_size);
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));
            mTextTimePaint = new Paint();
            mTextTimePaint = createTextPaint(resources.getColor(R.color.time_text));
            mTextDatePaint = new Paint();
            mTextDatePaint = createTextPaint(resources.getColor(R.color.date_text));
            mTextHighTempPaint = new Paint();
            mTextHighTempPaint = createTextPaint(resources.getColor(R.color.high_temp_text));
            mTextLowTempPaint = new Paint();
            mTextLowTempPaint = createTextPaint(resources.getColor(R.color.low_temp_text));
            mStrikePaint = new Paint();
            mStrikePaint.setColor(resources.getColor(R.color.date_text));
            TypedValue strokeWidth = new TypedValue();
            getResources().getValue(R.dimen.stroke_width, strokeWidth, true);
            mStrikePaint.setStrokeWidth(strokeWidth.getFloat());
            mStrikePaint.setAntiAlias(true);
            mStrikePaint.setStrokeCap(Paint.Cap.SQUARE);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset_square);
            mYOffset = resources.getDimension(isRound
                    ? R.dimen.digital_y_offset_round : R.dimen.digital_y_offset_square);
            float textTimeSize = resources.getDimension(isRound
                    ? R.dimen.digital_time_text_size_round : R.dimen.digital_time_text_size);
            float textDateSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            float textLowTempSize = resources.getDimension(isRound
                    ? R.dimen.digital_temp_text_size_round : R.dimen.digital_temp_text_size);
            float textHighTempSize = resources.getDimension(isRound
                    ? R.dimen.digital_temp_text_size_round : R.dimen.digital_temp_text_size);

            mTextTimePaint.setTextSize(textTimeSize);
            mTextDatePaint.setTextSize(textDateSize);
            mTextLowTempPaint.setTextSize(textLowTempSize);
            mTextHighTempPaint.setTextSize(textHighTempSize);
            textTimeX = mXOffset + (mTextTimePaint.measureText("0000") / 4);
            // Date
            textDateXRound = mXOffset + 0.3f * (mTextDatePaint.measureText(textDate));
            textDateXSquare = mXOffset + 0.6f * (mTextDatePaint.measureText(textDate));
            textDateY = mYOffset + mTextSpacingHeight;
            // Round watch
            if (isRound) {
                textDateX = textDateXRound;
                strokeStartX = mXOffset + (0.7f * mTextTimePaint.measureText(textTime));
                strokeStartY = mYOffset + (mTextSpacingHeight * 1.30f);
                strokeStopX = mXOffset + (1.1f * mTextTimePaint.measureText(textTime));
                strokeStopY = strokeStartY;
                textHighX = mXOffset + 0.3f * (mTextDatePaint.measureText("00.0 - 00.0"));
//                textDateX;
                textHighY = mYOffset + (mTextSpacingHeight * 2.5f);
                bitmapLeft = strokeStartX;
                bitmapTop = strokeStopY + (mTextSpacingHeight * 0.5f) + mWeatherBitmap.getHeight() / 4;
            } else {
                // Square watch
                textDateX = textDateXSquare;
                strokeStartX = mXOffset + (0.8f * mTextTimePaint.measureText(textTime));
                strokeStartY = mYOffset + (mTextSpacingHeight * 1.3f);
                strokeStopX = mXOffset + (1.2f * mTextTimePaint.measureText(textTime));
                strokeStopY = strokeStartY;
                bitmapLeft = strokeStartX;
                bitmapTop = strokeStopY + (mTextSpacingHeight * 0.5f) + mWeatherBitmap.getHeight() / 4;;
                textHighX = textDateX * 0.9f;
                textHighY = mYOffset + (mTextSpacingHeight * 2.2f);
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextTimePaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineWatchFaceService.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onResult(DataItemBuffer dataItems) {
            Log.e("Jerem", "onResult: ");
            for (DataItem dataItem:dataItems){
                if (dataItem.getUri().getPath().compareTo(getResources().getString(R.string.wearable_data_item_path)) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    Double HighTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_high_temp));
                    mHighTempText = HighTemp.toString();
                    Double LowTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_low_temp));
                    mLowTempText = LowTemp.toString();
                }
            }
            dataItems.release();
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            mDataEventBuffer = dataEventBuffer;
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                    continue;
                }
                DataItem dataItem = dataEvent.getDataItem();
                String path = dataItem.getUri().getPath();
                if (!path.equals(getResources().getString(R.string.wearable_data_item_path))) {
                    continue;
                }
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap dataMap = dataMapItem.getDataMap();
                Double HighTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_high_temp));
                mHighTempText = HighTemp.toString();
                Double LowTemp = dataMap.getDouble(getResources().getString(R.string.wearable_data_item_low_temp));
                mLowTempText = LowTemp.toString();
                mWeatherId = dataMap.getInt(getResources().getString(R.string.wearable_data_item_weather_id));
                if(mWeatherId != -1){
                    mWeatherBitmap = BitmapFactory.decodeResource(SunshineWatchFaceService.this.getResources()
                            ,Utility.getArtResourceForWeatherCondition(mWeatherId));
                }
                invalidate();
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            Log.e("SunshineWatchFace", "GoogleApiClient CONNECTED");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e("SunshineWatchFace", "GoogleApiClient Connection Suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e("SunshineWatchFace", "GoogleApiClient Connection Failed");
        }

        @Override
        public void onPeerConnected(Node node) {
            if (!mGoogleApiClient.isConnected()) {
                setGoogleApiClient();
//                mGoogleApiClient.connect();
            }
            Log.e("jerem", "onPeerConnected: ");
        }

        @Override
        public void onPeerDisconnected(Node node) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }

            Log.e("jerem", "onPeerDisconnected: ");

        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


}
