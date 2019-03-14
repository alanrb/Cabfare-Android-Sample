# Cabfare-Android-SDK
Cabfare Android SDK is used for Cabfare service integration.  Cabfare service allow passenger safety by providing driver and trip information for the vehicle the rider used to get to its destination..

## Overview
Cabfare Android SDK is a framework for Cabfare service integration.  This repository contains the Android SDK and the sample project implementation on how to use the Android SDK.

### SDK Flow
![alt SDK Flow](https://github.com/parousya/Cabfare-Android-SDK/blob/master/Android%20Cabfare%20SDK%20Flow.png)

## Installation
Cabfare SDK is distributed as a compiled bundle, and can be easily integrated into a new app or an existing codebase with standard tooling.

### Requirements
The Android SDK requires Android API Level >= 26.  The Android SDK version requirements for each release are tightly coupled.

## Initializing
To initialize Cabfare SDK, you will need to obtain the CLIENT_ID and CLIENT_SECRET from Cabfare.
Please contact support@cabfare.com for access.

`this` which is the first parameter is the application context.

You will need to choose between `Variant.TESTING` and `Variant.PRODUCTION` depending on the environment to use.  Please use `Variant.TESTING` for non production app.

```kotlin
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CabFare.init(this, CLIENT_ID, CLIENT_SECRET, Variant.TESTING);
    }
}
```

### Starting Client
You will have 2 choice in starting Cabfare client:
1. As a Driver
2. As a Rider

## Starting as a Driver
`this` is the activity object where the call is made.

```kotlin
CBFDriver.getInstance().signIn(
                    this,
                    driverId,
                    driverPwd,
                    object : CabFareCallback<DriverDetails> {
                        override
                        fun onSuccess(result: DriverDetails) {
                            // Sign In Successful
                        }

                        override
                        fun onError(error: CabFareException) {
                            // Sign In Failed
                        }
                    }
            )
````   

You will need to register for the broadcast events that you want to receive from the SDK.

This is an example on how you should register for intents:
`this` is the context

```kotlin
override fun onStart() {
    super.onStart()
    val intentFilter = IntentFilter(CabFare.ACTION_CABFARE_TRIP_UPDATED)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_NOT_FOUND)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_CANCELLED)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_CANCEL_ERROR)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_ENDED)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_END_ERROR)
    intentFilter.addAction(CabFare.ACTION_CABFARE_SHIFT_STARTED)
    intentFilter.addAction(CabFare.ACTION_CABFARE_SHIFT_START_ERROR)
    intentFilter.addAction(CabFare.ACTION_CABFARE_SHIFT_ENDED)
    intentFilter.addAction(CabFare.ACTION_CABFARE_SHIFT_END_ERROR)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_PAID)
    intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_PAY_ERROR)
    LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
}
```

This is an example on how we can listen to the broadcast of intents:
```kotlin
private val broadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            CabFare.ACTION_CABFARE_SHIFT_STARTED ->
                // When this action happen shift has started
                // do something here
            CabFare.ACTION_CABFARE_SHIFT_START_ERROR -> {
                // Shift cannot be started because of error
                // do something here
            }
            CabFare.ACTION_CABFARE_TRIP_UPDATED -> {
                // Trip info has been updated
                // do something here
            }
            CabFare.ACTION_CABFARE_SHIFT_ENDED -> {
                // When this action happen shift has ended
                // do something here
            }
            else ->
                // You might want to handle other intent action
        }

    }
}
```

Depending on the activity, you might want to receive other broadcast event only for someother events as 
shown in the example below

```kotlin
private val broadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            CabFare.ACTION_CABFARE_TRIP_UPDATED -> updateCurrentTripDetails(intent.getStringExtra(Constants.BROADCAST_MESSAGE))
            CabFare.ACTION_CABFARE_TRIP_PAID -> showFinishedPopup()
            CabFare.ACTION_CABFARE_TRIP_CANCELLED,
            CabFare.ACTION_CABFARE_TRIP_NOT_FOUND -> showCancelledPopup()
            CabFare.ACTION_CABFARE_SHIFT_ENDED -> finish()
            else -> driverTV.setText(intent?.action)
        }
    }
}
```

Please remeber to unregister the intents when you don't want to listen to the intents anymore 
```kotlin
LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
```

### Ending a Driver trip
```kotlin
CBFDriver.getInstance().endCurrentJourney()
```

### Getting the current trip status
`true` is to use server data and `false` is to use cached data 

```kotlin
CBFDriver.getInstance().getCurrentTrip(true)
```

### Driver Logout
```kotlin
CBFDriver.getInstance().signOut(object : CabFareCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                // successful signout
            }

            override fun onError(error: CabFareException) {
                // signout failed
            }

        })
```        

## Starting as a Rider
`this` is the activity object where the call is made. 

```kotlin
CBFRider.getInstance().signIn(this,
                                object : CabFareCallback<String> {
                                    override
                                    fun onSuccess(result: String) {
                                       // SignIn Successful                                     
                                    }

                                    override
                                    fun onError(error: CabFareException) {
                                       // SignIn Failed
                                    }
                                }
                        )
```

Similar to Driver, as Rider you will need to listen to broadcast event.
```kotlin
override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(CabFare.ACTION_CABFARE_TRIP_STARTED)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_UPDATED)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_START_FAILED)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_NOT_FOUND)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_CANCELLED)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_CANCEL_ERROR)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_ENDED)
        intentFilter.addAction(CabFare.ACTION_CABFARE_TRIP_END_ERROR)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }
```

Depending on the relevant events, you might want to handle it accordingly.
```kotlin
private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                CabFare.ACTION_CABFARE_TRIP_UPDATED -> {
                    val tripDetails = CBFRider.getInstance().getCurrentTrip(false)

                    if(tripDetails?.driver == null)
                        return

                    bt_cancel_trip.visibility = View.VISIBLE
                    var text = String.format("Trip Id: %s", tripDetails.tripId)
                    text = String.format("%s\nDriver Id: %s", text, tripDetails.driver?.driverId)
                    text = String.format("%s\nDriver Name: %s", text, tripDetails.driver?.firstName)
                    text = String.format("%s\nCompany: %s", text, tripDetails.driver?.company)
                    text = String.format("%s\nVehicle Id: %s", text, tripDetails.vehicle.registrationNumber)

                    val tripStart = Calendar.getInstance()
                    val df = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'", Locale.getDefault())
                    df.timeZone = TimeZone.getTimeZone("UTC");
                    tripStart.time = df.parse(tripDetails.pickupTime)
                    val sinceMilli = Calendar.getInstance().timeInMillis - tripStart.timeInMillis
                    val hour = (sinceMilli / (60 * 60 * 1000)).toInt()
                    val minutes = ((sinceMilli / (60 * 1000)) % 60).toInt()
                    val timeSince = String.format(Locale.getDefault(), "%02d:%02d", hour, minutes)
                    text = String.format("%s\nProgress Time: %s", text, timeSince)

                    text = String.format("%s\nPick Up: %s", text, tripDetails.pickupLocation.name)
                    text = String.format("%s\nTrip Status: %s", text, tripDetails.tripStatus().name)
                    text = String.format("%s\n\n%s", text, intent.getStringExtra(Constants.BROADCAST_MESSAGE))
                    tv_rider.text = text
                }
                CabFare.ACTION_CABFARE_TRIP_START_FAILED -> bt_retry.visibility = View.VISIBLE
                CabFare.ACTION_CABFARE_TRIP_ENDED -> showTripEndPopup("Journey Completed")
                CabFare.ACTION_CABFARE_TRIP_CANCELLED -> showTripEndPopup("Trip Cancelled")
                else -> tv_rider.setText(intent?.action)
            }
        }
    }
```

Please remeber to unregister the intents when you don't want to listen to the intents anymore 
```kotlin
LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
```

## Shift
Every driver in Cabfare will need to start a shift before they are ready for a journey with passengers.
 
### Starting Driver Shift
`this` is the activity object where the call is made

```kotlin
CBFDriver.getInstance().startShift(this)
```

After the shift started, the Cabfare SDK will broadcast intents with different action which you might want to action on.

## Trip
Every driver in Cabfare will be paired automatically with a rider as soon as the rider enter the vehicle. The rider in Cabfare SDK will have the ability to start trip automatically. Driver will automatically paired to the trip in the same vehicle that the rider is in.

All the events will be broaccasted by the LocalBroadcastManager.

## Push Notification
Please implement the the following for push notification.  Cabfare will only handle push notifications that comes for the SDK ignoring the rest.

```kotlin
FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(activity) { instanceIdResult ->
                                            val newToken = instanceIdResult.token
                                            newToken.let {
                                                notificationsUseCaseFactory.setFirebaseTokenUseCase(it).buildObservable()
                                                        .subscribe {
                                                            callback.onSuccess(result)
                                                        }
                                            }
                                        }
```
