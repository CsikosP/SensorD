package com.example.sensord.model

data class ServerUploadRequest(
    val situation: String,
    val isCollision: Boolean,
    val platform: String = "android",  // 👈 플랫폼 구분 필드 추가
    val data: List<SensorEntry>
)

data class SensorEntry(
    val timestamp: String,       // 예: "2025-06-30 17:00:00"
    val gyro: List<Float>,       // [gyroX, gyroY, gyroZ]
    val accel: List<Float>       // [accX, accY, accZ]
)
