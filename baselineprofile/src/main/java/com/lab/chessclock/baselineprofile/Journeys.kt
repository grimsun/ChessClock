package com.lab.chessclock.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "com.lab.chessclock"

private const val TIMEOUT_MS = 5_000L

/** Starts the clock, ends a few turns on each side, and pauses. */
fun MacrobenchmarkScope.playAFewMoves() {
    device.wait(Until.findObject(By.text("Start")), TIMEOUT_MS).click()
    // Each half of the screen is a player's button: tap the top half, then the bottom one.
    val width = device.displayWidth
    val height = device.displayHeight
    repeat(3) {
        device.click(width / 2, height / 4)
        device.waitForIdle()
        device.click(width / 2, height * 3 / 4)
        device.waitForIdle()
    }
    device.wait(Until.findObject(By.text("Pause")), TIMEOUT_MS).click()
}

/** Opens settings, scrolls through them, and goes back to the clock. */
fun MacrobenchmarkScope.openSettingsAndBack() {
    device.wait(Until.findObject(By.desc("Settings")), TIMEOUT_MS).click()
    device.wait(Until.hasObject(By.text("Time control")), TIMEOUT_MS)
    device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
    device.waitForIdle()
    device.pressBack()
    device.wait(Until.hasObject(By.text("Reset")), TIMEOUT_MS)
}
