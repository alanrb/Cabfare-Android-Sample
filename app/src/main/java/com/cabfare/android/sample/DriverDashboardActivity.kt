package com.cabfare.android.sample

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cabfare.android.sdk.*
import com.cabfare.android.sdk.exceptions.CabFareException
import com.cabfare.android.sdk.extensions.isInBackground
import com.cabfare.android.sdk.fcm.CBFNotificationsManager
import com.cabfare.android.sdk.fcm.NotificationData
import com.cabfare.android.sdk.model.DriverDetails
import java.util.*

class DriverDashboardActivity : AppCompatActivity() {

    private val signOutBT: Button by lazy { findViewById<Button>(R.id.bt_sign_out) }
    private val startShiftBT: Button by lazy { findViewById<Button>(R.id.bt_start_shift) }
    private val driverTV: TextView by lazy { findViewById<TextView>(R.id.tv_driver) }
    private val rangedBeaconsTV: TextView by lazy { findViewById<TextView>(R.id.tv_ranged_beacons) }
    private val driverIdTV: TextView by lazy { findViewById<TextView>(R.id.tv_driver_id) }

    private var isSigningOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DriverDashboardActivity", "OnCreate")
        setContentView(R.layout.activity_driver_dashboard)

        CabFare.getInstance().registerCabFareEventListener(cabFareEventListener)

        CBFDriver.getInstance().getDriverDetails(object : CabFareCallback<DriverDetails> {
            override fun onError(error: CabFareException) {
                // Do Nothing
            }

            override fun onSuccess(result: DriverDetails) {
                driverIdTV.text = String.format(Locale.getDefault(), "Driver ID: %s", result.driverId)
            }
        })

        startShiftBT.setOnClickListener {
            CBFDriver.getInstance().startShift(this)
            startShiftBT.visibility = View.INVISIBLE
        }

        signOutBT.setOnClickListener {
            driverTV.text = "Signing Out"
            isSigningOut = true

            if (CBFDriver.getInstance().isInShift())
                CBFDriver.getInstance().endShift()
            else
                signOut()
        }

        if (intent != null && intent.hasExtra(CBFNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA)) {
            val notificationData = intent.getSerializableExtra(CBFNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA)
            if (notificationData is NotificationData && notificationData.code == CBFNotificationsManager.CODE_TRIP_STARTED)
                showDriverTripDetails()
        }
    }

    private fun signOut() {
        CBFDriver.getInstance().signOut(object : CabFareCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                val pref = PreferenceManager.getDefaultSharedPreferences(this@DriverDashboardActivity)
                pref.edit().clear().apply()
                startActivity(Intent(this@DriverDashboardActivity, MainActivity::class.java))
                finish()
            }

            override fun onError(error: CabFareException) {
                driverTV.text = error.message
            }

        })
    }

    override fun onStart() {
        super.onStart()

        CBFDriver.getInstance().getCurrentTrip(true)

        if (CBFDriver.getInstance().isInShift()) {
            val beaconInfo = CBFDriver.getInstance().getPairedBeacon()
            val text = String.format("In Shift\n\nPaired Beacon:\n[%d, %d]", beaconInfo.beacon?.major, beaconInfo.beacon?.minor)

            driverTV.text = text

            startShift(true)
        } else
            startShift(false)

    }

    private val cabFareEventListener = object : CabFareEventListener {
        override fun onEvent(cabFareEvent: CabFareEvent, data: Bundle) {
            if (isFinishing)
                return

            Log.d("DriverDashboardActivity", cabFareEvent.name)
            when (cabFareEvent) {
                CabFareEvent.BEACON_RANGED ->
                    rangedBeaconsTV.text = data.getString(CabFare.BROADCAST_MESSAGE)
                CabFareEvent.BEACON_PAIRED -> driverTV.text = data.getString(CabFare.BROADCAST_MESSAGE)
                CabFareEvent.SHIFT_STARTED -> {
                    if (isInBackground())
                        this@DriverDashboardActivity.sendNotification("Shift Started")

                    driverTV.text = data.getString(CabFare.BROADCAST_MESSAGE)
                }
                CabFareEvent.SHIFT_RESUMED -> driverTV.text = data.getString(CabFare.BROADCAST_MESSAGE)
                CabFareEvent.SHIFT_START_ERROR -> {
                    if (isInBackground())
                        this@DriverDashboardActivity.sendNotification("Error in staring shift")

                    driverTV.text = data.getString(CabFare.BROADCAST_MESSAGE)
                    startShiftBT.visibility = View.VISIBLE
                }
                CabFareEvent.TRIP_UPDATED -> showDriverTripDetails()
                CabFareEvent.SHIFT_ENDED -> {
                    if (isSigningOut)
                        signOut()
                    else
                        showShiftEndedPopup()
                }
                CabFareEvent.TRIP_NOT_FOUND -> {
                    // Do Nothing
                }
                else ->
                    driverTV.text = cabFareEvent.name
            }
        }

    }

    private fun showShiftEndedPopup() {
        if (isInBackground()) {
            this@DriverDashboardActivity.sendNotification("Shift Ended")

            startShift(false)
        } else
            AlertDialog.Builder(this)
                    .setMessage("Shift has ended")
                    .setNeutralButton("Ok") { p0, p1 ->
                        startShift(false)
                    }.show()
    }

    private fun startShift(inBackground: Boolean) {
        if (!inBackground)
            driverTV.text = "Looking for beacons..."

        startShiftBT.callOnClick()
    }

    private fun showDriverTripDetails() {
        startActivity(Intent(this@DriverDashboardActivity, DriverTripActivity::class.java))
        finish()
    }
}
