package com.vic.inkflow.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vic.inkflow.util.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    /** Incremented every time thumbnails are invalidated (page insert / delete / re-open).
     *  Consumers can key their `remember()` to this value to force re-fetching fresh flows. */
    private val _thumbnailVersion = MutableStateFlow(0)
    val thumbnailVersion: StateFlow<Int> = _thumbnailVersion.asStateFlow()

    private val _isScrollingFast = MutableStateFlow(false)
    val isScrollingFast: StateFlow<Boolean> = _isScrollingFast.asStateFlow()

    private val _currentPdfUri = MutableStateFlow<Uri?>(null)
    val currentPdfUri: StateFlow<Uri?> = _currentPdfUri.asStateFlow()

    // Emits the index of the newly inserted page so the UI can auto-navigate to it.
    // Resets to null after each consumption.
    private val _lastInsertedPageIndex = MutableStateFlow<Int?>(null)
    val lastInsertedPageIndex: StateFlow<Int?> = _lastInsertedPageIndex.asStateFlow()

    /** Call once after consuming lastInsertedPageIndex to reset the event. */
    fun consumeInsertedPageEvent() { _lastInsertedPageIndex.value = null }

    // Emits the index of the deleted page so the UI can adjust currentPageIndex.
    private val _lastDeletedPageIndex = MutableStateFlow<Int?>(null)
    val lastDeletedPageIndex: StateFlow<Int?> = _lastDeletedPageIndex.asStateFlow()

    /** Call once after consuming lastDeletedPageIndex to reset the event. */
    fun consumeDeletedPageEvent() { _lastDeletedPageIndex.value = null }

    // --- Caching ---
    // Bitmaps are intentionally NOT pooled or manually recycled: Compose animations
    // (Crossfade) may hold references to evicted bitmaps for a frame or two, so
    // allowing the GC to collect them is the only safe approach.
    private val bitmapCache: LruCache<Int, Bitmap>
    private val thumbnailCache: LruCache<Int, Bitmap>

    // Stable StateFlow instances so the same object is returned for the same index.
    // This prevents new coroutines being spawned every time Compose re-enters a list item.
    // ConcurrentHashMap: reads (getPageThumbnail on Main) and clears (closeRendererOnly on IO)
    // may race, so we need a thread-safe map.
    private val thumbnailFlowCache = java.util.concurrent.ConcurrentHashMap<Int, MutableStateFlow<Bitmap?>>()
    private val bitmapFlowCache = java.util.concurrent.ConcurrentHashMap<Int, MutableStateFlow<Bitmap?>>()

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, bitmap: Bitmap): Int = bitmap.byteCount / 1024
        }

        thumbnailCache = object : LruCache<Int, Bitmap>(maxMemory / 20) {
            override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    fun openPdf(uri: Uri) {
        // ViewModel survives configuration changes — skip re-opening if the same PDF is already loaded
        if (_currentPdfUri.value == uri && pdfRenderer != null) return

        viewModelScope.launch(Dispatchers.IO) {
            // Close previous PDF under the mutex to avoid racing with renderPage
            renderMutex.withLock { closePdf() }
            _currentPdfUri.value = uri
            try {
                val fd = PdfManager.openPdfFileDescriptor(getApplication(), uri)
                    ?: return@launch
                val renderer = try {
                    PdfRenderer(fd)
                } catch (e: Exception) {
                    fd.close()
                    return@launch
                }
                renderMutex.withLock {
                    parcelFileDescriptor = fd
                    pdfRenderer = renderer
                }
                _pageCount.value = renderer.pageCount
            } catch (e: Exception) {
                android.util.Log.e("PdfViewModel", "Failed to open PDF: $uri", e)
            }
        }
    }

    /** 僅關閉 Renderer／FD，不清空 URI 或 pageCount（用於追加頁面後重新開啟）。 */
    private fun closeRendererOnly() {
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
        pdfRenderer = null
        parcelFileDescriptor = null
        bitmapCache.evictAll()
        thumbnailCache.evictAll()
        thumbnailFlowCache.clear()
        bitmapFlowCache.clear()
    }

    /** 在 Main 執行緒清空 flow cache，使下一次 getPageThumbnail/getPageBitmap 強制重新渲染。 */
    private fun clearFlowCaches() {
        thumbnailFlowCache.clear()
        bitmapFlowCache.clear()
    }

    /** 在目前 PDF（須為 file:// URI）的 [afterIndex] 頁之後插入一頁 A4 空白頁。
     *  afterIndex = -1 或 Int.MAX_VALUE 時追加到末尾。
     *  完成後重新載入 Renderer 並發出 lastInsertedPageIndex。 */
    fun insertBlankPage(context: Context, afterIndex: Int) {
        val fileUri = _currentPdfUri.value ?: return
        if (fileUri.scheme != "file") {
            android.util.Log.w("PdfViewModel", "insertBlankPage: only file:// URIs supported")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            renderMutex.withLock { closeRendererOnly() }
            val ok = com.vic.inkflow.util.PdfManager.insertBlankPage(fileUri, afterIndex)
            if (!ok) return@launch
            try {
                val fd = ParcelFileDescriptor.open(
                    File(fileUri.path!!),
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                val renderer = try {
                    PdfRenderer(fd)
                } catch (e: Exception) {
                    fd.close()
                    return@launch
                }
                renderMutex.withLock {
                    parcelFileDescriptor = fd
                    pdfRenderer = renderer
                }
                _pageCount.value = renderer.pageCount
                _thumbnailVersion.value++   // signal sidebar to re-fetch all thumbnails
                // Clamp afterIndex so it always points to a valid inserted page
                val newPageIndex = (afterIndex + 1).coerceIn(0, renderer.pageCount - 1)
                _lastInsertedPageIndex.value = newPageIndex
            } catch (e: Exception) {
                android.util.Log.e("PdfViewModel", "insertBlankPage reopen failed", e)
            }
        }
    }

    /** 刪除目前 PDF 中第 [pageIndex] 頁（file:// URI），重新載入 Renderer 並發出 lastDeletedPageIndex。 */
    fun deletePage(pageIndex: Int) {
        val fileUri = _currentPdfUri.value ?: return
        if (fileUri.scheme != "file") {
            android.util.Log.w("PdfViewModel", "deletePage: only file:// URIs supported")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            renderMutex.withLock { closeRendererOnly() }
            val ok = com.vic.inkflow.util.PdfManager.deletePage(fileUri, pageIndex)
            if (!ok) return@launch
            try {
                val fd = ParcelFileDescriptor.open(
                    File(fileUri.path!!),
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                val renderer = try {
                    PdfRenderer(fd)
                } catch (e: Exception) {
                    fd.close()
                    return@launch
                }
                renderMutex.withLock {
                    parcelFileDescriptor = fd
                    pdfRenderer = renderer
                }
                _pageCount.value = renderer.pageCount
                _thumbnailVersion.value++   // signal sidebar to re-fetch all thumbnails
                _lastDeletedPageIndex.value = pageIndex
            } catch (e: Exception) {
                android.util.Log.e("PdfViewModel", "deletePage reopen failed", e)
            }
        }
    }

    private fun closePdf() {
        _currentPdfUri.value = null
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
        pdfRenderer = null
        parcelFileDescriptor = null
        _pageCount.value = 0
        bitmapCache.evictAll()
        thumbnailCache.evictAll()
        thumbnailFlowCache.clear()
        bitmapFlowCache.clear()
    }

    fun getPageBitmap(pageIndex: Int): StateFlow<Bitmap?> {
        val existing = bitmapFlowCache[pageIndex]
        if (existing != null) return existing
        val flow = MutableStateFlow(bitmapCache[pageIndex])
        bitmapFlowCache[pageIndex] = flow
        if (flow.value == null) {
            viewModelScope.launch(Dispatchers.IO) {
                renderPage(pageIndex, highQuality = true)?.let {
                    flow.value = it
                }
            }
        }
        return flow
    }

    fun getPageThumbnail(pageIndex: Int): StateFlow<Bitmap?> {
        val existing = thumbnailFlowCache[pageIndex]
        if (existing != null) return existing
        val flow = MutableStateFlow(thumbnailCache[pageIndex])
        thumbnailFlowCache[pageIndex] = flow
        if (flow.value == null) {
            viewModelScope.launch(Dispatchers.IO) {
                renderPage(pageIndex, highQuality = false)?.let { flow.value = it }
            }
        }
        return flow
    }

    fun prefetchPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= pageCount.value) return
        if (bitmapCache[pageIndex] != null) return

        viewModelScope.launch(Dispatchers.IO) {
            renderPage(pageIndex, highQuality = true)
        }
    }
    
    fun setScrollingFast(isFast: Boolean) {
        _isScrollingFast.value = isFast
    }

    private suspend fun renderPage(pageIndex: Int, highQuality: Boolean): Bitmap? {
        if (pageIndex < 0 || pageIndex >= _pageCount.value) return null

        val cache = if (highQuality) bitmapCache else thumbnailCache
        cache[pageIndex]?.let { return it }

        return withContext(Dispatchers.IO) {
            renderMutex.withLock {
                pdfRenderer?.let { renderer ->
                    try {
                        val page = renderer.openPage(pageIndex)
                        val scale = if (highQuality) 2f else 0.4f
                        // For thumbnails, cap width at 240 px to keep memory reasonable
                        val rawW = (page.width * scale).toInt()
                        val rawH = (page.height * scale).toInt()
                        val width: Int
                        val height: Int
                        if (!highQuality && rawW > 240) {
                            width = 240
                            height = (rawH * 240f / rawW).toInt().coerceAtLeast(1)
                        } else {
                            width = rawW.coerceAtLeast(1)
                            height = rawH.coerceAtLeast(1)
                        }
                        // Always use ARGB_8888 for correct PDF rendering
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)

                        // Render page content; blank pages (e.g. inserted via PdfBox) may throw —
                        // we still return the white-filled bitmap so the thumbnail shows correctly.
                        try {
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        } catch (renderEx: Exception) {
                            android.util.Log.w("PdfViewModel", "page.render failed for page $pageIndex, returning white bitmap", renderEx)
                        } finally {
                            page.close()
                        }

                        cache.put(pageIndex, bitmap)
                        bitmap
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    private fun obtainBitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    override fun onCleared() {
        closePdf()
        super.onCleared()
    }
}
