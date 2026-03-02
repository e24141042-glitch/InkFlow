package com.vic.inkflow.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.ExperimentalComposeUiApi
import com.vic.inkflow.data.ImageAnnotationEntity
import com.vic.inkflow.data.PointEntity
import com.vic.inkflow.data.StrokeEntity
import com.vic.inkflow.data.StrokeWithPoints
import com.vic.inkflow.data.TextAnnotationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun InkCanvas(
    modifier: Modifier,
    viewModel: EditorViewModel
) {
    val committedStrokes by viewModel.currentStrokes.collectAsState()
    val selectedStrokes by viewModel.selectedStrokes.collectAsState()
    val lassoMoveOffset by viewModel.lassoMoveOffset.collectAsState()
    val activeTool by viewModel.selectedTool.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val selectedShapeSubType by viewModel.selectedShapeSubType.collectAsState()
    val textAnnotations by viewModel.currentTextAnnotations.collectAsState()
    val imageAnnotations by viewModel.currentImageAnnotations.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Active path for freehand/lasso/eraser
    val activePath = remember { Path() }
    var activePathVersion by remember { mutableIntStateOf(0) }
    val currentPathPoints = remember { mutableStateListOf<Offset>() }

    // Shape live-preview anchors (canvas pixel space)
    var activeShapeStart by remember { mutableStateOf<Offset?>(null) }
    var activeShapeEnd   by remember { mutableStateOf<Offset?>(null) }

    // Text / Stamp placement dialog
    var pendingTextPosition by remember { mutableStateOf<Offset?>(null) }
    var pendingIsStamp      by remember { mutableStateOf(false) }
    var textInputValue      by remember { mutableStateOf("") }

    // Canvas pixel size (updated via onSizeChanged, used for hit-testing in pointer input)
    var canvasPixelSize by remember { mutableStateOf(Size.Zero) }
    val canvasPixelSizeState = rememberUpdatedState(canvasPixelSize)

    // Text annotation interactive selection / move / resize state
    var selectedTextAnnotationId by remember { mutableStateOf<String?>(null) }
    var textMoveDelta by remember { mutableStateOf(Offset.Zero) }
    var textFontSizeDelta by remember { mutableFloatStateOf(0f) }

    // Latest snapshot of text annotations for use inside pointer-input coroutines
    val textAnnotationsRef = rememberUpdatedState(textAnnotations)
    val selectedTextIdRef  = rememberUpdatedState(selectedTextAnnotationId)

    // Image annotation interactive selection / move / resize state
    var selectedImageAnnotationId by remember { mutableStateOf<String?>(null) }
    var imageMovePreview   by remember { mutableStateOf(Offset.Zero) }
    var imageResizePreview by remember { mutableStateOf(Offset.Zero) }

    // Latest snapshot of image annotations for use inside pointer-input coroutines
    val imageAnnotationsRef    = rememberUpdatedState(imageAnnotations)
    val selectedImageIdRef     = rememberUpdatedState(selectedImageAnnotationId)

    // Image bitmap cache: uri-string → decoded Bitmap (null = load failed / placeholder)
    val loadedImages = remember { mutableStateMapOf<String, android.graphics.Bitmap?>() }

    // Image picker launcher — gallery opens on empty-canvas tap (IMAGE tool)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            val newId = viewModel.placeImageAnnotation(uri.toString())
            // Auto-select the freshly placed image for immediate move/resize
            selectedImageAnnotationId = newId
            imageMovePreview = Offset.Zero
            imageResizePreview = Offset.Zero
        }
        // Stay in IMAGE tool so the user can immediately adjust the placed image
    }

    // Open gallery whenever IMAGE tool is activated; clear selections on tool change
    LaunchedEffect(activeTool) {
        if (activeTool != Tool.TEXT) {
            selectedTextAnnotationId = null
            textMoveDelta = Offset.Zero
            textFontSizeDelta = 0f
        }
        if (activeTool != Tool.IMAGE) {
            selectedImageAnnotationId = null
            imageMovePreview = Offset.Zero
            imageResizePreview = Offset.Zero
        }
    }

    // Async bitmap loader — runs whenever annotation list changes
    LaunchedEffect(imageAnnotations) {
        imageAnnotations.forEach { ann ->
            if (ann.uri !in loadedImages) {
                loadedImages[ann.uri] = null
                scope.launch(Dispatchers.IO) {
                    val bmp = try {
                        context.contentResolver.openInputStream(Uri.parse(ann.uri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (_: Exception) { null }
                    withContext(Dispatchers.Main) { loadedImages[ann.uri] = bmp }
                }
            }
        }
    }

    // ---- Dialogs ----

    val pendingPos = pendingTextPosition
    if (pendingPos != null && !pendingIsStamp) {
        AlertDialog(
            onDismissRequest = { pendingTextPosition = null; textInputValue = "" },
            title = { Text("新增文字") },
            text = {
                OutlinedTextField(
                    value = textInputValue,
                    onValueChange = { textInputValue = it },
                    label = { Text("輸入文字") },
                    singleLine = false,
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInputValue.isNotBlank()) {
                        viewModel.addTextAnnotation(
                            text  = textInputValue,
                            canvasX = pendingPos.x,
                            canvasY = pendingPos.y,
                            fontSize = 14f,
                            color = selectedColor
                        )
                    }
                    textInputValue = ""
                    pendingTextPosition = null
                }) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTextPosition = null; textInputValue = "" }) {
                    Text("取消")
                }
            }
        )
    }

    if (pendingPos != null && pendingIsStamp) {
        val stamps = listOf("✓", "✗", "⭐", "❤", "★", "!", "?", "→")
        AlertDialog(
            onDismissRequest = { pendingTextPosition = null; pendingIsStamp = false },
            title = { Text("選擇印章") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stamps.forEach { stamp ->
                        TextButton(onClick = {
                            viewModel.addTextAnnotation(
                                text   = stamp,
                                canvasX = pendingPos.x,
                                canvasY = pendingPos.y,
                                fontSize = 48f,
                                color  = selectedColor,
                                isStamp = true
                            )
                            pendingTextPosition = null
                            pendingIsStamp = false
                        }) { Text(stamp, fontSize = 24.sp) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingTextPosition = null; pendingIsStamp = false }) {
                    Text("取消")
                }
            }
        )
    }

    // ---- Modifier chain ----

    val drawModifier = modifier
        .onSizeChanged { size ->
            canvasPixelSize = Size(size.width.toFloat(), size.height.toFloat())
            viewModel.setCanvasSize(size.width.toFloat(), size.height.toFloat())
        }
        .pointerInput(activeTool) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val firstEvent = awaitPointerEvent()
                if (firstEvent.changes.size > 1) return@awaitEachGesture

                val startOffset = down.position
                val isMoveMode = activeTool == Tool.LASSO && selectedStrokes.isNotEmpty()
                if (activeTool == Tool.LASSO && !isMoveMode) viewModel.clearSelection()

                // Text tool: selection, move, resize, or new text placement
                if (activeTool == Tool.TEXT) {
                    val cs = canvasPixelSizeState.value
                    val sx = if (cs.width > 0f) cs.width / EditorViewModel.MODEL_W else 1f
                    val sy = if (cs.height > 0f) cs.height / EditorViewModel.MODEL_H else 1f
                    val annotations = textAnnotationsRef.value
                    val selId = selectedTextIdRef.value
                    val selAnn = if (selId != null) annotations.firstOrNull { it.id == selId } else null

                    if (selAnn != null) {
                        val currentTextRect = textAnnotationHitRect(selAnn, sx, sy).translate(textMoveDelta)
                        val handleRect = textResizeHandleRect(currentTextRect)

                        // Resize handle hit
                        if (handleRect.contains(startOffset)) {
                            down.consume()
                            var accModelDelta = 0f
                            var drag = awaitDragOrCancellation(down.id)
                            while (drag != null && drag.pressed) {
                                // Standard resize UX: dragging toward bottom-right = bigger
                                val change = drag.positionChange()
                                val diagonal = (change.x + change.y) / 2f
                                accModelDelta += diagonal * EditorViewModel.MODEL_W / cs.width.coerceAtLeast(1f)
                                textFontSizeDelta = accModelDelta
                                activePathVersion++
                                drag.consume()
                                drag = awaitDragOrCancellation(drag.id)
                            }
                            viewModel.commitTextAnnotationResize(selAnn.id, selAnn.fontSize + accModelDelta)
                            textFontSizeDelta = 0f
                            activePathVersion++
                            return@awaitEachGesture
                        }

                        // Move: drag inside the selected text box
                        if (currentTextRect.contains(startOffset)) {
                            down.consume()
                            var totalDelta = Offset.Zero
                            var drag = awaitDragOrCancellation(down.id)
                            while (drag != null && drag.pressed) {
                                val delta = drag.positionChange()
                                totalDelta += delta
                                textMoveDelta += delta
                                activePathVersion++
                                drag.consume()
                                drag = awaitDragOrCancellation(drag.id)
                            }
                            viewModel.commitTextAnnotationMove(selAnn.id, totalDelta.x, totalDelta.y)
                            textMoveDelta = Offset.Zero
                            activePathVersion++
                            return@awaitEachGesture
                        }
                    }

                    // Tap on any annotation to select it
                    val hitAnn = annotations.firstOrNull { ann ->
                        textAnnotationHitRect(ann, sx, sy).contains(startOffset)
                    }
                    if (hitAnn != null) {
                        selectedTextAnnotationId = hitAnn.id
                        textMoveDelta = Offset.Zero
                        textFontSizeDelta = 0f
                        down.consume()
                        activePathVersion++
                        return@awaitEachGesture
                    }

                    // Tap on empty space: deselect and open new-text dialog
                    selectedTextAnnotationId = null
                    textMoveDelta = Offset.Zero
                    pendingIsStamp = false
                    pendingTextPosition = startOffset
                    down.consume()
                    return@awaitEachGesture
                }

                // Stamp: single tap to place
                if (activeTool == Tool.STAMP) {
                    down.consume()
                    pendingIsStamp = true
                    pendingTextPosition = startOffset
                    return@awaitEachGesture
                }

                // Image tool: selection, move, resize, or tap-to-pick
                if (activeTool == Tool.IMAGE) {
                    val cs = canvasPixelSizeState.value
                    val sx = if (cs.width > 0f) cs.width / EditorViewModel.MODEL_W else 1f
                    val sy = if (cs.height > 0f) cs.height / EditorViewModel.MODEL_H else 1f
                    val annotations = imageAnnotationsRef.value
                    val selId = selectedImageIdRef.value
                    val selAnn = if (selId != null) annotations.firstOrNull { it.id == selId } else null

                    if (selAnn != null) {
                        // Effective image rect in canvas space (committed position + in-flight move)
                        val imgRect = imageAnnotationRect(selAnn, sx, sy).let { r ->
                            Rect(r.left + imageMovePreview.x, r.top + imageMovePreview.y,
                                 r.right + imageMovePreview.x, r.bottom + imageMovePreview.y)
                        }
                        // Effective bottom-right corner (includes any in-flight resize)
                        val effectiveBR = Offset(
                            imgRect.right  + imageResizePreview.x,
                            imgRect.bottom + imageResizePreview.y
                        )
                        val hrImg = imageResizeHandleRect(effectiveBR)

                        // — Resize: tap on the bottom-right handle —
                        if (hrImg.contains(startOffset)) {
                            down.consume()
                            var drag = awaitDragOrCancellation(down.id)
                            while (drag != null && drag.pressed) {
                                // Direct mapping: handle follows the finger
                                imageResizePreview += drag.positionChange()
                                activePathVersion++
                                drag.consume()
                                drag = awaitDragOrCancellation(drag.id)
                            }
                            // Commit: convert effective canvas size back to model space
                            val newModelW = ((selAnn.modelWidth  * sx + imageResizePreview.x).coerceAtLeast(30f)) / sx
                            val newModelH = ((selAnn.modelHeight * sy + imageResizePreview.y).coerceAtLeast(30f)) / sy
                            viewModel.commitImageAnnotationResize(selAnn.id, newModelW, newModelH)
                            imageResizePreview = Offset.Zero
                            activePathVersion++
                            return@awaitEachGesture
                        }

                        // — Move: tap inside the image body —
                        if (imgRect.contains(startOffset)) {
                            down.consume()
                            var totalDelta = Offset.Zero
                            var drag = awaitDragOrCancellation(down.id)
                            while (drag != null && drag.pressed) {
                                val delta = drag.positionChange()
                                totalDelta += delta
                                imageMovePreview += delta
                                activePathVersion++
                                drag.consume()
                                drag = awaitDragOrCancellation(drag.id)
                            }
                            viewModel.commitImageAnnotationMove(selAnn.id, totalDelta.x, totalDelta.y)
                            imageMovePreview = Offset.Zero
                            activePathVersion++
                            return@awaitEachGesture
                        }
                    }

                    // Tap on another image to select it
                    val hitAnn = annotations.firstOrNull { ann ->
                        imageAnnotationRect(ann, sx, sy).contains(startOffset)
                    }
                    if (hitAnn != null) {
                        selectedImageAnnotationId = hitAnn.id
                        imageMovePreview = Offset.Zero
                        imageResizePreview = Offset.Zero
                        down.consume()
                        activePathVersion++
                        return@awaitEachGesture
                    }

                    // Tap on empty canvas: open gallery picker
                    down.consume()
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    return@awaitEachGesture
                }

                if (isMoveMode) {
                    down.consume()
                    var drag = awaitDragOrCancellation(down.id)
                    while (drag != null && drag.pressed) {
                        val delta = drag.positionChange()
                        if (delta != Offset.Zero) viewModel.moveSelectedStrokes(delta)
                        drag.consume()
                        drag = awaitDragOrCancellation(drag.id)
                    }
                    viewModel.commitMovedStrokes()
                    return@awaitEachGesture
                }

                // Shape tool: drag to define bounding box
                if (activeTool == Tool.SHAPE) {
                    activeShapeStart = startOffset
                    activeShapeEnd   = startOffset
                    activePathVersion++
                    down.consume()
                    var drag = awaitDragOrCancellation(down.id)
                    while (drag != null && drag.pressed) {
                        activeShapeEnd = drag.position
                        activePathVersion++
                        drag.consume()
                        drag = awaitDragOrCancellation(drag.id)
                    }
                    val end = activeShapeEnd
                    if (end != null) {
                        viewModel.saveShape(startOffset, end, selectedColor, strokeWidth)
                    }
                    activeShapeStart = null
                    activeShapeEnd   = null
                    activePathVersion++
                    return@awaitEachGesture
                }

                // Freehand / Lasso / Eraser
                activePath.reset()
                activePath.moveTo(startOffset.x, startOffset.y)
                currentPathPoints.clear()
                currentPathPoints.add(startOffset)
                activePathVersion++
                down.consume()

                var drag = awaitDragOrCancellation(down.id)
                while (drag != null && drag.pressed) {
                    drag.historical.forEach { historical ->
                        val hp   = historical.position
                        val prev = currentPathPoints.last()
                        activePath.quadraticBezierTo(prev.x, prev.y, (prev.x + hp.x) / 2f, (prev.y + hp.y) / 2f)
                        currentPathPoints.add(hp)
                    }
                    val newPoint  = drag.position
                    val prevPoint = currentPathPoints.last()
                    activePath.quadraticBezierTo(prevPoint.x, prevPoint.y, (prevPoint.x + newPoint.x) / 2f, (prevPoint.y + newPoint.y) / 2f)
                    currentPathPoints.add(newPoint)
                    activePathVersion++
                    drag.consume()

                    if (activeTool == Tool.ERASER) viewModel.deleteStrokesIntersecting(activePath)
                    drag = awaitDragOrCancellation(drag.id)
                }

                when (activeTool) {
                    Tool.PEN, Tool.HIGHLIGHTER -> {
                        // A single-point tap produces no drag points; duplicate it so
                        // saveStroke receives ≥2 points and StrokeCap.Round renders a dot.
                        val pts = if (currentPathPoints.size == 1)
                            listOf(currentPathPoints[0], currentPathPoints[0])
                        else
                            currentPathPoints.toList()
                        viewModel.saveStroke(pts, selectedColor, strokeWidth, activeTool)
                    }
                    Tool.LASSO -> {
                        activePath.close()
                        viewModel.selectStrokesInLasso(currentPathPoints.toList())
                    }
                    else -> { }
                }
                activePath.reset()
                activePathVersion++
                currentPathPoints.clear()
            }
        }
        .drawWithCache {
            val sx = size.width  / EditorViewModel.MODEL_W
            val sy = size.height / EditorViewModel.MODEL_H

            val selectedIds = selectedStrokes.map { it.stroke.id }.toSet()
            val cachedBitmap = ImageBitmap(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            val canvas = androidx.compose.ui.graphics.Canvas(cachedBitmap)

            canvas.save()
            canvas.scale(sx, sy)
            committedStrokes.forEach { swp ->
                if (swp.stroke.id !in selectedIds) {
                    if (swp.stroke.shapeType != null) {
                        drawShapeOnCanvas(canvas, swp.stroke, swp.points)
                    } else {
                        drawPathOnCanvas(canvas, swp.points.toComposePath(),
                            Color(swp.stroke.color), swp.stroke.strokeWidth, swp.stroke.isHighlighter)
                    }
                }
            }
            canvas.restore()

            val selectedPathData = selectedStrokes.map { swp ->
                Pair(swp, swp.points.toComposePath())
            }

            onDrawBehind {
                // Image annotations — drawn BELOW strokes (above PDF layer which is a separate Composable)
                imageAnnotations.forEach { ann ->
                    val bmp = loadedImages[ann.uri]
                    if (bmp != null) {
                        val isSelected = ann.id == selectedImageAnnotationId && activeTool == Tool.IMAGE
                        val dx = if (isSelected) imageMovePreview.x  else 0f
                        val dy = if (isSelected) imageMovePreview.y  else 0f
                        val dw = if (isSelected) imageResizePreview.x else 0f
                        val dh = if (isSelected) imageResizePreview.y else 0f
                        val canvasX = ann.modelX * sx + dx
                        val canvasY = ann.modelY * sy + dy
                        val canvasW = (ann.modelWidth  * sx + dw).coerceAtLeast(2f)
                        val canvasH = (ann.modelHeight * sy + dh).coerceAtLeast(2f)
                        drawImage(
                            image     = bmp.asImageBitmap(),
                            dstOffset = IntOffset(canvasX.toInt(), canvasY.toInt()),
                            dstSize   = IntSize(canvasW.toInt(), canvasH.toInt())
                        )
                    }
                }

                drawImage(cachedBitmap)

                // Selected strokes (highlighted, shifted by lasso delta)
                if (selectedPathData.isNotEmpty()) {
                    val delta = lassoMoveOffset
                    drawIntoCanvas { cvs ->
                        cvs.save()
                        cvs.scale(sx, sy)
                        // The canvas is translated by delta (model-space units).
                        // All drawing code uses original model coordinates — the translate
                        // handles the visual offset for both freehand paths and shapes,
                        // avoiding any double-delta issue.
                        cvs.translate(delta.x, delta.y)
                        selectedPathData.forEach { (swp, path) ->
                            val stroke = swp.stroke
                            if (stroke.shapeType != null) {
                                // Pass the original stroke bounds and actual points — the canvas
                                // translate already accounts for the delta offset.
                                drawShapeOnCanvas(cvs, stroke, swp.points, tintColor = Color(0xFF6366F1))
                            } else {
                                drawPathOnCanvas(cvs, path, Color(0xFF6366F1), stroke.strokeWidth, stroke.isHighlighter)
                            }
                        }
                        cvs.restore()
                    }
                }

                // Selection handles for the currently selected image
                val selImgId  = selectedImageAnnotationId
                val selImgAnn = if (selImgId != null && activeTool == Tool.IMAGE)
                    imageAnnotations.firstOrNull { it.id == selImgId } else null
                if (selImgAnn != null) {
                    val r = imageAnnotationRect(selImgAnn, sx, sy)
                    val imgSelRect = Rect(
                        r.left  + imageMovePreview.x,
                        r.top   + imageMovePreview.y,
                        r.right  + imageMovePreview.x + imageResizePreview.x,
                        r.bottom + imageMovePreview.y + imageResizePreview.y
                    )
                    // Dashed border
                    drawRect(
                        color   = Color(0xFF6366F1),
                        topLeft = Offset(imgSelRect.left, imgSelRect.top),
                        size    = Size(imgSelRect.width, imgSelRect.height),
                        style   = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                    )
                    // Resize handle at bottom-right
                    val hr = imageResizeHandleRect(Offset(imgSelRect.right, imgSelRect.bottom))
                    drawRect(
                        color   = Color(0xFF6366F1),
                        topLeft = Offset(hr.left, hr.top),
                        size    = Size(hr.width, hr.height)
                    )
                    // Diagonal arrow inside handle
                    drawLine(
                        color       = Color.White,
                        start       = Offset(hr.left + 5f, hr.bottom - 5f),
                        end         = Offset(hr.right - 5f, hr.top + 5f),
                        strokeWidth = 2f,
                        cap         = StrokeCap.Round
                    )
                }

                // Text annotations — apply move/resize delta for the selected annotation
                drawIntoCanvas { composeCanvas ->
                    textAnnotations.forEach { ann ->
                        val isSelected = ann.id == selectedTextAnnotationId
                        val dx = if (isSelected) textMoveDelta.x else 0f
                        val dy = if (isSelected) textMoveDelta.y else 0f
                        val effectiveFontSize = if (isSelected)
                            (ann.fontSize + textFontSizeDelta).coerceAtLeast(4f) * sy
                        else ann.fontSize * sy
                        val paint = android.graphics.Paint().apply {
                            textSize    = effectiveFontSize
                            color       = ann.colorArgb
                            isAntiAlias = true
                            typeface    = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        composeCanvas.nativeCanvas.drawText(
                            ann.text, ann.modelX * sx + dx, ann.modelY * sy + dy, paint
                        )
                    }
                }

                // Selection handles for the currently selected text annotation
                val selTextId  = selectedTextAnnotationId
                val selTextAnn = if (selTextId != null && activeTool == Tool.TEXT)
                    textAnnotations.firstOrNull { it.id == selTextId } else null
                if (selTextAnn != null) {
                    val fsDelta   = textFontSizeDelta
                    val textRect  = textAnnotationHitRect(selTextAnn, sx, sy, fsDelta).translate(textMoveDelta)
                    // Dashed selection border
                    drawRect(
                        color    = Color(0xFF6366F1),
                        topLeft  = Offset(textRect.left, textRect.top),
                        size     = Size(textRect.width, textRect.height),
                        style    = Stroke(
                            width      = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
                        )
                    )
                    // Solid resize handle at bottom-right corner
                    val hr = textResizeHandleRect(textRect)
                    drawRect(
                        color   = Color(0xFF6366F1),
                        topLeft = Offset(hr.left, hr.top),
                        size    = Size(hr.width, hr.height)
                    )
                    // Diagonal resize arrow indicator inside handle
                    drawLine(
                        color       = Color.White,
                        start       = Offset(hr.left + 4f, hr.bottom - 4f),
                        end         = Offset(hr.right - 4f, hr.top + 4f),
                        strokeWidth = 1.5f,
                        cap         = StrokeCap.Round
                    )
                }

                // Shape live-preview (canvas-pixel space — no model→canvas scaling needed here)
                val _v         = activePathVersion   // subscribe to version changes
                val shapeStart = activeShapeStart
                val shapeEnd   = activeShapeEnd
                if (activeTool == Tool.SHAPE && shapeStart != null && shapeEnd != null) {
                    val left   = minOf(shapeStart.x, shapeEnd.x)
                    val top    = minOf(shapeStart.y, shapeEnd.y)
                    val right  = maxOf(shapeStart.x, shapeEnd.x)
                    val bottom = maxOf(shapeStart.y, shapeEnd.y)
                    val previewStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    when (selectedShapeSubType) {
                        ShapeSubType.RECT ->
                            drawRect(selectedColor, topLeft = Offset(left, top), size = Size(right - left, bottom - top), style = previewStyle)
                        ShapeSubType.CIRCLE ->
                            drawOval(selectedColor, topLeft = Offset(left, top), size = Size(right - left, bottom - top), style = previewStyle)
                        ShapeSubType.LINE -> drawLine(selectedColor, shapeStart, shapeEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                        ShapeSubType.ARROW -> {
                            drawLine(selectedColor, shapeStart, shapeEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                            drawArrowHeadInScope(shapeStart, shapeEnd, selectedColor, strokeWidth)
                        }
                    }
                }

                // Active freehand / eraser / lasso path
                when (activeTool) {
                    Tool.PEN -> drawPath(activePath, selectedColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    Tool.HIGHLIGHTER -> drawIntoCanvas { cvs ->
                        cvs.drawPath(activePath, Paint().apply {
                            color = selectedColor.copy(alpha = 0.4f)
                            style = PaintingStyle.Stroke
                            this.strokeWidth = strokeWidth * 3f
                            strokeCap = StrokeCap.Round
                            strokeJoin = StrokeJoin.Round
                            blendMode = BlendMode.Multiply
                        })
                    }
                    Tool.LASSO  -> drawPath(activePath, Color.DarkGray, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                    Tool.ERASER -> drawPath(activePath, Color.Gray,     style = Stroke(width = 2f))
                    else -> { }
                }
            }
        }

    Canvas(modifier = drawModifier) { }
}

// ---- Text annotation hit-testing helpers ----

/**
 * Returns the bounding rect of [ann] in canvas-pixel space.
 * [fontSizeDelta] is an in-flight resize delta in model units (applied during drag).
 */
private fun textAnnotationHitRect(
    ann: TextAnnotationEntity, sx: Float, sy: Float, fontSizeDelta: Float = 0f
): Rect {
    val canvasX         = ann.modelX * sx
    val canvasY         = ann.modelY * sy
    val effectiveFontPx = (ann.fontSize + fontSizeDelta).coerceAtLeast(4f) * sy
    // Approximate text width; drawText baseline is at (canvasX, canvasY)
    val textWidth       = ann.text.length * effectiveFontPx * 0.65f + 8f
    return Rect(canvasX - 4f, canvasY - effectiveFontPx - 4f, canvasX + textWidth, canvasY + 4f)
}

/** Returns the 22×22 px resize handle rect anchored to the bottom-right of [textRect]. */
private fun textResizeHandleRect(textRect: Rect): Rect {
    val h = 22f
    return Rect(textRect.right - h / 2f, textRect.bottom - h / 2f, textRect.right + h / 2f, textRect.bottom + h / 2f)
}

// ---- Image annotation hit-testing helpers ----

/** Returns the canvas-pixel bounding rect for [ann] (committed DB values, no in-flight delta). */
private fun imageAnnotationRect(ann: ImageAnnotationEntity, sx: Float, sy: Float): Rect =
    Rect(ann.modelX * sx, ann.modelY * sy,
         (ann.modelX + ann.modelWidth) * sx, (ann.modelY + ann.modelHeight) * sy)

/** Returns the 28×28 px resize handle rect centered on [bottomRight]. */
private fun imageResizeHandleRect(bottomRight: Offset): Rect {
    val h = 28f
    return Rect(bottomRight.x - h / 2f, bottomRight.y - h / 2f,
                bottomRight.x + h / 2f, bottomRight.y + h / 2f)
}

// ---- Shape rendering helpers ----

private fun drawShapeOnCanvas(
    canvas: androidx.compose.ui.graphics.Canvas,
    stroke: StrokeEntity,
    points: List<PointEntity>,
    tintColor: Color? = null
) {
    val color = tintColor ?: Color(stroke.color)
    val paint = Paint().apply {
        this.color   = color
        style        = PaintingStyle.Stroke
        strokeWidth  = stroke.strokeWidth
        strokeCap    = StrokeCap.Round
        strokeJoin   = StrokeJoin.Round
    }
    val r = Rect(stroke.boundsLeft, stroke.boundsTop, stroke.boundsRight, stroke.boundsBottom)
    when (stroke.shapeType) {
        "RECT"   -> canvas.drawRect(r, paint)
        "CIRCLE" -> canvas.drawOval(r, paint)
        "LINE"   -> if (points.size >= 2) {
            canvas.drawLine(Offset(points.first().x, points.first().y), Offset(points.last().x, points.last().y), paint)
        }
        "ARROW"  -> if (points.size >= 2) {
            val p0 = Offset(points.first().x, points.first().y)
            val p1 = Offset(points.last().x,  points.last().y)
            canvas.drawLine(p0, p1, paint)
            drawArrowHeadOnCanvas(canvas, p0, p1, paint, stroke.strokeWidth)
        }
    }
}

private fun drawArrowHeadOnCanvas(
    canvas: androidx.compose.ui.graphics.Canvas,
    start: Offset, end: Offset, paint: Paint, sw: Float
) {
    val headSize   = sw * 5f + 10f
    val angle      = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val leftAngle  = angle + Math.PI * 0.75
    val rightAngle = angle - Math.PI * 0.75
    val lp = Offset(end.x + (headSize * cos(leftAngle)).toFloat(),  end.y + (headSize * sin(leftAngle)).toFloat())
    val rp = Offset(end.x + (headSize * cos(rightAngle)).toFloat(), end.y + (headSize * sin(rightAngle)).toFloat())
    canvas.drawLine(end, lp, paint)
    canvas.drawLine(end, rp, paint)
}

private fun DrawScope.drawArrowHeadInScope(start: Offset, end: Offset, color: Color, sw: Float) {
    val headSize   = sw * 5f + 10f
    val angle      = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val leftAngle  = angle + Math.PI * 0.75
    val rightAngle = angle - Math.PI * 0.75
    val lp = Offset(end.x + (headSize * cos(leftAngle)).toFloat(),  end.y + (headSize * sin(leftAngle)).toFloat())
    val rp = Offset(end.x + (headSize * cos(rightAngle)).toFloat(), end.y + (headSize * sin(rightAngle)).toFloat())
    drawLine(color, end, lp, strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color, end, rp, strokeWidth = sw, cap = StrokeCap.Round)
}

// ---- Freehand path helpers ----

private fun List<PointEntity>.toComposePath(): Path {
    val path = Path()
    if (this.size < 2) return path
    path.moveTo(this.first().x, this.first().y)
    for (i in 1 until this.size) {
        val p1 = this[i - 1]; val p2 = this[i]
        path.quadraticBezierTo(p1.x, p1.y, (p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
    }
    this.lastOrNull()?.let { path.lineTo(it.x, it.y) }
    return path
}

private fun drawPathOnCanvas(
    canvas: androidx.compose.ui.graphics.Canvas,
    path: Path, color: Color, strokeWidth: Float, isHighlighter: Boolean = false
) {
    canvas.drawPath(path, Paint().apply {
        this.color       = if (isHighlighter) color.copy(alpha = 0.4f) else color
        this.style       = PaintingStyle.Stroke
        this.strokeWidth = if (isHighlighter) strokeWidth * 3f else strokeWidth
        this.strokeCap   = StrokeCap.Round
        this.strokeJoin  = StrokeJoin.Round
        if (isHighlighter) this.blendMode = BlendMode.Multiply
    })
}

