package com.buzbuz.smartautoclicker.detection

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
import android.view.accessibility.AccessibilityEvent


/** Detects accessibility event for conditions detection. */
abstract class AccessibilityEventDetector : AccessibilityService() {

//    /** Records the latest AccessibilityEvents */
//    private val windowContentChangeEvents = LinkedList<AccessibilityEvent>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        println("$TAG:onAccessibilityEvent=$event")

        // -- 过滤一些系统特殊包名 --
        if (event?.packageName == null) {
            return
        }

//        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
//            if (windowContentChangeEvents.size > 5) {
//                windowContentChangeEvents.pop()
//            }
//            windowContentChangeEvents.add(event)
//        }
    }

    fun detectCondition(processName: String): DetectionResult.Event {
//        val latest = windowContentChangeEvents.lastOrNull() ?: return DetectionResult.Event(false)
//        println("$TAG:latestEvent=$latest")

        val currentPackageName = rootInActiveWindow?.packageName ?: return DetectionResult.Event(false)
        return DetectionResult.Event(
            processName == currentPackageName || if (processName.length > currentPackageName.length) processName.contains(
                currentPackageName
            ) else currentPackageName.contains(processName)
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        synchronized(Companion) {
            INSTANCE = this
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanInstance()
        return super.onUnbind(intent)
    }

    companion object {

        private const val TAG = "AccessibilityEventDetector"

        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        var INSTANCE: AccessibilityEventDetector? = null
            internal set

        /** Clear this singleton instance, forcing to instantiates it again. */
        private fun cleanInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}