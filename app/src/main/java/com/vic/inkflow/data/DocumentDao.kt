package com.vic.inkflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY lastOpenedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsSync(): List<DocumentEntity>

    @Query("DELETE FROM documents WHERE uri = :uri")
    suspend fun delete(uri: String): Int

    @Query("UPDATE documents SET lastPageIndex = :pageIndex WHERE uri = :uri")
    suspend fun updateLastPage(uri: String, pageIndex: Int)

    @Query("UPDATE documents SET displayName = :newName WHERE uri = :uri")
    suspend fun renameDocument(uri: String, newName: String)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavoriteStatus(uri: String, isFavorite: Boolean)

    @Query("UPDATE documents SET folderId = :folderId WHERE uri = :uri")
    suspend fun updateFolder(uri: String, folderId: String?)

    @Query("UPDATE documents SET folderId = :folderId WHERE uri IN (:uris)")
    suspend fun moveDocumentsToFolder(uris: List<String>, folderId: String?)

    @Query("UPDATE documents SET folderId = NULL WHERE folderId IN (:folderIds)")
    suspend fun clearFolderAssignmentsInFolders(folderIds: List<String>)

    @Query("UPDATE documents SET lastOpenedAt = :timestamp WHERE uri = :uri")
    suspend fun updateLastOpenedAt(uri: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT lastPageIndex FROM documents WHERE uri = :uri")
    suspend fun getLastPageIndex(uri: String): Int?
}
