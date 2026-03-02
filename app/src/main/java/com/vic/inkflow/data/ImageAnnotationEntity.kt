package com.vic.inkflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * An image annotation placed on a PDF page.
 * All coordinates are in model space (595 × 842).
 * [uri] is a content:// or file:// URI obtained via the system media picker.
 */
@Entity(tableName = "image_annotations")
data class ImageAnnotationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentUri: String,
    val pageIndex: Int,
    val uri: String,
    val modelX: Float,
    val modelY: Float,
    val modelWidth: Float,
    val modelHeight: Float
)
