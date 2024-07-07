package com.example.camera_book

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera_book.ui.theme.Camera_BookTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>

    private var lastPictureBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TextToSpeech 초기화
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.KOREAN
                textToSpeech.speak("찰칵백과가 정상적으로 실행되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        // 사진 촬영 결과 처리 런처 초기화
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                lastPictureBitmap = result.data?.extras?.get("data") as Bitmap?
                lastPictureBitmap?.let {
                    textToSpeech.speak("사진이 정상적으로 촬영되었습니다. 사진을 다시 찍으시겠습니까? 사진을 다시 찍으시려면 네 또는 응 이라고 대답해 주십시오.", TextToSpeech.QUEUE_FLUSH, null, null)
                    startSpeechRecognition()
                }
                setContent {
                    Camera_BookTheme {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            CameraApp(lastPictureBitmap, modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }

        // 권한 요청 런처 초기화
        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // 음성 인식 런처 초기화
        speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                matches?.let {
                    if (it.contains("네") || it.contains("응")) { // 사용자가 긍정적인 응답을 한 경우
                        launchCamera()
                    }
                }
            }
        }

        // 앱 시작 시 카메라 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            Camera_BookTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraApp(lastPictureBitmap, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizerLauncher.launch(intent)
    }

    override fun onDestroy() {
        // TextToSpeech 객체 해제
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun CameraApp(imageBitmap: Bitmap?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        imageBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(300.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraAppPreview() {
    Camera_BookTheme {
        CameraApp(null)
    }
}

