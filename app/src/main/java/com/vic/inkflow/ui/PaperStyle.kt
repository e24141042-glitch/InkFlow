package com.vic.inkflow.ui

/**
 * Defines the orientation and background template of a page canvas.
 * All size values are in PDF points (1/72 inch).
 */

enum class PageBackground {
    BLANK,
    RULED,         // 橫線（~1cm 間距）
    NARROW_RULED,  // 密行（~6.35mm 間距）
    WIDE_RULED,    // 寬行（~15mm 間距）
    GRID,          // 方格（~1cm）
    DOT_GRID       // 點格（~1cm）
}

data class PaperStyle(
    val widthPt: Float = 595f,
    val heightPt: Float = 842f,
    val background: PageBackground = PageBackground.BLANK
) {
    val aspectRatio: Float get() = widthPt / heightPt

    companion object {
        // ISO A-series
        val A3_PORTRAIT      = PaperStyle(widthPt = 842f,  heightPt = 1191f)
        val A3_LANDSCAPE     = PaperStyle(widthPt = 1191f, heightPt = 842f)
        val A4_PORTRAIT      = PaperStyle(widthPt = 595f,  heightPt = 842f)
        val A4_LANDSCAPE     = PaperStyle(widthPt = 842f,  heightPt = 595f)
        val A5_PORTRAIT      = PaperStyle(widthPt = 420f,  heightPt = 595f)
        val A5_LANDSCAPE     = PaperStyle(widthPt = 595f,  heightPt = 420f)
        // ISO B-series
        val B5_PORTRAIT      = PaperStyle(widthPt = 499f,  heightPt = 709f)
        val B5_LANDSCAPE     = PaperStyle(widthPt = 709f,  heightPt = 499f)
        // US Letter
        val LETTER_PORTRAIT  = PaperStyle(widthPt = 612f,  heightPt = 792f)
        val LETTER_LANDSCAPE = PaperStyle(widthPt = 792f,  heightPt = 612f)

        /** Construct a PaperStyle that best fits the given PDF page dimensions. */
        fun fromPdfPage(pdfW: Float, pdfH: Float, background: PageBackground = PageBackground.BLANK) =
            PaperStyle(widthPt = pdfW, heightPt = pdfH, background = background)
    }
}
