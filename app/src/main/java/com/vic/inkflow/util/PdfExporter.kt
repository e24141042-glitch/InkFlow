package com.vic.inkflow.util

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.vic.inkflow.data.ImageAnnotationEntity
import com.vic.inkflow.data.PointF
import com.vic.inkflow.data.StrokeWithPoints
import com.vic.inkflow.data.TextAnnotationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Handles the expert-level logic for exporting annotated PDFs.
 */
object PdfExporter {

    /** Fixed model coordinate space (A4 PDF points). Must match EditorViewModel constants. */
    private const val MODEL_W = 595f
    private const val MODEL_H = 842f

    /**
     * Bitmap scale factor used when rasterising text annotations.
     * 2 pixels per model-point ≈ 144 DPI — good quality without excessive file size.
     */
    private const val BITMAP_SCALE = 2f

    /**
     * Exports a new PDF with vector strokes, shapes, text, and images drawn on top.
     */
    suspend fun export(
        originalPdfUri: Uri,
        strokes: List<StrokeWithPoints>,
        textAnnotations: List<TextAnnotationEntity> = emptyList(),
        imageAnnotations: List<ImageAnnotationEntity> = emptyList(),
        context: Context,
        fileName: String = "InkFlow_Export.pdf",
        modelW: Float = MODEL_W,
        modelH: Float = MODEL_H
    ) {
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val destinationUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (destinationUri == null) {
                    showToast(context, "Failed to create destination file.")
                    return@withContext
                }

                context.contentResolver.openInputStream(originalPdfUri).use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    val strokesByPage = strokes.groupBy { it.stroke.pageIndex }
                    val textByPage   = textAnnotations.groupBy { it.pageIndex }
                    val imageByPage  = imageAnnotations.groupBy { it.pageIndex }

                    document.pages.forEachIndexed { pageIndex, page ->
                        val pageStrokes = strokesByPage[pageIndex]
                        val pageTexts   = textByPage[pageIndex]
                        val pageImages  = imageByPage[pageIndex]

                        if (!pageStrokes.isNullOrEmpty() || !pageTexts.isNullOrEmpty() || !pageImages.isNullOrEmpty()) {
                            val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                            val pageHeight = page.mediaBox.height
                            val pageWidth  = page.mediaBox.width

                            pageImages?.forEach { ann ->
                                drawImageAnnotation(contentStream, document, context, ann, pageWidth, pageHeight)
                            }

                            pageStrokes?.forEach { stroke ->
                                if (stroke.stroke.shapeType != null) {
                                    drawShape(contentStream, stroke, pageWidth, pageHeight, modelW, modelH)
                                } else {
                                    drawStroke(contentStream, stroke, pageWidth, pageHeight, modelW, modelH)
                                }
                            }

                            pageTexts?.forEach { ann ->
                                drawTextAnnotation(contentStream, document, context, ann, pageWidth, pageHeight)
                            }

                            contentStream.close()
                        }
                    }

                    resolver.openOutputStream(destinationUri).use { outputStream ->
                        if (outputStream == null) {
                            showToast(context, "Failed to open output stream.")
                            return@use
                        }
                        document.save(outputStream)
                    }
                    document.close()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(destinationUri, contentValues, null, null)
                }

                showToast(context, "PDF Exported to Downloads folder!")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast(context, "Error during PDF export: ${e.message}")
            }
        }
    }

    private fun drawStroke(stream: PDPageContentStream, stroke: StrokeWithPoints, pageWidth: Float, pageHeight: Float, modelW: Float = MODEL_W, modelH: Float = MODEL_H) {
        val points = stroke.points.map { PointF(it.x, it.y) }
        if (points.size < 2) return

        // Fix #1: scale model-space coords to actual PDF page dimensions.
        val ratioX = pageWidth  / modelW
        val ratioY = pageHeight / modelH

        val color = android.graphics.Color.valueOf(stroke.stroke.color)
        // Fix #2 + #5: apply stroke alpha; highlighters get 40% opacity.
        // color.alpha() already returns [0.0, 1.0] — do NOT divide by 255.
        val strokingAlpha = if (stroke.stroke.isHighlighter) 0.4f else color.alpha()

        stream.saveGraphicsState()
        val gs = PDExtendedGraphicsState()
        gs.setStrokingAlphaConstant(strokingAlpha)
        stream.setGraphicsStateParameters(gs)
        stream.setStrokingColor(color.red(), color.green(), color.blue())
        stream.setLineWidth(stroke.stroke.strokeWidth * ratioX)

        // Local helpers: model → PDF coordinate space.
        fun mx(x: Float) = x * ratioX
        fun my(y: Float) = pageHeight - y * ratioY

        val firstPoint = points.first()
        stream.moveTo(mx(firstPoint.x), my(firstPoint.y))

        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            val midPoint    = PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
            val startPoint  = if (i == 1) firstPoint else PointF((points[i - 2].x + p1.x) / 2f, (points[i - 2].y + p1.y) / 2f)
            val c1x = startPoint.x + 2/3f * (p1.x - startPoint.x)
            val c1y = startPoint.y + 2/3f * (p1.y - startPoint.y)
            val c2x = midPoint.x   + 2/3f * (p1.x - midPoint.x)
            val c2y = midPoint.y   + 2/3f * (p1.y - midPoint.y)
            stream.curveTo(mx(c1x), my(c1y), mx(c2x), my(c2y), mx(midPoint.x), my(midPoint.y))
        }
        stream.lineTo(mx(points.last().x), my(points.last().y))
        stream.stroke()
        stream.restoreGraphicsState()
    }

    private fun drawShape(stream: PDPageContentStream, stroke: StrokeWithPoints, pageWidth: Float, pageHeight: Float, modelW: Float = MODEL_W, modelH: Float = MODEL_H) {
        // Fix #1: scale model-space bounds to actual PDF page dimensions.
        val ratioX = pageWidth  / modelW
        val ratioY = pageHeight / modelH

        val color = android.graphics.Color.valueOf(stroke.stroke.color)
        // Fix #2 + #5: apply stroke alpha.
        // color.alpha() already returns [0.0, 1.0] — do NOT divide by 255.
        val alpha = color.alpha()

        val s = stroke.stroke
        val pdfLeft   = s.boundsLeft * ratioX
        val pdfBottom = pageHeight - s.boundsBottom * ratioY
        val pdfWidth  = (s.boundsRight  - s.boundsLeft) * ratioX
        val pdfHeight = (s.boundsBottom - s.boundsTop)  * ratioY
        val scaledSW  = s.strokeWidth * ratioX

        stream.saveGraphicsState()
        val gs = PDExtendedGraphicsState()
        gs.setStrokingAlphaConstant(alpha)
        stream.setGraphicsStateParameters(gs)
        stream.setStrokingColor(color.red(), color.green(), color.blue())
        stream.setLineWidth(scaledSW)

        when (s.shapeType) {
            "RECT" -> {
                stream.addRect(pdfLeft, pdfBottom, pdfWidth, pdfHeight)
                stream.stroke()
            }
            "CIRCLE" -> {
                // Approximate ellipse with 4 cubic Bézier arcs (κ ≈ 0.5523).
                val cx = pdfLeft + pdfWidth  / 2f
                val cy = pdfBottom + pdfHeight / 2f
                val rx = pdfWidth  / 2f
                val ry = pdfHeight / 2f
                val k  = 0.5522848f
                stream.moveTo(cx, cy + ry)
                stream.curveTo(cx + k * rx, cy + ry, cx + rx, cy + k * ry, cx + rx, cy)
                stream.curveTo(cx + rx, cy - k * ry, cx + k * rx, cy - ry, cx, cy - ry)
                stream.curveTo(cx - k * rx, cy - ry, cx - rx, cy - k * ry, cx - rx, cy)
                stream.curveTo(cx - rx, cy + k * ry, cx - k * rx, cy + ry, cx, cy + ry)
                stream.closeAndStroke()
            }
            "LINE", "ARROW" -> {
                val pts = stroke.points
                if (pts.size >= 2) {
                    // Convert to PDF space before drawing (Y-flipped, scaled).
                    val pdfP0x = pts.first().x * ratioX
                    val pdfP0y = pageHeight - pts.first().y * ratioY
                    val pdfP1x = pts.last().x  * ratioX
                    val pdfP1y = pageHeight - pts.last().y  * ratioY
                    stream.moveTo(pdfP0x, pdfP0y)
                    stream.lineTo(pdfP1x, pdfP1y)
                    stream.stroke()
                    if (s.shapeType == "ARROW") {
                        // Pass PDF-space coords; drawArrowHeadPdf no longer needs pageHeight.
                        drawArrowHeadPdf(stream, pdfP0x, pdfP0y, pdfP1x, pdfP1y, scaledSW)
                    }
                }
            }
        }
        stream.restoreGraphicsState()
    }

    /**
     * Draws a two-line arrow head.
     * All coordinates must already be in PDF space (Y-up, scaled).
     * [sw] is the already-scaled stroke width in PDF points.
     */
    private fun drawArrowHeadPdf(
        stream: PDPageContentStream,
        sx: Float, sy: Float, ex: Float, ey: Float,
        sw: Float
    ) {
        val headSize   = sw * 5f + 10f
        // Angle is computed in PDF space (Y-up), so atan2 is correct as-is.
        val angle      = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val leftAngle  = angle + Math.PI * 0.75
        val rightAngle = angle - Math.PI * 0.75
        val lx = ex + (headSize * cos(leftAngle)).toFloat()
        val ly = ey + (headSize * sin(leftAngle)).toFloat()
        val rx = ex + (headSize * cos(rightAngle)).toFloat()
        val ry = ey + (headSize * sin(rightAngle)).toFloat()

        // Coords are already in PDF space, no Y-flip needed.
        stream.moveTo(ex, ey)
        stream.lineTo(lx, ly)
        stream.stroke()
        stream.moveTo(ex, ey)
        stream.lineTo(rx, ry)
        stream.stroke()
    }

    /**
     * Fix #3: Renders text (including CJK, emoji, stamps) via Android Canvas → Bitmap → PDF image.
     * This avoids the Latin-only limitation of PDType1Font and supports the full Unicode range.
     */
    private fun drawTextAnnotation(
        stream: PDPageContentStream,
        document: PDDocument,
        context: Context,
        ann: TextAnnotationEntity,
        pageWidth: Float,
        pageHeight: Float
    ) {
        try {
            val ratioX = pageWidth  / MODEL_W
            val ratioY = pageHeight / MODEL_H

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = ann.fontSize * BITMAP_SCALE
                color    = ann.colorArgb
                typeface = if (ann.isStamp) android.graphics.Typeface.DEFAULT
                           else android.graphics.Typeface.DEFAULT_BOLD
            }

            val textWidth = paint.measureText(ann.text).coerceAtLeast(1f)
            val fm        = paint.fontMetrics
            val bmpW      = (textWidth + 4f).toInt()
            val bmpH      = (-fm.ascent + fm.descent + 4f).toInt().coerceAtLeast(1)

            val bmp = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(bmp).drawText(ann.text, 2f, -fm.ascent + 2f, paint)

            val baos = ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
            bmp.recycle()

            val imgXObj = PDImageXObject.createFromByteArray(document, baos.toByteArray(), "txt")

            // Convert bitmap pixel dimensions back to PDF points, then apply page ratio.
            val pdfW = bmpW / BITMAP_SCALE * ratioX
            val pdfH = bmpH / BITMAP_SCALE * ratioY
            val pdfX = ann.modelX * ratioX
            // PDF Y-origin is bottom-left; subtract pdfH so the text top aligns with modelY.
            val pdfY = pageHeight - ann.modelY * ratioY - pdfH

            stream.drawImage(imgXObj, pdfX, pdfY, pdfW, pdfH)
        } catch (_: Exception) { /* skip on any rendering failure */ }
    }

    private fun drawImageAnnotation(
        stream: PDPageContentStream,
        document: PDDocument,
        context: Context,
        ann: ImageAnnotationEntity,
        pageWidth: Float,
        pageHeight: Float
    ) {
        try {
            val bmp = context.contentResolver.openInputStream(Uri.parse(ann.uri))?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return

            // Fix #1: scale model coords to PDF page dimensions.
            val ratioX = pageWidth  / MODEL_W
            val ratioY = pageHeight / MODEL_H

            val baos = ByteArrayOutputStream()
            // Fix #4: preserve alpha channel — use PNG for images with transparency, JPEG otherwise.
            val format  = if (bmp.hasAlpha()) android.graphics.Bitmap.CompressFormat.PNG
                          else android.graphics.Bitmap.CompressFormat.JPEG
            val quality = if (format == android.graphics.Bitmap.CompressFormat.PNG) 100 else 90
            bmp.compress(format, quality, baos)
            bmp.recycle()

            val imgXObj = PDImageXObject.createFromByteArray(document, baos.toByteArray(), "img")

            val pdfX = ann.modelX     * ratioX
            val pdfW = ann.modelWidth  * ratioX
            val pdfH = ann.modelHeight * ratioY
            val pdfY = pageHeight - ann.modelY * ratioY - pdfH
            stream.drawImage(imgXObj, pdfX, pdfY, pdfW, pdfH)
        } catch (_: Exception) { /* skip unreadable images */ }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
