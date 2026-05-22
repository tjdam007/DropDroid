package io.github.tjdam007.dropdroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions

class QrPairingActivity : ComponentActivity() {
  private var paired = false

  private val requestCameraPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) startScanner() else finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      startScanner()
    } else {
      requestCameraPermission.launch(Manifest.permission.CAMERA)
    }
  }

  private fun startScanner() {
    val previewView = PreviewView(this)
    val hint =
      TextView(this).apply {
        text = "Scan the DropDroid pairing QR"
        setTextColor(android.graphics.Color.WHITE)
        textSize = 18f
        setPadding(32, 32, 32, 32)
        setBackgroundColor(0x88000000.toInt())
      }
    val root = FrameLayout(this).apply {
      addView(previewView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
      addView(hint, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
    }
    setContentView(root)

    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
      .build()
    val scanner = BarcodeScanning.getClient(options)
    val controller = LifecycleCameraController(this)
    controller.setImageAnalysisAnalyzer(
      ContextCompat.getMainExecutor(this),
      MlKitAnalyzer(
        listOf(scanner),
        CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
        ContextCompat.getMainExecutor(this),
      ) { result ->
        if (paired) return@MlKitAnalyzer
        val barcode = result?.getValue(scanner)?.firstOrNull()
        val rawValue = barcode?.rawValue ?: return@MlKitAnalyzer
        runCatching { ApkDropServer.pairWithPortal(this, rawValue) }
          .onSuccess { success ->
            if (success) {
              paired = true
              Toast.makeText(this, "DropDroid paired", Toast.LENGTH_SHORT).show()
              finish()
            }
          }
      },
    )
    previewView.controller = controller
    controller.bindToLifecycle(this)
  }
}
