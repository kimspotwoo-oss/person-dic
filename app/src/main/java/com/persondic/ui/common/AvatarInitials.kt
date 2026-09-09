package com.persondic.ui.common

/**
 * The letters an avatar shows when there is no photo.
 *
 * One character was the obvious choice and the wrong one here: 김경모, 김민준 and 김범채 are three
 * different people and were three identical 김 circles — half the list wearing the same badge. The
 * surname is the part that repeats, so the surname is the part to drop. The last two characters of
 * a Hangul name are the given name in the ordinary three-character case, the whole name in a
 * two-character one, and still the given name behind a two-character surname like 남궁.
 *
 * A name written in letters keeps the usual initials, because there it is the front of each word
 * that distinguishes rather than the back.
 */
fun avatarInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ""

    val words = trimmed.split(WHITESPACE).filter { it.isNotEmpty() }
    val hangul = trimmed.any { it.isHangul() }

    return when {
        !hangul && words.size >= 2 -> words.take(2).map { it.first().uppercaseChar() }.joinToString("")
        hangul -> trimmed.takeLast(2)
        else -> trimmed.take(2)
    }
}

private val WHITESPACE = Regex("\\s+")

private fun Char.isHangul(): Boolean =
    this in '가'..'힣' || this in 'ᄀ'..'ᇿ' || this in '㄰'..'㆏'
