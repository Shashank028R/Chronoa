package com.studycompanion.app.tracking.windowstate

import android.app.Activity
import com.studycompanion.app.tracking.engine.FocusEngine
import com.studycompanion.app.tracking.engine.FocusEvent

/**
 * Observes multi-window and picture-in-picture state where reliably detectable by Android APIs.
 *
 * Platform Limitation Note:
 * Android API Activity.isInMultiWindowMode() and Activity.isInPictureInPictureMode() describe the calling
 * activity's own window mode. Android does not expose a public cross-app API to observe whether a third-party
 * application is in split-screen or floating mode. We report capability limits honestly and do not use
 * prohibited AccessibilityService workarounds.
 */
class WindowStateObserver(
    private val focusEngine: FocusEngine
) {

    /**
     * Reports window state changes observed on our own activity.
     */
    fun onActivityWindowModeChanged(
        inMultiWindow: Boolean,
        inPictureInPicture: Boolean
    ) {
        val now = System.currentTimeMillis()
        focusEngine.onEvent(
            FocusEvent.WindowModeChanged(
                inMultiWindow = inMultiWindow,
                inFloating = inPictureInPicture,
                timestamp = now
            )
        )
    }

    /**
     * Convenience check for an activity reference.
     */
    fun checkActivityWindowMode(activity: Activity) {
        val inMulti = activity.isInMultiWindowMode
        val inPip = activity.isInPictureInPictureMode
        onActivityWindowModeChanged(inMulti, inPip)
    }
}
