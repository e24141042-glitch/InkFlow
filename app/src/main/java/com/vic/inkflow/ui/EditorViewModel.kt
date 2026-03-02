package com.vic.inkflow.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.vic.inkflow.data.AppDatabase
import com.vic.inkflow.data.ImageAnnotationEntity
import com.vic.inkflow.data.PointEntity
import com.vic.inkflow.data.StrokeDao
import com.vic.inkflow.data.StrokeEntity
import com.vic.inkflow.data.StrokeWithPoints
import com.vic.inkflow.data.TextAnnotationEntity
import com.vic.inkflow.util.IntersectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class Tool {
    PEN,
    HIGHLIGHTER,
    ERASER,
    LASSO,
    SHAPE,
    TEXT,
    IMAGE,
    STAMP
}

enum class ShapeSubType { RECT, CIRCLE, LINE, ARROW }

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModel(private val db: AppDatabase, val documentUri: String) : ViewModel() {

    companion object {
        /** Fixed model coordinate space (A4 PDF points).  All DB-persisted coordinates
         *  are stored in this space so thumbnail rendering and PDF export are device-independent. */
        const val MODEL_W = 595f
        const val MODEL_H = 842f
    }

    private val strokeDao: StrokeDao = db.strokeDao()
    private val textAnnotationDao = db.textAnnotationDao()
    private val imageAnnotationDao = db.imageAnnotationDao()

    // Canvas pixel dimensions — reported by InkCanvas via setCanvasSize().
    // Default to MODEL dimensions so normalisation is identity before the first size report.
    private var canvasW = MODEL_W
    private var canvasH = MODEL_H

    fun setCanvasSize(w: Float, h: Float) {
        if (w > 0f && h > 0f) {
            canvasW = w
            canvasH = h
        }
    }

    private val _selectedTool = MutableStateFlow(Tool.PEN)
    val selectedTool: StateFlow<Tool> = _selectedTool.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color.Black)
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(5f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _selectedShapeSubType = MutableStateFlow(ShapeSubType.RECT)
    val selectedShapeSubType: StateFlow<ShapeSubType> = _selectedShapeSubType.asStateFlow()

    fun onShapeSubTypeSelected(type: ShapeSubType) { _selectedShapeSubType.value = type }

    private val _pageIndex = MutableStateFlow(0)
    val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    val currentStrokes: StateFlow<List<StrokeWithPoints>> = pageIndex.flatMapLatest { index ->
        strokeDao.getStrokesForPage(documentUri, index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTextAnnotations: StateFlow<List<TextAnnotationEntity>> = pageIndex.flatMapLatest { index ->
        textAnnotationDao.getForPage(documentUri, index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentImageAnnotations: StateFlow<List<ImageAnnotationEntity>> = pageIndex.flatMapLatest { index ->
        imageAnnotationDao.getForPage(documentUri, index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Undo / Redo stacks
    private val undoStack: ArrayDeque<DrawCommand> = ArrayDeque()
    private val redoStack: ArrayDeque<DrawCommand> = ArrayDeque()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun pushUndo(command: DrawCommand) {
        undoStack.addLast(command)
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
    }

    fun onToolSelected(tool: Tool) {
        _selectedTool.value = tool
    }

    fun setActivePage(index: Int) {
        _pageIndex.value = index
    }

    fun onColorSelected(color: Color) {
        _selectedColor.value = color
    }

    fun onStrokeWidthChanged(width: Float) {
        _strokeWidth.value = width
    }

    fun saveStroke(points: List<Offset>, color: Color, strokeWidth: Float, tool: Tool = Tool.PEN) {
        if (points.size < 2) return
        // Capture canvas dimensions on the calling (Main) thread before switching to IO.
        val cW = canvasW
        val cH = canvasH

        viewModelScope.launch(Dispatchers.IO) {
            val strokeId = UUID.randomUUID().toString()

            // Normalise from canvas-pixel space to model space (MODEL_W × MODEL_H).
            val scaleX = MODEL_W / cW
            val scaleY = MODEL_H / cH
            val normalizedPoints = points.map { Offset(it.x * scaleX, it.y * scaleY) }
            val normalizedStrokeWidth = strokeWidth * scaleX

            val path = Path().apply {
                moveTo(normalizedPoints.first().x, normalizedPoints.first().y)
                normalizedPoints.forEach { lineTo(it.x, it.y) }
            }
            val bounds = path.getBounds()

            val strokeEntity = StrokeEntity(
                id = strokeId,
                documentUri = documentUri,
                pageIndex = pageIndex.value,
                color = color.toArgb(),
                strokeWidth = normalizedStrokeWidth,
                boundsLeft = bounds.left,
                boundsTop = bounds.top,
                boundsRight = bounds.right,
                boundsBottom = bounds.bottom,
                isHighlighter = tool == Tool.HIGHLIGHTER
            )
            val pointEntities = normalizedPoints.map { PointEntity(strokeId = strokeId, x = it.x, y = it.y) }

            db.withTransaction {
                strokeDao.insertStroke(strokeEntity)
                strokeDao.insertPoints(pointEntities)
            }

            val command = DrawCommand.AddStroke(StrokeWithPoints(strokeEntity, pointEntities))
            withContext(Dispatchers.Main) { pushUndo(command) }
        }
    }

    fun deleteStrokesIntersecting(toolPath: Path) {
        val cW = canvasW
        val cH = canvasH
        viewModelScope.launch(Dispatchers.Default) {
            // Scale eraser path from canvas-pixel space to model space before comparing.
            val scaledEraserPath = android.graphics.Path(toolPath.asAndroidPath()).also { p ->
                val m = android.graphics.Matrix().apply { setScale(MODEL_W / cW, MODEL_H / cH) }
                p.transform(m)
            }
            val intersectingStrokes = IntersectionUtils.findIntersectingStrokes(
                eraserPath = scaledEraserPath,
                strokes = currentStrokes.value
            )
            if (intersectingStrokes.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    strokeDao.deleteStrokesByIds(intersectingStrokes.map { it.stroke.id })
                }
                val command = DrawCommand.RemoveStrokes(intersectingStrokes)
                withContext(Dispatchers.Main) { pushUndo(command) }
            }
            // Also erase text and image annotations whose model coords fall within the eraser bounds.
            val eraserBounds = android.graphics.RectF().also { scaledEraserPath.computeBounds(it, true) }
            val hitTexts = currentTextAnnotations.value.filter { ann ->
                // Estimate the bounding box of the text in model space.
                // isStamp = oversized emoji: treat as a square of fontSize × fontSize.
                // Regular text: width ≈ charCount × fontSize × 0.6, height ≈ fontSize × 1.2.
                // Y-axis: modelY is the text baseline, so the top edge is (modelY - height).
                val estimatedW = if (ann.isStamp) ann.fontSize else ann.text.length * ann.fontSize * 0.6f
                val estimatedH = if (ann.isStamp) ann.fontSize else ann.fontSize * 1.2f
                val annBounds = android.graphics.RectF(
                    ann.modelX,
                    ann.modelY - estimatedH,
                    ann.modelX + estimatedW,
                    ann.modelY
                )
                android.graphics.RectF.intersects(eraserBounds, annBounds)
            }
            hitTexts.forEach { ann ->
                withContext(Dispatchers.IO) { textAnnotationDao.deleteById(ann.id) }
                withContext(Dispatchers.Main) { pushUndo(DrawCommand.RemoveTextAnnotation(ann)) }
            }
            val hitImages = currentImageAnnotations.value.filter { ann ->
                android.graphics.RectF.intersects(
                    eraserBounds,
                    android.graphics.RectF(ann.modelX, ann.modelY, ann.modelX + ann.modelWidth, ann.modelY + ann.modelHeight)
                )
            }
            hitImages.forEach { ann ->
                withContext(Dispatchers.IO) { imageAnnotationDao.deleteById(ann.id) }
                withContext(Dispatchers.Main) { pushUndo(DrawCommand.RemoveImageAnnotation(ann)) }
            }
        }
    }

    /** Save a geometric shape (RECT / CIRCLE / LINE / ARROW) as a StrokeEntity. */
    fun saveShape(startPoint: Offset, endPoint: Offset, color: Color, strokeWidth: Float) {
        val cW = canvasW
        val cH = canvasH
        val shapeType = _selectedShapeSubType.value.name
        viewModelScope.launch(Dispatchers.IO) {
            val strokeId = UUID.randomUUID().toString()
            val scaleX = MODEL_W / cW
            val scaleY = MODEL_H / cH
            val p0 = Offset(startPoint.x * scaleX, startPoint.y * scaleY)
            val p1 = Offset(endPoint.x * scaleX, endPoint.y * scaleY)
            val strokeEntity = StrokeEntity(
                id = strokeId,
                documentUri = documentUri,
                pageIndex = pageIndex.value,
                color = color.toArgb(),
                strokeWidth = strokeWidth * scaleX,
                boundsLeft = minOf(p0.x, p1.x),
                boundsTop = minOf(p0.y, p1.y),
                boundsRight = maxOf(p0.x, p1.x),
                boundsBottom = maxOf(p0.y, p1.y),
                isHighlighter = false,
                shapeType = shapeType
            )
            val pointEntities = listOf(
                PointEntity(strokeId = strokeId, x = p0.x, y = p0.y),
                PointEntity(strokeId = strokeId, x = p1.x, y = p1.y)
            )
            db.withTransaction {
                strokeDao.insertStroke(strokeEntity)
                strokeDao.insertPoints(pointEntities)
            }
            val command = DrawCommand.AddStroke(StrokeWithPoints(strokeEntity, pointEntities))
            withContext(Dispatchers.Main) { pushUndo(command) }
        }
    }

    fun addTextAnnotation(text: String, canvasX: Float, canvasY: Float, fontSize: Float, color: Color, isStamp: Boolean = false) {
        if (text.isBlank()) return
        val ann = TextAnnotationEntity(
            documentUri = documentUri,
            pageIndex = pageIndex.value,
            text = text,
            modelX = canvasX * MODEL_W / canvasW,
            modelY = canvasY * MODEL_H / canvasH,
            fontSize = fontSize * MODEL_W / canvasW,
            colorArgb = color.toArgb(),
            isStamp = isStamp
        )
        viewModelScope.launch(Dispatchers.IO) {
            textAnnotationDao.insert(ann)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.AddTextAnnotation(ann)) }
        }
    }

    fun deleteTextAnnotation(id: String) {
        val ann = currentTextAnnotations.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            textAnnotationDao.deleteById(id)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.RemoveTextAnnotation(ann)) }
        }
    }

    fun commitTextAnnotationMove(id: String, canvasDeltaX: Float, canvasDeltaY: Float) {
        if (canvasDeltaX == 0f && canvasDeltaY == 0f) return
        val old = currentTextAnnotations.value.firstOrNull { it.id == id } ?: return
        val scaleX = MODEL_W / canvasW
        val scaleY = MODEL_H / canvasH
        val updated = old.copy(
            modelX = old.modelX + canvasDeltaX * scaleX,
            modelY = old.modelY + canvasDeltaY * scaleY
        )
        viewModelScope.launch(Dispatchers.IO) {
            textAnnotationDao.update(updated)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.MoveTextAnnotation(old, updated)) }
        }
    }

    fun commitTextAnnotationResize(id: String, newFontSizeModel: Float) {
        val old = currentTextAnnotations.value.firstOrNull { it.id == id } ?: return
        val clamped = newFontSizeModel.coerceAtLeast(4f)
        if (old.fontSize == clamped) return
        val updated = old.copy(fontSize = clamped)
        viewModelScope.launch(Dispatchers.IO) {
            textAnnotationDao.update(updated)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.ResizeTextAnnotation(old, updated)) }
        }
    }

    fun addImageAnnotation(uri: String, canvasX: Float, canvasY: Float, canvasWidth: Float, canvasHeight: Float) {
        val scaleX = MODEL_W / canvasW
        val scaleY = MODEL_H / canvasH
        val ann = ImageAnnotationEntity(
            documentUri = documentUri,
            pageIndex = pageIndex.value,
            uri = uri,
            modelX = canvasX * scaleX,
            modelY = canvasY * scaleY,
            modelWidth = canvasWidth * scaleX,
            modelHeight = canvasHeight * scaleY
        )
        viewModelScope.launch(Dispatchers.IO) {
            imageAnnotationDao.insert(ann)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.AddImageAnnotation(ann)) }
        }
    }

    /** Places an image at the center of the model canvas (used when picked via gallery). Returns the new annotation ID. */
    fun placeImageAnnotation(uri: String): String {
        val ann = ImageAnnotationEntity(
            documentUri = documentUri,
            pageIndex = pageIndex.value,
            uri = uri,
            modelX = MODEL_W * 0.1f,
            modelY = MODEL_H * 0.1f,
            modelWidth = MODEL_W * 0.8f,
            modelHeight = MODEL_H * 0.5f
        )
        viewModelScope.launch(Dispatchers.IO) {
            imageAnnotationDao.insert(ann)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.AddImageAnnotation(ann)) }
        }
        return ann.id
    }

    fun commitImageAnnotationMove(id: String, canvasDeltaX: Float, canvasDeltaY: Float) {
        if (canvasDeltaX == 0f && canvasDeltaY == 0f) return
        val old = currentImageAnnotations.value.firstOrNull { it.id == id } ?: return
        val scaleX = MODEL_W / canvasW
        val scaleY = MODEL_H / canvasH
        val updated = old.copy(
            modelX = old.modelX + canvasDeltaX * scaleX,
            modelY = old.modelY + canvasDeltaY * scaleY
        )
        viewModelScope.launch(Dispatchers.IO) {
            imageAnnotationDao.update(updated)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.MoveImageAnnotation(old, updated)) }
        }
    }

    fun commitImageAnnotationResize(id: String, newModelWidth: Float, newModelHeight: Float) {
        val old = currentImageAnnotations.value.firstOrNull { it.id == id } ?: return
        val updated = old.copy(
            modelWidth  = newModelWidth.coerceAtLeast(30f),
            modelHeight = newModelHeight.coerceAtLeast(30f)
        )
        viewModelScope.launch(Dispatchers.IO) {
            imageAnnotationDao.update(updated)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.ResizeImageAnnotation(old, updated)) }
        }
    }

    fun deleteImageAnnotation(id: String) {
        val ann = currentImageAnnotations.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            imageAnnotationDao.deleteById(id)
            withContext(Dispatchers.Main) { pushUndo(DrawCommand.RemoveImageAnnotation(ann)) }
        }
    }

    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (command) {
                is DrawCommand.AddStroke -> {
                    strokeDao.deleteStrokesByIds(listOf(command.stroke.stroke.id))
                }
                is DrawCommand.RemoveStrokes -> {
                    command.strokes.forEach { strokeWithPoints ->
                        db.withTransaction {
                            strokeDao.insertStroke(strokeWithPoints.stroke)
                            strokeDao.insertPoints(strokeWithPoints.points)
                        }
                    }
                }
                is DrawCommand.MoveStrokes -> {
                    command.originals.forEach { swp ->
                        db.withTransaction {
                            strokeDao.deletePointsForStroke(swp.stroke.id)
                            strokeDao.insertStroke(swp.stroke)
                            strokeDao.insertPoints(swp.points)
                        }
                    }
                }
                is DrawCommand.AddTextAnnotation -> {
                    textAnnotationDao.deleteById(command.annotation.id)
                }
                is DrawCommand.RemoveTextAnnotation -> {
                    textAnnotationDao.insert(command.annotation)
                }
                is DrawCommand.MoveTextAnnotation -> {
                    textAnnotationDao.update(command.original)
                }
                is DrawCommand.ResizeTextAnnotation -> {
                    textAnnotationDao.update(command.original)
                }
                is DrawCommand.AddImageAnnotation -> {
                    imageAnnotationDao.deleteById(command.annotation.id)
                }
                is DrawCommand.RemoveImageAnnotation -> {
                    imageAnnotationDao.insert(command.annotation)
                }
                is DrawCommand.MoveImageAnnotation -> {
                    imageAnnotationDao.update(command.original)
                }
                is DrawCommand.ResizeImageAnnotation -> {
                    imageAnnotationDao.update(command.original)
                }
            }
            withContext(Dispatchers.Main) {
                redoStack.addLast(command)
                _canUndo.value = undoStack.isNotEmpty()
                _canRedo.value = true
            }
        }
    }

    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (command) {
                is DrawCommand.AddStroke -> {
                    db.withTransaction {
                        strokeDao.insertStroke(command.stroke.stroke)
                        strokeDao.insertPoints(command.stroke.points)
                    }
                }
                is DrawCommand.RemoveStrokes -> {
                    strokeDao.deleteStrokesByIds(command.strokes.map { it.stroke.id })
                }
                is DrawCommand.MoveStrokes -> {
                    command.originals.forEach { swp ->
                        val shiftedPoints = swp.points.map { pt ->
                            PointEntity(strokeId = swp.stroke.id, x = pt.x + command.delta.x, y = pt.y + command.delta.y)
                        }
                        val shiftedStroke = swp.stroke.copy(
                            boundsLeft   = swp.stroke.boundsLeft   + command.delta.x,
                            boundsTop    = swp.stroke.boundsTop    + command.delta.y,
                            boundsRight  = swp.stroke.boundsRight  + command.delta.x,
                            boundsBottom = swp.stroke.boundsBottom + command.delta.y
                        )
                        db.withTransaction {
                            strokeDao.deletePointsForStroke(swp.stroke.id)
                            strokeDao.insertStroke(shiftedStroke)
                            strokeDao.insertPoints(shiftedPoints)
                        }
                    }
                }
                is DrawCommand.AddTextAnnotation -> {
                    textAnnotationDao.insert(command.annotation)
                }
                is DrawCommand.RemoveTextAnnotation -> {
                    textAnnotationDao.deleteById(command.annotation.id)
                }
                is DrawCommand.MoveTextAnnotation -> {
                    textAnnotationDao.update(command.updated)
                }
                is DrawCommand.ResizeTextAnnotation -> {
                    textAnnotationDao.update(command.updated)
                }
                is DrawCommand.AddImageAnnotation -> {
                    imageAnnotationDao.insert(command.annotation)
                }
                is DrawCommand.RemoveImageAnnotation -> {
                    imageAnnotationDao.deleteById(command.annotation.id)
                }
                is DrawCommand.MoveImageAnnotation -> {
                    imageAnnotationDao.update(command.updated)
                }
                is DrawCommand.ResizeImageAnnotation -> {
                    imageAnnotationDao.update(command.updated)
                }
            }
            withContext(Dispatchers.Main) {
                undoStack.addLast(command)
                _canUndo.value = true
                _canRedo.value = redoStack.isNotEmpty()
            }
        }
    }

    // Lasso selection state
    private val _selectedStrokes = MutableStateFlow<List<StrokeWithPoints>>(emptyList())
    val selectedStrokes: StateFlow<List<StrokeWithPoints>> = _selectedStrokes.asStateFlow()

    private val _lassoMoveOffset = MutableStateFlow(Offset.Zero)
    val lassoMoveOffset: StateFlow<Offset> = _lassoMoveOffset.asStateFlow()

    fun selectStrokesInLasso(polygon: List<Offset>) {
        val cW = canvasW
        val cH = canvasH
        // Normalise lasso polygon to model space.
        val normalizedPolygon = polygon.map { Offset(it.x * MODEL_W / cW, it.y * MODEL_H / cH) }
        viewModelScope.launch(Dispatchers.Default) {
            val selected = IntersectionUtils.findStrokesInLasso(normalizedPolygon, currentStrokes.value)
            withContext(Dispatchers.Main) { _selectedStrokes.value = selected }
        }
    }

    fun moveSelectedStrokes(delta: Offset) {
        // Normalise drag delta to model space so it lines up with stored stroke coordinates.
        val normalizedDelta = Offset(delta.x * MODEL_W / canvasW, delta.y * MODEL_H / canvasH)
        _lassoMoveOffset.value = _lassoMoveOffset.value + normalizedDelta
    }

    fun commitMovedStrokes() {
        val strokes = _selectedStrokes.value
        val delta = _lassoMoveOffset.value
        // Reset visual state immediately on Main thread
        _lassoMoveOffset.value = Offset.Zero
        _selectedStrokes.value = emptyList()
        if (strokes.isEmpty() || (delta.x == 0f && delta.y == 0f)) return
        viewModelScope.launch(Dispatchers.IO) {
            strokes.forEach { swp ->
                val shiftedPoints = swp.points.map { pt ->
                    PointEntity(strokeId = swp.stroke.id, x = pt.x + delta.x, y = pt.y + delta.y)
                }
                val shiftedStroke = swp.stroke.copy(
                    boundsLeft   = swp.stroke.boundsLeft   + delta.x,
                    boundsTop    = swp.stroke.boundsTop    + delta.y,
                    boundsRight  = swp.stroke.boundsRight  + delta.x,
                    boundsBottom = swp.stroke.boundsBottom + delta.y
                )
                db.withTransaction {
                    strokeDao.deletePointsForStroke(swp.stroke.id)
                    strokeDao.insertStroke(shiftedStroke)
                    strokeDao.insertPoints(shiftedPoints)
                }
            }
            val command = DrawCommand.MoveStrokes(strokes, delta)
            withContext(Dispatchers.Main) { pushUndo(command) }
        }
    }

    fun clearSelection() {
        _selectedStrokes.value = emptyList()
        _lassoMoveOffset.value = Offset.Zero
    }
}
