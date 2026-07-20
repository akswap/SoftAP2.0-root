package com.example.data

import kotlinx.coroutines.flow.Flow

class HotspotRepository(private val db: AppDatabase) {
    val allProfiles: Flow<List<HotspotProfile>> = db.hotspotProfileDao().getAllProfiles()
    val allBlocked: Flow<List<BlockedDevice>> = db.blockedDeviceDao().getAllBlocked()
    val recentLogs: Flow<List<CommandLog>> = db.commandLogDao().getRecentLogs()

    suspend fun insertProfile(profile: HotspotProfile) {
        db.hotspotProfileDao().insertProfile(profile)
    }

    suspend fun deleteProfile(profile: HotspotProfile) {
        db.hotspotProfileDao().deleteProfile(profile)
    }

    suspend fun blockDevice(device: BlockedDevice) {
        db.blockedDeviceDao().insertBlock(device)
    }

    suspend fun unblockDevice(device: BlockedDevice) {
        db.blockedDeviceDao().unblockDevice(device)
    }

    suspend fun addLog(log: CommandLog) {
        db.commandLogDao().insertLog(log)
    }

    suspend fun clearLogs() {
        db.commandLogDao().clearLogs()
    }
}
