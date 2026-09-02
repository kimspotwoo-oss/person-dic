package com.persondic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "person")
data class Person(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val displayName: String,
    val alias: String? = null,
    val groupTag: String? = null,
    val metOn: LocalDate? = null,
    val metStory: String? = null,
    val photoUri: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
