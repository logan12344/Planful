package com.production.planful.activities

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.production.planful.R
import com.production.planful.commons.activities.BaseSplashActivity
import com.production.planful.extensions.getNewEventTimestampFromCode
import com.production.planful.helpers.*
import org.joda.time.DateTime

class SplashActivity : BaseSplashActivity() {
    private val SPLASH_DELAY = 1_500L
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun initActivity() {
        setContentView(R.layout.splash_activity)
        handler.postDelayed({
            when {
                intent.extras?.containsKey(DAY_CODE) == true -> Intent(
                    this,
                    MainActivity::class.java
                ).apply {
                    putExtra(DAY_CODE, intent.getStringExtra(DAY_CODE))
                    putExtra(VIEW_TO_OPEN, intent.getIntExtra(VIEW_TO_OPEN, LAST_VIEW))
                    startActivity(this)
                }
                intent.extras?.containsKey(EVENT_ID) == true -> Intent(
                    this,
                    MainActivity::class.java
                ).apply {
                    putExtra(EVENT_ID, intent.getLongExtra(EVENT_ID, 0L))
                    putExtra(EVENT_OCCURRENCE_TS, intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L))
                    startActivity(this)
                }
                intent.action == SHORTCUT_NEW_EVENT -> {
                    val dayCode = Formatter.getDayCodeFromDateTime(DateTime())
                    Intent(this, EventActivity::class.java).apply {
                        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
                        startActivity(this)
                    }
                }
                intent.action == SHORTCUT_NEW_TASK -> {
                    val dayCode = Formatter.getDayCodeFromDateTime(DateTime())
                    Intent(this, TaskActivity::class.java).apply {
                        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
                        startActivity(this)
                    }
                }
                else -> startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, SPLASH_DELAY)
    }
}
