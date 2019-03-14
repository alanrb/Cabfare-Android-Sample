package com.cabfare.android.sample

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.cabfare.android.sdk.CBFDriver
import com.cabfare.android.sdk.CabFareCallback
import com.cabfare.android.sdk.exceptions.CabFareException
import com.cabfare.android.sdk.model.DriverDetails
import com.google.firebase.iid.FirebaseInstanceId

class DriverLoginActivity : AppCompatActivity() {

    val driverIdET: EditText by lazy { findViewById(R.id.et_driver_id) as EditText }
    val driverPasswordET: EditText by lazy { findViewById(R.id.et_driver_password) as EditText }
    val loginBT: Button by lazy { findViewById(R.id.bt_driver_login) as Button }
    val driverTV: TextView by lazy { findViewById(R.id.tv_driver) as TextView }

    val pref: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)

        if (pref.getBoolean(Constants.KEY_IS_DRIVER_LOGGED_IN, false))
            CBFDriver.getInstance().getDriverDetails(
                    object : CabFareCallback<DriverDetails> {
                        override
                        fun onSuccess(result: DriverDetails) {
                            var text = String.format("Name: %s %s", result.firstName, result.lastName)
                            text = String.format("%s\nDriver Id: %s", text, result.driverId)
                            text = String.format("%s\nABN: %s", text, result.abn)
                            text = String.format("%s\nCompany: %s", text, result.company)
                            text = String.format("%s\nService Fee: %s", text, result.serviceFee)
                            driverTV.setText(text)
                        }

                        override
                        fun onError(error: CabFareException) {
                            driverTV.setText(error.message)
                        }
                    }
            )

        loginBT.setOnClickListener {
            val driverId = driverIdET.text.toString()
            val driverPwd = driverPasswordET.text.toString()

            if (driverId.isBlank()) {
                Toast.makeText(this@DriverLoginActivity, "Please enter the driver id", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (driverPwd.isBlank()) {
                Toast.makeText(this@DriverLoginActivity, "Please enter the driver pin", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(this) { instanceIdResult ->
                val newToken = instanceIdResult.token
                Log.e("newToken", newToken)
            }

            showLoading()
            CBFDriver.getInstance().signIn(
                    this,
                    driverId,
                    driverPwd,
                    object : CabFareCallback<DriverDetails> {
                        override
                        fun onSuccess(result: DriverDetails) {
                            hideLoading()
                            var text = String.format("Name: %s %s", result.firstName, result.lastName)
                            text = String.format("%s\nDriver Id: %s", text, result.driverId)
                            text = String.format("%s\nABN: %s", text, result.abn)
                            text = String.format("%s\nCompany: %s", text, result.company)
                            text = String.format("%s\nService Fee: %s", text, result.serviceFee)
                            driverTV.setText(text)

                            pref.edit().putBoolean(Constants.KEY_IS_DRIVER_LOGGED_IN, true).apply()

                            startActivity(Intent(this@DriverLoginActivity, DriverDashboardActivity::class.java))
                            finish()
                        }

                        override
                        fun onError(error: CabFareException) {
                            hideLoading()
                            driverTV.setText(error.message)
                        }
                    }
            )
        }
    }
}
