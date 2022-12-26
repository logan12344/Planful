package com.production.planful.commons.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import com.production.planful.R
import com.production.planful.commons.dialogs.ConfirmationAdvancedDialog
import com.production.planful.commons.dialogs.RadioGroupDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import com.production.planful.commons.models.MyTheme
import com.production.planful.commons.models.RadioItem
import com.production.planful.commons.models.SharedTheme
import com.production.planful.commons.views.MyTextView
import kotlinx.android.synthetic.main.activity_customization.*

class CustomizationActivity : BaseSimpleActivity() {
    private val THEME_LIGHT = 0
    private val THEME_DARK = 1
    private val THEME_BLACK_WHITE = 4
    private val THEME_CUSTOM = 5
    private val THEME_SHARED = 6
    private val THEME_WHITE = 7
    private val THEME_AUTO = 8
    private val THEME_SYSTEM = 9    // Material You

    private var curTextColor = 0
    private var curBackgroundColor = 0
    private var curPrimaryColor = 0
    private var curAccentColor = 0
    private var curAppIconColor = 0
    private var curSelectedThemeId = 0
    private var originalAppIconColor = 0
    private var lastSavePromptTS = 0L
    private var curNavigationBarColor = INVALID_NAVIGATION_BAR_COLOR
    private var hasUnsavedChanges = false
    private var predefinedThemes = LinkedHashMap<Int, MyTheme>()
    private var storedSharedTheme: SharedTheme? = null

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customization)
        setupToolbar(customization_toolbar, NavigationIcon.Cross)

        if (baseConfig.defaultNavigationBarColor == INVALID_NAVIGATION_BAR_COLOR && baseConfig.navigationBarColor == INVALID_NAVIGATION_BAR_COLOR) {
            baseConfig.defaultNavigationBarColor = window.navigationBarColor
            baseConfig.navigationBarColor = window.navigationBarColor
        }

        setupOptionsMenu()
        refreshMenuItems()
        initColorVariables()

        setupThemes()
        baseConfig.isUsingSharedTheme = false

        val textColor = if (baseConfig.isUsingSystemTheme) {
            getProperTextColor()
        } else {
            baseConfig.textColor
        }

        updateLabelColors(textColor)
        originalAppIconColor = baseConfig.appIconColor
    }

    override fun onResume() {
        super.onResume()

        if (!baseConfig.isUsingSystemTheme) {
            updateBackgroundColor(getCurrentBackgroundColor())
            updateActionbarColor(getCurrentStatusBarColor())
            updateNavigationBarColor(curNavigationBarColor)
        }
    }

    private fun refreshMenuItems() {
        customization_toolbar.menu.findItem(R.id.save).isVisible = hasUnsavedChanges
    }

    private fun setupOptionsMenu() {
        customization_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> {
                    saveChanges(true)
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (hasUnsavedChanges && System.currentTimeMillis() - lastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL) {
            promptSaveDiscard()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupThemes() {
        predefinedThemes.apply {
            put(
                THEME_LIGHT,
                MyTheme(
                    getString(R.string.light_theme),
                    R.color.theme_light_text_color,
                    R.color.theme_light_background_color,
                    R.color.color_primary,
                    R.color.color_primary
                )
            )
            put(
                THEME_DARK,
                MyTheme(
                    getString(R.string.dark_theme),
                    R.color.theme_dark_text_color,
                    R.color.theme_dark_background_color,
                    R.color.color_primary,
                    R.color.color_primary
                )
            )

            if (storedSharedTheme != null) {
                put(THEME_SHARED, MyTheme(getString(R.string.shared), 0, 0, 0, 0))
            }
        }
        setupThemePicker()
    }

    private fun setupThemePicker() {
        curSelectedThemeId = getCurrentThemeId()
        customization_theme.text = getThemeText()
        customization_theme_holder.setOnClickListener {
            themePickerClicked()
        }
    }

    private fun themePickerClicked() {
        val items = arrayListOf<RadioItem>()
        for ((key, value) in predefinedThemes) {
            items.add(RadioItem(key, value.label))
        }

        RadioGroupDialog(this@CustomizationActivity, items, curSelectedThemeId) {
            updateColorTheme(it as Int, true)
            if (it != THEME_CUSTOM && it != THEME_SHARED && it != THEME_AUTO && it != THEME_SYSTEM && !baseConfig.wasCustomThemeSwitchDescriptionShown) {
                baseConfig.wasCustomThemeSwitchDescriptionShown = true
                toast(R.string.changing_color_description)
            }
            updateMenuItemColors(customization_toolbar.menu, true, getCurrentStatusBarColor())
            setupToolbar(customization_toolbar, NavigationIcon.Cross, getCurrentStatusBarColor())
        }
    }

    private fun updateColorTheme(themeId: Int, useStored: Boolean = false) {
        curSelectedThemeId = themeId
        customization_theme.text = getThemeText()

        resources.apply {
            if (curSelectedThemeId == THEME_CUSTOM) {
                if (useStored) {
                    curTextColor = baseConfig.customTextColor
                    curBackgroundColor = baseConfig.customBackgroundColor
                    curPrimaryColor = baseConfig.customPrimaryColor
                    curAccentColor = baseConfig.customAccentColor
                    curNavigationBarColor = baseConfig.customNavigationBarColor
                    curAppIconColor = baseConfig.customAppIconColor
                    updateMenuItemColors(customization_toolbar.menu, true, curPrimaryColor)
                    setupToolbar(customization_toolbar, NavigationIcon.Cross, curPrimaryColor)
                } else {
                    baseConfig.customPrimaryColor = curPrimaryColor
                    baseConfig.customAccentColor = curAccentColor
                    baseConfig.customBackgroundColor = curBackgroundColor
                    baseConfig.customTextColor = curTextColor
                    baseConfig.customNavigationBarColor = curNavigationBarColor
                    baseConfig.customAppIconColor = curAppIconColor
                }
            } else if (curSelectedThemeId == THEME_SHARED) {
                if (useStored) {
                    storedSharedTheme?.apply {
                        curTextColor = textColor
                        curBackgroundColor = backgroundColor
                        curPrimaryColor = primaryColor
                        curAccentColor = accentColor
                        curAppIconColor = appIconColor
                        curNavigationBarColor = navigationBarColor
                    }
                    updateMenuItemColors(customization_toolbar.menu, true, curPrimaryColor)
                    setupToolbar(customization_toolbar, NavigationIcon.Cross, curPrimaryColor)
                }
            } else {
                val theme = predefinedThemes[curSelectedThemeId]!!
                curTextColor = getColor(theme.textColorId)
                curBackgroundColor = getColor(theme.backgroundColorId)

                if (curSelectedThemeId != THEME_AUTO && curSelectedThemeId != THEME_SYSTEM) {
                    curPrimaryColor = getColor(theme.primaryColorId)
                    curAccentColor = getColor(R.color.color_primary)
                    curAppIconColor = getColor(theme.appIconColorId)
                }

                curNavigationBarColor = getThemeNavigationColor(curSelectedThemeId)
                colorChanged()
                updateMenuItemColors(customization_toolbar.menu, true, getCurrentStatusBarColor())
                setupToolbar(
                    customization_toolbar,
                    NavigationIcon.Cross,
                    getCurrentStatusBarColor()
                )
            }
        }

        hasUnsavedChanges = true
        refreshMenuItems()
        updateLabelColors(getCurrentTextColor())
        updateBackgroundColor(getCurrentBackgroundColor())
        updateActionbarColor(getCurrentStatusBarColor())
        updateNavigationBarColor(curNavigationBarColor, true)
    }

    private fun getCurrentThemeId(): Int {
        if (baseConfig.isUsingSharedTheme) {
            return THEME_SHARED
        } else if ((baseConfig.isUsingSystemTheme && !hasUnsavedChanges) || curSelectedThemeId == THEME_SYSTEM) {
            return THEME_SYSTEM
        } else if (baseConfig.isUsingAutoTheme || curSelectedThemeId == THEME_AUTO) {
            return THEME_AUTO
        }

        var themeId = THEME_CUSTOM
        resources.apply {
            for ((key, value) in predefinedThemes.filter { it.key != THEME_CUSTOM && it.key != THEME_SHARED && it.key != THEME_AUTO && it.key != THEME_SYSTEM }) {
                if (curTextColor == getColor(value.textColorId) &&
                    curBackgroundColor == getColor(value.backgroundColorId) &&
                    curPrimaryColor == getColor(value.primaryColorId) &&
                    curAppIconColor == getColor(value.appIconColorId) &&
                    (curNavigationBarColor == baseConfig.defaultNavigationBarColor || curNavigationBarColor == -2)
                ) {
                    themeId = key
                }
            }
        }

        return themeId
    }

    private fun getThemeText(): String {
        var label = getString(R.string.custom)
        for ((key, value) in predefinedThemes) {
            if (key == curSelectedThemeId) {
                label = value.label
            }
        }
        return label
    }

    private fun getThemeNavigationColor(themeId: Int) = when (themeId) {
        THEME_BLACK_WHITE -> Color.BLACK
        THEME_WHITE -> Color.WHITE
        THEME_AUTO -> if (isUsingSystemDarkTheme()) Color.BLACK else -2
        THEME_LIGHT -> Color.WHITE
        THEME_DARK -> Color.BLACK
        else -> baseConfig.defaultNavigationBarColor
    }

    private fun promptSaveDiscard() {
        lastSavePromptTS = System.currentTimeMillis()
        ConfirmationAdvancedDialog(
            this,
            "",
            R.string.save_before_closing,
            R.string.save,
            R.string.discard
        ) {
            if (it) {
                saveChanges(true)
            } else {
                resetColors()
                finish()
            }
        }
    }

    private fun saveChanges(finishAfterSave: Boolean) {
        val didAppIconColorChange = curAppIconColor != originalAppIconColor
        baseConfig.apply {
            textColor = curTextColor
            backgroundColor = curBackgroundColor
            primaryColor = curPrimaryColor
            accentColor = curAccentColor
            appIconColor = curAppIconColor

            // -1 is used as an invalid value, lets make use of it for white
            navigationBarColor = if (curNavigationBarColor == INVALID_NAVIGATION_BAR_COLOR) {
                -2
            } else {
                curNavigationBarColor
            }
        }

        if (didAppIconColorChange) {
            checkAppIconColor()
        }

        if (curSelectedThemeId == THEME_SHARED) {
            val newSharedTheme = SharedTheme(
                curTextColor,
                curBackgroundColor,
                curPrimaryColor,
                curAppIconColor,
                curNavigationBarColor,
                0,
                curAccentColor
            )
            updateSharedTheme(newSharedTheme)
            Intent().apply {
                action = MyContentProvider.SHARED_THEME_UPDATED
                sendBroadcast(this)
            }
        }

        baseConfig.isUsingSharedTheme = curSelectedThemeId == THEME_SHARED
        baseConfig.shouldUseSharedTheme = curSelectedThemeId == THEME_SHARED
        baseConfig.isUsingAutoTheme = curSelectedThemeId == THEME_AUTO
        baseConfig.isUsingSystemTheme = curSelectedThemeId == THEME_SYSTEM

        hasUnsavedChanges = false
        if (finishAfterSave) {
            finish()
        } else {
            refreshMenuItems()
        }
    }

    private fun resetColors() {
        hasUnsavedChanges = false
        initColorVariables()
        updateBackgroundColor()
        updateActionbarColor()
        updateNavigationBarColor()
        refreshMenuItems()
        updateLabelColors(getCurrentTextColor())
    }

    private fun initColorVariables() {
        curTextColor = baseConfig.textColor
        curBackgroundColor = baseConfig.backgroundColor
        curPrimaryColor = baseConfig.primaryColor
        curAccentColor = baseConfig.accentColor
        curAppIconColor = baseConfig.appIconColor
        curNavigationBarColor = baseConfig.navigationBarColor
    }

    private fun colorChanged() {
        hasUnsavedChanges = true
        refreshMenuItems()
    }

    private fun updateLabelColors(textColor: Int) {
        arrayListOf<MyTextView>(
            customization_theme_label,
            customization_theme
        ).forEach {
            it.setTextColor(textColor)
        }
    }

    private fun getCurrentTextColor() =
        if (customization_theme.value == getString(R.string.system_default)) {
            resources.getColor(R.color.you_neutral_text_color)
        } else {
            curTextColor
        }

    private fun getCurrentBackgroundColor() =
        if (customization_theme.value == getString(R.string.system_default)) {
            resources.getColor(R.color.you_background_color)
        } else {
            curBackgroundColor
        }

    private fun getCurrentPrimaryColor() =
        if (customization_theme.value == getString(R.string.system_default)) {
            resources.getColor(R.color.you_primary_color)
        } else {
            curPrimaryColor
        }

    private fun getCurrentStatusBarColor() =
        if (customization_theme.value == getString(R.string.system_default)) {
            resources.getColor(R.color.you_status_bar_color)
        } else {
            curPrimaryColor
        }
}
