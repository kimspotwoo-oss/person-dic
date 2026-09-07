package com.persondic.ui.groupmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NamePackingTest {

    private val three = listOf("김민준", "이서연", "박지호")

    @Test
    fun everythingFitsInAGenerousBox() {
        val packed = packNames(three, widthDp = 300f, heightDp = 100f)

        assertEquals(three, packed.shown)
        assertEquals(0, packed.hidden)
    }

    @Test
    fun aBoxTooShortForOneLineShowsNothing() {
        val packed = packNames(three, widthDp = 300f, heightDp = 5f)

        assertTrue(packed.shown.isEmpty())
        assertEquals(3, packed.hidden)
    }

    @Test
    fun namesWrapOntoTheNextLine() {
        // One 3-character name is 36dp, so 90dp holds two per line but not three.
        val onOneLine = packNames(three, widthDp = 90f, heightDp = 16f)
        val onTwoLines = packNames(three, widthDp = 90f, heightDp = 32f)

        assertTrue(onOneLine.shown.size < onTwoLines.shown.size)
        assertEquals(three, onTwoLines.shown)
    }

    @Test
    fun whatIsShownIsAlwaysAPrefixOfTheNames() {
        val packed = packNames(three, widthDp = 60f, heightDp = 16f)

        assertEquals(three.take(packed.shown.size), packed.shown)
    }

    @Test
    fun theCountsAlwaysAddUp() {
        listOf(20f, 45f, 80f, 140f, 400f).forEach { width ->
            listOf(10f, 16f, 40f, 120f).forEach { height ->
                val packed = packNames(three, width, height)
                assertEquals(
                    "width=$width height=$height",
                    three.size,
                    packed.shown.size + packed.hidden,
                )
            }
        }
    }

    @Test
    fun roomIsLeftForTheOverflowMarkerWhenSomethingIsHidden() {
        val names = List(20) { "김민준" }

        val packed = packNames(names, widthDp = 120f, heightDp = 32f)

        assertTrue("something must be hidden here", packed.hidden > 0)
        // Two 120dp lines hold 3 names each without the marker; the marker costs about one slot.
        assertTrue("marker must not push names off the box", packed.shown.size <= 5)
    }

    @Test
    fun aSingleNameTooWideForTheBoxIsNotShownHalfCut() {
        val packed = packNames(listOf("아주아주아주긴이름입니다"), widthDp = 40f, heightDp = 60f)

        assertTrue(packed.shown.isEmpty())
        assertEquals(1, packed.hidden)
    }

    @Test
    fun latinNamesAreNarrowerThanHangul() {
        val hangul = packNames(List(6) { "김민준" }, widthDp = 100f, heightDp = 16f)
        val latin = packNames(List(6) { "Ann" }, widthDp = 100f, heightDp = 16f)

        assertTrue(latin.shown.size > hangul.shown.size)
    }

    @Test
    fun theMarkerIsDroppedWhenItWouldCostSeveralNames() {
        // "Ann" is 21dp and the gap 8dp, so a 100dp line holds three — but only one once 56dp is
        // set aside for the marker. Two names is too much to pay for it.
        val packed = packNames(List(6) { "Ann" }, widthDp = 100f, heightDp = 16f)

        assertEquals(3, packed.shown.size)
        assertEquals(false, packed.showOverflowMarker)
        assertEquals(3, packed.hidden)
    }

    @Test
    fun theMarkerIsKeptWhenItOnlyCostsOneName() {
        val packed = packNames(List(20) { "김민준" }, widthDp = 120f, heightDp = 32f)

        assertEquals(true, packed.showOverflowMarker)
    }

    @Test
    fun emptyInputIsNotAnOverflow() {
        val packed = packNames(emptyList(), widthDp = 100f, heightDp = 100f)

        assertTrue(packed.shown.isEmpty())
        assertEquals(0, packed.hidden)
    }
}
