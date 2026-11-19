package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandProfileDao {
    @Query("SELECT * FROM brand_profiles ORDER BY isDefault DESC, updatedAt DESC")
    fun observeAll(): Flow<List<BrandProfileEntity>>

    @Query("SELECT COUNT(*) FROM brand_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: BrandProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<BrandProfileEntity>)

    @Update
    suspend fun update(profile: BrandProfileEntity)

    @Delete
    suspend fun delete(profile: BrandProfileEntity)
}

@Dao
interface OverlayPresetDao {
    @Query("SELECT * FROM overlay_presets ORDER BY id ASC")
    fun observeAll(): Flow<List<OverlayPresetEntity>>

    @Query("SELECT COUNT(*) FROM overlay_presets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: OverlayPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<OverlayPresetEntity>)

    @Update
    suspend fun update(preset: OverlayPresetEntity)

    @Delete
    suspend fun delete(preset: OverlayPresetEntity)
}

@Dao
interface StreamDestinationDao {
    @Query("SELECT * FROM stream_destinations ORDER BY id ASC")
    fun observeAll(): Flow<List<StreamDestinationEntity>>

    @Query("SELECT COUNT(*) FROM stream_destinations")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(destination: StreamDestinationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(destinations: List<StreamDestinationEntity>)

    @Update
    suspend fun update(destination: StreamDestinationEntity)

    @Delete
    suspend fun delete(destination: StreamDestinationEntity)
}

@Dao
interface BroadcastSessionDao {
    @Query("SELECT * FROM broadcast_sessions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BroadcastSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: BroadcastSessionEntity): Long

    @Delete
    suspend fun delete(session: BroadcastSessionEntity)
}
