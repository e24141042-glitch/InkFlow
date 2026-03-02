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

    // ─── 新功能：建立空白 PDF ─────────────────────────────────────────────────

    /** 建立一個單頁 A4 空白 PDF，存到 App 私有目錄，回傳 file:// Uri。失敗回傳 null。 */
    suspend fun createBlankPdf(context: Context): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val dest = File(pdfDir(context), "${UUID.randomUUID()}.pdf")
                PDDocument().use { doc ->
                    doc.addPage(PDPage(PDRectangle.A4))
                    doc.save(dest)
                }
                Uri.fromFile(dest)
            } catch (e: Exception) {
                Log.e(TAG, "createBlankPdf failed", e)
                null
            }
        }

    // ─── 新功能：在現有 PDF 末尾加入空白頁 ───────────────────────────────────

    /** 在 file:// URI 的 PDF 中，於 [afterIndex] 頁之後插入一頁 A4 空白頁。
     *  afterIndex = -1 或超過末頁時，直接追加到最後。 */
    suspend fun insertBlankPage(fileUri: Uri, afterIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (fileUri.scheme != "file") {
                Log.w(TAG, "insertBlankPage: only file:// URIs are supported")
                return@withContext false
            }
            try {
                val file = File(fileUri.path!!)
                PDDocument.load(file).use { doc ->
                    val newPage = PDPage(PDRectangle.A4)
                    val insertBefore = afterIndex + 1
                    if (insertBefore < doc.numberOfPages) {
                        doc.pages.insertBefore(newPage, doc.getPage(insertBefore))
                    } else {
                        doc.addPage(newPage)
                    }
                    doc.save(file)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "insertBlankPage failed", e)
                false
            }
        }

    /** @deprecated Use insertBlankPage instead. */
    @Deprecated("Use insertBlankPage(fileUri, afterIndex) instead", ReplaceWith("insertBlankPage(fileUri, Int.MAX_VALUE)"))
    suspend fun appendBlankPage(fileUri: Uri): Boolean = insertBlankPage(fileUri, Int.MAX_VALUE)

    /** 刪除 file:// URI PDF 中第 [pageIndex] 頁。若僅剩一頁則拒絕刪除並回傳 false。 */
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
                    doc.save(file)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "deletePage failed", e)
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