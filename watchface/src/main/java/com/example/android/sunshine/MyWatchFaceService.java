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

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Watch Face for sunshine weather app
 */

public class MyWatchFaceService extends CanvasWatchFaceService {

    //TypeFace required for text
    private static final Typeface MY_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface MY_BOAD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        //Initilizing GoogleApiClient
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MyWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();


        /**
         * Handles time zone and locale changes.
         */
        final BroadcastReceiver broadcastReceiverTimeZOne = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };

        /**
         * Unregistering an unregistered receiver throws an exception. Keep track of the
         * registration state to prevent that.
         */
        boolean isTimeZoneReceiverRegistered = false;


        //Defining all the required variables
        Paint backgroungPaintWatchface;
        Paint normalPaintForText;
        Paint timePaint;
        Paint highDegreePaint;

        Bitmap bitmapWeather;

        Calendar calendar;
        Date date;
        SimpleDateFormat simpleDateFormat;
        java.text.DateFormat dateFormat;
        float verticalMargin;
        float lineHeight;
        String amString;
        String pmString;
        String degreeHigh, degreeLow;
        int interactive_BackgroundColor = Color.parseColor("#03A9F4");
        int interactive_HourDigitsColor = Color.parseColor("White");

        boolean isAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            //Connecting to Google Api Client , Never forget this , this single line can waste your
            //whole day :D
            mGoogleApiClient.connect();

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            //initializing all the variables
            Resources resources = MyWatchFaceService.this.getResources();
            verticalMargin = resources.getDimension(R.dimen.digital_y_offset);
            lineHeight = resources.getDimension(R.dimen.digital_line_height);
            amString = "AM";
            pmString = "PM";
            degreeHigh = "25\u00B0";
            degreeLow = "17\u00B0";
            isDataUpdated = false;


            bitmapWeather = BitmapFactory.decodeResource(resources, R.drawable.ic_clear);
            bitmapWeather = Bitmap.createScaledBitmap(bitmapWeather, 50, 50, true);

            backgroungPaintWatchface = new Paint();
            backgroungPaintWatchface.setColor(interactive_BackgroundColor);
            normalPaintForText = new Paint();
            normalPaintForText.setColor(Color.parseColor("#91E9FD"));
            normalPaintForText.setTypeface(MY_TYPEFACE);
            normalPaintForText.setAntiAlias(true);
            timePaint = new Paint();
            timePaint.setColor(interactive_HourDigitsColor);
            timePaint.setTypeface(MY_TYPEFACE);
            timePaint.setAntiAlias(true);
            highDegreePaint = new Paint();
            highDegreePaint.setColor(Color.parseColor("#91E9FD"));
            highDegreePaint.setTypeface(MY_BOAD_TYPEFACE);
            highDegreePaint.setAntiAlias(true);
            calendar = Calendar.getInstance();
            date = new Date();
            initFormats();
        }


        boolean isDataUpdated = false;

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.e("Watchface", "onVisibilityChanged");
            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone and date formats, in case they changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
                initFormats();
            } else {
                isDataUpdated = false;
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }
        }

        private void initFormats() {
            simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            simpleDateFormat.setCalendar(calendar);
            dateFormat = DateFormat.getMediumDateFormat(MyWatchFaceService.this);
            dateFormat.setCalendar(calendar);
        }

        //Registering receiver for Time zone change
        private void registerReceiver() {
            if (isTimeZoneReceiverRegistered) {
                return;
            }
            isTimeZoneReceiverRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            MyWatchFaceService.this.registerReceiver(broadcastReceiverTimeZOne, filter);
        }

        private void unregisterReceiver() {
            if (!isTimeZoneReceiverRegistered) {
                return;
            }
            isTimeZoneReceiverRegistered = false;
            MyWatchFaceService.this.unregisterReceiver(broadcastReceiverTimeZOne);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            normalPaintForText.setTextSize(resources.getDimension(R.dimen.digital_date_text_size));
            highDegreePaint.setTextSize(resources.getDimension(R.dimen.digital_date_text_size) + 5);
            timePaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            isAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            backgroungPaintWatchface.setColor(isInAmbientMode() ? Color.parseColor("Black") : interactive_BackgroundColor);
            if (isAmbient) {
                boolean antiAlias = !inAmbientMode;
                normalPaintForText.setAntiAlias(antiAlias);
                timePaint.setAntiAlias(antiAlias);
            }
            invalidate();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        private String getAmPmString(int amPm) {
            return amPm == Calendar.AM ? amString : pmString;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);
            date.setTime(now);
            boolean is24Hour = DateFormat.is24HourFormat(MyWatchFaceService.this);


            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), backgroungPaintWatchface);


            float mXOffset = bounds.width() / 2;
            float x = mXOffset;


            //timestring will contain time ex "1:40 PM"
            String timeString;

            //Adding hours to timeString
            if (is24Hour) {
                timeString = formatTwoDigitNumber(calendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = calendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                timeString = String.valueOf(hour) + ":";
            }

            //Adding minutes to timeString
            String minuteString = formatTwoDigitNumber(calendar.get(Calendar.MINUTE));
            timeString += minuteString;

            //Adding AM or Pm if time is not in 24hr format
            if (!is24Hour) {
                timeString += " " + getAmPmString(
                        calendar.get(Calendar.AM_PM));
            }
            x -= (timePaint.measureText(timeString) / 2);
            //drawing the timeString
            canvas.drawText(timeString, x, verticalMargin, timePaint);

            // Drawing Day , Date , WeatherIcon , temperature
            x = mXOffset;
            String dayNDate = simpleDateFormat.format(date) + "," + dateFormat.format(date).replace(',', ' ');
            x -= (normalPaintForText.measureText(dayNDate) / 2);
            canvas.drawText(
                    dayNDate,
                    x, verticalMargin + lineHeight, normalPaintForText);
            canvas.drawBitmap(bitmapWeather, x, verticalMargin + lineHeight * 2 -30, new Paint());
            canvas.drawText(degreeHigh, x + 60, verticalMargin + lineHeight * 2 + 10, highDegreePaint);
            canvas.drawText("/ "+degreeLow, x + 80 + normalPaintForText.measureText(degreeHigh), verticalMargin + lineHeight * 2 + 10, normalPaintForText);


        }


        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            if (!isDataUpdated) {
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/sendMeData");
                putDataMapReq.getDataMap().putString("time", System.currentTimeMillis() + "");
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                Wearable.DataApi.deleteDataItems(mGoogleApiClient, putDataReq.getUri());
                putDataReq.setUrgent();
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                if (!dataItemResult.getStatus().isSuccess()) {
                                    Log.e("MyWatchFace", "ERROR: failed to putDataItem, status code: "
                                            + dataItemResult.getStatus().getStatusCode());
                                } else {
                                    Log.e("MyWatchFace", "SendMeData request has been send"
                                            + dataItemResult.getStatus().getStatusCode());
                                }
                            }

                        });
            }
            Log.e("MainActivity", "Google client onConnected() inside wear watchFace " + isDataUpdated);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e("MainActivity", "onConnectionSuspended()");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e("MainActivity", "onConnectionFailed()");
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.e("MyWatchFaceService", "Inside onDataChanged()");
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    Log.e("MyWatchFaceService", "path=" + item.getUri().getPath());
                    if (item.getUri().getPath().compareTo("/data") == 0) {
                        isDataUpdated = true;
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        degreeHigh = dataMap.getString("highTempKey");
                        degreeLow = dataMap.getString("lowTempKey");
                        Asset weatherAssest = dataMap.getAsset("image");
                        loadBitmapFromAsset(weatherAssest);
                    }
                }
            }
        }

        public void loadBitmapFromAsset(final Asset asset) {
            if (asset == null) {
                throw new IllegalArgumentException("Asset must be non-null");
            }

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    // convert asset into a file descriptor and block until it's ready
                    InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                            mGoogleApiClient, asset).await().getInputStream();
                    // decode the stream into a bitmap
                    bitmapWeather = BitmapFactory.decodeStream(assetInputStream);
                    bitmapWeather = Bitmap.createScaledBitmap(bitmapWeather, 50, 50, true);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    invalidate();
                }
            }.execute();
        }
    }
}
