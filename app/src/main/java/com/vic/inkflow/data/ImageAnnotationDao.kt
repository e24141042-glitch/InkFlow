package com.vic.inkflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageAnnotationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: ImageAnnotationEntity)

    @Update
    suspend fun update(annotation: ImageAnnotationEntity)

    @Query("SELECT * FROM image_annotations WHERE documentUri = :documentUri AND pageIndex = :pageIndex")
    fun getForPage(documentUri: String, pageIndex: Int): Flow<List<ImageAnnotationEntity>>

    @Query("SELECT * FROM image_annotations WHERE documentUri = :documentUri")
    suspend fun getAllForDocument(documentUri: String): List<ImageAnnotationEntity>

    @Query("DELETE FROM image_annotations WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM image_annotations WHERE documentUri = :documentUri AND pageIndex = :pageIndex")
    suspend fun deleteForPage(documentUri: String, pageIndex: Int)

    @Query("UPDATE image_annotations SET pageIndex = pageIndex - 1 WHERE documentUri = :documentUri AND pageIndex > :deletedPageIndex")
    suspend fun shiftPageIndicesDown(documentUri: String, deletedPageIndex: Int)

    @Query("DELETE FROM image_annotations WHERE documentUri = :documentUri")
    suspend fun deleteForDocument(documentUri: String)
}
