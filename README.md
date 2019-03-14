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
override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CabFare.getInstance().registerCabFareEventListener(cabFareEventListener)
}
```

This is an example on how we can listen to Cabfare events:
```kotlin
private val cabFareEventListener = object : CabFareEventListener {
        override fun onEvent(cabFareEvent: CabFareEvent, data: Bundle) {
            if (isFinishing)
                return
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

Depending on the activity, you might only care about specific events as 
shown in the example below

```kotlin
private val cabFareEventListener = object : CabFareEventListener {
        override fun onEvent(cabFareEvent: CabFareEvent, data: Bundle) {
            if (isFinishing)
                return

            when (cabFareEvent) {
                CabFareEvent.BEACON_RANGED -> // Updated List of Beacons
                CabFareEvent.TRIP_ENDED -> // Trip Ended
                CabFareEvent.TRIP_UPDATED -> // Trip Updated
                CabFareEvent.TRIP_PAID -> // Trip Paid for
                CabFareEvent.TRIP_CANCELLED -> // Trip Cancelled
                CabFareEvent.TRIP_NOT_FOUND -> // Trip Not Found
                CabFareEvent.SHIFT_ENDED -> // Driver Shift Ended
                else -> {
                /* Do Nothing*/
                }
            }
        }
}
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

Similar to Driver, as Rider you will need to listen to cabfare events.
```kotlin
 override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CabFare.getInstance().registerCabFareEventListener(cabFareEventListener)
}
```

Depending on the relevant events, you might want to handle it accordingly.
```kotlin
private val cabFareEventListener = object : CabFareEventListener {
        override fun onEvent(cabFareEvent: CabFareEvent, data: Bundle) {
            if (isFinishing)
                return

            hideLoading()
            Log.d("RiderTripActivity", cabFareEvent.name)
            when (cabFareEvent) {
                CabFareEvent.BEACON_PAIRED -> // Paired with a beacon
                CabFareEvent.TRIP_STARTED -> // Trip started
                CabFareEvent.TRIP_UPDATED -> // Trip details updated
                CabFareEvent.TRIP_NOT_FOUND -> // Trip not found
                CabFareEvent.TRIP_START_FAILED -> // Error when trying to start a trip
                else -> {
                /* Do Nothing*/
                }
            }
        }
}
```

## Shift
Every driver in Cabfare will need to start a shift before they are ready for a journey with passengers.
 
### Starting Driver Shift
`this` is the activity object where the call is made

```kotlin
CBFDriver.getInstance().startShift(this)
```

After the shift started, the Cabfare SDK will start emitting different events which you might want to take action on.

## Trip
Every driver in Cabfare will be paired automatically with a rider as soon as the rider enter the vehicle. The rider in Cabfare SDK will have the ability to start trip automatically. Driver will automatically paired to the trip in the same vehicle that the rider is in.

All the events will be emitted via the CabFareEventListener.
