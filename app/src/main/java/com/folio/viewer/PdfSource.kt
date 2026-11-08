package com.folio.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File

/** Thin wrapper over the platform PdfRenderer (one page open at a time, so calls are serialized). */
class PdfSource(file: File) : Closeable {
    private val pfd: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer = try {
        PdfRenderer(pfd)
    } catch (e: Exception) {
        pfd.close()
        throw e
    }
    private val lock = Any()

    @Volatile
    private var closed = false

    val pageCount: Int = renderer.pageCount

    /** Aspect ratio (w/h) of the first page, used for placeholders while pages render. */
    val placeholderRatio: Float = if (pageCount > 0) {
        val page = renderer.openPage(0)
        try {
            page.width.toFloat() / page.height.toFloat()
        } finally {
            page.close()
        }
    } else {
        0.707f
    }

    fun render(index: Int, width: Int): Bitmap = synchronized(lock) {
        check(!closed) { "PDF closed" }
        val page = renderer.openPage(index)
        try {
            val h = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        } finally {
            page.close()
        }
    }

    override fun close() {
        synchronized(lock) {
            if (!closed) {
                closed = true
                renderer.close()
                pfd.close()
            }
        }
    }
}
