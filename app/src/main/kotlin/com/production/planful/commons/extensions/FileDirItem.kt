package com.production.planful.commons.extensions

import android.content.Context
import com.production.planful.commons.models.FileDirItem

fun FileDirItem.isRecycleBinPath(context: Context): Boolean {
    return path.startsWith(context.recycleBinPath)
}
