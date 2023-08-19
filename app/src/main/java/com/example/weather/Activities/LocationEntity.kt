package com.example.weather.Activities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val locationName: String
)




