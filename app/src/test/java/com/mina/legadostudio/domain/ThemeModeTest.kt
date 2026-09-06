package com.mina.legadostudio
import com.mina.legadostudio.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class ThemeModeTest {
    @Test fun fromKeyRoundTrip() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromKey(it.key)) }
    }
    @Test fun fromKeyUnknownFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("sepia"))
    }
    @Test fun resolveDarkMatrix() {
        assertFalse(ThemeMode.resolveDark(ThemeMode.LIGHT, systemDark = true))
        assertFalse(ThemeMode.resolveDark(ThemeMode.LIGHT, systemDark = false))
        assertTrue(ThemeMode.resolveDark(ThemeMode.DARK, systemDark = false))
        assertTrue(ThemeMode.resolveDark(ThemeMode.DARK, systemDark = true))
        assertTrue(ThemeMode.resolveDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(ThemeMode.resolveDark(ThemeMode.SYSTEM, systemDark = false))
    }
    @Test fun labelsAreStable() {
        assertEquals(listOf("浅色", "跟随系统", "深色"), ThemeMode.entries.map { it.label })
    }
}
