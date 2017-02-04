package com.example.android.sunshine;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        putDataMapReq.getDataMap().putString("highTempKey", "45\u00B0");
        putDataMapReq.getDataMap().putString("lowTempKey", "11\u00B0");
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.deleteDataItems(mGoogleApiClient, putDataReq.getUri());
        putDataReq.setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                    + dataItemResult.getStatus().getStatusCode());
                        }
                    }
                });
    }
}
