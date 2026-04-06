package com.vic.inkflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_preferences")
data class DocumentPreferenceEntity(
    @PrimaryKey
    val documentUri: String,
    val tool: String? = null,
    val colorArgb: Int? = null,
    val strokeWidth: Float? = null,
    val penStrokeWidth: Float? = null,
    val highlighterStrokeWidth: Float? = null,
    val shapeSubType: String? = null,
    /** Legacy boolean field — kept for migration reads only; new code writes [inputMode]. */
    val stylusOnlyMode: Boolean? = null,
    val inputMode: String? = null,
    val recentColorsCsv: String? = null,
    val highlighterColorArgb: Int? = null,
    val pageBackground: String? = null,
    val paperWidthPt: Float? = null,
    val paperHeightPt: Float? = null,
    val quickSwipeEraserEnabled: Boolean? = null
)
