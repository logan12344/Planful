package com.production.planful

import androidx.multidex.MultiDexApplication
import com.production.planful.commons.extensions.checkUseEnglish

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }
}
