package com.production.planful.commons.extensions

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.MediaStore.*
import android.provider.OpenableColumns
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.view.WindowManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.loader.content.CursorLoader
import com.github.ajalt.reprint.core.Reprint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.production.planful.R
import com.production.planful.commons.helpers.*
import com.production.planful.commons.models.AlarmSound
import com.production.planful.commons.models.BlockedNumber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun Context.getSharedPrefs(): SharedPreferences = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

val Context.areSystemAnimationsEnabled: Boolean
    get() = Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        0f
    ) > 0f

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (_: Exception) {
    }
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.error), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)
val Context.sdCardPath: String get() = baseConfig.sdCardPath
val Context.internalStoragePath: String get() = baseConfig.internalStoragePath
val Context.otgPath: String get() = baseConfig.OTGPath

fun isFingerPrintSensorAvailable() = isMarshmallowPlus() && Reprint.isHardwarePresent()

fun Context.isBiometricIdAvailable(): Boolean = when (BiometricManager.from(this)
    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
    BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> true
    else -> false
}

private fun Context.queryCursorDesc(
    uri: Uri,
    projection: Array<String>,
    sortColumn: String,
    limit: Int,
): Cursor? {
    return if (isRPlus()) {
        val queryArgs = bundleOf(
            ContentResolver.QUERY_ARG_LIMIT to limit,
            ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(sortColumn),
        )
        contentResolver.query(uri, projection, queryArgs, null)
    } else {
        val sortOrder = "$sortColumn DESC LIMIT $limit"
        contentResolver.query(uri, projection, null, null, sortOrder)
    }
}

fun Context.getDataColumn(
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null
): String? {
    try {
        val projection = arrayOf(Files.FileColumns.DATA)
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val data = cursor.getStringValue(Files.FileColumns.DATA)
                if (data != "null") {
                    return data
                }
            }
        }
    } catch (_: Exception) {
    }
    return null
}

fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(
    this,
    getPermissionString(permId)
) == PackageManager.PERMISSION_GRANTED

fun getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
    PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
    PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
    PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
    PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
    PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
    PERMISSION_MEDIA_LOCATION -> if (isQPlus()) Manifest.permission.ACCESS_MEDIA_LOCATION else ""
    PERMISSION_POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
    PERMISSION_READ_MEDIA_IMAGES -> Manifest.permission.READ_MEDIA_IMAGES
    PERMISSION_READ_MEDIA_VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
    PERMISSION_READ_MEDIA_AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
    else -> ""
}

fun Context.launchActivityIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(R.string.no_app_found)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.getFilePublicUri(file: File, applicationId: String): Uri {
    // for images/videos/gifs try getting a media content uri first, like content://media/external/images/media/438
    // if media content uri is null, get our custom uri like content://com.production.planful.gallery.provider/external_files/emulated/0/DCIM/IMG_20171104_233915.jpg
    var uri = if (file.isMediaFile()) {
        getMediaContentUri(file.absolutePath)
    } else {
        getMediaContent(file.absolutePath, Files.getContentUri("external"))
    }

    if (uri == null) {
        uri = FileProvider.getUriForFile(this, "$applicationId.provider", file)
    }

    return uri!!
}

fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> Video.Media.EXTERNAL_CONTENT_URI
        else -> Files.getContentUri("external")
    }

    return getMediaContent(path, uri)
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(Images.Media._ID)
    val selection = Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getIntValue(Images.Media._ID).toString()
                return Uri.withAppendedPath(uri, id)
            }
        }
    } catch (_: Exception) {
    }
    return null
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

fun Context.getFilenameFromUri(uri: Uri): String {
    return if (uri.scheme == "file") {
        File(uri.toString()).name
    } else {
        getFilenameFromContentUri(uri) ?: uri.lastPathSegment ?: ""
    }
}

fun Context.getMimeTypeFromUri(uri: Uri): String {
    var mimetype = uri.path?.getMimeType() ?: ""
    if (mimetype.isEmpty()) {
        try {
            mimetype = contentResolver.getType(uri) ?: ""
        } catch (_: IllegalStateException) {
        }
    }
    return mimetype
}

fun Context.ensurePublicUri(path: String, applicationId: String): Uri? {
    return when {
        hasProperStoredAndroidTreeUri(path) && isRestrictedSAFOnlyRoot(path) -> {
            getAndroidSAFUri(path)
        }
        hasProperStoredDocumentUriSdk30(path) && isAccessibleWithSAFSdk30(path) -> {
            createDocumentUriUsingFirstParentTreeUri(path)
        }
        isPathOnOTG(path) -> {
            getDocumentFile(path)?.uri
        }
        else -> {
            val uri = Uri.parse(path)
            if (uri.scheme == "content") {
                uri
            } else {
                val newPath = if (uri.toString().startsWith("/")) uri.toString() else uri.path
                val file = File(newPath)
                getFilePublicUri(file, applicationId)
            }
        }
    }
}

fun Context.getFilenameFromContentUri(uri: Uri): String? {
    val projection = arrayOf(
        OpenableColumns.DISPLAY_NAME
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
            }
        }
    } catch (_: Exception) {
    }
    return null
}

fun Context.getSizeFromContentUri(uri: Uri): Long {
    val projection = arrayOf(OpenableColumns.SIZE)
    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(OpenableColumns.SIZE)
            }
        }
    } catch (_: Exception) {
    }
    return 0L
}

fun Context.getMyContentProviderCursorLoader() =
    CursorLoader(this, MyContentProvider.MY_CONTENT_URI, null, null, null, null)

fun getCurrentFormattedDateTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
    return simpleDateFormat.format(Date(System.currentTimeMillis()))
}

fun Context.updateSDCardPath() {
    ensureBackgroundThread {
        val oldPath = baseConfig.sdCardPath
        baseConfig.sdCardPath = getSDCardPath()
        if (oldPath != baseConfig.sdCardPath) {
            baseConfig.sdTreeUri = ""
        }
    }
}

fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}

fun Context.isPackageInstalled(pkgName: String): Boolean {
    return try {
        packageManager.getPackageInfo(pkgName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

// format day bits to strings like "Mon, Tue, Wed"
fun Context.getSelectedDaysString(bitMask: Int): String {
    val dayBits = arrayListOf(
        MONDAY_BIT,
        TUESDAY_BIT,
        WEDNESDAY_BIT,
        THURSDAY_BIT,
        FRIDAY_BIT,
        SATURDAY_BIT,
        SUNDAY_BIT
    )
    val weekDays = resources.getStringArray(R.array.week_days_short).toList() as ArrayList<String>

    if (baseConfig.isSundayFirst) {
        dayBits.moveLastItemToFront()
        weekDays.moveLastItemToFront()
    }

    var days = ""
    dayBits.forEachIndexed { index, bit ->
        if (bitMask and bit != 0) {
            days += "${weekDays[index]}, "
        }
    }
    return days.trim().trimEnd(',')
}

fun Context.formatMinutesToTimeString(totalMinutes: Int) =
    formatSecondsToTimeString(totalMinutes * 60)

fun Context.formatSecondsToTimeString(totalSeconds: Int): String {
    val days = totalSeconds / DAY_SECONDS
    val hours = (totalSeconds % DAY_SECONDS) / HOUR_SECONDS
    val minutes = (totalSeconds % HOUR_SECONDS) / MINUTE_SECONDS
    val seconds = totalSeconds % MINUTE_SECONDS
    val timesString = StringBuilder()
    if (days > 0) {
        val daysString = String.format(resources.getQuantityString(R.plurals.days, days, days))
        timesString.append("$daysString, ")
    }

    if (hours > 0) {
        val hoursString = String.format(resources.getQuantityString(R.plurals.hours, hours, hours))
        timesString.append("$hoursString, ")
    }

    if (minutes > 0) {
        val minutesString =
            String.format(resources.getQuantityString(R.plurals.minutes, minutes, minutes))
        timesString.append("$minutesString, ")
    }

    if (seconds > 0) {
        val secondsString =
            String.format(resources.getQuantityString(R.plurals.seconds, seconds, seconds))
        timesString.append(secondsString)
    }

    var result = timesString.toString().trim().trimEnd(',')
    if (result.isEmpty()) {
        result = String.format(resources.getQuantityString(R.plurals.minutes, 0, 0))
    }
    return result
}

fun Context.getFormattedMinutes(minutes: Int, showBefore: Boolean = true) =
    getFormattedSeconds(if (minutes == -1) minutes else minutes * 60, showBefore)

fun Context.getFormattedSeconds(seconds: Int, showBefore: Boolean = true) = when (seconds) {
    -1 -> getString(R.string.no_reminder)
    0 -> getString(R.string.at_start)
    else -> {
        when {
            seconds < 0 && seconds > -60 * 60 * 24 -> {
                val minutes = -seconds / 60
                getString(R.string.during_day_at).format(minutes / 60, minutes % 60)
            }
            seconds % YEAR_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.years_before else R.plurals.by_years
                resources.getQuantityString(base, seconds / YEAR_SECONDS, seconds / YEAR_SECONDS)
            }
            seconds % MONTH_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.months_before else R.plurals.by_months
                resources.getQuantityString(base, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            }
            seconds % WEEK_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.weeks_before else R.plurals.by_weeks
                resources.getQuantityString(base, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            }
            seconds % DAY_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.days_before else R.plurals.by_days
                resources.getQuantityString(base, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
            }
            seconds % HOUR_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.hours_before else R.plurals.by_hours
                resources.getQuantityString(base, seconds / HOUR_SECONDS, seconds / HOUR_SECONDS)
            }
            seconds % MINUTE_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.minutes_before else R.plurals.by_minutes
                resources.getQuantityString(
                    base,
                    seconds / MINUTE_SECONDS,
                    seconds / MINUTE_SECONDS
                )
            }
            else -> {
                val base = if (showBefore) R.plurals.seconds_before else R.plurals.by_seconds
                resources.getQuantityString(base, seconds, seconds)
            }
        }
    }
}

fun Context.getDefaultAlarmTitle(type: Int): String {
    val alarmString = getString(R.string.alarm)
    return try {
        RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(type))?.getTitle(this)
            ?: alarmString
    } catch (e: Exception) {
        alarmString
    }
}

fun Context.getDefaultAlarmSound(type: Int) =
    AlarmSound(0, getDefaultAlarmTitle(type), RingtoneManager.getDefaultUri(type).toString())

fun Context.grantReadUriPermission(uriString: String) {
    try {
        // ensure custom reminder sounds play well
        grantUriPermission(
            "com.android.systemui",
            Uri.parse(uriString),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (ignored: Exception) {
    }
}

fun Context.storeNewYourAlarmSound(resultData: Intent): AlarmSound {
    val uri = resultData.data
    var filename = getFilenameFromUri(uri!!)
    if (filename.isEmpty()) {
        filename = getString(R.string.alarm)
    }

    val token = object : TypeToken<ArrayList<AlarmSound>>() {}.type
    val yourAlarmSounds = Gson().fromJson<ArrayList<AlarmSound>>(baseConfig.yourAlarmSounds, token)
        ?: ArrayList()
    val newAlarmSoundId =
        (yourAlarmSounds.maxByOrNull { it.id }?.id ?: YOUR_ALARM_SOUNDS_MIN_ID) + 1
    val newAlarmSound = AlarmSound(newAlarmSoundId, filename, uri.toString())
    if (yourAlarmSounds.firstOrNull { it.uri == uri.toString() } == null) {
        yourAlarmSounds.add(newAlarmSound)
    }

    baseConfig.yourAlarmSounds = Gson().toJson(yourAlarmSounds)

    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    contentResolver.takePersistableUriPermission(uri, takeFlags)

    return newAlarmSound
}

fun Context.getLaunchIntent() = packageManager.getLaunchIntentForPackage(baseConfig.appId)

fun Context.getTimeFormat() = if (baseConfig.use24HourFormat) TIME_FORMAT_24 else TIME_FORMAT_12

fun Context.getResolution(path: String): Point? {
    return if (path.isImageFast() || path.isImageSlow()) {
        getImageResolution(path)
    } else if (path.isVideoFast() || path.isVideoSlow()) {
        getVideoResolution(path)
    } else {
        null
    }
}

fun Context.getImageResolution(path: String): Point? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    if (isRestrictedSAFOnlyRoot(path)) {
        BitmapFactory.decodeStream(
            contentResolver.openInputStream(getAndroidSAFUri(path)),
            null,
            options
        )
    } else {
        BitmapFactory.decodeFile(path, options)
    }

    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) {
        Point(options.outWidth, options.outHeight)
    } else {
        null
    }
}

fun Context.getVideoResolution(path: String): Point? {
    var point = try {
        val retriever = MediaMetadataRetriever()
        if (isRestrictedSAFOnlyRoot(path)) {
            retriever.setDataSource(this, getAndroidSAFUri(path))
        } else {
            retriever.setDataSource(path)
        }

        val width =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        Point(width, height)
    } catch (ignored: Exception) {
        null
    }

    if (point == null && path.startsWith("content://", true)) {
        try {
            val fd = contentResolver.openFileDescriptor(Uri.parse(path), "r")?.fileDescriptor
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)
            val width =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                    .toInt()
            point = Point(width, height)
        } catch (ignored: Exception) {
        }
    }

    return point
}

fun Context.getDuration(path: String): Int? {
    val projection = arrayOf(
        MediaColumns.DURATION
    )

    val uri = getFileUri(path)
    val selection =
        if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs =
        if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return (cursor.getIntValue(MediaColumns.DURATION) / 1000.toDouble()).roundToLong()
                    .toInt()
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt() / 1000f).roundToInt()
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getFontSizeText() = getString(
    when (baseConfig.fontSize) {
        FONT_SIZE_SMALL -> R.string.small
        FONT_SIZE_MEDIUM -> R.string.medium
        FONT_SIZE_LARGE -> R.string.large
        else -> R.string.extra_large
    }
)

fun Context.getTextSize() = when (baseConfig.fontSize) {
    FONT_SIZE_SMALL -> resources.getDimension(R.dimen.smaller_text_size)
    FONT_SIZE_MEDIUM -> resources.getDimension(R.dimen.bigger_text_size)
    FONT_SIZE_LARGE -> resources.getDimension(R.dimen.big_text_size)
    else -> resources.getDimension(R.dimen.extra_big_text_size)
}

val Context.telecomManager: TelecomManager get() = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val Context.shortcutManager: ShortcutManager get() = getSystemService(ShortcutManager::class.java) as ShortcutManager

val Context.navigationBarOnSide: Boolean get() = usableScreenSize.x < realScreenSize.x && usableScreenSize.x > usableScreenSize.y
val Context.navigationBarOnBottom: Boolean get() = usableScreenSize.y < realScreenSize.y

val Context.navigationBarSize: Point
    get() = when {
        navigationBarOnSide -> Point(newNavigationBarHeight, usableScreenSize.y)
        navigationBarOnBottom -> Point(usableScreenSize.x, newNavigationBarHeight)
        else -> Point()
    }

val Context.newNavigationBarHeight: Int
    get() {
        var navigationBarHeight = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

val Context.realScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }

// we need the Default Dialer functionality only in Simple Dialer and in Simple Contacts for now
fun Context.isDefaultDialer(): Boolean {
    return if (!packageName.startsWith("com.production.planful.contacts") && !packageName.startsWith(
            "com.production.planful.dialer"
        )
    ) {
        true
    } else if ((packageName.startsWith("com.production.planful.contacts") || packageName.startsWith(
            "com.production.planful.dialer"
        )) && isQPlus()
    ) {
        val roleManager = getSystemService(RoleManager::class.java)
        roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        isMarshmallowPlus() && telecomManager.defaultDialerPackage == packageName
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.getBlockedNumbers(): ArrayList<BlockedNumber> {
    val blockedNumbers = ArrayList<BlockedNumber>()
    if (!isNougatPlus() || !isDefaultDialer()) {
        return blockedNumbers
    }

    val uri = BlockedNumbers.CONTENT_URI
    val projection = arrayOf(
        BlockedNumbers.COLUMN_ID,
        BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
        BlockedNumbers.COLUMN_E164_NUMBER
    )

    queryCursor(uri, projection) { cursor ->
        val id = cursor.getLongValue(BlockedNumbers.COLUMN_ID)
        val number = cursor.getStringValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
        val normalizedNumber = cursor.getStringValue(BlockedNumbers.COLUMN_E164_NUMBER)
        val comparableNumber = normalizedNumber.trimToComparableNumber()
        val blockedNumber = BlockedNumber(id, number, normalizedNumber, comparableNumber)
        blockedNumbers.add(blockedNumber)
    }

    return blockedNumbers
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.addBlockedNumber(number: String) {
    ContentValues().apply {
        put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        put(BlockedNumbers.COLUMN_E164_NUMBER, PhoneNumberUtils.normalizeNumber(number))
        try {
            contentResolver.insert(BlockedNumbers.CONTENT_URI, this)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.deleteBlockedNumber(number: String) {
    val selection = "${BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
    val selectionArgs = arrayOf(number)
    contentResolver.delete(BlockedNumbers.CONTENT_URI, selection, selectionArgs)
}

fun Context.isNumberBlockedByPattern(
    number: String,
    blockedNumbers: ArrayList<BlockedNumber> = getBlockedNumbers()
): Boolean {
    for (blockedNumber in blockedNumbers) {
        val num = blockedNumber.number
        if (num.isBlockedNumberPattern()) {
            val pattern = num.replace("+", "\\+").replace("*", ".*")
            if (number.matches(pattern.toRegex())) {
                return true
            }
        }
    }
    return false
}

fun Context.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.app_name), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    val toastText = String.format(getString(R.string.value_copied_to_clipboard_show), text)
    toast(toastText)
}
