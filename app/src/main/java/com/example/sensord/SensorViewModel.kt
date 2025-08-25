package com.example.sensord

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensord.model.SensorData
import com.example.sensord.model.SensorEntry
import com.example.sensord.model.ServerUploadRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val intervalNanos = 100_000_000L // 100ms
    private var lastTimestamp: Long = 0L

    private val _sensorDataList = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorDataList = _sensorDataList.asStateFlow()

    private val _hasUploaded = MutableStateFlow(false)
    val hasUploaded = _hasUploaded.asStateFlow()

    private val _isCollecting = MutableStateFlow(false)
    val isCollecting = _isCollecting.asStateFlow()

    private var tempData = mutableListOf<SensorData>()
    private var accValues = FloatArray(3)
    private var gyroValues = FloatArray(3)

    fun startCollecting() {
        stopCollecting()

        tempData.clear()
        _sensorDataList.value = emptyList()
        _hasUploaded.value = false
        _isCollecting.value = true
        lastTimestamp = 0L
        accValues = FloatArray(3)
        gyroValues = FloatArray(3)

        if (accelerometer == null || gyroscope == null) {
            Log.e("SensorViewModel", "센서가 지원되지 않습니다.")
            _isCollecting.value = false
            return
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stopCollecting() {
        sensorManager.unregisterListener(this)
        lastTimestamp = 0L
        _isCollecting.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accValues = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> gyroValues = event.values.clone()
        }

        val now = System.nanoTime()
        if (now - lastTimestamp < intervalNanos) return
        lastTimestamp = now

        val data = SensorData(
            timestamp = System.currentTimeMillis(),
            accX = accValues[0],
            accY = accValues[1],
            accZ = accValues[2],
            gyroX = gyroValues[0],
            gyroY = gyroValues[1],
            gyroZ = gyroValues[2]
        )

        tempData.add(data)

        if (tempData.size >= 100) {
            stopCollecting()
            _sensorDataList.value = tempData.toList()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun uploadData(isCollision: Boolean, onResult: (Boolean, String) -> Unit) {
        if (_hasUploaded.value) {
            onResult(false, "이미 전송된 데이터입니다.")
            return
        }

        val entries = sensorDataList.value.map {
            SensorEntry(
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp)),
                gyro = listOf(it.gyroX, it.gyroY, it.gyroZ),
                accel = listOf(it.accX, it.accY, it.accZ)
            )
        }

        val requestBody = ServerUploadRequest(
            situation = "테스트 상황",
            isCollision = isCollision,
            platform = "android", // ✅ 플랫폼 필드 추가됨
            data = entries
        )

        val gson = Gson()
        val json = gson.toJson(requestBody)
        val mediaType = "application/json".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://www.loenkorea.com/api/collision-data/save-json")
            .put(body)
            .build()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = OkHttpClient().newCall(request).execute()
                val responseText = response.body?.string() ?: "응답 없음"
                if (response.isSuccessful) {
                    _hasUploaded.value = true
                }
                onResult(response.isSuccessful, responseText)
            } catch (e: Exception) {
                onResult(false, "전송 실패: ${e.message}")
            }
        }
    }

    fun resetState() {
        stopCollecting()
        tempData.clear()
        _sensorDataList.value = emptyList()
        _hasUploaded.value = false
        _isCollecting.value = false
        accValues = FloatArray(3)
        gyroValues = FloatArray(3)
        lastTimestamp = 0L
    }
}
