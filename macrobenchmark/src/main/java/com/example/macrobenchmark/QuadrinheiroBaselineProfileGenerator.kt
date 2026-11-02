package com.example.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuadrinheiroBaselineProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()

    @Test fun generate() = profile.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.enterGuestLibrary()
        device.exerciseLibraryAndSettings()
    }
}
