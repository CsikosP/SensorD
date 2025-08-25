package com.example.sensord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.sensord.ui.SensorScreen
import com.example.sensord.ui.theme.SensorDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorDTheme {
                SensorScreen() // ✅ 우리가 만든 센서 수집 UI로 진입
            }
        }
    }
}
