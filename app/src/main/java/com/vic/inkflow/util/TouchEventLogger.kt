package com.vic.inkflow.util

import android.util.Log
import com.vic.inkflow.BuildConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 * Debug-only logger for raw touch metrics.
 *
 * Purpose: collect real-device data to tune [PalmRejectionFilter] thresholds.
 * All methods are no-ops in release builds (BuildConfig.DEBUG == false).
 *
 * ## How to use
 *
 * 1. Run the app in debug mode on the target tablet.
 * 2. Filter Logcat by tag: `PALM_LOG`
 * 3. Perform a mix of:
 *    - Normal fingertip drawing strokes  → look for `outcome=ACCEPTED`
 *    - Deliberate palm rests / slides    → look for `outcome=REJECTED_*`
 * 4. Compare DOWN line metrics across both groups:
 *    - `touchMajor`  (px) — THE key discriminator: fingertip ≈ 20–40 px, palm ≈ 80–300 px
 *    - `sizeW`       (px) — Compose-level contact width (should correlate with touchMajor)
 *    - `pressure`         — secondary; palms often show > 0.7–0.9 on many devices
 *    - `pointers`         — palm contact often spawns multiple pointer IDs
 *
 * ## Log format (tab-separated key=value for easy spreadsheet import)
 *
 * ```
 * PALM_LOG  DOWN | id=12 | mode=PALM_REJECTION | rawTool=FINGER | pressure=0.421
 *               | sizeW=28.0 sizeH=24.5 | touchMajor=31.2 touchMinor=26.8 | toolMajor=35.0
 *               | pointers=1 | pos=(412,601)
 * PALM_LOG  END  | id=12 | outcome=ACCEPTED | points=87 | maxPressure=0.531
 *               | maxSizeW=30.0 maxSizeH=27.0
 * ```
 */
object TouchEventLogger {

    private const val TAG = "PALM_LOG"

    /** Monotonically increasing session counter — wraps at Int.MAX_VALUE but that's fine. */
    private val counter = AtomicInteger(0)

    /** Allocate a unique ID for each new gesture. Call once at the start of awaitEachGesture. */
    fun newSession(): Int = counter.incrementAndGet()

    /**
     * Log metrics captured from the first DOWN + awaitPointerEvent() frame.
     *
     * @param sessionId       From [newSession].
     * @param mode            Current InputMode name (FREE / PALM_REJECTION / STYLUS_ONLY).
     * @param composeToolType PointerInputChange.type.toString() — Compose's classification.
     * @param pressure        PointerInputChange.pressure (0.0–1.0+).
     * @param sizeWidthPx     PointerInputChange.size.width — Compose contact width in px.
     * @param sizeHeightPx    PointerInputChange.size.height — Compose contact height in px.
     * @param touchMajorPx    MotionEvent.getTouchMajor(0) — major axis of touch ellipse in px.
     *                        Most reliable contact-size measurement; 0 if nativeEvent unavailable.
     * @param touchMinorPx    MotionEvent.getTouchMinor(0) — minor axis in px.
     * @param toolMajorPx     MotionEvent.getToolMajor(0) — reported tool size in px.
     * @param nativeToolType  MotionEvent.getToolType(0) mapped to readable name.
     * @param pointerCount    MotionEvent.pointerCount — number of simultaneous contacts.
     * @param posX            Initial touch X in canvas pixel space.
     * @param posY            Initial touch Y in canvas pixel space.
     */
    fun logDown(
        sessionId: Int,
        mode: String,
        composeToolType: String,
        pressure: Float,
        sizeWidthPx: Float,
        sizeHeightPx: Float,
        touchMajorPx: Float,
        touchMinorPx: Float,
        toolMajorPx: Float,
        nativeToolType: Int,
        pointerCount: Int,
        posX: Float,
        posY: Float
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            "DOWN | id=$sessionId | mode=$mode | composeTool=$composeToolType" +
            " | rawTool=${nativeToolType.toToolTypeName()}" +
            " | pressure=${"%.3f".format(pressure)}" +
            " | sizeW=${"%.1f".format(sizeWidthPx)} sizeH=${"%.1f".format(sizeHeightPx)}" +
            " | touchMajor=${"%.1f".format(touchMajorPx)} touchMinor=${"%.1f".format(touchMinorPx)}" +
            " | toolMajor=${"%.1f".format(toolMajorPx)}" +
            " | pointers=$pointerCount" +
            " | pos=(${posX.toInt()},${posY.toInt()})"
        )
    }

    /**
     * Log the outcome of a freehand drawing gesture (PEN / HIGHLIGHTER / ERASER / LASSO).
     * Call this at every exit point of the freehand path.
     *
     * @param outcome One of:
     *   - `ACCEPTED`            — stroke committed to DB
     *   - `REJECTED_ENTRY`      — blocked at start by [PalmRejectionFilter.shouldReject]
     *   - `REJECTED_PRESSURE`   — mid-stroke pressure spike triggered soft rejection
     *   - `CANCELLED_SYSTEM`    — OS cancelled the gesture (e.g. native palm detection)
     * @param pointCount        currentPathPoints.size at exit.
     * @param maxPressure       Highest pressure seen during the entire drag.
     * @param maxSizeWidthPx    Highest PointerInputChange.size.width seen during drag.
     * @param maxSizeHeightPx   Highest PointerInputChange.size.height seen during drag.
     */
    fun logOutcome(
        sessionId: Int,
        outcome: String,
        pointCount: Int,
        maxPressure: Float,
        maxSizeWidthPx: Float,
        maxSizeHeightPx: Float
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            "END  | id=$sessionId | outcome=$outcome | points=$pointCount" +
            " | maxPressure=${"%.3f".format(maxPressure)}" +
            " | maxSizeW=${"%.1f".format(maxSizeWidthPx)} maxSizeH=${"%.1f".format(maxSizeHeightPx)}"
        )
    }

    // MotionEvent.TOOL_TYPE_* constants (API 14+, well within our min SDK 32)
    private fun Int.toToolTypeName(): String = when (this) {
        0 -> "UNKNOWN"
        1 -> "FINGER"
        2 -> "STYLUS"
        3 -> "MOUSE"
        4 -> "ERASER"
        else -> "RAW($this)"
    }
}
