package com.deer.gpssample;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by deer on 2017/11/20.
 * Encapsulate using of the Google location service.
 * (Manager is the middle man between presenter and service.)
 */

public class GetLocationManager {

    private static GetLocationManager instance;
    public static GetLocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new GetLocationManager(context);
        }
        return instance;
    }

    public interface ConnectToGoogleCallback {
        void onConnectedSuccess();

        void onConnectNeedRetry(ConnectionResult result);

        void onConnectedFail(int errorCode);
    }

    public interface GetLocationCallback {
        void onGetLocation(double lat, double lng);

        void onLocationIsNotChanged(double lat, double lng);
    }

    /*-------------- GetLocationManager ----------------*/
    private Location latestLocation;
    private String languageCode;
    private String addrDistrict;

    private GoogleApiClient googleApiClient;
    private ConnectToGoogleCallback connectToGoogleCallback;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private GetLocationManager(Context context) {
        languageCode = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getString("lang", "");
        buildGoogleApiClient(context);
    }

    /**
     *  Check the client is connected before using function to request location.
     */
    public boolean isConnectedClient() {
        return googleApiClient != null && googleApiClient.isConnected();
    }

    private void buildGoogleApiClient(Context context) {

        googleApiClient = new GoogleApiClient
                .Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    public void onConnected(@Nullable Bundle bundle) {
                        connectToGoogleCallback.onConnectedSuccess();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {}
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        if (mResolvingError) {
                            // Already attempting to resolve an error.
                            return;
                        } else if (result.hasResolution()) {
                            mResolvingError = true;
                            connectToGoogleCallback.onConnectNeedRetry(result);
                        } else {
                            // Show dialog using GoogleApiAvailability.getErrorDialog()
                            connectToGoogleCallback.onConnectedFail(result.getErrorCode());
                            mResolvingError = true;
                        }
                    }
                })
                .build();
    }

    /**
     *  After checking isConnectedClient and get the return is false,
     *  call this function to connect api client.
     */
    public void connectToGoogleApiClient(ConnectToGoogleCallback connectToGoogleCallback) {
        if (this.connectToGoogleCallback == null && connectToGoogleCallback != null) {
            this.connectToGoogleCallback = connectToGoogleCallback;
            googleApiClient.connect();
        } else {
            if (!googleApiClient.isConnecting() &&
                    !googleApiClient.isConnected()) {
                googleApiClient.connect();
            }
        }
    }

    public void forceRequestLocation(final GetLocationCallback getLocationCallback) {
        callFusedLocationApi(getLocationCallback);
    }

    private void callFusedLocationApi(final GetLocationCallback getLocationCallback) {
        /* Set location request */
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setFastestInterval(1000)
                .setInterval(5000)
                .setNumUpdates(1);

        /* Start request location*/
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, new LocationListener() {
                @Override
                public void onLocationChanged(Location lastLocation) {
                    if (lastLocation != null) {
                        latestLocation = lastLocation;
                        getLocationCallback.onGetLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
                    }
                    else {
                    }
                }
            });
        } catch (IllegalStateException e2) {
            e2.printStackTrace();
        } catch (NullPointerException exception) {
            exception.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public String getAddressDistrict(Context context, String languageCode) {
        return getAddressDistrict(context, languageCode, latestLocation.getLatitude(), latestLocation.getLongitude());
    }

    private String getAddressDistrict(Context context, String languageCode, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, new Locale(languageCode));
            List<Address> addresses;
            addresses = geocoder.getFromLocation(
                    lat, lng, 1);
            if(!addresses.isEmpty()){
                Address address = addresses.get(0);
                if (address != null) {
                    String area1 = address.getAdminArea() != null ? address.getAdminArea() : "";
                    String area2 = address.getLocality() != null ? address.getLocality() : "";
                    String area3 = address.getThoroughfare() != null ? address.getThoroughfare() : "";

                    return area1 + area2 + area3;
                } else {
                    return "";
                }
            } else {
                // If the address list is empty, the back end of the location service could be not available probably.
                return "";
            }
        } catch (IOException ioException) {
            return "";
        } catch (NullPointerException npException) {
            return "";
        }
    }

    public String readLanguageCode() {
        if(languageCode.contains("zh"))
            return languageCode;
        else
            return  "en";
    }
}
