package com.cabfare.android.sample

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cabfare.android.sdk.CBFDriver
import com.cabfare.android.sdk.CabFare
import com.cabfare.android.sdk.CabFareEvent
import com.cabfare.android.sdk.CabFareEventListener
import com.cabfare.android.sdk.extensions.isInBackground
import com.cabfare.android.sdk.model.TripStatus
import java.text.SimpleDateFormat
import java.util.*

class DriverTripActivity : AppCompatActivity() {

    val rangedBeaconsTV: TextView by lazy { findViewById<TextView>(R.id.tv_ranged_beacons) }
    val driverTV: TextView by lazy { findViewById<TextView>(R.id.tv_driver_trip) }
    val endJourneyBT: Button by lazy { findViewById<Button>(R.id.bt_end_current_journey) }
    val getCurrentTripBT: Button by lazy { findViewById<Button>(R.id.bt_get_current_trip) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_trip)

        CabFare.getInstance().registerCabFareEventListener(cabFareEventListener)

        endJourneyBT.setOnClickListener {
            CBFDriver.getInstance().endCurrentJourney()
        }

        getCurrentTripBT.setOnClickListener {
            CBFDriver.getInstance().getCurrentTrip(true)
        }

        updateCurrentTripDetails(null)
    }

    override fun onStart() {
        super.onStart()
        CBFDriver.getInstance().getCurrentTrip(true)
    }

    private val cabFareEventListener = object : CabFareEventListener {
        override fun onEvent(cabFareEvent: CabFareEvent, data: Bundle) {
            if (isFinishing)
                return

            Log.d("DriverTripActivity", cabFareEvent.name)
            when (cabFareEvent) {
                CabFareEvent.BEACON_RANGED -> rangedBeaconsTV.text = data.getString(CabFare.BROADCAST_MESSAGE)
                CabFareEvent.TRIP_ENDED,
                CabFareEvent.TRIP_UPDATED -> updateCurrentTripDetails(data.getString(CabFare.BROADCAST_MESSAGE))
                CabFareEvent.TRIP_PAID -> showFinishedPopup(data.getString(CabFare.BROADCAST_MESSAGE))
                CabFareEvent.TRIP_CANCELLED,
                CabFareEvent.TRIP_NOT_FOUND -> showCancelledPopup()
                CabFareEvent.SHIFT_ENDED -> finish()
                else -> {/* Do Nothing*/
                }
            }
        }

    }

    private fun updateCurrentTripDetails(broadcastMsg: String?) {
        val tripDetails = CBFDriver.getInstance().getCurrentTrip(false)

        val allBeacons = CBFDriver.getInstance().getRangedBeacons()
        if (allBeacons != null) {
            var rangedBeacons = ""
            for (b in allBeacons) {
                if (rangedBeacons == "") {
                    rangedBeacons = "Ranged Beacons"
                    rangedBeacons = String.format("%s\n[%d, %d]", rangedBeacons, b.beacon.major, b.beacon.minor)
                } else
                    rangedBeacons = String.format("%s, [%d, %d]", rangedBeacons, b.beacon.major, b.beacon.minor)
            }

            rangedBeaconsTV.text = rangedBeacons
        }

        if (tripDetails != null) {
            var text = ""
            if (broadcastMsg != null)
                text = String.format("%s\n\n%s", text, broadcastMsg)

            text = String.format("%s\n\nPick Up: %s", text, tripDetails.pickupLocation.name)

            val tripStart = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            df.timeZone = TimeZone.getTimeZone("UTC");
            tripStart.time = df.parse(tripDetails.pickupTime)
//            tripStart.time = df.parse("2018-11-29T12:15:00.000Z")

            val sinceMilli = Calendar.getInstance().timeInMillis - tripStart.timeInMillis
            val hour = (sinceMilli / (60 * 60 * 1000)).toInt()
            val minutes = ((sinceMilli / (60 * 1000)) % 60).toInt()
            val timeSince = String.format(Locale.getDefault(), "%02d:%02d", hour, minutes)
            text = String.format("%s\nTrip In Progress: %s", text, timeSince)

            text = String.format("%s\nTrip Id: %s", text, tripDetails.tripId)

            driverTV.text = text

            when (tripDetails.tripStatus()) {

                TripStatus.STARTED -> {
                    endJourneyBT.visibility = View.VISIBLE
                }
                TripStatus.AWAITING_FARE,
                TripStatus.AWAITING_PAYMENT,
                TripStatus.PROCESSING_PAYMENT,
                TripStatus.FINISHED -> CBFDriver.getInstance().payCash()
                TripStatus.CANCELED -> showCancelledPopup()
            }
        }
    }

    private fun showCancelledPopup() {
        if (isInBackground()) {
            this@DriverTripActivity.sendNotification("Trip Cancelled")
            val intent = Intent(this@DriverTripActivity, DriverDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        } else if (!isFinishing)
            AlertDialog.Builder(this)
                    .setMessage("Trip has been cancelled")
                    .setNeutralButton("Ok") { p0, p1 ->
                        p0.dismiss()
                        val intent = Intent(this@DriverTripActivity, DriverDashboardActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    }.show()
    }

    private fun showFinishedPopup(broadcastMsg: String?) {
        if (isInBackground())
            this@DriverTripActivity.sendNotification("Trip Finished")
        else if (!isFinishing) {
            var message = broadcastMsg
            val tripDetails = CBFDriver.getInstance().getCurrentTrip(false)
            if (tripDetails != null) {
                message = String.format("%s\n\nTrip Id: %s", message, tripDetails.tripId)
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
                }
            }

            AlertDialog.Builder(this)
                    .setTitle("Journey has ended")
                    .setMessage(message)
                    .setNeutralButton("Ok") { p0, p1 ->
                        p0.dismiss()
                        val intent = Intent(this@DriverTripActivity, DriverDashboardActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    }.show()
        }
    }
}
