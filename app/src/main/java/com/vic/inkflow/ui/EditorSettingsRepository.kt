package com.vic.inkflow.ui

import androidx.room.withTransaction
import com.vic.inkflow.data.AppDatabase
import com.vic.inkflow.data.DocumentPreferenceDao
import com.vic.inkflow.data.DocumentPreferenceEntity

data class DrawingPreferences(
    val tool: Tool,
    val colorArgb: Int,
    val highlighterColorArgb: Int,
    val penStrokeWidth: Float,
    val highlighterStrokeWidth: Float,
    val shapeSubType: ShapeSubType,
    val inputMode: InputMode,
    val recentColors: List<Int>,
    val background: PageBackground,
    val paperWidthPt: Float?,
    val paperHeightPt: Float?,
    val quickSwipeEraserEnabled: Boolean
)

class EditorSettingsRepository(
    private val db: AppDatabase,
    private val documentPreferenceDao: DocumentPreferenceDao
) {
    companion object {
        private const val DEFAULT_PEN_STROKE_WIDTH = 4f
        private const val DEFAULT_HIGHLIGHTER_STROKE_WIDTH = 8f
        private const val DEFAULT_HIGHLIGHTER_COLOR_ARGB = 0xFFFFC700.toInt()
        private val DEFAULT_RECENT_COLORS = listOf(
            0xFF000000.toInt(),
            0xFFFFC700.toInt(),
            0xFFF44336.toInt(),
            0xFF4CAF50.toInt()
        )
    }

    suspend fun resolvePreferences(documentUri: String): DrawingPreferences {
        val local = documentPreferenceDao.getByDocumentUri(documentUri)
        return DrawingPreferences(
            tool = local?.tool?.toToolOrNull() ?: Tool.PEN,
            colorArgb = local?.colorArgb ?: 0xFF000000.toInt(),
            highlighterColorArgb = local?.highlighterColorArgb ?: DEFAULT_HIGHLIGHTER_COLOR_ARGB,
            penStrokeWidth = local?.penStrokeWidth ?: local?.strokeWidth ?: DEFAULT_PEN_STROKE_WIDTH,
            highlighterStrokeWidth = local?.highlighterStrokeWidth ?: local?.strokeWidth ?: DEFAULT_HIGHLIGHTER_STROKE_WIDTH,
            shapeSubType = local?.shapeSubType?.toShapeSubTypeOrNull() ?: ShapeSubType.RECT,
            inputMode = local?.inputMode?.toInputModeOrNull()
                ?: (if (local?.stylusOnlyMode == true) InputMode.STYLUS_ONLY else InputMode.FREE),
            recentColors = local?.recentColorsCsv?.toColorListOrNull() ?: DEFAULT_RECENT_COLORS,
            background = local?.pageBackground?.toPageBackgroundOrNull() ?: PageBackground.BLANK,
            paperWidthPt = local?.paperWidthPt,
            paperHeightPt = local?.paperHeightPt,
            quickSwipeEraserEnabled = local?.quickSwipeEraserEnabled ?: false
        )
    }

    suspend fun setTool(documentUri: String, tool: Tool) {
        upsertDocument(documentUri) { copy(tool = tool.name) }
    }

    suspend fun setColor(documentUri: String, tool: Tool, colorArgb: Int, recentColors: List<Int>) {
        upsertDocument(documentUri) {
            when (tool) {
                Tool.HIGHLIGHTER -> copy(highlighterColorArgb = colorArgb, recentColorsCsv = recentColors.toCsv())
                else -> copy(colorArgb = colorArgb, recentColorsCsv = recentColors.toCsv())
            }
        }
    }

    suspend fun setStrokeWidth(documentUri: String, tool: Tool, strokeWidth: Float) {
        upsertDocument(documentUri) {
            when (tool) {
                Tool.HIGHLIGHTER -> copy(strokeWidth = strokeWidth, highlighterStrokeWidth = strokeWidth)
                Tool.PEN -> copy(strokeWidth = strokeWidth, penStrokeWidth = strokeWidth)
                else -> this
            }
        }
    }

    suspend fun setShapeSubType(documentUri: String, shapeSubType: ShapeSubType) {
        upsertDocument(documentUri) { copy(shapeSubType = shapeSubType.name) }
    }

    suspend fun setInputMode(documentUri: String, inputMode: InputMode) {
        upsertDocument(documentUri) { copy(inputMode = inputMode.name) }
    }

    suspend fun setBackground(documentUri: String, background: PageBackground) {
        upsertDocument(documentUri) { copy(pageBackground = background.name) }
    }

    suspend fun setPaperStyle(documentUri: String, style: PaperStyle) {
        upsertDocument(documentUri) {
            copy(
                pageBackground = style.background.name,
                paperWidthPt = style.widthPt,
                paperHeightPt = style.heightPt
            )
        }
    }

    suspend fun setQuickSwipeEraserEnabled(documentUri: String, enabled: Boolean) {
        upsertDocument(documentUri) { copy(quickSwipeEraserEnabled = enabled) }
    }

    suspend fun resetDocument(documentUri: String) {
        documentPreferenceDao.deleteByDocumentUri(documentUri)
    }

    private suspend fun upsertDocument(
        documentUri: String,
        update: DocumentPreferenceEntity.() -> DocumentPreferenceEntity
    ) {
        db.withTransaction {
            val current = documentPreferenceDao.getByDocumentUri(documentUri)
                ?: DocumentPreferenceEntity(documentUri = documentUri)
            documentPreferenceDao.upsert(current.update())
        }
    }
}

private fun String.toToolOrNull(): Tool? =
    runCatching { Tool.valueOf(this) }.getOrNull()

private fun String.toShapeSubTypeOrNull(): ShapeSubType? =
    runCatching { ShapeSubType.valueOf(this) }.getOrNull()

private fun String.toInputModeOrNull(): InputMode? =
    runCatching { InputMode.valueOf(this) }.getOrNull()

private fun String.toPageBackgroundOrNull(): PageBackground? =
    runCatching { PageBackground.valueOf(this) }.getOrNull()

private fun String.toColorListOrNull(): List<Int>? {
    if (isBlank()) return null
    val parsed = split(',').mapNotNull { it.toIntOrNull() }
    return if (parsed.isEmpty()) null else parsed.take(8)
}

private fun List<Int>.toCsv(): String =
    take(8).joinToString(",")
