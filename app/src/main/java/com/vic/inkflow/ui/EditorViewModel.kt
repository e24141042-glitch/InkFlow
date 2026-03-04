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
import com.vic.inkflow.util.EnvelopeUtils
import com.vic.inkflow.util.StrokePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

enum class LassoSubType { FREEFORM, RECT }

enum class InputMode {
    FREE,             // 全開放，所有觸控都可畫
    PALM_REJECTION,   // 演算法過濾手掌，保留細筆跡
    STYLUS_ONLY       // 僅硬體觸控筆（PointerType.Stylus）可畫
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModel(
    private val db: AppDatabase,
    val documentUri: String,
    private val settingsRepository: EditorSettingsRepository
) : ViewModel() {

    companion object {
        /** Default model coordinate space (A4 portrait PDF points). */
        const val MODEL_W = 595f
        const val MODEL_H = 842f
    }

    // ── Paper Style ──────────────────────────────────────────────────────────────
    private val _paperStyle = MutableStateFlow(PaperStyle())
    val paperStyle: StateFlow<PaperStyle> = _paperStyle.asStateFlow()

    /** Model coordinate width for this document. All DB coordinates are in [0, modelWidth]. */
    val modelWidth: Float get() = _paperStyle.value.widthPt
    /** Model coordinate height for this document. All DB coordinates are in [0, modelHeight]. */
    val modelHeight: Float get() = _paperStyle.value.heightPt

    /**
     * Called once when a PDF is opened to set the model space to match the first page.
     * Must be called before any strokes are drawn.
     */
    fun initializePaperSize(w: Float, h: Float) {
        if (w > 0f && h > 0f) {
            _paperStyle.value = _paperStyle.value.copy(widthPt = w, heightPt = h)
            canvasW = w
            canvasH = h
        }
    }

    /** Updates the paper style (size + background template). */
    fun setPaperStyle(style: PaperStyle) {
        _paperStyle.value = style
        canvasW = style.widthPt
        canvasH = style.heightPt
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setPaperStyle(documentUri, style)
        }
    }

    private val strokeDao: StrokeDao = db.strokeDao()
    private val textAnnotationDao = db.textAnnotationDao()
    private val imageAnnotationDao = db.imageAnnotationDao()

    // Canvas pixel dimensions — reported by InkCanvas via setCanvasSize().
    // Default to MODEL dimensions so normalisation is identity before the first size report.
    private var canvasW = modelWidth
    private var canvasH = modelHeight

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

    // Per-tool color memory (updated on init and whenever the user picks a color)
    private var penColor: Color = Color.Black
    private var highlighterColor: Color = Color(0xFFFFC700)

    private val _recentColors = MutableStateFlow(
        listOf(
            Color(0xFF000000),
            Color(0xFFFFC700),
            Color(0xFFF44336),
            Color(0xFF4CAF50)
        )
    )
    val recentColors: StateFlow<List<Color>> = _recentColors.asStateFlow()

    private val _strokeWidth = MutableStateFlow(5f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _inputMode = MutableStateFlow(InputMode.FREE)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    private val _selectedShapeSubType = MutableStateFlow(ShapeSubType.RECT)
    val selectedShapeSubType: StateFlow<ShapeSubType> = _selectedShapeSubType.asStateFlow()

    private val _selectedLassoSubType = MutableStateFlow(LassoSubType.FREEFORM)
    val selectedLassoSubType: StateFlow<LassoSubType> = _selectedLassoSubType.asStateFlow()

    fun onLassoSubTypeSelected(type: LassoSubType) {
        _selectedLassoSubType.value = type
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = settingsRepository.resolvePreferences(documentUri)
            withContext(Dispatchers.Main) {
                _selectedTool.value = prefs.tool
                penColor = Color(prefs.colorArgb)
                highlighterColor = Color(prefs.highlighterColorArgb)
                _selectedColor.value = if (prefs.tool == Tool.HIGHLIGHTER) highlighterColor else penColor
                _strokeWidth.value = prefs.strokeWidth
                _selectedShapeSubType.value = prefs.shapeSubType
                _inputMode.value = prefs.inputMode
                _recentColors.value = prefs.recentColors.map { Color(it) }
                val restoredStyle = _paperStyle.value.copy(
                    background = prefs.background,
                    widthPt = prefs.paperWidthPt ?: _paperStyle.value.widthPt,
                    heightPt = prefs.paperHeightPt ?: _paperStyle.value.heightPt
                )
                _paperStyle.value = restoredStyle
                canvasW = restoredStyle.widthPt
                canvasH = restoredStyle.heightPt
            }
        }
    }

    fun cycleInputMode() {
        val next = when (_inputMode.value) {
            InputMode.FREE -> InputMode.PALM_REJECTION
            InputMode.PALM_REJECTION -> InputMode.STYLUS_ONLY
            InputMode.STYLUS_ONLY -> InputMode.FREE
        }
        _inputMode.value = next
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setInputMode(documentUri, next)
        }
    }

    fun onShapeSubTypeSelected(type: ShapeSubType) {
        _selectedShapeSubType.value = type
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setShapeSubType(documentUri, type)
        }
    }

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
        when (tool) {
            Tool.PEN -> _selectedColor.value = penColor
            Tool.HIGHLIGHTER -> _selectedColor.value = highlighterColor
            else -> {}
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setTool(documentUri, tool)
        }
    }

    fun setActivePage(index: Int) {
        _pageIndex.value = index
    }

    fun onColorSelected(color: Color) {
        _selectedColor.value = color
        val updatedRecent = listOf(color) + _recentColors.value.filterNot { it == color }
        _recentColors.value = updatedRecent.take(8)
        when (_selectedTool.value) {
            Tool.PEN -> penColor = color
            Tool.HIGHLIGHTER -> highlighterColor = color
            else -> {}
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setColor(
                documentUri = documentUri,
                tool = _selectedTool.value,
                colorArgb = color.toArgb(),
                recentColors = _recentColors.value.map { it.toArgb() }
            )
        }
    }

    fun onStrokeWidthChanged(width: Float) {
        _strokeWidth.value = width
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setStrokeWidth(documentUri, width)
        }
    }

    fun saveStroke(points: List<StrokePoint>, color: Color, tool: Tool = Tool.PEN, baseWidth: Float = 5f) {
        if (points.size < 2) return
        // Capture canvas dimensions on the calling (Main) thread before switching to IO.
        val cW = canvasW
        val cH = canvasH

        viewModelScope.launch(Dispatchers.IO) {
            val strokeId = UUID.randomUUID().toString()

            // Normalise from canvas-pixel space to model space.
            val scaleX = modelWidth / cW
            val scaleY = modelHeight / cH
            
            // Map points to model space and construct path for bounds
            val normalizedPoints = points.map { 
                StrokePoint(it.x * scaleX, it.y * scaleY, it.width * scaleX) 
            }

            val path = EnvelopeUtils.generateEnvelopePath(normalizedPoints)
            val bounds = path.getBounds()

            val strokeEntity = StrokeEntity(
                id = strokeId,
                documentUri = documentUri,
                pageIndex = pageIndex.value,
                color = color.toArgb(),
                // Store the user-selected base width (normalised to model space) so PDF export
                // uses the correct line thickness rather than the velocity-derived per-point width.
                strokeWidth = baseWidth * scaleX,
                boundsLeft = bounds.left,
                boundsTop = bounds.top,
                boundsRight = bounds.right,
                boundsBottom = bounds.bottom,
                isHighlighter = tool == Tool.HIGHLIGHTER
            )
            val pointEntities = normalizedPoints.map { PointEntity(strokeId = strokeId, x = it.x, y = it.y, width = it.width) }

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
                val m = android.graphics.Matrix().apply { setScale(modelWidth / cW, modelHeight / cH) }
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
            val scaleX = modelWidth / cW
            val scaleY = modelHeight / cH
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
            modelX = canvasX * modelWidth / canvasW,
            modelY = canvasY * modelHeight / canvasH,
            fontSize = fontSize * modelWidth / canvasW,
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
        val scaleX = modelWidth / canvasW
        val scaleY = modelHeight / canvasH
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
        val scaleX = modelWidth / canvasW
        val scaleY = modelHeight / canvasH
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
    fun placeImageAnnotation(uri: String, imagePixelWidth: Int = 0, imagePixelHeight: Int = 0): String {
        // Compute initial model-space dimensions that respect the image's original aspect ratio.
        val (initW, initH) = if (imagePixelWidth > 0 && imagePixelHeight > 0) {
            val aspectRatio = imagePixelHeight.toFloat() / imagePixelWidth.toFloat()
            var w = modelWidth * 0.8f
            var h = w * aspectRatio
            // If a tall image would overflow the page height, clamp to 90 % of page height instead.
            if (h > modelHeight * 0.9f) {
                h = modelHeight * 0.9f
                w = h / aspectRatio
            }
            Pair(w, h)
        } else {
            // Fallback: original fixed dimensions (backward-compatible with old call sites).
            Pair(modelWidth * 0.8f, modelHeight * 0.5f)
        }
        val ann = ImageAnnotationEntity(
            documentUri = documentUri,
            pageIndex = pageIndex.value,
            uri = uri,
            modelX = modelWidth * 0.1f,
            modelY = modelHeight * 0.1f,
            modelWidth = initW,
            modelHeight = initH
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
        val scaleX = modelWidth / canvasW
        val scaleY = modelHeight / canvasH
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

    private val _lassoPolygon = MutableStateFlow<List<Offset>>(emptyList())
    val lassoPolygon: StateFlow<List<Offset>> = _lassoPolygon.asStateFlow()

    private val _lassoMoveOffset = MutableStateFlow(Offset.Zero)
    val lassoMoveOffset: StateFlow<Offset> = _lassoMoveOffset.asStateFlow()

    // Guard extraction workflow from accidental re-entry (e.g., rapid double taps).
    private val extractionMutex = Mutex()

    fun selectStrokesInLasso(polygon: List<Offset>) {
        val cW = canvasW
        val cH = canvasH

        // Compose already applies the graphicsLayer inverse transform when routing
        // screen-space touches to InkCanvas (a child of the graphicsLayer-modified
        // Surface), so `polygon` arrives in canvas layout-space — the same space that
        // saveStroke() normalises with p.x * modelWidth / canvasW.  Applying the inverse
        // a second time would double-invert and shift the lasso at any zoom ≠ 1.
        val normalizedPolygon = polygon.map { Offset(it.x * modelWidth / cW, it.y * modelHeight / cH) }
        _lassoPolygon.value = normalizedPolygon
        viewModelScope.launch(Dispatchers.Default) {
            val selected = IntersectionUtils.findStrokesInLasso(normalizedPolygon, currentStrokes.value)
            withContext(Dispatchers.Main) { _selectedStrokes.value = selected }
        }
    }

    fun moveSelectedStrokes(delta: Offset) {
        // Normalise drag delta to model space so it lines up with stored stroke coordinates.
        val normalizedDelta = Offset(delta.x * modelWidth / canvasW, delta.y * modelHeight / canvasH)
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
        _lassoPolygon.value = emptyList()
        _lassoMoveOffset.value = Offset.Zero
    }

    suspend fun extractRegionToNewPage(
        context: android.content.Context,
        sourcePageIndex: Int,
        targetPageIndex: Int,
        pdfPageBitmap: android.graphics.Bitmap?
    ) {
        val polygon = _lassoPolygon.value
        if (polygon.isEmpty()) return

        if (!extractionMutex.tryLock()) return
        try {
            withContext(Dispatchers.IO) {
            val sourceStrokes = strokeDao.getStrokesForPage(documentUri, sourcePageIndex).first()
            val sourceImageAnnotations = imageAnnotationDao.getForPage(documentUri, sourcePageIndex).first()
            val sourceTextAnnotations = textAnnotationDao.getForPage(documentUri, sourcePageIndex).first()

            // Bounding box of lasso polygon in model space
            val minX = polygon.minOf { it.x }
            val minY = polygon.minOf { it.y }
            val maxX = polygon.maxOf { it.x }
            val maxY = polygon.maxOf { it.y }
            if (maxX <= minX || maxY <= minY) return@withContext

            val cropW = maxX - minX
            val cropH = maxY - minY

            // ── Step 1: Render full model page onto a bitmap ──────────────────────
            // Each model unit = renderScale pixels so all coordinates stay simple.
            val renderScale = 2f
            val fullW = (modelWidth * renderScale).toInt()
            val fullH = (modelHeight * renderScale).toInt()
            val fullBitmap = android.graphics.Bitmap.createBitmap(
                fullW, fullH, android.graphics.Bitmap.Config.ARGB_8888
            )
            val fullCanvas = android.graphics.Canvas(fullBitmap)

            // Uniform scale: model (x, y) → pixel (x*renderScale, y*renderScale)
            fullCanvas.scale(renderScale, renderScale)
            fullCanvas.drawColor(android.graphics.Color.WHITE)

            // PDF layer: prefer UI snapshot; if unavailable, render directly from source PDF.
            // Track whether the bitmap was created locally so we can recycle it after drawing.
            val localFallbackBitmap = if (pdfPageBitmap == null) renderPdfPageFromDocumentUri(sourcePageIndex, fullW, fullH) else null
            val resolvedPdfBitmap = pdfPageBitmap ?: localFallbackBitmap
            if (resolvedPdfBitmap == null) {
                // Avoid generating a wrong composite (missing PDF layer).
                return@withContext
            }
            fullCanvas.drawBitmap(
                resolvedPdfBitmap,
                android.graphics.Rect(0, 0, resolvedPdfBitmap.width, resolvedPdfBitmap.height),
                android.graphics.RectF(0f, 0f, modelWidth, modelHeight),
                android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
            )
            // Release the locally-rendered fallback immediately; the caller-owned pdfPageBitmap
            // must NOT be recycled here (it lives in PdfViewModel's LruCache).
            localFallbackBitmap?.recycle()

            // Image annotations layer
            for (img in sourceImageAnnotations) {
                val bmp = loadBitmapFromUri(context, img.uri)
                if (bmp != null) {
                    fullCanvas.drawBitmap(
                        bmp, null,
                        android.graphics.RectF(img.modelX, img.modelY, img.modelX + img.modelWidth, img.modelY + img.modelHeight),
                        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                    )
                    bmp.recycle()
                }
            }

            // Strokes layer
            val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            for (swp in sourceStrokes) {
                val isHL = swp.stroke.isHighlighter
                strokePaint.color = swp.stroke.color
                strokePaint.strokeWidth = swp.stroke.strokeWidth * (if (isHL) 3f else 1f)
                strokePaint.alpha = if (isHL) (255 * 0.4f).toInt() else 255
                strokePaint.xfermode = if (isHL) android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY) else null

                if (swp.stroke.shapeType != null) {
                    val r = android.graphics.RectF(swp.stroke.boundsLeft, swp.stroke.boundsTop, swp.stroke.boundsRight, swp.stroke.boundsBottom)
                    when (swp.stroke.shapeType) {
                        "RECT"  -> fullCanvas.drawRect(r, strokePaint)
                        "OVAL"  -> fullCanvas.drawOval(r, strokePaint)
                        "LINE", "ARROW" -> if (swp.points.size >= 2) {
                            val p0 = swp.points.first(); val p1 = swp.points.last()
                            fullCanvas.drawLine(p0.x, p0.y, p1.x, p1.y, strokePaint)
                        }
                    }
                } else {
                    val pts = swp.points
                    if (pts.size >= 2) {
                        val path = android.graphics.Path()
                        path.moveTo(pts[0].x, pts[0].y)
                        if (pts.size < 3) {
                            for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
                        } else {
                            for (i in 1 until pts.size) {
                                val prev = pts[i - 1]; val curr = pts[i]
                                path.quadTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                            }
                            path.lineTo(pts.last().x, pts.last().y)
                        }
                        fullCanvas.drawPath(path, strokePaint)
                    }
                }
            }

            // Text / stamp layer
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
            }
            for (txt in sourceTextAnnotations) {
                textPaint.textSize = txt.fontSize
                textPaint.color = txt.colorArgb
                var y = txt.modelY + txt.fontSize
                txt.text.split("\n").forEach { line ->
                    fullCanvas.drawText(line, txt.modelX, y, textPaint)
                    y += txt.fontSize * 1.2f
                }
            }

            // ── Step 2: Crop lasso bounding box from full bitmap ──────────────────
            val cropPixX    = (minX * renderScale).toInt().coerceIn(0, fullW - 1)
            val cropPixY    = (minY * renderScale).toInt().coerceIn(0, fullH - 1)
            val cropPixW    = (cropW * renderScale).toInt().coerceAtLeast(1).coerceAtMost(fullW - cropPixX)
            val cropPixH    = (cropH * renderScale).toInt().coerceAtLeast(1).coerceAtMost(fullH - cropPixY)
            val croppedBitmap = android.graphics.Bitmap.createBitmap(fullBitmap, cropPixX, cropPixY, cropPixW, cropPixH)
            fullBitmap.recycle()

            // ── Step 3: Apply lasso polygon mask on the cropped bitmap ────────────
            val maskedBitmap = android.graphics.Bitmap.createBitmap(cropPixW, cropPixH, android.graphics.Bitmap.Config.ARGB_8888)
            val maskCanvas = android.graphics.Canvas(maskedBitmap)
            // Keep outside-lasso area transparent instead of white.
            maskCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            // Polygon in crop-local pixel coords (origin = minX, minY in model space)
            val polyPath = android.graphics.Path()
            polyPath.moveTo((polygon.first().x - minX) * renderScale, (polygon.first().y - minY) * renderScale)
            for (i in 1 until polygon.size) {
                polyPath.lineTo((polygon[i].x - minX) * renderScale, (polygon[i].y - minY) * renderScale)
            }
            polyPath.close()
            maskCanvas.clipPath(polyPath)
            maskCanvas.drawBitmap(croppedBitmap, 0f, 0f, null)
            croppedBitmap.recycle()

            // Trim transparent borders so placement/aspect matches the actually selected region.
            val trimmedBitmap = trimTransparentEdges(maskedBitmap)
            if (trimmedBitmap !== maskedBitmap) maskedBitmap.recycle()

            // ── Step 4: Save PNG ──────────────────────────────────────────────────
            val trimmedPixelW = trimmedBitmap.width.coerceAtLeast(1)
            val trimmedPixelH = trimmedBitmap.height.coerceAtLeast(1)
            val file = java.io.File(context.filesDir, "extracted_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { out ->
                trimmedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            trimmedBitmap.recycle()

            // ── Step 5: Place image on new page (preserve exact aspect ratio) ─────
            val finalSourceModelW = trimmedPixelW / renderScale
            val finalSourceModelH = trimmedPixelH / renderScale

            val pad    = 20f
            val availW = modelWidth - 2 * pad
            val availH = modelHeight - 2 * pad
            val placementScale = minOf(availW / finalSourceModelW, availH / finalSourceModelH)
            val finalW  = finalSourceModelW * placementScale
            val finalH  = finalSourceModelH * placementScale
            val shiftX  = pad + (availW - finalW) / 2f   // horizontally centred
            val shiftY  = pad                             // pinned to top of page

            val newImage = ImageAnnotationEntity(
                id          = java.util.UUID.randomUUID().toString(),
                documentUri = documentUri,
                pageIndex   = targetPageIndex,
                modelX      = shiftX,
                modelY      = shiftY,
                modelWidth  = finalW,
                modelHeight = finalH,
                uri         = android.net.Uri.fromFile(file).toString()
            )

            db.withTransaction { imageAnnotationDao.insert(newImage) }

            withContext(Dispatchers.Main) {
                clearSelection()
                onToolSelected(Tool.PEN)
            }
            }
        } finally {
            extractionMutex.unlock()
        }
    }

    /** Load a bitmap from content:// or file:// or raw file-path URI. */
    private fun loadBitmapFromUri(context: android.content.Context, uri: String): android.graphics.Bitmap? {
        return try {
            val parsed = android.net.Uri.parse(uri)
            if (parsed.scheme == "content") {
                context.contentResolver.openInputStream(parsed)?.use { android.graphics.BitmapFactory.decodeStream(it) }
            } else {
                // file:// or raw path
                val path = if (parsed.scheme == "file") parsed.path ?: uri else uri
                android.graphics.BitmapFactory.decodeFile(path)
            }
        } catch (_: Exception) { null }
    }

    /** Render a source PDF page directly from documentUri (file://) for extraction fallback. */
    private fun renderPdfPageFromDocumentUri(
        pageIndex: Int,
        outWidth: Int,
        outHeight: Int
    ): android.graphics.Bitmap? {
        return try {
            val parsed = android.net.Uri.parse(documentUri)
            if (parsed.scheme != "file") return null
            val path = parsed.path ?: return null
            val fd = android.os.ParcelFileDescriptor.open(
                java.io.File(path),
                android.os.ParcelFileDescriptor.MODE_READ_ONLY
            )
            try {
                val renderer = android.graphics.pdf.PdfRenderer(fd)
                try {
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    val bmp = android.graphics.Bitmap.createBitmap(
                        outWidth.coerceAtLeast(1),
                        outHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    val page = renderer.openPage(pageIndex)
                    try {
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    } finally {
                        page.close()
                    }
                    bmp
                } finally {
                    renderer.close()
                }
            } finally {
                fd.close()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Crop transparent margins from ARGB bitmap; returns same instance if no trimming is needed. */
    private fun trimTransparentEdges(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        // Scan row by row to minimise memory allocation (avoids 16MB array for full page)
        val rowPixels = IntArray(w)
        for (y in 0 until h) {
            src.getPixels(rowPixels, 0, w, 0, y, w, 1)
            var rowHasOpaque = false
            for (x in 0 until w) {
                if (android.graphics.Color.alpha(rowPixels[x]) > 0) {
                    rowHasOpaque = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                }
            }
            if (rowHasOpaque) {
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        if (maxX < minX || maxY < minY) return src

        val outW = (maxX - minX + 1).coerceAtLeast(1)
        val outH = (maxY - minY + 1).coerceAtLeast(1)
        if (outW == w && outH == h) return src
        return android.graphics.Bitmap.createBitmap(src, minX, minY, outW, outH)
    }

}
