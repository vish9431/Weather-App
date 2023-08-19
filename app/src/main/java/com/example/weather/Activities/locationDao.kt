package com.example.weather.Activities

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import retrofit2.http.DELETE

@Dao
interface locationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(location: LocationEntity)

//    @DELETE
//    suspend fun delete(location: LocationEntity)


    @Query("SELECT * FROM locations")
    fun getAllLocations(): Flow<List<LocationEntity>>
}

