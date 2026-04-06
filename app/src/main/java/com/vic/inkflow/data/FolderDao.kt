package com.vic.inkflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("UPDATE folders SET name = :newName, updatedAt = :updatedAt WHERE id = :folderId")
    suspend fun rename(folderId: String, newName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :folderId")
    suspend fun updateSortOrder(folderId: String, sortOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE folders
        SET parentFolderId = :parentFolderId,
            sortOrder = :sortOrder,
            updatedAt = :updatedAt
        WHERE id = :folderId
        """
    )
    suspend fun moveToParent(
        folderId: String,
        parentFolderId: String?,
        sortOrder: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1) + 1
        FROM folders
        WHERE (:parentFolderId IS NULL AND parentFolderId IS NULL)
           OR parentFolderId = :parentFolderId
        """
    )
    suspend fun getNextSortOrder(parentFolderId: String?): Int

    @Query(
        """
        WITH RECURSIVE descendants(id) AS (
            SELECT id FROM folders WHERE id = :folderId
            UNION ALL
            SELECT f.id
            FROM folders f
            INNER JOIN descendants d ON f.parentFolderId = d.id
        )
        SELECT id FROM descendants
        """
    )
    suspend fun getFolderAndDescendantIds(folderId: String): List<String>

    @Query("DELETE FROM folders WHERE id IN (:folderIds)")
    suspend fun deleteByIds(folderIds: List<String>)
}
