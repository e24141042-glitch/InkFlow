package com.vic.inkflow.util

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Stateless palm-rejection classifier.
 *
 * ## Device-adaptive touchMajor detection
 *
 * MotionEvent.getTouchMajor() returns different units depending on the OEM:
 *  - Most AOSP / stock devices  → pixels (fingertip ≈ 40–80 px, palm ≈ 100–400 px)
 *  - MIUI / Xiaomi normalized   → a small float (fingertip ≈ 1.2, palm presumably > 3–5)
 *
 * This filter detects which regime the device is in:
 *  - If touchMajor >= PIXEL_UNIT_THRESHOLD (10.0) → compare against 45 dp converted to px
 *  - If touchMajor < PIXEL_UNIT_THRESHOLD        → compare against RAW_UNIT_PALM_THRESHOLD
 *
 * **Pressure is intentionally NOT used** as a rejection signal.
 * Many devices (notably MIUI) report pressure=1.000 for all ordinary finger contacts,
 * which makes it useless as a discriminator and causes false rejections.
 *
 * ## Contact-size zones (MIUI tablet, normalized units)
 *
 * Zone          | touchMajor range | Action
 * --------------|------------------|-------------------------
 * Stylus        | 0.1 – 0.5        | Draw
 * Finger        | 0.7 – 1.6        | Pan only (pass-through)
 * Palm          | 2.0 – 3.8        | Hard reject
 *
 * Thresholds:
 *   STYLUS_TOUCH_MAJOR_THRESHOLD = 0.6  (midpoint of 0.5–0.7 gap)
 *   RAW_UNIT_PALM_THRESHOLD      = 1.8  (midpoint of 1.6–2.0 gap)
 */
object PalmRejectionFilter {

    /**
     * If getTouchMajor() returns a value >= this, assume it is in pixels.
     * Below this → assume device-normalized small-float units (e.g., MIUI).
     */
    private const val PIXEL_UNIT_THRESHOLD = 10.0f

    /**
     * Palm threshold for pixel-unit devices: fingertip ≈ 10–30 dp, palm ≥ 50 dp.
     * Expressed in dp so the comparison is DPI-independent.
     */
    private val MAX_TOUCH_MAJOR_DP = 45.dp

    /**
     * Stylus / finger boundary for normalized-unit devices (MIUI, etc.).
     * Stylus max observed = 0.5, finger min observed = 0.7 → midpoint = 0.6.
     * Touches below this are treated as stylus (draw); above as finger (pan).
     */
    const val STYLUS_TOUCH_MAJOR_THRESHOLD = 0.6f

    /**
     * Palm threshold for normalized-unit devices (MIUI, etc.).
     * Finger max observed = 1.6, palm min observed = 2.0 → midpoint = 1.8.
     * Touches above this are hard-rejected (neither draw nor pan).
     */
    const val RAW_UNIT_PALM_THRESHOLD = 1.8f

    /**
     * @param touchMajorRaw  Raw value from [android.view.MotionEvent.getTouchMajor].
     *                       Pass 0f if unavailable; only the multi-pointer check will apply.
     * @param concurrentPointers  Total active pointers in the current gesture frame.
     * @param density        Compose [Density] for dp→px conversion on pixel-unit devices.
     * @return `true` if the contact should be rejected as a palm / unintentional touch.
     */
    /**
     * Returns true if the contact should be hard-rejected as a palm or multi-finger gesture.
     * Does NOT cover the finger-vs-stylus distinction; use [isFinger] for that.
     */
    fun shouldReject(
        touchMajorPx: Float,
        @Suppress("UNUSED_PARAMETER") pressure: Float,
        concurrentPointers: Int,
        density: Density
    ): Boolean {
        // Multi-pointer frame → palm resting alongside a finger, or pinch gesture
        if (concurrentPointers > 1) return true

        // Contact area too large → palm
        if (touchMajorPx > 0f) {
            if (touchMajorPx >= PIXEL_UNIT_THRESHOLD) {
                val maxPx = with(density) { MAX_TOUCH_MAJOR_DP.toPx() }
                if (touchMajorPx > maxPx) return true
            } else {
                if (touchMajorPx > RAW_UNIT_PALM_THRESHOLD) return true
            }
        }

        return false
    }

    /**
     * Returns true if the contact looks like a finger (not a stylus tip).
     * Only meaningful after [shouldReject] has already returned false.
     *
     * A return value of false (unknown / stylus-sized) → allow drawing.
     * A return value of true (finger-sized)            → pass through for panning.
     */
    fun isFinger(touchMajorPx: Float, density: Density): Boolean {
        if (touchMajorPx <= 0f) return false  // no data → assume stylus, allow draw
        return if (touchMajorPx >= PIXEL_UNIT_THRESHOLD) {
            // Pixel-unit device: stylus tip typically < ~8 dp
            val stylusMaxPx = with(density) { 8.dp.toPx() }
            touchMajorPx > stylusMaxPx
        } else {
            // Normalized-unit device (e.g., MIUI)
            touchMajorPx > STYLUS_TOUCH_MAJOR_THRESHOLD
        }
    }
}
