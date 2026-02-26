package net.ezpos.console.feature.release.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {

    // ── 解析：合法输入 ──

    @Test
    fun `parse valid three-part version`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
    }

    @Test
    fun `parse trims whitespace`() {
        assertEquals(SemVer(1, 0, 0), SemVer.parse("  1.0.0  "))
    }

    @Test
    fun `parse zero version`() {
        assertEquals(SemVer(0, 0, 0), SemVer.parse("0.0.0"))
    }

    @Test
    fun `parse large numbers`() {
        assertEquals(SemVer(999, 888, 777), SemVer.parse("999.888.777"))
    }

    // ── 解析：非法输入 ──

    @Test
    fun `parse returns null for two parts`() {
        assertNull(SemVer.parse("1.2"))
    }

    @Test
    fun `parse returns null for four parts`() {
        assertNull(SemVer.parse("1.2.3.4"))
    }

    @Test
    fun `parse returns null for non-numeric`() {
        assertNull(SemVer.parse("a.b.c"))
    }

    @Test
    fun `parse returns null for empty string`() {
        assertNull(SemVer.parse(""))
    }

    @Test
    fun `parse returns null for pre-release suffix`() {
        assertNull(SemVer.parse("1.2.3-beta"))
    }

    // ── 比较 ──

    @Test
    fun `major version takes precedence`() {
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 9, 9))
    }

    @Test
    fun `minor version compared when major equal`() {
        assertTrue(SemVer(1, 2, 0) > SemVer(1, 1, 9))
    }

    @Test
    fun `patch version compared when major and minor equal`() {
        assertTrue(SemVer(1, 1, 2) > SemVer(1, 1, 1))
    }

    @Test
    fun `equal versions compare to zero`() {
        assertEquals(0, SemVer(1, 2, 3).compareTo(SemVer(1, 2, 3)))
    }

    @Test
    fun `less than comparison`() {
        assertTrue(SemVer(1, 0, 0) < SemVer(1, 0, 1))
    }

    // ── toString ──

    @Test
    fun `toString produces x dot y dot z`() {
        assertEquals("1.2.3", SemVer(1, 2, 3).toString())
    }
}
