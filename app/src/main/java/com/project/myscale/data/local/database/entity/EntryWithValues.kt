package com.project.myscale.data.local.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EntryWithValues(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val values: List<MeasurementValueEntity>
)
