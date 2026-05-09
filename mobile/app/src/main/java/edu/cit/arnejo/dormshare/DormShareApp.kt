package edu.cit.arnejo.dormshare

import android.app.Application
import edu.cit.arnejo.dormshare.api.RetrofitClient

/**
 * Custom Application class that initialises app-wide singletons.
 *
 * Registered in AndroidManifest.xml via android:name=".DormShareApp".
 */
class DormShareApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Provide the application context to RetrofitClient so its auth
        // interceptor can read the persisted JWT from SharedPreferences
        // even after process death / recreation.
        RetrofitClient.init(this)
    }
}
