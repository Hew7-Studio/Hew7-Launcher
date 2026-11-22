package dev.hew7.launcher.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BingPathTest {
    @Test fun acceptsBingImagePath() = assertTrue(isBingPath("/th?id=OHR.Example_EN-US123"))

    @Test fun rejectsPathsThatChangeTheHost() {
        for (bad in listOf("", "/", "@evil.com/x", "//evil.com/x", "/\\evil.com", "th?id=x", "/a b", "/a\nb")) {
            assertFalse(bad, isBingPath(bad))
        }
    }
}
