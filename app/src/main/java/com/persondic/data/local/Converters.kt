package com.persondic.data.local

import androidx.room.TypeConverter
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromUuid(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun toUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromFactCategory(value: FactCategory): String = value.name

    @TypeConverter
    fun toFactCategory(value: String): FactCategory = FactCategory.valueOf(value)

    @TypeConverter
    fun fromVolatility(value: Volatility): String = value.name

    @TypeConverter
    fun toVolatility(value: String): Volatility = Volatility.valueOf(value)

    @TypeConverter
    fun fromSensitivity(value: Sensitivity): String = value.name

    @TypeConverter
    fun toSensitivity(value: String): Sensitivity = Sensitivity.valueOf(value)

    @TypeConverter
    fun fromInteractionKind(value: InteractionKind): String = value.name

    @TypeConverter
    fun toInteractionKind(value: String): InteractionKind = InteractionKind.valueOf(value)

    @TypeConverter
    fun fromDirection(value: Direction): String = value.name

    @TypeConverter
    fun toDirection(value: String): Direction = Direction.valueOf(value)

    @TypeConverter
    fun fromCommitmentStatus(value: CommitmentStatus): String = value.name

    @TypeConverter
    fun toCommitmentStatus(value: String): CommitmentStatus = CommitmentStatus.valueOf(value)
}
