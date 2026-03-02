package com.vic.inkflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TextAnnotationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: TextAnnotationEntity)

    @Query("SELECT * FROM text_annotations WHERE documentUri = :documentUri AND pageIndex = :pageIndex")
    fun getForPage(documentUri: String, pageIndex: Int): Flow<List<TextAnnotationEntity>>

    @Query("SELECT * FROM text_annotations WHERE documentUri = :documentUri")
    suspend fun getAllForDocument(documentUri: String): List<TextAnnotationEntity>

    @Update
    suspend fun update(annotation: TextAnnotationEntity)

    @Query("DELETE FROM text_annotations WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM text_annotations WHERE documentUri = :documentUri AND pageIndex = :pageIndex")
    suspend fun deleteForPage(documentUri: String, pageIndex: Int)

    @Query("UPDATE text_annotations SET pageIndex = pageIndex - 1 WHERE documentUri = :documentUri AND pageIndex > :deletedPageIndex")
    suspend fun shiftPageIndicesDown(documentUri: String, deletedPageIndex: Int)

    @Query("DELETE FROM text_annotations WHERE documentUri = :documentUri")
    suspend fun deleteForDocument(documentUri: String)
}
