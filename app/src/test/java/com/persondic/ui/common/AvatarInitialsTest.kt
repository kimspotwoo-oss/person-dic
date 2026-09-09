package com.persondic.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AvatarInitialsTest {

    @Test
    fun aKoreanNameShowsItsGivenName() {
        assertEquals("경모", avatarInitials("김경모"))
        assertEquals("민준", avatarInitials("김민준"))
        assertEquals("범채", avatarInitials("김범채"))
    }

    @Test
    fun theThreeKimsNoLongerWearTheSameBadge() {
        val badges = listOf("김경모", "김민준", "김범채").map(::avatarInitials)

        assertEquals(badges.size, badges.toSet().size)
        badges.forEach { assertNotEquals("김", it) }
    }

    @Test
    fun aTwoCharacterSurnameStillLosesTheSurname() {
        assertEquals("민수", avatarInitials("남궁민수"))
    }

    @Test
    fun aTwoCharacterNameIsShownWhole() {
        assertEquals("김구", avatarInitials("김구"))
    }

    @Test
    fun aSingleCharacterIsAllThereIsToShow() {
        assertEquals("이", avatarInitials("이"))
    }

    @Test
    fun aNameInLettersKeepsItsInitials() {
        assertEquals("JS", avatarInitials("John Smith"))
        assertEquals("JS", avatarInitials("john smith"))
        assertEquals("Jo", avatarInitials("John"))
    }

    @Test
    fun spacingIsNotPartOfTheName() {
        assertEquals("길동", avatarInitials("  홍길동  "))
        assertEquals("길동", avatarInitials("홍 길동"))
        assertEquals("", avatarInitials("   "))
        assertEquals("", avatarInitials(""))
    }
}
