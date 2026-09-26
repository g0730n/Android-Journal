// AppLockState.kt
package com.example.journalappv1

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object AppLockState : DefaultLifecycleObserver {
    var isUnlocked = false

    override fun onStop(owner: LifecycleOwner) {
        // Whole app left the foreground — require the PIN again next time.
        isUnlocked = false
    }
}