package com.cabfare.android.sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.ProgressDialog
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.cabfare.android.sdk.R
import com.cabfare.android.sdk.fcm.CBFNotificationsManager

var progressDialog: ProgressDialog? = null

const val CHANNEL_ID = "Sample Channel Id"

fun AppCompatActivity.showLoading() {
    if (progressDialog == null)
        progressDialog = ProgressDialog.show(this, null, "Loading")
    else
        progressDialog?.show()
}

fun AppCompatActivity.hideLoading() {
    progressDialog?.dismiss()
    progressDialog = null
}

fun AppCompatActivity.sendNotification(message: String) {

    createNotificationChannel()
    Log.d("CabFareMessaging", message)
    val pm = packageManager
    val intent = pm.getLaunchIntentForPackage(applicationContext.packageName)
    intent.putExtra(CBFNotificationsManager.NOTIFICATION_PAYLOAD_EXTRA, message)

    var contentIntent: PendingIntent? = null
    if (intent != null)
        contentIntent = PendingIntent.getActivity(this.applicationContext, 0, intent, FLAG_UPDATE_CURRENT)

    val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(resources.getString(R.string.app_name))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle() // make notification expandable
                    .bigText(message))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)

    val notificationManager: NotificationManagerCompat =
            NotificationManagerCompat.from(applicationContext)
    notificationManager.notify(123, notificationBuilder.build())
}

fun AppCompatActivity.createNotificationChannel() {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Normal"
        val description = "All Notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)

        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        channel.description = description
        channel.enableVibration(true)
        channel.setSound(defaultSoundUri, attributes)

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
    }
}