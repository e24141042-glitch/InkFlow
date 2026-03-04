package com.vic.inkflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DocumentPreferenceDao {

    @Query("SELECT * FROM document_preferences WHERE documentUri = :documentUri LIMIT 1")
    suspend fun getByDocumentUri(documentUri: String): DocumentPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentPreferenceEntity)

    @Query("DELETE FROM document_preferences WHERE documentUri = :documentUri")
    suspend fun deleteByDocumentUri(documentUri: String)

    @Query("DELETE FROM document_preferences")
    suspend fun clearAll()
}
