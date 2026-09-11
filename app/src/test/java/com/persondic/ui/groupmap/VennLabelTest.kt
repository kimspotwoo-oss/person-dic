package com.persondic.ui.groupmap

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VennLabelTest {

    private fun bubble(tag: String, size: Int) =
        GroupBubble(tag, (1..size).map { UUID.randomUUID() }.toSet())

    private fun box(centerY: Float, height: Float) =
        RegionBox(center = Offset(0.5f, centerY), width = 0.4f, height = height)

    private fun region(tag: String?, box: RegionBox) =
        VennRegion(label = tag ?: "겹침", memberIds = emptyList(), box = box, exclusiveTag = tag)

    // --- vennRegions marks which regions belong to one tag alone ---

    @Test
    fun aSingleTagRegionCarriesItsOwnTag() {
        val regions = vennRegions(listOf(bubble("고려대학교", 4), bubble("1학년", 6)))

        assertEquals("고려대학교", regions.single { it.label == "#고려대학교만" }.exclusiveTag)
        assertEquals("1학년", regions.single { it.label == "#1학년만" }.exclusiveTag)
    }

    @Test
    fun anOverlapRegionBelongsToNobodyAlone() {
        val regions = vennRegions(listOf(bubble("고려대학교", 4), bubble("1학년", 6)))

        assertNull(regions.single { it.label.contains("∩") }.exclusiveTag)
    }

    // --- splitOffTagStrip: one rectangle, not two independent guesses at it ---

    @Test
    fun theTagSitsInTheStripItWasGivenAndNamesGetWhatIsLeft() {
        val whole = box(centerY = 0.5f, height = 0.3f)
        val placement = splitOffTagStrip(region("고려대학교", whole), tagStripHeight = 0.06f)

        // The strip is the top 0.06 of the box: centred at 0.5 - 0.15 + 0.03 = 0.38.
        assertEquals(0.38f, placement.anchor.y, 1e-4f)
        // What's left starts right where the strip ends and reaches the original bottom.
        val namesTop = placement.namesBox.center.y - placement.namesBox.height / 2f
        val stripBottom = placement.anchor.y + 0.03f
        assertEquals(stripBottom, namesTop, 1e-4f)
        assertEquals(whole.center.y + whole.height / 2f, namesBoxBottom(placement.namesBox), 1e-4f)
    }

    @Test
    fun theStripAndTheNamesNeverOverlap() {
        val whole = box(centerY = 0.5f, height = 0.3f)
        val stripHeight = 0.06f
        val placement = splitOffTagStrip(region("1학년", whole), stripHeight)

        val stripBottom = placement.anchor.y + stripHeight / 2f
        val namesTop = placement.namesBox.center.y - placement.namesBox.height / 2f
        assertTrue("the tag strip reaches into the names box", stripBottom <= namesTop + 1e-4f)
    }

    @Test
    fun anOverlapRegionKeepsItsBoxWhole() {
        val whole = box(centerY = 0.5f, height = 0.3f)
        val placement = splitOffTagStrip(region(null, whole), tagStripHeight = 0.06f)

        assertEquals(whole, placement.namesBox)
        assertEquals(whole.center, placement.anchor)
    }

    @Test
    fun aStripTallerThanTheBoxIsIgnoredRatherThanGoingNegative() {
        val whole = box(centerY = 0.5f, height = 0.02f)
        val placement = splitOffTagStrip(region("06", whole), tagStripHeight = 0.5f)

        assertEquals(whole, placement.namesBox)
        assertTrue(placement.namesBox.height > 0f)
    }

    @Test
    fun aZeroOrNegativeStripLeavesTheBoxAlone() {
        val whole = box(centerY = 0.5f, height = 0.2f)

        assertEquals(whole, splitOffTagStrip(region("06", whole), tagStripHeight = 0f).namesBox)
        assertEquals(whole, splitOffTagStrip(region("06", whole), tagStripHeight = -0.1f).namesBox)
    }

    private fun namesBoxBottom(box: RegionBox) = box.center.y + box.height / 2f
}
