package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HotspotProfileDao {
    @Query("SELECT * FROM hotspot_profiles ORDER BY timestamp DESC")
    fun getAllProfiles(): Flow<List<HotspotProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HotspotProfile)

    @Delete
    suspend fun deleteProfile(profile: HotspotProfile)
}

@Dao
interface BlockedDeviceDao {
    @Query("SELECT * FROM blocked_devices ORDER BY timestamp DESC")
    fun getAllBlocked(): Flow<List<BlockedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(device: BlockedDevice)

    @Delete
    suspend fun unblockDevice(device: BlockedDevice)
}

@Dao
interface CommandLogDao {
    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<CommandLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommandLog)

    @Query("DELETE FROM command_logs")
    suspend fun clearLogs()
}
