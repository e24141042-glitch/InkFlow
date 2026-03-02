package com.vic.inkflow.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

// This class is not an entity itself, but its fields will be embedded in PointEntity.
// Kept for compatibility with other parts of the code.
data class PointF(val x: Float, val y: Float)

/**
 * [REFACTORED] Represents the metadata for a single stroke, without the point data.
 * Added bounding box fields for efficient broad-phase collision detection.
 */
@Entity(tableName = "strokes")
data class StrokeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentUri: String,   // scopes this stroke to a specific document
    val pageIndex: Int,
    val color: Int,
    val strokeWidth: Float,

    // Performance Optimization: Cached bounding box
    val boundsLeft: Float = 0f,
    val boundsTop: Float = 0f,
    val boundsRight: Float = 0f,
    val boundsBottom: Float = 0f,

    // Tool type — true = BlendMode.Multiply highlighter, false = normal pen
    val isHighlighter: Boolean = false,

    /**
     * null = freehand stroke.
     * "RECT" | "CIRCLE" | "LINE" | "ARROW" = geometric shape.
     * Points: RECT/CIRCLE use bounds; LINE/ARROW use first & last PointEntity.
     */
    val shapeType: String? = null
)

/**
 * [NEW] Represents a single point within a stroke.
 * It is linked to a StrokeEntity via a foreign key.
 */
@Entity(
    tableName = "points",
    foreignKeys = [ForeignKey(
        entity = StrokeEntity::class,
        parentColumns = ["id"],
        childColumns = ["strokeId"],
        onDelete = ForeignKey.CASCADE // Ensures points are deleted when their parent stroke is.
    )],
    indices = [Index(value = ["strokeId"])] // Speeds up queries for points of a specific stroke.
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val strokeId: String,
    val x: Float,
    val y: Float
)

/**
 * [NEW] A data class for holding the result of a one-to-many query.
 * Room will automatically populate the 'stroke' and its related 'points'.
 */
data class StrokeWithPoints(
    @Embedded val stroke: StrokeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "strokeId"
    )
    val points: List<PointEntity>
)
