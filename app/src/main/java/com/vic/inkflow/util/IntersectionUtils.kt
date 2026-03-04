package com.vic.inkflow.util

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidPath
import com.vic.inkflow.data.PointEntity
import com.vic.inkflow.data.StrokeWithPoints
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt

object IntersectionUtils {

    /**
     * Helper to add thickness to a path.
     */
    private fun Path.stroked(width: Float): Path {
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val outPath = Path()
        paint.getFillPath(this, outPath)
        return outPath
    }

    /**
     * Eraser: Two-phase collision detection using AABB broad phase + path intersection.
     */
    fun findIntersectingStrokes(eraserPath: Path, strokes: List<StrokeWithPoints>): List<StrokeWithPoints> {
        val intersectingStrokes = mutableListOf<StrokeWithPoints>()

        val strokedEraser = eraserPath.stroked(20f)
        val eraserBounds = RectF()
        strokedEraser.computeBounds(eraserBounds, true)

        for (strokeWithPoints in strokes) {
            val stroke = strokeWithPoints.stroke
            val strokeBounds = RectF(stroke.boundsLeft, stroke.boundsTop, stroke.boundsRight, stroke.boundsBottom)

            if (RectF.intersects(eraserBounds, strokeBounds)) {
                val strokePath = if (strokeWithPoints.stroke.shapeType != null)
                    strokeWithPoints.toShapePath()
                else {
                    val points = strokeWithPoints.points.map { StrokePoint(it.x, it.y, it.width) }
                    EnvelopeUtils.generateEnvelopePath(points).asAndroidPath()
                }
                val intersectionPath = Path()
                intersectionPath.op(strokePath, strokedEraser, Path.Op.INTERSECT)

                if (!intersectionPath.isEmpty) {
                    intersectingStrokes.add(strokeWithPoints)
                }
            }
        }
        return intersectingStrokes
    }

    /**
     * Lasso: Ray-casting (Even-Odd Rule) to find strokes whose centroid lies inside the polygon.
     * [polygon] is the ordered list of lasso vertices in model coordinates.
     */
    fun findStrokesInLasso(polygon: List<Offset>, strokes: List<StrokeWithPoints>): List<StrokeWithPoints> {
        if (polygon.size < 3) return emptyList()
        return strokes.filter { strokeWithPoints ->
            val points = strokeWithPoints.points
            if (points.isEmpty()) return@filter false
            val cx = points.sumOf { it.x.toDouble() } / points.size
            val cy = points.sumOf { it.y.toDouble() } / points.size
            isPointInPolygon(cx.toFloat(), cy.toFloat(), polygon)
        }
    }

    /**
     * Even-Odd Rule ray-casting: cast a ray in the +X direction from (px, py).
     * Returns true if the point is inside the polygon.
     */
    private fun isPointInPolygon(px: Float, py: Float, polygon: List<Offset>): Boolean {
        var inside = false
        val n = polygon.size
        var j = n - 1
        for (i in 0 until n) {
            val xi = polygon[i].x; val yi = polygon[i].y
            val xj = polygon[j].x; val yj = polygon[j].y
            val intersects = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * Build the actual geometric outline path for a shape stroke so the eraser
     * can intersect against it correctly.  For freehand strokes use [toPath] instead.
     */
    private fun StrokeWithPoints.toShapePath(): Path {
        val path = Path()
        val s = stroke
        val bounds = RectF(s.boundsLeft, s.boundsTop, s.boundsRight, s.boundsBottom)
        when (s.shapeType) {
            "RECT" -> path.addRect(bounds, Path.Direction.CW)
            "CIRCLE" -> path.addOval(bounds, Path.Direction.CW)
            "LINE" -> {
                val pts = points
                if (pts.size >= 2) {
                    buildThickSegment(
                        path,
                        pts.first().x, pts.first().y,
                        pts.last().x,  pts.last().y,
                        s.strokeWidth / 2f + 8f
                    )
                }
            }
            "ARROW" -> {
                val pts = points
                if (pts.size >= 2) {
                    val p0x = pts.first().x; val p0y = pts.first().y
                    val p1x = pts.last().x;  val p1y = pts.last().y
                    val hw = s.strokeWidth / 2f + 8f
                    // Main shaft
                    buildThickSegment(path, p0x, p0y, p1x, p1y, hw)
                    // Arrowhead wings
                    val headSize = s.strokeWidth * 5f + 10f
                    val angle = atan2((p1y - p0y).toDouble(), (p1x - p0x).toDouble())
                    val la = angle + Math.PI * 0.75
                    val ra = angle - Math.PI * 0.75
                    buildThickSegment(
                        path, p1x, p1y,
                        (p1x + headSize * cos(la)).toFloat(), (p1y + headSize * sin(la)).toFloat(),
                        hw
                    )
                    buildThickSegment(
                        path, p1x, p1y,
                        (p1x + headSize * cos(ra)).toFloat(), (p1y + headSize * sin(ra)).toFloat(),
                        hw
                    )
                }
            }
            else -> path.addRect(bounds, Path.Direction.CW)
        }
        return path
    }

    /**
     * Builds a filled rectangular polygon (thick line) around the segment from (x0,y0) to (x1,y1)
     * with the given half-width [hw].  The resulting path has area so Path.Op.INTERSECT works correctly.
     */
    private fun buildThickSegment(path: Path, x0: Float, y0: Float, x1: Float, y1: Float, hw: Float) {
        val dx = x1 - x0
        val dy = y1 - y0
        val len = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 0.001f) return
        val nx = -dy / len * hw
        val ny =  dx / len * hw
        path.moveTo(x0 + nx, y0 + ny)
        path.lineTo(x1 + nx, y1 + ny)
        path.lineTo(x1 - nx, y1 - ny)
        path.lineTo(x0 - nx, y0 - ny)
        path.close()
    }
}

