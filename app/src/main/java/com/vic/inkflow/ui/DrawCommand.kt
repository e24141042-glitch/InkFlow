package com.vic.inkflow.ui

import androidx.compose.ui.geometry.Offset
import com.vic.inkflow.data.ImageAnnotationEntity
import com.vic.inkflow.data.StrokeWithPoints
import com.vic.inkflow.data.TextAnnotationEntity

sealed class DrawCommand {
    data class AddStroke(val stroke: StrokeWithPoints) : DrawCommand()
    data class RemoveStrokes(val strokes: List<StrokeWithPoints>) : DrawCommand()
    data class MoveStrokes(val originals: List<StrokeWithPoints>, val delta: Offset) : DrawCommand()
    data class ResizeStrokes(val originals: List<StrokeWithPoints>, val updated: List<StrokeWithPoints>) : DrawCommand()
    data class AddTextAnnotation(val annotation: TextAnnotationEntity) : DrawCommand()
    data class RemoveTextAnnotation(val annotation: TextAnnotationEntity) : DrawCommand()
    data class MoveTextAnnotation(val original: TextAnnotationEntity, val updated: TextAnnotationEntity) : DrawCommand()
    data class ResizeTextAnnotation(val original: TextAnnotationEntity, val updated: TextAnnotationEntity) : DrawCommand()
    data class AddImageAnnotation(val annotation: ImageAnnotationEntity) : DrawCommand()
    data class RemoveImageAnnotation(val annotation: ImageAnnotationEntity) : DrawCommand()
    data class MoveImageAnnotation(val original: ImageAnnotationEntity, val updated: ImageAnnotationEntity) : DrawCommand()
    data class ResizeImageAnnotation(val original: ImageAnnotationEntity, val updated: ImageAnnotationEntity) : DrawCommand()
    data class MoveSelectionMixed(
        val strokeOriginals: List<StrokeWithPoints>,
        val strokeUpdated: List<StrokeWithPoints>,
        val imageOriginals: List<ImageAnnotationEntity>,
        val imageUpdated: List<ImageAnnotationEntity>
    ) : DrawCommand()
    data class ResizeSelectionMixed(
        val strokeOriginals: List<StrokeWithPoints>,
        val strokeUpdated: List<StrokeWithPoints>,
        val imageOriginals: List<ImageAnnotationEntity>,
        val imageUpdated: List<ImageAnnotationEntity>
    ) : DrawCommand()
    data class AddSelectionCopies(val strokes: List<StrokeWithPoints>, val images: List<ImageAnnotationEntity>) : DrawCommand()
    data class RemoveSelectionMixed(val strokes: List<StrokeWithPoints>, val images: List<ImageAnnotationEntity>) : DrawCommand()
}

