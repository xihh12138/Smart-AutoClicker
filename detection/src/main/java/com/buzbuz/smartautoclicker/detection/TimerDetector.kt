package com.buzbuz.smartautoclicker.detection

interface TimerDetector {

    fun detectCondition(conditionId: Long, period: Long): DetectionResult.Timer

}

class TimerDetectorImpl : TimerDetector {

    private val startTimeMills: Long = System.currentTimeMillis()

    private val conditionRecorder = HashMap<Long, Int>()

    override fun detectCondition(conditionId: Long, period: Long): DetectionResult.Timer {
        val record = conditionRecorder[conditionId] ?: 0
        val currentTimeMills = System.currentTimeMillis()

        val elapsedTime = currentTimeMills - startTimeMills

        return if (record * period + period > elapsedTime) {
            DetectionResult.Timer(false, elapsedTime, record)
        } else {
            conditionRecorder[conditionId] = record + 1

            DetectionResult.Timer(true, elapsedTime, record + 1)
        }
    }
}