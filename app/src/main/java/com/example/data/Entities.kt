package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hotspot_profiles")
data class HotspotProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileName: String,
    val ssid: String,
    val password: String,
    val securityType: String, // WPA2, WPA3, WPA3_TRANSITION, OPEN
    val band2g: Boolean,
    val band5g: Boolean,
    val band6g: Boolean,
    val mloEnabled: Boolean,
    val channelBandwidth: String = "320",
    val channel5g: String = "Auto",
    val channel6g: String = "Auto",
    val channel6gMode: String = "psc",
    val indoorAp6g: Boolean = true,
    val region: String, // US, UK, IN, JP, etc.
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_devices")
data class BlockedDevice(
    @PrimaryKey val macAddress: String,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_logs")
data class CommandLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean
)
