package com.buzbuz.smartautoclicker.detection

import android.graphics.Point
import androidx.annotation.Keep

/**
 * The results of a condition detection.
 * */
sealed class DetectionResult {

    abstract var isDetected: Boolean

    inline fun asImage() = this as? Image

    /**
     * The results of a image condition detection.
     * @param isDetected true if the condition have been detected. false if not.
     * @param position contains the center of the detected condition in screen coordinates.
     * @param confidenceRate
     */
    data class Image(
        override var isDetected: Boolean = false,
        val position: Point = Point(),
        var confidenceRate: Double = 0.0
    ) : DetectionResult() {

        /**
         * Set the results of the detection.
         * Used by native code only.
         */
        @Keep
        fun setResults(isDetected: Boolean, centerX: Int, centerY: Int, confidenceRate: Double) {
            this.isDetected = isDetected
            position.set(centerX, centerY)
            this.confidenceRate = confidenceRate
        }
    }

    data class Event(
        override var isDetected: Boolean = false
    ) : DetectionResult()

    data class Timer(
        override var isDetected: Boolean = false,
        val elapsedTime: Long,
        val triggerTimes: Int
    ) : DetectionResult()
}