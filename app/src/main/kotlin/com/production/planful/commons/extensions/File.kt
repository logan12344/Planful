package com.production.planful.commons.extensions

import android.content.Context
import com.production.planful.commons.helpers.NOMEDIA
import com.production.planful.commons.models.FileDirItem
import java.io.File

fun File.isMediaFile() = absolutePath.isMediaFile()

fun File.getProperSize(countHiddenItems: Boolean): Long {
    return if (isDirectory) {
        getDirectorySize(this, countHiddenItems)
    } else {
        length()
    }
}

private fun getDirectorySize(dir: File, countHiddenItems: Boolean): Long {
    var size = 0L
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    size += getDirectorySize(files[i], countHiddenItems)
                } else if (!files[i].name.startsWith('.') && !dir.name.startsWith('.') || countHiddenItems) {
                    size += files[i].length()
                }
            }
        }
    }
    return size
}

private fun getDirectoryFileCount(dir: File, countHiddenItems: Boolean): Int {
    var count = -1
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            count++
            for (i in files.indices) {
                val file = files[i]
                if (file.isDirectory) {
                    count++
                    count += getDirectoryFileCount(file, countHiddenItems)
                } else if (!file.name.startsWith('.') || countHiddenItems) {
                    count++
                }
            }
        }
    }
    return count
}

fun File.toFileDirItem(context: Context) = FileDirItem(
    absolutePath,
    name,
    context.getIsPathDirectory(absolutePath),
    0,
    length(),
    lastModified()
)

fun File.containsNoMedia(): Boolean {
    return if (!isDirectory) {
        false
    } else {
        File(this, NOMEDIA).exists()
    }
}

