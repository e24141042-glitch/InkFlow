package com.vic.inkflow.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PdfManager {
    private const val TAG = "PdfManager"

    /** App 私有外部儲存目錄（不需任何權限，空間比 filesDir 大）。 */
    private fun pdfDir(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir  // fallback 到內部儲存
        return dir.also { it.mkdirs() }
    }

    // ─── 新功能：複製 PDF 到 App 私有目錄 ─────────────────────────────────────

    /** 將外部 content:// PDF 複製到 App 私有目錄，回傳 file:// Uri。失敗回傳 null。 */
    suspend fun copyPdfToAppDir(context: Context, sourceUri: Uri): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dest = File(pdfDir(context), "${UUID.randomUUID()}.pdf")
                context.contentResolver.openInputStream(sourceUri)!!.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                Uri.fromFile(dest)
            } catch (e: Exception) {
                Log.e(TAG, "copyPdfToAppDir failed", e)
                null
            }
        }

    suspend fun copyImageToAppDir(context: Context, sourceUri: Uri): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dest = File(pdfDir(context), "${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                Uri.fromFile(dest)
            } catch (e: Exception) {
                Log.e(TAG, "copyImageToAppDir failed", e)
                null
            }
        }

    // ─── 新功能：建立空白 PDF ─────────────────────────────────────────────────

    /** 建立一個單頁 A4 空白 PDF，存到 App 私有目錄，回傳 file:// Uri。失敗回傳 null。 */
    suspend fun createBlankPdf(
        context: Context,
        pageWidthPt: Float = PDRectangle.A4.width,
        pageHeightPt: Float = PDRectangle.A4.height
    ): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dest = File(pdfDir(context), "${UUID.randomUUID()}.pdf")
                PDDocument().use { doc ->
                    doc.addPage(PDPage(PDRectangle(pageWidthPt, pageHeightPt)))
                    doc.save(dest)
                }
                Uri.fromFile(dest)
            } catch (e: Exception) {
                Log.e(TAG, "createBlankPdf failed", e)
                null
            }
        }

    // ─── 新功能：在現有 PDF 末尾加入空白頁 ───────────────────────────────────

    /** 在 file:// URI 的 PDF 中，於 [afterIndex] 頁之後插入一頁空白頁。
     *  頁面尺寸由 [pageWidthPt] 和 [pageHeightPt] 決定（預設為 A4 直向）。
     *  afterIndex = -1 或超過末頁時，直接追加到最後。
     *  採用「寫入暫存檔 → 原子重命名」策略，確保操作失敗時原始 PDF 不受損。 */
    suspend fun insertBlankPage(
        fileUri: Uri,
        afterIndex: Int,
        pageWidthPt: Float = PDRectangle.A4.width,
        pageHeightPt: Float = PDRectangle.A4.height
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (fileUri.scheme != "file") {
                Log.w(TAG, "insertBlankPage: only file:// URIs are supported")
                return@withContext false
            }
            try {
                val file = File(fileUri.path!!)
                PDDocument.load(file).use { doc ->
                    val newPage = PDPage(PDRectangle(pageWidthPt, pageHeightPt))
                    val insertBefore = afterIndex + 1
                    if (insertBefore < doc.numberOfPages) {
                        doc.pages.insertBefore(newPage, doc.getPage(insertBefore))
                    } else {
                        doc.addPage(newPage)
                    }
                    // Atomic write: save to a temp file first, then rename over the original.
                    // This prevents file corruption if the process is killed mid-write.
                    val tmpFile = File(file.parent, "${file.nameWithoutExtension}.tmp_${System.currentTimeMillis()}.pdf")
                    try {
                        doc.save(tmpFile)
                        if (!tmpFile.renameTo(file)) {
                            // renameTo can fail across mount points; fall back to copy+delete
                            tmpFile.copyTo(file, overwrite = true)
                            tmpFile.delete()
                        }
                    } catch (e: Exception) {
                        tmpFile.delete()
                        throw e
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "insertBlankPage failed", e)
                false
            }
        }

    /** Returns the number of pages in a PDF addressed by a file:// Uri. */
    suspend fun getPdfPageCount(fileUri: Uri): Int =
        withContext(Dispatchers.IO) {
            if (fileUri.scheme != "file") {
                Log.w(TAG, "getPdfPageCount: only file:// URIs are supported")
                return@withContext 0
            }
            try {
                val file = File(fileUri.path!!)
                PDDocument.load(file).use { doc -> doc.numberOfPages }
            } catch (e: Exception) {
                Log.e(TAG, "getPdfPageCount failed", e)
                0
            }
        }

    /**
     * Inserts all pages from [sourceFileUri] into [targetFileUri] after [afterIndex].
     * Both URIs must be file:// and the target file is updated atomically via a temp file.
     */
    suspend fun insertPdfPages(
        targetFileUri: Uri,
        sourceFileUri: Uri,
        afterIndex: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (targetFileUri.scheme != "file" || sourceFileUri.scheme != "file") {
                Log.w(TAG, "insertPdfPages: only file:// URIs are supported")
                return@withContext false
            }
            try {
                val targetFile = File(targetFileUri.path!!)
                val sourceFile = File(sourceFileUri.path!!)
                PDDocument.load(targetFile).use { targetDoc ->
                    PDDocument.load(sourceFile).use { sourceDoc ->
                        val sourceCount = sourceDoc.numberOfPages
                        if (sourceCount <= 0) {
                            Log.w(TAG, "insertPdfPages: source PDF has no pages")
                            return@use
                        }

                        val insertionIndex = (afterIndex + 1).coerceIn(0, targetDoc.numberOfPages)
                        Log.d(
                            TAG,
                            "insertPdfPages: targetCount=${targetDoc.numberOfPages}, sourceCount=$sourceCount, afterIndex=$afterIndex, insertionIndex=$insertionIndex"
                        )

                        // Deterministic insertion: import one page at a time and place it
                        // directly at the next insertion slot. This avoids unstable page-tree
                        // reordering when trying to batch-move already imported pages.
                        var insertPos = insertionIndex
                        repeat(sourceCount) { srcIndex ->
                            val imported = targetDoc.importPage(sourceDoc.getPage(srcIndex))
                            // importPage appends at tail first; remove that tail occurrence,
                            // then insert at the intended position.
                            targetDoc.removePage(targetDoc.numberOfPages - 1)
                            if (insertPos < targetDoc.numberOfPages) {
                                targetDoc.pages.insertBefore(imported, targetDoc.getPage(insertPos))
                            } else {
                                targetDoc.addPage(imported)
                            }
                            insertPos++
                        }

                        val tmpFile = File(targetFile.parent, "${targetFile.nameWithoutExtension}.tmp_${System.currentTimeMillis()}.pdf")
                        try {
                            targetDoc.save(tmpFile)
                            if (!tmpFile.renameTo(targetFile)) {
                                tmpFile.copyTo(targetFile, overwrite = true)
                                tmpFile.delete()
                            }
                        } catch (e: Exception) {
                            tmpFile.delete()
                            throw e
                        }
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "insertPdfPages failed", e)
                false
            }
        }

    /** @deprecated Use insertBlankPage instead. */
    @Deprecated("Use insertBlankPage(fileUri, afterIndex) instead", ReplaceWith("insertBlankPage(fileUri, Int.MAX_VALUE)"))
    suspend fun appendBlankPage(fileUri: Uri): Boolean = insertBlankPage(fileUri, Int.MAX_VALUE)

    /** 刪除 file:// URI PDF 中第 [pageIndex] 頁。若僅剩一頁則拒絕刪除並回傳 false。
     *  採用「寫入暫存檔 → 原子重命名」策略，確保操作失敗時原始 PDF 不受損。 */
    suspend fun deletePage(fileUri: Uri, pageIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (fileUri.scheme != "file") {
                Log.w(TAG, "deletePage: only file:// URIs are supported")
                return@withContext false
            }
            try {
                val file = File(fileUri.path!!)
                PDDocument.load(file).use { doc ->
                    if (doc.numberOfPages <= 1) {
                        Log.w(TAG, "deletePage: cannot delete the only page")
                        return@withContext false
                    }
                    doc.removePage(pageIndex)
                    // Atomic write: save to a temp file first, then rename over the original.
                    // This prevents file corruption if the process is killed mid-write.
                    val tmpFile = File(file.parent, "${file.nameWithoutExtension}.tmp_${System.currentTimeMillis()}.pdf")
                    try {
                        doc.save(tmpFile)
                        if (!tmpFile.renameTo(file)) {
                            // renameTo can fail across mount points; fall back to copy+delete
                            tmpFile.copyTo(file, overwrite = true)
                            tmpFile.delete()
                        }
                    } catch (e: Exception) {
                        tmpFile.delete()
                        throw e
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "deletePage failed", e)
                false
            }
        }

    /** 將 PDF 的第 [fromIndex] 頁移動到 [toIndex]。
     *  採用「寫入暫存檔 → 原子重命名」策略，確保操作失敗時原始 PDF 不受損。 */
    suspend fun movePage(fileUri: Uri, fromIndex: Int, toIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (fileUri.scheme != "file") {
                Log.w(TAG, "movePage: only file:// URIs are supported")
                return@withContext false
            }
            if (fromIndex == toIndex) return@withContext true
            try {
                val file = File(fileUri.path!!)
                PDDocument.load(file).use { doc ->
                    if (fromIndex < 0 || fromIndex >= doc.numberOfPages || 
                        toIndex < 0 || toIndex >= doc.numberOfPages) return@withContext false
                    
                    val pageToMove = doc.getPage(fromIndex)
                    doc.removePage(fromIndex) // 移除後，後面的頁面 index 會減 1
                    
                    if (toIndex >= doc.numberOfPages) { // 移到最後一頁
                        doc.addPage(pageToMove)
                    } else {
                        // 因為已移除 fromIndex，toIndex 代表的就是最終想要的絕對位置索引
                        doc.pages.insertBefore(pageToMove, doc.getPage(toIndex))
                    }
                    
                    val tmpFile = File(file.parent, ".tmp_.pdf")
                    try {
                        doc.save(tmpFile)
                        if (!tmpFile.renameTo(file)) {
                            tmpFile.copyTo(file, overwrite = true)
                            tmpFile.delete()
                        }
                    } catch (e: Exception) {
                        tmpFile.delete()
                        throw e
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "movePage failed", e)
                false
            }
        }

    /** 批次刪除 file:// URI PDF 中的多個頁面。[pageIndices] 必須是在原始文件中的絕對索引。
     *  採用「一次載入 → 全數刪除 → 一次存檔」策略，大幅提升 I/O 效能。*/
    suspend fun deletePages(fileUri: Uri, pageIndices: List<Int>): Boolean =
        withContext(Dispatchers.IO) {
            if (fileUri.scheme != "file" || pageIndices.isEmpty()) return@withContext false
            try {
                val file = File(fileUri.path!!)
                PDDocument.load(file).use { doc ->
                    if (doc.numberOfPages <= pageIndices.size) {
                        Log.w(TAG, "deletePages: cannot delete all pages")
                        return@withContext false
                    }
                    
                    // 從後往前刪除，這樣已經刪除的頁面就不會影響到前面頁面的索引
                    val sortedDescending = pageIndices.sortedDescending()
                    for (index in sortedDescending) {
                        if (index in 0 until doc.numberOfPages) {
                            doc.removePage(index)
                        }
                    }
                    
                    val tmpFile = File(file.parent, ".tmp_.pdf")
                    try {
                        doc.save(tmpFile)
                        if (!tmpFile.renameTo(file)) {
                            tmpFile.copyTo(file, overwrite = true)
                            tmpFile.delete()
                        }
                    } catch (e: Exception) {
                        tmpFile.delete()
                        throw e
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "deletePages failed", e)
                false
            }
        }

    // ─── 雙 Scheme 支援 ───────────────────────────────────────────────────────

    /**
     * 開啟 ParcelFileDescriptor，同時支援 file:// 與 content:// URI。
     * 必須在 IO 執行緒調用。
     */
    fun openPdfFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file") {
                ParcelFileDescriptor.open(
                    File(uri.path!!),
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openPdfFileDescriptor failed for $uri", e)
            null
        }
    }

    /**
     * Reads the width and height (in PDF points) of the first page of the given PDF.
     * Returns null if the PDF cannot be read.
     * Must be called on the IO thread.
     */
    fun readFirstPageSize(context: Context, uri: Uri): Pair<Float, Float>? {
        val pfd = openPdfFileDescriptor(context, uri) ?: return null
        return try {
            val renderer = PdfRenderer(pfd)
            try {
                val page = renderer.openPage(0)
                try {
                    val w = page.width.toFloat()
                    val h = page.height.toFloat()
                    if (w > 0f && h > 0f) Pair(w, h) else null
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "readFirstPageSize failed for $uri", e)
            null
        } finally {
            pfd.close()
        }
    }

    // ─── 向後相容（舊有 content:// URI 仍可用）───────────────────────────────

    /** @deprecated 新文件改用 copyPdfToAppDir()。舊有 content:// 記錄靠此繼續讀取。 */
    @Deprecated("Use copyPdfToAppDir() for new imports. This is kept for backward compatibility with existing content:// URIs in the database.")
    fun takePersistableUriPermission(context: Context, uri: Uri): Boolean {
        return try {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "takePersistableUriPermission failed for $uri", e)
            false
        }
    }

    fun closePdfRenderer(renderer: PdfRenderer?) {
        try {
            renderer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PdfRenderer", e)
        }
    }
}