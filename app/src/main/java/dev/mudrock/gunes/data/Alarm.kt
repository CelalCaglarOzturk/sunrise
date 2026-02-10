package dev.mudrock.gunes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val time: String, // HH:mm
    val enabled: Boolean,
    val label: String,
    val useSunTime: Boolean,
    val sunOffset: Int, // minutes
    val repeatDays: String, // Comma separated integers 0-6
    val deleteAfterUse: Boolean,
    val soundUri: String,
    val soundName: String,
    val vibrate: Boolean,
    val volume: Int,
    val gradualVolume: Boolean,
    val gradualDuration: Int
)
