package com.vic.inkflow.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class StrokePoint(val x: Float, val y: Float, val width: Float)

object EnvelopeUtils {
    /**
     * Generates a filled polygon (Envelope) path from a list of stroke points with individual widths.
     * This mimics perfect-freehand style to render variable-width strokes efficiently in a single draw call.
     */
    fun generateEnvelopePath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        if (points.size == 1) {
            val p = points.first()
            val r = p.width / 2f
            path.addOval(Rect(p.x - r, p.y - r, p.x + r, p.y + r))
            return path
        }

        val leftPoints = mutableListOf<Offset>()
        val rightPoints = mutableListOf<Offset>()

        // 1. Calculate left and right offset points based on normals
        for (i in points.indices) {
            val curr = points[i]
            
            // To find the tangent, we look at the previous and next points that are sufficiently far apart
            var prevIndex = i - 1
            while (prevIndex >= 0 && hypot(curr.x - points[prevIndex].x, curr.y - points[prevIndex].y) < 1f) {
                prevIndex--
            }
            val prev = if (prevIndex >= 0) points[prevIndex] else curr

            var nextIndex = i + 1
            while (nextIndex < points.size && hypot(points[nextIndex].x - curr.x, points[nextIndex].y - curr.y) < 1f) {
                nextIndex++
            }
            val next = if (nextIndex < points.size) points[nextIndex] else curr
            
            var dx = next.x - prev.x
            var dy = next.y - prev.y
            
            // For endpoints, fallback to current to prev/next
            if (prev === curr && next !== curr) {
                dx = next.x - curr.x
                dy = next.y - curr.y
            } else if (next === curr && prev !== curr) {
                dx = curr.x - prev.x
                dy = curr.y - prev.y
            }

            val len = hypot(dx, dy)
            if (len > 0.01f) {
                dx /= len
                dy /= len
            } else {
                dx = 1f
                dy = 0f
            }

            // Normal vector (rotated 90 degrees)
            val nx = -dy
            val ny = dx

            val r = curr.width / 2f
            leftPoints.add(Offset(curr.x + nx * r, curr.y + ny * r))
            rightPoints.add(Offset(curr.x - nx * r, curr.y - ny * r))
            
            // 核心修復：在每一個繪圖點都加上一個圓形 (Oval)。
            // 1. 消除留白：這能完美彌補自交錯 (Self-intersection) 時因為 NonZero 演算法產生的中空透明問題。
            // 2. 消除稜角：不管 Envelope 的外緣因為貝茲曲線拉伸得多凌亂，圓形會確實將每一個轉角都補成豐滿的「純圓角」。
            path.addOval(Rect(curr.x - r, curr.y - r, curr.x + r, curr.y + r))
        }

        // 2. Begin Path at the first left point
        path.moveTo(leftPoints.first().x, leftPoints.first().y)

        // 3. Draw left edge using Beziers for smooth contours
        for (i in 1 until leftPoints.size) {
            val p1 = leftPoints[i - 1]
            val p2 = leftPoints[i]
            path.quadraticBezierTo(p1.x, p1.y, (p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        }
        path.lineTo(leftPoints.last().x, leftPoints.last().y)

        // 4. Cap the end with a semi-circle
        val endPt = points.last()
        val endR = endPt.width / 2f
        val endDx = leftPoints.last().x - endPt.x
        val endDy = leftPoints.last().y - endPt.y
        val endAngle = atan2(endDy, endDx)
        val capSteps = 12 // Increased cap steps for rounder tips
        for (i in 0..capSteps) { // Start at 0 to connect seamlessly
            // 使用「減去」角度 (-)，確保弧線是往筆畫「外部（前方）」畫，而不是往筆畫內部凹陷
            val a = endAngle - (Math.PI.toFloat() * i / capSteps)
            path.lineTo(endPt.x + cos(a) * endR, endPt.y + sin(a) * endR)
        }

        // 5. Draw right edge backwards
        path.lineTo(rightPoints.last().x, rightPoints.last().y)
        for (i in rightPoints.indices.reversed().drop(1)) {
            val p1 = rightPoints[i + 1]
            val p2 = rightPoints[i]
            path.quadraticBezierTo(p1.x, p1.y, (p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        }
        path.lineTo(rightPoints.first().x, rightPoints.first().y)

        // 6. Cap the start with a semi-circle
        val startPt = points.first()
        val startR = startPt.width / 2f
        val startDx = rightPoints.first().x - startPt.x
        val startDy = rightPoints.first().y - startPt.y
        val startAngle = atan2(startDy, startDx)
        for (i in 0..capSteps) { // Start at 0 to connect seamlessly
            // 使用「減去」角度 (-)，確保弧線是往筆畫「外部（後方）」畫，而不是往筆畫內部凹陷
            val a = startAngle - (Math.PI.toFloat() * i / capSteps)
            path.lineTo(startPt.x + cos(a) * startR, startPt.y + sin(a) * startR)
        }

        path.close()
        return path
    }
}