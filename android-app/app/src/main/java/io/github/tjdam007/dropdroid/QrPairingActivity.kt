package io.github.tjdam007.dropdroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import io.github.tjdam007.dropdroid.design.DropRadius
import io.github.tjdam007.dropdroid.design.DropSpacing
import io.github.tjdam007.dropdroid.theme.MyApplicationTheme

class QrPairingActivity : ComponentActivity() {
  private var paired = false
  private var hasCameraPermission by mutableStateOf(false)
  private var cameraDenied by mutableStateOf(false)
  private var scanError by mutableStateOf<String?>(null)

  private val requestCameraPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
      cameraDenied = !granted
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppThemeController.load(this)
    hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    if (!hasCameraPermission) {
      requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    setContent {
      val themePreference by AppThemeController.theme.collectAsStateWithLifecycle()
      MyApplicationTheme(themePreference = themePreference) {
        QrPairingScreen(
          hasCameraPermission = hasCameraPermission,
          cameraDenied = cameraDenied,
          scanError = scanError,
          onCancel = { finish() },
          onOpenSettings = { openAppSettings() },
          onCreatePreview = { previewView -> bindScanner(previewView) },
        )
      }
    }
  }

  private fun bindScanner(previewView: PreviewView) {
    val options =
      BarcodeScannerOptions.Builder()
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
        val rawValue = result?.getValue(scanner)?.firstOrNull()?.rawValue ?: return@MlKitAnalyzer
        runCatching { ApkDropServer.pairWithPortal(this, rawValue) }
          .onSuccess { success ->
            if (success) {
              paired = true
              window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
              finish()
            } else {
              scanError = "Invalid QR. Use the code shown on the DropDroid desktop portal."
            }
          }
          .onFailure {
            scanError = "Could not read this QR. Try scanning the desktop portal again."
          }
      },
    )
    previewView.controller = controller
    controller.bindToLifecycle(this)
  }

  private fun openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrPairingScreen(
  hasCameraPermission: Boolean,
  cameraDenied: Boolean,
  scanError: String?,
  onCancel: () -> Unit,
  onOpenSettings: () -> Unit,
  onCreatePreview: (PreviewView) -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Pair phone") },
        navigationIcon = {
          IconButton(onClick = onCancel) {
            Icon(
              painter = painterResource(R.drawable.ic_lucide_x),
              contentDescription = "Close scanner",
              modifier = Modifier.size(20.dp),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
          ),
      )
    },
  ) { paddingValues ->
    if (hasCameraPermission) {
      ScannerSurface(
        scanError = scanError,
        onCreatePreview = onCreatePreview,
        modifier = Modifier.padding(paddingValues),
      )
    } else {
      CameraPermissionScreen(
        cameraDenied = cameraDenied,
        onCancel = onCancel,
        onOpenSettings = onOpenSettings,
        modifier = Modifier.padding(paddingValues),
      )
    }
  }
}

@Composable
private fun ScannerSurface(
  scanError: String?,
  onCreatePreview: (PreviewView) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        PreviewView(context).apply {
          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          scaleType = PreviewView.ScaleType.FILL_CENTER
          onCreatePreview(this)
        }
      },
    )
    ScannerOverlay(scanError)
  }
}

@Composable
private fun ScannerOverlay(scanError: String?) {
  Box(modifier = Modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val frameWidth = size.width * 0.74f
      val frameTop = size.height * 0.18f
      val left = (size.width - frameWidth) / 2f
      val frameSize = Size(frameWidth, frameWidth)
      drawRoundRect(
        color = Color.White,
        topLeft = Offset(left, frameTop),
        size = frameSize,
        cornerRadius = CornerRadius(32.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
      )
    }
    Column(
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(DropSpacing.lg),
    ) {
      Surface(
        shape = RoundedCornerShape(DropRadius.xl),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DropSpacing.lg)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              painter = painterResource(R.drawable.ic_lucide_qr_code),
              contentDescription = null,
              modifier = Modifier.size(28.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(DropSpacing.md))
            Column {
              Text("Scan desktop QR", style = MaterialTheme.typography.titleLarge)
              Text(
                "Point the camera at the DropDroid portal QR.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          if (scanError != null) {
            Spacer(Modifier.height(DropSpacing.md))
            Text(scanError, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
private fun CameraPermissionScreen(
  cameraDenied: Boolean,
  onCancel: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().padding(DropSpacing.lg), contentAlignment = Alignment.Center) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(DropRadius.xl),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
    ) {
      Column(modifier = Modifier.padding(DropSpacing.lg), verticalArrangement = Arrangement.spacedBy(DropSpacing.md)) {
        Icon(
          painter = painterResource(R.drawable.ic_lucide_qr_code),
          contentDescription = null,
          modifier = Modifier.size(40.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Text("Camera access needed", style = MaterialTheme.typography.titleLarge)
        Text(
          if (cameraDenied) {
            "DropDroid needs the camera to scan the secure pairing QR from your desktop portal."
          } else {
            "Approve camera access to scan the secure pairing QR."
          },
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DropSpacing.md)) {
          OutlinedButton(onClick = onCancel) { Text("Cancel") }
          Button(onClick = onOpenSettings) { Text("Open settings") }
        }
      }
    }
  }
}
