package com.production.planful.commons.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.core.net.toUri
import com.production.planful.R
import com.production.planful.commons.dialogs.ConfirmationAdvancedDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : BaseSimpleActivity() {
    private var appName = ""
    private var primaryColor = 0

    private var firstVersionClickTS = 0L
    private var clicksSinceFirstClick = 0
    private val EASTER_EGG_TIME_LIMIT = 3000L
    private val EASTER_EGG_REQUIRED_CLICKS = 7

    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        appName = intent.getStringExtra(APP_NAME) ?: ""
        val textColor = getProperTextColor()
        val backgroundColor = getProperBackgroundColor()
        primaryColor = getProperPrimaryColor()

        arrayOf(
            about_email_icon,
            about_version_icon
        ).forEach {
            it.applyColorFilter(textColor)
        }

        arrayOf(about_support, about_other).forEach {
            it.setTextColor(primaryColor)
        }

        arrayOf(
            about_support_holder,
            about_other_holder
        ).forEach {
            it.background.applyColorFilter(backgroundColor.getContrastColor())
        }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(about_nested_scrollview)
        setupToolbar(about_toolbar, NavigationIcon.Arrow)

        setupEmail()
        setupVersion()
    }

    private fun setupEmail() {

        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            about_email_holder.beGone()
        }

        about_email_holder.setOnClickListener {
            val appVersion = String.format(
                getString(
                    R.string.app_version,
                    intent.getStringExtra(APP_VERSION_NAME)
                )
            )
            val deviceOS = String.format(getString(R.string.device_os), Build.VERSION.RELEASE)
            val newline = "\n"
            val separator = "------------------------------"
            val body = "$appVersion$newline$deviceOS$newline$separator$newline$newline"

            val address = getString(R.string.my_email)
            val selectorIntent = Intent(ACTION_SENDTO)
                .setData("mailto:$address".toUri())
            val emailIntent = Intent(ACTION_SEND).apply {
                putExtra(EXTRA_EMAIL, arrayOf(address))
                putExtra(EXTRA_SUBJECT, appName)
                putExtra(EXTRA_TEXT, body)
                selector = selectorIntent
            }

            try {
                startActivity(emailIntent)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_email_client_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun setupVersion() {
        val version = intent.getStringExtra(APP_VERSION_NAME) ?: ""

        val fullVersion = String.format(getString(R.string.version_placeholder, version))
        about_version.text = fullVersion
        about_version_holder.setOnClickListener {
            if (firstVersionClickTS == 0L) {
                firstVersionClickTS = System.currentTimeMillis()
                Handler().postDelayed({
                    firstVersionClickTS = 0L
                    clicksSinceFirstClick = 0
                }, EASTER_EGG_TIME_LIMIT)
            }

            clicksSinceFirstClick++
            if (clicksSinceFirstClick >= EASTER_EGG_REQUIRED_CLICKS) {
                toast(R.string.hello)
                firstVersionClickTS = 0L
                clicksSinceFirstClick = 0
            }
        }
    }
}
