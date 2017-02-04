package com.example.android.sunshine.sync;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manish Menaria on 04-Feb-17.
 */

public class DataLayerListenerService extends WearableListenerService {

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e("DataListenerService", "DataLayerListenerService failed to connect to GoogleApiClient, "
                        + "error code: " + connectionResult.getErrorCode());
                return;
            }
        }

        for(DataEvent event:dataEvents){
            if(event.getType()==DataEvent.TYPE_CHANGED){
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/sendMeData") == 0) {
                    Log.e("MyWatchFace", "SendMeData request received");
                    //Here is all the implementation require for GoUbiguitious Project
                    //Query to get info for watchface
                    ContentResolver sunshineContentResolver = getApplicationContext().getContentResolver();
                    Cursor cursor = sunshineContentResolver.query(WeatherContract.WeatherEntry.CONTENT_URI,
                            null, null, null, null);
                    if(cursor==null && !cursor.moveToFirst()){
                        return;
                    }
                    boolean b = cursor.moveToFirst();
                /*Toast.makeText(mContext, cursor.getInt(4) + "\n" + cursor.getInt(3) + "\n" + cursor.getInt(2),
                        Toast.LENGTH_SHORT).show();*/

                    String highTempString = cursor.getInt(4) + "\u00B0";
                    String lowTempString = cursor.getInt(3) + "\u00B0";
                    int weatherId = cursor.getInt(2);
                    cursor.close();
                    int weatherImageId = SunshineWeatherUtils
                            .getLargeArtResourceIdForWeatherCondition(weatherId);
                    Log.e("DataToSend",highTempString+" "+lowTempString+" "+weatherId);



                    // I am sending low & temp along with the required icon to show in watch face

                    Bitmap bitmap = SunshineSyncTask.getBitMapFormVectorDrawable(weatherImageId,getApplicationContext());
                    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    bitmap = Bitmap.createScaledBitmap(bitmap, 40, 40, true);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
                    Asset asset = Asset.createFromBytes(byteStream.toByteArray());

                    PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
                    putDataMapReq.getDataMap().putString("highTempKey", highTempString);
                    putDataMapReq.getDataMap().putString("lowTempKey", lowTempString);
                    //Using extra time field so that dataItem will be send to watchface for sure
                    //bcs time will always have diffrent values
                    putDataMapReq.getDataMap().putString("time", System.currentTimeMillis() + "");
                    putDataMapReq.getDataMap().putAsset("image", asset);
                    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                    Wearable.DataApi.deleteDataItems(mGoogleApiClient, putDataReq.getUri());
                    putDataReq.setUrgent();
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq)
                            .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                @Override
                                public void onResult(DataApi.DataItemResult dataItemResult) {
                                    if (!dataItemResult.getStatus().isSuccess()) {
                                        Log.e("SunshineSyncTask", "ERROR: failed to putDataItem, status code: "
                                                + dataItemResult.getStatus().getStatusCode());
                                    } else {
                                        Log.e("SunshineSyncTask", "Data is send succesfully");
                                    }
                                }
                            });
                }
            }
        }
    }
}