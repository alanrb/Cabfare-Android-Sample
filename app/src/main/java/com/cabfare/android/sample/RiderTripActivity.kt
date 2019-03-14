package com.cabfare.android.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.cabfare.android.sdk.*
import com.cabfare.android.sdk.exceptions.CabFareException
import com.cabfare.android.sdk.extensions.isInBackground
import com.cabfare.android.sdk.fcm.CBFNotificationsManager
import com.cabfare.android.sdk.model.TripDetails
import com.cabfare.android.sdk.model.TripStatus
import kotlinx.android.synthetic.main.activity_rider_dashboard.*
import java.text.SimpleDateFormat
import java.util.*

class RiderTripActivity : AppCompatActivity() {

    private val mTripStartWaitTimer = Handler()
    private var mIsTripStartAllowed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider_dashboard)

        CabFare.getInstance().registerCabFareEventListener(cabFareEventListener)

        bt_retry.setOnClickListener {
            tv_ranged_beacons.text = null
            if (!mIsTripStartAllowed) {
                tv_rider.setText(R.string.msg_trip_start_not_allowed)
                return@setOnClickListener
            }

            tv_rider.setText(R.string.msg_trip_not_found)
            CBFRider.getInstance().startTrip(this)
            bt_retry.visibility = View.INVISIBLE
        }

        bt_sign_out.setOnClickListener {
            tv_rider.text = "Signing Out"
            CBFRider.getInstance().signOut(object : CabFareCallback<Boolean> {
                override fun onSuccess(result: Boolean) {

                    val pref = PreferenceManager.getDefaultSharedPreferences(this@RiderTripActivity)
                    pref.edit().clear().apply()
                    startActivity(Intent(this@RiderTripActivity, MainActivity::class.java))
                    finish()
                }

                override fun onError(error: CabFareException) {
                    tv_rider.text = error.message
                }

            })
        }

        bt_cancel_trip.setOnClickListener {
            CBFRider.getInstance().cancelCurrentTrip()
        }

        if (intent != null && intent.hasExtra(CBFNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA)) {
            showLastTripDetailsIfPossible()
        } else
            CBFRider.getInstance().startTrip(this)
    }

    private fun showLastTripDetailsIfPossible() {
        if (isInBackground()) return

        val pref = PreferenceManager.getDefaultSharedPreferences(this@RiderTripActivity)

        val tripId = pref.getString(Constants.KEY_LAST_TRIP_ID, null) ?: return
        pref.edit().remove(Constants.KEY_LAST_TRIP_ID).apply()

        showLoading()
        CBFRider.getInstance().getTripDetailsForTripId(tripId, object : CabFareCallback<TripDetails> {
            override fun onError(error: CabFareException) {
                // Do Nothing
            }

            override fun onSuccess(result: TripDetails) {
                hideLoading()

                when (result.tripStatus()) {
                    TripStatus.FINISHED -> {
                        showTripEndPopup("Journey Completed", result)
                    }
                    TripStatus.CANCELED -> {
                        showTripEndPopup("Trip Cancelled", result)
                    }
                    else -> {
                        //Do Nothing
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (CBFRider.getInstance().getCurrentTrip(false) == null)
            showLastTripDetailsIfPossible()
    }

    private val cabFareEventListener = object : CabFareEventListener {
        override fun onEvent(cabFareEvent: CabFareEvent, data: Bundle) {
            if (isFinishing)
                return

            hideLoading()
            Log.d("RiderTripActivity", cabFareEvent.name)
            when (cabFareEvent) {
                CabFareEvent.BEACON_PAIRED -> tv_rider.text = data.getString(CabFare.BROADCAST_MESSAGE)
                CabFareEvent.TRIP_STARTED -> updateCurrentTripDetailsFromEvent(data)
                CabFareEvent.TRIP_UPDATED -> updateCurrentTripDetailsFromEvent(data)
                CabFareEvent.TRIP_NOT_FOUND -> {
                    resetUI()
                    showLastTripDetailsIfPossible()
                }
                CabFareEvent.TRIP_START_FAILED -> {
                    if (isInBackground())
                        this@RiderTripActivity.sendNotification("Trip Start Failed")
                    tv_rider.text = cabFareEvent.name
                    bt_retry.visibility = View.VISIBLE
                }
                CabFareEvent.TRIP_ENDED,
                CabFareEvent.TRIP_CANCELLED -> showLastTripDetailsIfPossible()
                CabFareEvent.BEACON_RANGED -> tv_ranged_beacons.text = data.getString(CabFare.BROADCAST_MESSAGE)
                else -> tv_rider.text = cabFareEvent.name
            }
        }
    }

    private fun updateCurrentTripDetailsFromEvent(data: Bundle) {
        val tripDetails = CBFRider.getInstance().getCurrentTrip(false) ?: return

        val pref = PreferenceManager.getDefaultSharedPreferences(this@RiderTripActivity)
        pref.edit().putString(Constants.KEY_LAST_TRIP_ID, tripDetails.tripId).apply()

        bt_cancel_trip.visibility = View.VISIBLE

        var text = ""
        if (data.getString(CabFare.BROADCAST_MESSAGE) != null)
            text = data.getString(CabFare.BROADCAST_MESSAGE)

        text = String.format("%s\n\nTrip Id: %s", text, tripDetails.tripId)
        text = String.format("%s\nDriver Id: %s", text, tripDetails.driver?.driverId)
        text = String.format("%s\nDriver Name: %s", text, tripDetails.driver?.firstName)
        text = String.format("%s\nCompany: %s", text, tripDetails.driver?.company)
        text = String.format("%s\nVehicle Id: %s", text, tripDetails.vehicle?.registrationNumber)

        val tripStart = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        df.timeZone = TimeZone.getTimeZone("UTC")
        tripStart.time = df.parse(tripDetails.pickupTime)
        val sinceMilli = Calendar.getInstance().timeInMillis - tripStart.timeInMillis
        val hour = (sinceMilli / (60 * 60 * 1000)).toInt()
        val minutes = ((sinceMilli / (60 * 1000)) % 60).toInt()
        val timeSince = String.format(Locale.getDefault(), "%02d:%02d", hour, minutes)
        text = String.format("%s\nProgress Time: %s", text, timeSince)

        text = String.format("%s\nPick Up: %s", text, tripDetails.pickupLocation.name)
        text = String.format("%s\nTrip Status: %s", text, tripDetails.tripStatus().name)
        tv_rider.text = text
    }

    private fun showTripEndPopup(s: String, tripDetails: TripDetails?) {

        var message = ""
        var tripStartThresholdMilli = 0L

        if (tripDetails != null) {
            message = String.format("Trip Id: %s", tripDetails.tripId)
            message = String.format("%s\n\nPick Up: %s", message, tripDetails.pickupLocation.name)

            val tripStart = Calendar.getInstance()
            val tripEnd = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            df.timeZone = TimeZone.getTimeZone("UTC");
            tripStart.time = df.parse(tripDetails.pickupTime)

            message = String.format("%s\nPickup Time: %s", message, SimpleDateFormat("dd MMM yyyy 'at' hh:mm aaa", Locale.getDefault()).format(tripStart.time))

            message = String.format("%s\n\nDrop: %s", message, tripDetails.dropoffLocation?.name)

            if (tripDetails.dropoffTime != null) {
                tripEnd.time = df.parse(tripDetails.dropoffTime)
                message = String.format("%s\nDrop Time: %s", message, SimpleDateFormat("dd MMM yyyy 'at' hh:mm aaa", Locale.getDefault()).format(tripEnd.time))
                tripStartThresholdMilli = tripEnd.timeInMillis + CBFRider.getInstance().getTripStartWaitWindow() * 1000L - System.currentTimeMillis()

                tripStartThresholdMilli = if (tripStartThresholdMilli < 0) 0 else tripStartThresholdMilli
            }
        }

        AlertDialog.Builder(this)
                .setTitle(s)
                .setMessage(message)
                .setNeutralButton("Ok") { p0, p1 ->
                    resetUI()

                    mIsTripStartAllowed = false
                    mTripStartWaitTimer.postDelayed({
                        mIsTripStartAllowed = true
                    }, tripStartThresholdMilli)
                }.show()
    }

    private fun resetUI() {
        bt_retry.visibility = View.VISIBLE
        bt_cancel_trip.visibility = View.INVISIBLE
        tv_rider.setText(R.string.msg_trip_not_found)
    }

    override fun onDestroy() {
        super.onDestroy()
        mTripStartWaitTimer.removeCallbacksAndMessages(null)
    }
}
