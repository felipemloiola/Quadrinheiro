package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Prevents Android's native SkHeifCodec / HeifDecoderImpl from ever being invoked on
 * HEIF/HEIC/AVIF (`ftyp`) or non-raster streams, which otherwise logs
 * `E/HeifDecoderImpl: getSize: not supported!` on devices/emulators without HEIF hardware decoders.
 */
object SafeImageFormats {

    const val ACCEPT_RASTER_HEADER = "image/jpeg,image/png,image/webp;q=0.9,*/*;q=0.5"

    fun isHeifOrAvifHeader(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (length < 12) return false
        // ISO Base Media File Format ('ftyp' at bytes 4..7) used by HEIC, HEIF, MIF1, AVIF
        return bytes[4] == 0x66.toByte() &&
            bytes[5] == 0x74.toByte() &&
            bytes[6] == 0x79.toByte() &&
            bytes[7] == 0x70.toByte()
    }

    fun isSupportedRasterHeader(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (length < 12) return false
        if (isHeifOrAvifHeader(bytes, length)) return false

        // JPEG: FF D8 FF
        val isJpeg = (bytes[0].toInt() and 0xFF) == 0xFF &&
            (bytes[1].toInt() and 0xFF) == 0xD8 &&
            (bytes[2].toInt() and 0xFF) == 0xFF
        if (isJpeg) return true

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        val isPng = (bytes[0].toInt() and 0xFF) == 0x89 &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
        if (isPng) return true

        // WebP: 'RIFF' .... 'WEBP'
        val isWebp = bytes[0] == 0x52.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() &&
            bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() &&
            bytes[11] == 0x50.toByte()
        if (isWebp) return true

        // GIF: 'GIF8'
        val isGif = bytes[0] == 0x47.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x38.toByte()
        if (isGif) return true

        // BMP: 'BM'
        val isBmp = bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()
        if (isBmp) return true

        // ICO: 00 00 01 00
        val isIco = bytes[0] == 0x00.toByte() &&
            bytes[1] == 0x00.toByte() &&
            bytes[2] == 0x01.toByte() &&
            bytes[3] == 0x00.toByte()
        return isIco
    }

    fun decodeBoundsSafely(bytes: ByteArray): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            outWidth = -1
            outHeight = -1
        }
        if (!isSupportedRasterHeader(bytes)) return bounds
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return bounds
    }

    fun decodeByteArraySafely(bytes: ByteArray, options: BitmapFactory.Options? = null): Bitmap? {
        if (!isSupportedRasterHeader(bytes)) return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    fun decodeStreamSafely(stream: InputStream, options: BitmapFactory.Options? = null): Bitmap? {
        val buffered = if (stream.markSupported()) stream else BufferedInputStream(stream, 32)
        buffered.mark(16)
        val header = ByteArray(12)
        var totalRead = 0
        while (totalRead < 12) {
            val r = buffered.read(header, totalRead, 12 - totalRead)
            if (r <= 0) break
            totalRead += r
        }
        buffered.reset()
        if (!isSupportedRasterHeader(header, totalRead)) {
            if (options?.inJustDecodeBounds == true) {
                options.outWidth = -1
                options.outHeight = -1
            }
            return null
        }
        return BitmapFactory.decodeStream(buffered, null, options)
    }
}

class RejectHeifAvifDecoderFactory : Decoder.Factory {
    override fun create(
        result: SourceResult,
        options: Options,
        imageLoader: ImageLoader
    ): Decoder? {
        return try {
            val source = result.source.source()
            if (!source.request(12L)) {
                Decoder { throw IOException("Image stream shorter than 12 bytes") }
            } else {
                val header = source.peek().readByteArray(12L)
                if (!SafeImageFormats.isSupportedRasterHeader(header)) {
                    Decoder { throw IOException("Unsupported or HEIF/AVIF image stream rejected before BitmapFactory") }
                } else {
                    null
                }
            }
        } catch (_: Throwable) {
            Decoder { throw IOException("Failed to inspect image header") }
        }
    }
}
