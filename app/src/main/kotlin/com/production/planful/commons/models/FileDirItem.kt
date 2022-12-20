package com.production.planful.commons.models

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import java.io.File
import java.util.*

open class FileDirItem(
    val path: String,
    val name: String = "",
    var isDirectory: Boolean = false,
    var children: Int = 0,
    var size: Long = 0L,
    var modified: Long = 0L,
    var mediaStoreId: Long = 0L
) :
    Comparable<FileDirItem> {
    companion object {
        var sorting = 0
    }

    override fun toString() =
        "FileDirItem(path=$path, name=$name, isDirectory=$isDirectory, children=$children, size=$size, modified=$modified, mediaStoreId=$mediaStoreId)"

    override fun compareTo(other: FileDirItem): Int {
        return if (isDirectory && !other.isDirectory) {
            -1
        } else if (!isDirectory && other.isDirectory) {
            1
        } else {
            var result: Int
            when {
                sorting and SORT_BY_NAME != 0 -> {
                    result = if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                        AlphanumericComparator().compare(
                            name.normalizeString().lowercase(Locale.getDefault()),
                            other.name.normalizeString().lowercase(Locale.getDefault())
                        )
                    } else {
                        name.normalizeString().lowercase(Locale.getDefault())
                            .compareTo(other.name.normalizeString().lowercase(Locale.getDefault()))
                    }
                }
                sorting and SORT_BY_SIZE != 0 -> result = when {
                    size == other.size -> 0
                    size > other.size -> 1
                    else -> -1
                }
                sorting and SORT_BY_DATE_MODIFIED != 0 -> {
                    result = when {
                        modified == other.modified -> 0
                        modified > other.modified -> 1
                        else -> -1
                    }
                }
                else -> {
                    result =
                        getExtension().lowercase(Locale.getDefault()).compareTo(other.getExtension()
                            .lowercase(Locale.getDefault()))
                }
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }
            result
        }
    }

    fun getExtension() = if (isDirectory) name else path.substringAfterLast('.', "")

    fun getProperSize(context: Context, countHidden: Boolean): Long {
        return when {
            context.isRestrictedSAFOnlyRoot(path) -> context.getAndroidSAFFileSize(path)
            context.isPathOnOTG(path) -> context.getDocumentFile(path)?.getItemSize(countHidden)
                ?: 0
            isNougatPlus() && path.startsWith("content://") -> {
                try {
                    context.contentResolver.openInputStream(Uri.parse(path))?.available()?.toLong()
                        ?: 0L
                } catch (e: Exception) {
                    context.getSizeFromContentUri(Uri.parse(path))
                }
            }
            else -> File(path).getProperSize(countHidden)
        }
    }

    fun getParentPath() = path.getParentPath()

    fun assembleContentUri(): Uri {
        val uri = when {
            path.isImageFast() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            path.isVideoFast() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }

        return Uri.withAppendedPath(uri, mediaStoreId.toString())
    }
}
