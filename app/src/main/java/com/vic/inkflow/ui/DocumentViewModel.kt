package com.vic.inkflow.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.inkflow.data.AppDatabase
import com.vic.inkflow.data.DocumentDao
import com.vic.inkflow.data.DocumentEntity
import com.vic.inkflow.data.StrokeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(private val documentDao: DocumentDao, private val strokeDao: StrokeDao, private val db: AppDatabase) : ViewModel() {

    val documents: StateFlow<List<DocumentEntity>> = documentDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordOpened(uri: String, displayName: String) {
        viewModelScope.launch {
            documentDao.upsert(DocumentEntity(uri = uri, displayName = displayName))
        }
    }

    fun delete(uri: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Delete all strokes and annotations belonging to this document first.
            strokeDao.deleteStrokesForDocument(uri)
            db.textAnnotationDao().deleteForDocument(uri)
            db.imageAnnotationDao().deleteForDocument(uri)
            // Delete the physical file for app-private documents (file:// URIs).
            try {
                val parsed = android.net.Uri.parse(uri)
                if (parsed.scheme == "file") {
                    val file = java.io.File(parsed.path!!)
                    if (file.exists()) file.delete()
                }
            } catch (_: Exception) { }
            documentDao.delete(uri)
        }
    }

    /** Re-resolve display names that were saved as raw URI segments (e.g. "document:1000000062"). */
    fun fixStaleNames(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val stalePattern = Regex("^[a-z]+:\\d+$", RegexOption.IGNORE_CASE)
            val snapshot = documents.value
            snapshot.filter { stalePattern.matches(it.displayName) }.forEach { doc ->
                val uri = Uri.parse(doc.uri)
                val resolved = try {
                    context.contentResolver.query(
                        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                } catch (_: Exception) { null }
                if (resolved != null && resolved != doc.displayName) {
                    documentDao.upsert(doc.copy(displayName = resolved))
                }
            }
        }
    }

    fun updateLastPage(uri: String, pageIndex: Int) {
        viewModelScope.launch {
            documentDao.updateLastPage(uri, pageIndex)
        }
    }

    suspend fun getLastPageIndex(uri: String): Int {
        return documentDao.getLastPageIndex(uri) ?: 0
    }

    fun rename(uri: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.renameDocument(uri, newName.trim().ifEmpty { "未命名筆記" })
        }
    }
}

class DocumentViewModelFactory(private val dao: DocumentDao, private val strokeDao: StrokeDao, private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DocumentViewModel(dao, strokeDao, db) as T
    }
}
