package io.github.tjdam007.dropdroid.ui.main

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.tjdam007.dropdroid.ApkDropServer
import io.github.tjdam007.dropdroid.QrPairingActivity
import io.github.tjdam007.dropdroid.ReceivedFile
import io.github.tjdam007.dropdroid.ReceiverState
import io.github.tjdam007.dropdroid.theme.MyApplicationTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by ApkDropServer.state.collectAsStateWithLifecycle()
  MainScreen(state = state, modifier = modifier)
}

@Composable
internal fun MainScreen(state: ReceiverState, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val folderPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        ApkDropServer.setDestinationTree(context, uri)
      }
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
  ) {
    BoxWithConstraints(
      modifier =
        Modifier
          .verticalScroll(rememberScrollState())
          .padding(bottom = 18.dp),
    ) {
      if (maxWidth >= 720.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            HeroPanel(state)

            Spacer(Modifier.height(18.dp))
            InfoPanel(state)

            Spacer(Modifier.height(18.dp))
            DestinationPanel(
              state = state,
              onPickFolder = { folderPicker.launch(null) },
              onUseDefault = { ApkDropServer.resetDestination(context) },
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            ControlsSection(
              state = state,
              onScanQr = { context.startActivity(Intent(context, QrPairingActivity::class.java)) },
              onAllowApkInstalls = { ApkDropServer.openInstallPermissionSettings(context) },
            )

            Spacer(Modifier.height(18.dp))
            StatusPanel(state)

            Spacer(Modifier.height(18.dp))
            RecentFilesPanel(state.receivedFiles, onOpen = { ApkDropServer.openReceivedFile(context, it) })
          }
        }
      } else {
        Column {
          HeroPanel(state)

          Spacer(Modifier.height(18.dp))
          InfoPanel(state)

          Spacer(Modifier.height(18.dp))
          DestinationPanel(
            state = state,
            onPickFolder = { folderPicker.launch(null) },
            onUseDefault = { ApkDropServer.resetDestination(context) },
          )

          Spacer(Modifier.height(18.dp))
          ControlsSection(
            state = state,
            onScanQr = { context.startActivity(Intent(context, QrPairingActivity::class.java)) },
            onAllowApkInstalls = { ApkDropServer.openInstallPermissionSettings(context) },
          )

          Spacer(Modifier.height(18.dp))
          StatusPanel(state)

          Spacer(Modifier.height(18.dp))
          RecentFilesPanel(state.receivedFiles, onOpen = { ApkDropServer.openReceivedFile(context, it) })
        }
      }
    }
  }
}

@Composable
private fun HeroPanel(state: ReceiverState) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.primary)
        .padding(22.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Text("DD", color = Color.White, fontWeight = FontWeight.ExtraBold)
      }
      Spacer(Modifier.width(14.dp))
      Column {
        Text("DropDroid", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("Local-only file sharing", color = Color.White.copy(alpha = 0.78f))
      }
    }

    Spacer(Modifier.height(22.dp))
    Text(
      "Keep this phone open and on the same local connection as the desktop sender.",
      style = MaterialTheme.typography.titleLarge,
      color = Color.White,
    )
    Spacer(Modifier.height(12.dp))
    StatusPill(if (state.running) "Receiver online" else "Receiver offline")
  }
}

@Composable
private fun InfoPanel(state: ReceiverState) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(18.dp),
  ) {
    Text("Connection", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(14.dp))
    InfoRow("Device IP", state.ipAddress)
    Spacer(Modifier.height(12.dp))
    InfoRow("Port", state.port.toString())
    Spacer(Modifier.height(12.dp))
    InfoRow("Mode", "Local connection")
    Spacer(Modifier.height(12.dp))
    InfoRow("Pairing", if (state.isPaired) "Required + active" else "Required")
    Spacer(Modifier.height(12.dp))
    AddressBlock(state.ipAddresses)
    Spacer(Modifier.height(12.dp))
    Text(
      "If desktop discovery picks a bad address or times out, type one of these IPs into Manual IP on the portal.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AddressBlock(addresses: List<String>) {
  Column {
    Text("Reachable IPs", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    if (addresses.isEmpty()) {
      Text("No local address found", fontWeight = FontWeight.SemiBold)
    } else {
      addresses.forEach { address ->
        Text(address, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}

@Composable
private fun DestinationPanel(state: ReceiverState, onPickFolder: () -> Unit, onUseDefault: () -> Unit) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(18.dp),
  ) {
    Text("Save location", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(state.destinationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = onPickFolder) {
        Text("Choose folder")
      }
      TextButton(onClick = onUseDefault) {
        Text("Use default")
      }
    }
  }
}

@Composable
private fun ControlsSection(state: ReceiverState, onScanQr: () -> Unit, onAllowApkInstalls: () -> Unit) {
  val context = LocalContext.current
  Column {
    FeaturePanel(
      title = if (state.isPaired) "Secure portal paired" else "Pair secure portal",
      body = if (state.isPaired) "Only the paired portal can send files to this phone." else "Scan the QR shown on the desktop portal before receiving files.",
      action = {
        Button(onClick = onScanQr) {
          Text(if (state.isPaired) "Rescan" else "Scan QR")
        }
      },
    )

    Spacer(Modifier.height(10.dp))
    FeaturePanel(
      title = "APK install helper",
      body = "Share any file normally. When the file is an APK, this toggle opens Android's installer after the transfer.",
      action = {
        Switch(
          checked = state.autoOpenApkInstaller,
          onCheckedChange = { ApkDropServer.setAutoOpenApkInstaller(context, it) },
        )
      },
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Spacer(Modifier.height(10.dp))
      OutlinedButton(
        onClick = onAllowApkInstalls,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Allow installs from DropDroid")
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun FeaturePanel(title: String, body: String, action: @Composable () -> Unit) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(18.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(6.dp))
      Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.width(16.dp))
    action()
  }
}

@Composable
private fun StatusPanel(state: ReceiverState) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(18.dp),
  ) {
    Text("Latest transfer", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    if (state.isReceiving) {
      Text(state.receivingFileName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(10.dp))
      LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      Text(
        "${state.receivingBytes.toReadableSize()} / ${state.receivingTotalBytes.toReadableSize()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(14.dp))
    }
    Text(state.lastMessage, style = MaterialTheme.typography.bodyLarge)
    if (state.lastFileName.isNotBlank()) {
      Spacer(Modifier.height(8.dp))
      Text(state.lastFileName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    Spacer(Modifier.height(12.dp))
    Text(
      "Android will always ask before installing an APK.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun RecentFilesPanel(files: List<ReceivedFile>, onOpen: (ReceivedFile) -> Unit) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(18.dp),
  ) {
    Text("Recent files", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    if (files.isEmpty()) {
      Text("Received files will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      return@Column
    }
    files.forEachIndexed { index, file ->
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen(file) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(file.name, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.height(2.dp))
          Text(file.sizeBytes.toReadableSize(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Open", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }
      if (index != files.lastIndex) {
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
      }
    }
  }
}

@Composable
private fun StatusPill(text: String) {
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.16f))
        .padding(horizontal = 14.dp, vertical = 8.dp),
  ) {
    Text(text, color = Color.White, fontWeight = FontWeight.Bold)
  }
}

private fun Long.toReadableSize(): String {
  if (this < 1024) return "$this B"
  val units = listOf("KB", "MB", "GB")
  var value = this.toDouble()
  var unit = "B"
  for (next in units) {
    value /= 1024.0
    unit = next
    if (value < 1024.0) break
  }
  return "%.1f %s".format(java.util.Locale.US, value, unit)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MyApplicationTheme {
    MainScreen(
      ReceiverState(
        running = true,
        ipAddress = "192.168.1.42",
        autoOpenApkInstaller = true,
        receivedFiles = listOf(ReceivedFile("sample.pdf", "content://sample", "application/pdf", 2448000, 0)),
        lastFileName = "sample.apk",
        lastMessage = "Received sample.apk (24.1 MB)",
      ),
    )
  }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  MyApplicationTheme { MainScreen(ReceiverState(running = true, ipAddress = "192.168.1.42")) }
}
