package com.example.delivery_solutions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class DeliverySolutions {

    public static void startLocationMonitoring(Context context) throws DSException {
        if (!(context instanceof Activity)) {
            throw new DSException("Activity context is required");
        }

        context.startActivity(new Intent(context, GeoActivity.class));
    }
}
