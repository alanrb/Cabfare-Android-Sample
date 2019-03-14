package com.cabfare.android.sample

import android.app.Application
import com.cabfare.android.sdk.CabFare
import com.cabfare.android.sdk.api.model.Variant

/**
 * Created by thomasruffie
 *
 * CabFare custom Application class
 */


val SAMPLE_CLIENT_ID = "ECDD30800A72596AA1CF8F50693A8E84"
val SAMPLE_CLIENT_SECRET = "5CB7D2C081D6BB70B4E98A1194BF799A"

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CabFare.init(this.applicationContext, SAMPLE_CLIENT_ID, SAMPLE_CLIENT_SECRET, Variant.TESTING)

    }
}