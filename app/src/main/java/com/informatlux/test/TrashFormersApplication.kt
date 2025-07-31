package com.informatlux.test

import android.app.Application
import org.osmdroid.config.Configuration

class TrashFormersApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        UserManager.initialize(this)
        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        // Setting a user agent is important for map tile providers
        Configuration.getInstance().userAgentValue = packageName
    }
}
