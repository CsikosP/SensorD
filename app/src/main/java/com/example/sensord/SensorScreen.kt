package com.example.sensord.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensord.SensorViewModel
import kotlinx.coroutines.delay

@Composable
fun SensorScreen(sensorViewModel: SensorViewModel = viewModel()) {
    val sensorData by sensorViewModel.sensorDataList.collectAsState()
    val hasUploaded by sensorViewModel.hasUploaded.collectAsState()
    val isCollecting by sensorViewModel.isCollecting.collectAsState() // ✅ ViewModel 상태 사용

    var countdown = remember { mutableIntStateOf(10) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var uploadResult by remember { mutableStateOf<String?>(null) }

    // 10초 타이머
    LaunchedEffect(isCollecting) {
        if (isCollecting) {
            countdown.intValue = 10
            for (i in 10 downTo 1) {
                delay(1000L)
                countdown.intValue = i - 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("센서 데이터 수집 화면", style = MaterialTheme.typography.headlineMedium)

        Text("수집 상태: ${if (isCollecting) "진행 중..." else if (sensorData.size >= 100) "완료됨 ✅" else "대기 중"}")
        Text("남은 시간: ${if (isCollecting) "${countdown.intValue}s" else "-"}")
        Text("수집된 데이터: ${sensorData.size}개")

        // ✅ 수집 시작 버튼
        Button(
            onClick = {
                sensorViewModel.resetState()
                uploadResult = null
                selectedType = null
                sensorViewModel.startCollecting()
            },
            enabled = !isCollecting && (sensorData.size < 100 || hasUploaded)
        ) {
            Text("수집 시작")
        }

        // ✅ 수집 완료 후 UI
        if (sensorData.size >= 100) {
            Text("🚦 사고 여부를 선택하세요")

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RadioButton(
                    selected = selectedType == "accident",
                    onClick = { selectedType = "accident" },
                    enabled = !hasUploaded
                )
                Text("사고")

                RadioButton(
                    selected = selectedType == "non-accident",
                    onClick = { selectedType = "non-accident" },
                    enabled = !hasUploaded
                )
                Text("비사고")
            }

            // ✅ 전송하기 버튼
            Button(
                onClick = {
                    if (selectedType == null) {
                        uploadResult = "❌ 먼저 사고 여부를 선택해주세요"
                    } else {
                        sensorViewModel.uploadData(
                            isCollision = (selectedType == "accident")
                        ) { success, msg ->
                            uploadResult = if (success) "✅ 전송 성공\n$msg" else "❌ 전송 실패\n$msg"
                        }
                    }
                },
                enabled = selectedType != null && !hasUploaded
            ) {
                Text("전송하기")
            }

            if (hasUploaded) {
                Text("📦 이미 전송된 데이터입니다. 새로 수집하려면 '수집 시작'을 다시 누르세요.")
            }

            uploadResult?.let {
                Text(it)
            }
        }
    }
}
