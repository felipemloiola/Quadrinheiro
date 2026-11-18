package com.kyant.backdrop.catalog.utils

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap {
    val src = this.asAndroidBitmap()
    val softwareSrc = if (
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && src.config == Bitmap.Config.HARDWARE) ||
        src.config != Bitmap.Config.ARGB_8888
    ) {
        src.copy(Bitmap.Config.ARGB_8888, false) ?: src
    } else {
        src
    }
    val scaled = Bitmap.createScaledBitmap(
        softwareSrc,
        width.coerceAtLeast(1),
        height.coerceAtLeast(1),
        false
    )
    val result = if (scaled.config != Bitmap.Config.ARGB_8888) {
        (scaled.copy(Bitmap.Config.ARGB_8888, false) ?: scaled).also {
            if (it !== scaled) scaled.recycle()
        }
    } else {
        scaled
    }
    if (softwareSrc !== src && softwareSrc !== result) {
        softwareSrc.recycle()
    }
    return result.asImageBitmap()
}
