package com.production.planful.commons.models.contacts

import android.graphics.Bitmap
import com.production.planful.commons.extensions.normalizeString
import com.production.planful.commons.helpers.*
import com.production.planful.commons.models.PhoneNumber

data class Contact(
    var id: Int,
    var prefix: String,
    var firstName: String,
    var middleName: String,
    var surname: String,
    var suffix: String,
    var nickname: String,
    var photoUri: String,
    var phoneNumbers: ArrayList<PhoneNumber>,
    var emails: ArrayList<Email>,
    var addresses: ArrayList<Address>,
    var events: ArrayList<Event>,
    var source: String,
    var starred: Int,
    var contactId: Int,
    var thumbnailUri: String,
    var photo: Bitmap?,
    var notes: String,
    var organization: Organization,
    var websites: ArrayList<String>,
    var IMs: ArrayList<IM>,
    var mimetype: String,
    var ringtone: String?
) : Comparable<Contact> {
    companion object {
        var sorting = 0
        var startWithSurname = false
    }

    override fun compareTo(other: Contact): Int {
        var result = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> {
                val firstString = firstName.normalizeString()
                val secondString = other.firstName.normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            sorting and SORT_BY_MIDDLE_NAME != 0 -> {
                val firstString = middleName.normalizeString()
                val secondString = other.middleName.normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            sorting and SORT_BY_SURNAME != 0 -> {
                val firstString = surname.normalizeString()
                val secondString = other.surname.normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            sorting and SORT_BY_FULL_NAME != 0 -> {
                val firstString = getNameToDisplay().normalizeString()
                val secondString = other.getNameToDisplay().normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            else -> compareUsingIds(other)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }

    private fun compareUsingStrings(
        firstString: String,
        secondString: String,
        other: Contact
    ): Int {
        var firstValue = firstString
        var secondValue = secondString

        if (firstValue.isEmpty() && firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty()) {
            val fullCompany = getFullCompany()
            if (fullCompany.isNotEmpty()) {
                firstValue = fullCompany.normalizeString()
            } else if (emails.isNotEmpty()) {
                firstValue = emails.first().value
            }
        }

        if (secondValue.isEmpty() && other.firstName.isEmpty() && other.middleName.isEmpty() && other.surname.isEmpty()) {
            val otherFullCompany = other.getFullCompany()
            if (otherFullCompany.isNotEmpty()) {
                secondValue = otherFullCompany.normalizeString()
            } else if (other.emails.isNotEmpty()) {
                secondValue = other.emails.first().value
            }
        }

        return if (firstValue.firstOrNull()?.isLetter() == true && secondValue.firstOrNull()
                ?.isLetter() == false
        ) {
            -1
        } else if (firstValue.firstOrNull()?.isLetter() == false && secondValue.firstOrNull()
                ?.isLetter() == true
        ) {
            1
        } else {
            if (firstValue.isEmpty() && secondValue.isNotEmpty()) {
                1
            } else if (firstValue.isNotEmpty() && secondValue.isEmpty()) {
                -1
            } else {
                if (firstValue.equals(secondValue, ignoreCase = true)) {
                    getNameToDisplay().compareTo(other.getNameToDisplay(), true)
                } else {
                    firstValue.compareTo(secondValue, true)
                }
            }
        }
    }

    private fun compareUsingIds(other: Contact): Int {
        val firstId = id
        val secondId = other.id
        return firstId.compareTo(secondId)
    }

    fun getNameToDisplay(): String {
        val firstMiddle = "$firstName $middleName".trim()
        val firstPart = if (startWithSurname) {
            if (surname.isNotEmpty() && firstMiddle.isNotEmpty()) {
                "$surname,"
            } else {
                surname
            }
        } else {
            firstMiddle
        }
        val lastPart = if (startWithSurname) firstMiddle else surname
        val suffixComma = if (suffix.isEmpty()) "" else ", $suffix"
        val fullName = "$prefix $firstPart $lastPart$suffixComma".trim()
        return fullName.ifEmpty {
            if (organization.isNotEmpty()) {
                getFullCompany()
            } else {
                emails.firstOrNull()?.value?.trim() ?: ""
            }
        }
    }

    fun getFullCompany(): String {
        var fullOrganization =
            if (organization.company.isEmpty()) "" else "${organization.company}, "
        fullOrganization += organization.jobPosition
        return fullOrganization.trim().trimEnd(',')
    }

}
