package com.underwood.idgenerator

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class SystemClock(private val period: Long) {
    companion object {
        val instance = SystemClock(1)
        val now = AtomicLong(System.currentTimeMillis())

        fun now(): Long {
            return SystemClock.now.get()
        }
    }
    init {
        scheduleClockUpdating()
    }

    fun scheduleClockUpdating() {
        val scheduler = Executors.newSingleThreadScheduledExecutor { Thread(it, "System Clock") }
        scheduler.scheduleAtFixedRate({ now.set(System.currentTimeMillis()) }, period, period, TimeUnit.MILLISECONDS)
    }
}