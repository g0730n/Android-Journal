// JournalApp.kt
package com.example.journalappv1

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

class JournalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLockState)
    }
}