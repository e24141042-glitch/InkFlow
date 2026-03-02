package com.vic.inkflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A text (or stamp) annotation placed on a PDF page.
 * modelX/modelY are in model space (595 × 842 PDF points).
 */
@Entity(tableName = "text_annotations")
data class TextAnnotationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentUri: String,
    val pageIndex: Int,
    val text: String,
    val modelX: Float,
    val modelY: Float,
    /** Font size in model-space points (same scale as strokeWidth). */
    val fontSize: Float = 16f,
    val colorArgb: Int = android.graphics.Color.BLACK,
    /** true = oversized emoji stamp, false = regular text label */
    val isStamp: Boolean = false
)
