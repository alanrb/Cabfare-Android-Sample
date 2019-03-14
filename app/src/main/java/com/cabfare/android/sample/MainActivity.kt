package com.cabfare.android.sample

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import com.cabfare.android.sdk.CBFRider
import com.cabfare.android.sdk.CabFare
import com.cabfare.android.sdk.CabFareCallback
import com.cabfare.android.sdk.exceptions.CabFareException
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    val pref: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "OnCreate")
        setContentView(R.layout.activity_main)

        tv_client_id.text = String.format(Locale.getDefault(), "Client ID: %s", SAMPLE_CLIENT_ID)
        tv_tag.text = String.format(Locale.getDefault(), "Tag: %s", CabFare.getInstance().getEstimoteTag())

        when {
            pref.getBoolean(Constants.KEY_IS_DRIVER_LOGGED_IN, false) -> {
                val intent = Intent(this@MainActivity, DriverDashboardActivity::class.java)
                intent.putExtras(getIntent())
                startActivity(intent)
                finish()
            }
            pref.getBoolean(Constants.KEY_IS_RIDER_LOGGED_IN, false) -> {
                val intent = Intent(this@MainActivity, RiderTripActivity::class.java)
                intent.putExtras(getIntent())
                startActivity(intent)
                finish()
            }
            else -> {
                findViewById<Button>(R.id.bt_rider_login)
                        .setOnClickListener {
                            showLoading()
                            CBFRider.getInstance().signIn(this,
                                    object : CabFareCallback<String> {
                                        override
                                        fun onSuccess(result: String) {
                                            hideLoading()
                                            pref.edit().putBoolean(Constants.KEY_IS_RIDER_LOGGED_IN, true).apply()

                                            startActivity(Intent(this@MainActivity, RiderTripActivity::class.java))
                                            finish()
                                        }

                                        override
                                        fun onError(error: CabFareException) {
                                            hideLoading()
                                        }
                                    }
                            )
                        }

                findViewById<Button>(R.id.bt_driver_login)
                        .setOnClickListener {
                            startActivity(Intent(this@MainActivity, DriverLoginActivity::class.java))
                            finish()
                        }
            }
        }
    }
}
