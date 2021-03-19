package com.example.delivery_solutions;

import android.content.Context;
import android.content.Intent;

import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;

public class DSGeoLocation extends BackgroundGeolocation {
    private static DSGeoLocation geoLocation;

    public static DSGeoLocation getInstance(Context context) {
        if (geoLocation == null) {
            geoLocation = new DSGeoLocation(context, null);
        }
        return geoLocation;
    }

    public DSGeoLocation(Context context, Intent intent) {
        super(context, intent);
    }
}
