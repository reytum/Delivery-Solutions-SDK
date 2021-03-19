package com.example.delivery_solutions;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.transistorsoft.locationmanager.adapter.callback.TSConnectivityChangeCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSGeofenceCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSHttpResponseCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationProviderChangeCallback;
import com.transistorsoft.locationmanager.config.TransistorAuthorizationToken;
import com.transistorsoft.locationmanager.event.ConnectivityChangeEvent;
import com.transistorsoft.locationmanager.event.GeofenceEvent;
import com.transistorsoft.locationmanager.event.LocationProviderChangeEvent;
import com.transistorsoft.locationmanager.http.HttpResponse;
import com.transistorsoft.locationmanager.location.TSCurrentPositionRequest;
import com.transistorsoft.locationmanager.location.TSLocation;
import com.transistorsoft.locationmanager.logger.TSLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GeoActivity extends AppCompatActivity {

    private static String TAG = "DSGeoActivity";

    private FloatingActionButton mBtnChangePace;
    private SwitchCompat mBtnEnable;
    private FloatingActionButton mBtnCurrentPosition;
    private TextView mLocationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo);

        // Initialize UI items.
        mBtnChangePace = findViewById(R.id.btnChangePace);

        mBtnChangePace.setOnClickListener(createdChangePaceClickListener());

        mBtnEnable = findViewById(R.id.btnEnable);
        mBtnEnable.setOnCheckedChangeListener(createEnableSwitchListener());

        mBtnCurrentPosition = findViewById(R.id.btnCurrentPosition);
        mBtnCurrentPosition.setOnClickListener(createCurrentPositionClickListener());
        mLocationView = findViewById(R.id.content);

        // @config Enter your own unique organization & username here (eg: Github username)
        // View your tracking in browser by visiting:  http://tracker.transistorsoft.com/your-org-name
        String organization = "your-org-name";
        String username = "your-username";

        // URL to Transistor Software Demo Server:
        final String url = "http://tracker.transistorsoft.com";
        // Register with Transistorsoft Demo server to receive a JSON Web Token.
        DSAuthToken.findOrCreate(getApplicationContext(), organization, username, url, new DSAuthToken.Callback() {
            @Override
            public void onSuccess(TransistorAuthorizationToken token) {
                configureBackgroundGeolocation(token, url);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "*** TransistorAuthorization error: " + error);
            }
        });
    }

    private void configureBackgroundGeolocation(TransistorAuthorizationToken token, String url) {
        Context context = getApplicationContext();
        DSGeoLocation bgGeo = DSGeoLocation.getInstance(context);
        final DSConfig config = new DSConfig(context);

        // Build TSAuthorization instance for authorization with demo server via JSON Web Token.
        Map<String, Object> refreshPayload = new HashMap<>();
        refreshPayload.put("refresh_token", "{refreshToken}");

        // Configure TSAuthorization for automatic JWT authorization-handling (including automatic token refresh).
        DSAuth auth = new DSAuth();
        auth.setStrategy("JWT");
        auth.setAccessToken(token.getAccessToken());
        auth.setRefreshToken(token.getRefreshToken());
        auth.setExpires(token.getExpires());
        auth.setRefreshPayload(refreshPayload);
        auth.setRefreshUrl(url + "/api/refresh_token");

        // Configure the SDK:
        config.updateWithBuilder()
                // Configure Debugging
                .setDebug(true)
                .setLogLevel(5)
                // Configure Geolocation
                .setDesiredAccuracy(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setDistanceFilter(50f)
                .setStopTimeout(1L)
                // Configure Application behaviour
                .setForegroundService(true)
                .setStopOnTerminate(false)
                .setStartOnBoot(true)
                // Configure HTTP
                .setAuthorization(auth)
                .setUrl(url + "/api/locations")
                .setHeader("X-FOO", "FOO")  // <-- Optional HTTP headers
                .setHeader("X-BAR", "BAR")
                .commit();

        // Finally, signal #ready to the plugin.
        DSGeoLocation.getInstance(getApplicationContext()).ready(new DSCallback() {
            @Override
            public void onSuccess() {
                TSLog.logger.debug("[ready] success");
                mBtnEnable.setChecked(config.getEnabled());
            }

            @Override
            public void onFailure(String error) {
                TSLog.logger.debug("[ready] FAILURE: " + error);
            }
        });

        // Listen to motionchange event
        bgGeo.onMotionChange(createMotionChangeCallback());

        // Listen to location event
        bgGeo.onLocation(createLocationCallback());

        // Listen to geofence event
        bgGeo.onGeofence(createGeofenceCallback());

        // Listen to connectivitychange event
        bgGeo.onConnectivityChange(createConnectivityChangeCallback());

        // Listen to providerchange event
        bgGeo.onLocationProviderChange(createProviderChangeCallback());

        // Listen to http event
        bgGeo.onHttp(createHttpCallback());
    }

    private View.OnClickListener createdChangePaceClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle BackgroundGeolocation ON or OFF.
                DSConfig config = new DSConfig(getApplicationContext());
                DSGeoLocation bgGeo = DSGeoLocation.getInstance(getApplicationContext());

                boolean isMoving = !config.getIsMoving();
                bgGeo.changePace(isMoving);
                int icon = (isMoving) ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
                mBtnChangePace.setImageResource(icon);
            }
        };
    }

    private CompoundButton.OnCheckedChangeListener createEnableSwitchListener() {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isMoving) {
                DSGeoLocation bgGeo = DSGeoLocation.getInstance(getApplicationContext());
                mBtnChangePace.setEnabled(isMoving);
                if (isMoving) {
                    bgGeo.start();
                } else {
                    bgGeo.stop();
                }
            }
        };
    }

    private View.OnClickListener createCurrentPositionClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Optional #extras to attach to recorded location.
                JSONObject extras = new JSONObject();
                try {
                    extras.put("jobId", 1234);
                } catch (JSONException e) {
                }
                // Build position request.
                TSCurrentPositionRequest request = new TSCurrentPositionRequest.Builder(getApplicationContext())
                        .setPersist(true)       // <-- yes, persist to database
                        .setSamples(3)          // <-- fetch 3 location samples and return highest accuracy
                        .setExtras(extras)      // <-- optional #extras
                        .setMaximumAge(5000L)   // <-- if a location <= 5s ago is available, return it.
                        .setDesiredAccuracy(40) // <-- if a location having accuracy <= 40 arrives, return it right away.
                        .build();

                DSGeoLocation bgGeo = DSGeoLocation.getInstance(getApplicationContext());
                bgGeo.getCurrentPosition(request);
            }
        };
    }

    private TSLocationCallback createMotionChangeCallback() {
        return new TSLocationCallback() {
            @Override
            public void onLocation(TSLocation location) {
                TSLog.logger.debug("[motionchange] " + location.getJson());
                int icon = (location.getIsMoving()) ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
                mBtnChangePace.setImageResource(icon);
            }

            @Override
            public void onError(Integer error) {
                TSLog.logger.debug("[motionchange] FAILURE: " + error);
            }
        };
    }

    private TSLocationCallback createLocationCallback() {
        return new TSLocationCallback() {
            @Override
            public void onLocation(TSLocation location) {
                TSLog.logger.debug("[location] " + location.toJson());
                try {
                    CharSequence json = location.toJson().toString(2);
                    mLocationView.setText(json);
                } catch (JSONException e) {

                }
            }

            @Override
            public void onError(Integer error) {
                TSLog.logger.debug("[location] FAILURE: " + error);
            }
        };
    }

    private TSHttpResponseCallback createHttpCallback() {
        return new TSHttpResponseCallback() {
            @Override
            public void onHttpResponse(HttpResponse httpResponse) {
                TSLog.logger.debug("[http] " + httpResponse.status + ": " + httpResponse.responseText);
            }
        };
    }

    private TSGeofenceCallback createGeofenceCallback() {
        return new TSGeofenceCallback() {
            @Override
            public void onGeofence(GeofenceEvent event) {
                TSLog.logger.debug("[geofence] " + event.toJson());
            }
        };
    }

    private TSConnectivityChangeCallback createConnectivityChangeCallback() {
        return new TSConnectivityChangeCallback() {
            @Override
            public void onConnectivityChange(ConnectivityChangeEvent event) {
                TSLog.logger.debug("[connectivitychange] Network connected? " + event.hasConnection());
            }
        };
    }

    private TSLocationProviderChangeCallback createProviderChangeCallback() {
        return new TSLocationProviderChangeCallback() {
            @Override
            public void onLocationProviderChange(LocationProviderChangeEvent event) {
                TSLog.logger.debug("[providerchange] " + event.toJson());
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}