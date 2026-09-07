package com.persondic.domain

import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.model.Volatility
import java.time.DateTimeException
import java.time.LocalDate
import java.time.MonthDay

data class ParsedFact(
    val body: String,
    val category: FactCategory,
    val volatility: Volatility,
    val sensitivity: Sensitivity,
    val pinned: Boolean,
)

/**
 * A birthday as written in the text. Either half can be missing: "생일: 3-15" gives only a
 * [monthDay], "년생: 1994" gives only a [year].
 */
data class ParsedBirthday(
    val monthDay: LocalDate? = null,
    val year: Int? = null,
    val isLunar: Boolean = false,
)

data class ParsedAttribute(
    val label: String,
    val value: String,
)

data class ParsedCommitment(
    val body: String,
    val direction: Direction,
)

data class ParsedPerson(
    val displayName: String,
    val alias: String? = null,
    val metStory: String? = null,
    val tags: List<String> = emptyList(),
    val birthday: ParsedBirthday? = null,
    val attributes: List<ParsedAttribute> = emptyList(),
    val facts: List<ParsedFact> = emptyList(),
    val commitments: List<ParsedCommitment> = emptyList(),
)

data class QuickAddResult(
    val people: List<ParsedPerson> = emptyList(),
    val warnings: List<String> = emptyList(),
)

private val CATEGORY_MARKERS = mapOf(
    "관계" to FactCategory.CONTEXT,
    "계기" to FactCategory.CONTEXT,
    "취향" to FactCategory.PREFERENCE,
    "생활" to FactCategory.LIFE,
    "화제" to FactCategory.HOOK,
)

private val VOLATILITY_MARKERS = mapOf(
    "영구" to Volatility.PERMANENT,
    "천천히" to Volatility.SLOW,
    "계절" to Volatility.SEASONAL,
    "일회" to Volatility.EVENT,
)

private val SENSITIVITY_MARKERS = mapOf(
    "비공개" to Sensitivity.PRIVATE,
    "민감" to Sensitivity.RESTRICTED,
)

private const val PIN_MARKER = "주의"

private val DEFAULT_CATEGORY = FactCategory.LIFE
private val DEFAULT_VOLATILITY = Volatility.SEASONAL

/**
 * Parses the quick-add text format into people to create. Everything except a name is optional,
 * and unrecognised lines are reported as warnings rather than silently dropped, so a typo never
 * turns into a missing fact the user thinks they saved.
 */
fun parseQuickAdd(input: String): QuickAddResult {
    val warnings = mutableListOf<String>()
    val people = mutableListOf<ParsedPerson>()

    val blocks = input.split(Regex("(?m)^\\s*-{3,}\\s*$"))
    blocks.forEach { block ->
        parseBlock(block, warnings)?.let { people += it }
    }

    return QuickAddResult(people = people, warnings = warnings)
}

private fun parseBlock(block: String, warnings: MutableList<String>): ParsedPerson? {
    val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return null

    var displayName: String? = null
    var alias: String? = null
    var metStory: String? = null
    var birthday: ParsedBirthday? = null
    val tags = mutableListOf<String>()
    val attributes = mutableListOf<ParsedAttribute>()
    val facts = mutableListOf<ParsedFact>()
    val commitments = mutableListOf<ParsedCommitment>()

    lines.forEach { line ->
        when {
            line.startsWith("- ") || line == "-" -> {
                val rest = line.removePrefix("-").trim()
                if (rest.isEmpty()) {
                    warnings += "내용이 비어 있는 사실 줄을 건너뛰었습니다: \"$line\""
                } else {
                    facts += parseFact(rest)
                }
            }

            line.startsWith("?") -> {
                val body = line.removePrefix("?").trim()
                if (body.isEmpty()) {
                    warnings += "내용이 비어 있는 약속 줄을 건너뛰었습니다: \"$line\""
                } else {
                    commitments += ParsedCommitment(body, Direction.THEY_OWE)
                }
            }

            line.startsWith("!") -> {
                val body = line.removePrefix("!").trim()
                if (body.isEmpty()) {
                    warnings += "내용이 비어 있는 약속 줄을 건너뛰었습니다: \"$line\""
                } else {
                    commitments += ParsedCommitment(body, Direction.I_OWE)
                }
            }

            line.startsWith("생일:") -> {
                val text = line.removePrefix("생일:").trim()
                val parsed = parseBirthday(text)
                if (parsed == null) {
                    warnings += "생일 날짜를 읽지 못했습니다: \"$text\" (1990-03-15 또는 3-15 형식)"
                } else {
                    // A 년생: line may already have set the year; keep whichever half each gave.
                    birthday = ParsedBirthday(
                        monthDay = parsed.monthDay,
                        year = parsed.year ?: birthday?.year,
                        isLunar = parsed.isLunar,
                    )
                }
            }

            line.startsWith("년생:") -> {
                val text = line.removePrefix("년생:").trim().removeSuffix("년생").removeSuffix("년").trim()
                val year = text.toIntOrNull()?.takeIf { it in EARLIEST_BIRTH_YEAR..LATEST_BIRTH_YEAR }
                if (year == null) {
                    warnings += "몇년생인지 읽지 못했습니다: \"$text\" (1994 형식)"
                } else {
                    birthday = (birthday ?: ParsedBirthday()).copy(year = year)
                }
            }

            line.startsWith("+") -> {
                val body = line.removePrefix("+").trim()
                val separator = body.indexOf(':')
                val label = if (separator >= 0) body.take(separator).trim() else ""
                val value = if (separator >= 0) body.substring(separator + 1).trim() else ""
                if (label.isEmpty() || value.isEmpty()) {
                    warnings += "고정 정보는 \"+항목: 내용\" 형식이어야 합니다: \"$line\""
                } else {
                    attributes += ParsedAttribute(label, value)
                }
            }

            line.startsWith("별명:") -> alias = line.removePrefix("별명:").trim().takeIf { it.isNotEmpty() }

            line.startsWith("계기:") -> metStory = line.removePrefix("계기:").trim().takeIf { it.isNotEmpty() }

            displayName == null -> {
                val tokens = line.split(Regex("\\s+"))
                tags += tokens.filter { it.length > 1 && (it.startsWith("@") || it.startsWith("#")) }
                    .map { it.drop(1) }
                val name = tokens.filterNot { it.startsWith("@") || it.startsWith("#") }
                    .joinToString(" ")
                    .trim()
                if (name.isEmpty()) {
                    warnings += "이름을 찾을 수 없는 줄입니다: \"$line\""
                } else {
                    displayName = name
                }
            }

            else -> warnings += "알 수 없는 줄이라 건너뛰었습니다: \"$line\""
        }
    }

    val name = displayName
    if (name == null) {
        if (facts.isNotEmpty() || commitments.isNotEmpty()) {
            warnings += "이름이 없어 블록 하나를 통째로 건너뛰었습니다."
        }
        return null
    }

    return ParsedPerson(
        displayName = name,
        alias = alias,
        metStory = metStory,
        tags = tags.distinct(),
        birthday = birthday,
        attributes = attributes.distinctBy { it.label },
        facts = facts,
        commitments = commitments,
    )
}

private fun parseFact(text: String): ParsedFact {
    val tokens = text.split(Regex("\\s+"))
    val markers = tokens.filter { it.startsWith("*") && it.length > 1 }.map { it.drop(1) }
    val body = tokens.filterNot { it.startsWith("*") }.joinToString(" ").trim()

    var category = DEFAULT_CATEGORY
    var volatility = DEFAULT_VOLATILITY
    var sensitivity = Sensitivity.NORMAL
    var pinned = false

    markers.forEach { marker ->
        CATEGORY_MARKERS[marker]?.let { category = it }
        VOLATILITY_MARKERS[marker]?.let { volatility = it }
        SENSITIVITY_MARKERS[marker]?.let { sensitivity = it }
        if (marker == PIN_MARKER) pinned = true
    }

    return ParsedFact(
        body = body.ifEmpty { text },
        category = category,
        volatility = volatility,
        sensitivity = sensitivity,
        pinned = pinned,
    )
}

/**
 * Reads "1990-03-15", "1990.3.15", "3-15" and the 음력 prefix. A month and day with no year is
 * normal — you often know the day without the year — and stores the stand-in year instead of
 * being rejected.
 */
private fun parseBirthday(text: String): ParsedBirthday? {
    val isLunar = text.startsWith("음력")
    val digits = text.removePrefix("음력").trim().replace('.', '-').replace('/', '-').trim('-')
    val parts = digits.split("-").map { it.trim() }.filter { it.isNotEmpty() }

    return try {
        when (parts.size) {
            // A full date still only stores the month and day; the year goes to its own field.
            3 -> ParsedBirthday(
                monthDay = MonthDay.of(parts[1].toInt(), parts[2].toInt()).atYear(BIRTHDAY_YEAR_UNKNOWN),
                year = parts[0].toInt().takeIf { it in EARLIEST_BIRTH_YEAR..LATEST_BIRTH_YEAR },
                isLunar = isLunar,
            )
            2 -> ParsedBirthday(
                monthDay = MonthDay.of(parts[0].toInt(), parts[1].toInt()).atYear(BIRTHDAY_YEAR_UNKNOWN),
                isLunar = isLunar,
            )
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    } catch (e: DateTimeException) {
        null
    }
}

private const val EARLIEST_BIRTH_YEAR = 1900
private const val LATEST_BIRTH_YEAR = 2200
