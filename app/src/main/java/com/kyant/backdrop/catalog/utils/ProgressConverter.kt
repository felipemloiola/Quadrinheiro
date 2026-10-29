package com.kyant.backdrop.catalog.utils

import kotlin.math.tanh

fun interface ProgressConverter {

    fun convert(progress: Float): Float

    companion object {

        val Default: ProgressConverter = ProgressConverter { progress ->
            (1f - (1f / (progress + 1f))) * 2f
        }

        fun tanh(
            maxProgress: Float,
            slope: Float = 1f
        ): ProgressConverter {
            return ProgressConverter { progress ->
                (maxProgress * tanh((slope * progress / maxProgress).toDouble())).toFloat()
            }
        }
    }
}
