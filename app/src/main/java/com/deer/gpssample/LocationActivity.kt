package com.deer.gpssample

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.CallSuper
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_text.*

const val REQUEST_LOCATION_PERMISSIONS = 123

class LocationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)

        tv_location_btn.setOnClickListener {
            tv_location_btn.isEnabled = false
            if(checkGoogleServiceAvailable()) {
                if (locationPermissionGranted()) {
                    getLocation()
                } else {
                    requestLocationPermission()
                }
            }
        }
    }

    private fun checkGoogleServiceAvailable(): Boolean {
        val isAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        return when(isAvailable) {
            ConnectionResult.SUCCESS -> {
                Snackbar.make(root, R.string.hint_google_service_connect_success, Snackbar.LENGTH_SHORT).show()
                true
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Snackbar.make(root, R.string.error_hint_google_service_update, Snackbar.LENGTH_SHORT).show()
                false
            }
            else -> {
                Snackbar.make(root, R.string.error_hint_google_service_no, Snackbar.LENGTH_SHORT).show()
                false
            }
        }
    }

    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else
                getLocationActionFail()
        }
    }

    private fun locationPermissionGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /* If the mobile device android sdk version is up to 23 */
    @TargetApi(Build.VERSION_CODES.M)
    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            //  Show a dialog to explain location permission's purpose.
            val gpsHintSnack = Snackbar.make(root, R.string.request_hint_gps, Snackbar.LENGTH_INDEFINITE)
            gpsHintSnack.setAction(android.R.string.yes, View.OnClickListener{
                _ ->
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSIONS)
            })
            gpsHintSnack.show()
            getLocationActionFail()

        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSIONS)
        }
    }

    private fun checkDeviceLocationStatus(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false

        try {
            gps_enabled = lm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Log.e(this.packageName, "checkDeviceLocationStatus: Can not get provider status: " + e.message)
        }

        return if (!gps_enabled) {
            val gpsHintSnack = Snackbar.make(root, R.string.error_hint_gps, Snackbar.LENGTH_INDEFINITE)
            gpsHintSnack.setAction(R.string.option_gps_open, View.OnClickListener{
                _ ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(myIntent)
            })
            gpsHintSnack.show()
            getLocationActionFail()

            false
        }
        else
            true
    }

    private var getLocationManager: GetLocationManager? = null
    private fun getLocation() {
        if(checkDeviceLocationStatus(this)) {
            getLocationManager = GetLocationManager.getInstance(this)
            if(getLocationManager!!.isConnectedClient) {
                val snack = Snackbar.make(root, R.string.hint_google_service_connect_success, Snackbar.LENGTH_INDEFINITE)
                snack.show()
                requestLocation(snack)
            } else {
                reconnectToGoogle()
            }
        }
    }

    private fun getLocationActionSuccess(lat: Double, lng: Double) {
        tv_location_btn.isEnabled = true
        tv_location_lat.text = """lat ${lat.toString()}"""
        tv_location_lng.text = "lng "+ lng.toString()
    }

    private fun getLocationActionFail() {
        tv_location_btn.isEnabled = true
    }

    private fun requestLocation(snack: Snackbar) {
        getLocationManager!!.forceRequestLocation(object : GetLocationManager.GetLocationCallback {
            override fun onGetLocation(lat: Double, lng: Double) {
                snack.dismiss()
                getLocationActionSuccess(lat, lng)
            }

            override fun onLocationIsNotChanged(lat: Double, lng: Double) {}
        })
    }

    private fun reconnectToGoogle() {
        getLocationManager!!.connectToGoogleApiClient(object : GetLocationManager.ConnectToGoogleCallback {
            override fun onConnectedSuccess() {
                val snack = Snackbar.make(root, R.string.hint_google_service_connect_success, Snackbar.LENGTH_INDEFINITE)
                    snack.show()
                requestLocation(snack)
            }

            override fun onConnectNeedRetry(result: ConnectionResult?) {
                Snackbar.make(root, R.string.error_hint_google_service_connect_retry, Snackbar.LENGTH_SHORT).show()
                reconnectToGoogle()
            }

            override fun onConnectedFail(errorCode: Int) {
                getLocationActionFail()
                Snackbar.make(root, R.string.error_hint_google_service_connect_fail, Snackbar.LENGTH_SHORT).show()
            }
        })
    }
}
