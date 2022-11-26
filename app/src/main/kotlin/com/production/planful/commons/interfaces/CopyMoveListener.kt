package com.production.planful.commons.interfaces

interface CopyMoveListener {
    fun copySucceeded(
        copyOnly: Boolean,
        copiedAll: Boolean,
        destinationPath: String,
        wasCopyingOneFileOnly: Boolean
    )

    fun copyFailed()
}
