package com.production.planful.commons.helpers

import android.telephony.PhoneNumberUtils
import com.production.planful.commons.activities.BaseSimpleActivity
import com.production.planful.commons.extensions.addBlockedNumber
import com.production.planful.commons.extensions.showErrorToast
import java.io.File

class BlockedNumbersImporter(
    private val activity: BaseSimpleActivity,
) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK
    }

    fun importBlockedNumbers(path: String): ImportResult {
        return try {
            val inputStream = File(path).inputStream()
            val numbers = inputStream.bufferedReader().use {
                val content = it.readText().split(BLOCKED_NUMBERS_EXPORT_DELIMITER)
                content.filter { text -> PhoneNumberUtils.isGlobalPhoneNumber(text) }
            }
            if (numbers.isNotEmpty()) {
                numbers.forEach { number ->
                    activity.addBlockedNumber(number)
                }
                ImportResult.IMPORT_OK
            } else {
                ImportResult.IMPORT_FAIL
            }

        } catch (e: Exception) {
            activity.showErrorToast(e)
            ImportResult.IMPORT_FAIL
        }
    }
}
