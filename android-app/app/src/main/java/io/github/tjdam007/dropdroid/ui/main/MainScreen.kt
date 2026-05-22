package io.github.tjdam007.dropdroid.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.tjdam007.dropdroid.ApkDropServer
import io.github.tjdam007.dropdroid.AppThemeController
import io.github.tjdam007.dropdroid.AppThemePreference
import io.github.tjdam007.dropdroid.BuildConfig
import io.github.tjdam007.dropdroid.QrPairingActivity
import io.github.tjdam007.dropdroid.R
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

private enum class AppTab(val label: String, val iconRes: Int) {
  Receive("Receive", R.drawable.ic_lucide_download),
  Connect("Connect", R.drawable.ic_lucide_qr_code),
  Files("Files", R.drawable.ic_lucide_files),
  Settings("Settings", R.drawable.ic_lucide_settings),
}

@Composable
internal fun MainScreen(state: ReceiverState, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var selectedTab by rememberSaveable { mutableStateOf(AppTab.Receive.name) }
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

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = selectedTab == tab.name,
            onClick = { selectedTab = tab.name },
            icon = {
              Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = null,
              )
            },
            label = { Text(tab.label) },
          )
        }
      }
    },
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .verticalScroll(rememberScrollState())
          .padding(paddingValues)
          .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      when (AppTab.valueOf(selectedTab)) {
        AppTab.Receive -> ReceiveTab(state)
        AppTab.Connect ->
          ConnectTab(
            state = state,
            onScanQr = { context.startActivity(Intent(context, QrPairingActivity::class.java)) },
          )
        AppTab.Files -> FilesTab(state.receivedFiles, onOpen = { ApkDropServer.openReceivedFile(context, it) })
        AppTab.Settings ->
          SettingsTab(
            state = state,
            onPickFolder = { folderPicker.launch(null) },
            onUseDefault = { ApkDropServer.resetDestination(context) },
            onAllowApkInstalls = { ApkDropServer.openInstallPermissionSettings(context) },
          )
      }
      Spacer(Modifier.height(40.dp))
    }
  }
}

@Composable
private fun ReceiveTab(state: ReceiverState) {
  HeroPanel(state)
  Spacer(Modifier.height(10.dp))
  StatusPanel(state)
}

@Composable
private fun ConnectTab(state: ReceiverState, onScanQr: () -> Unit) {
  ScreenTitle("Connect", "Pair this phone and confirm local reachability.")
  Spacer(Modifier.height(10.dp))
  InfoPanel(state)
  Spacer(Modifier.height(10.dp))
  PairingPanel(state, onScanQr)
}

@Composable
private fun SettingsTab(
  state: ReceiverState,
  onPickFolder: () -> Unit,
  onUseDefault: () -> Unit,
  onAllowApkInstalls: () -> Unit,
) {
  ScreenTitle("Settings", "Match the portal, tune receiving, and view project details.")
  Spacer(Modifier.height(10.dp))
  ThemePanel()
  Spacer(Modifier.height(10.dp))
  DestinationPanel(state, onPickFolder, onUseDefault)
  Spacer(Modifier.height(10.dp))
  ApkHelperPanel(state, onAllowApkInstalls)
  Spacer(Modifier.height(10.dp))
  AboutPanel()
}

@Composable
private fun FilesTab(files: List<ReceivedFile>, onOpen: (ReceivedFile) -> Unit) {
  ScreenTitle("Files", "Open recent transfers with Android apps.")
  Spacer(Modifier.height(10.dp))
  RecentFilesPanel(files, onOpen)
}

@Composable
private fun HeroPanel(state: ReceiverState) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Text("DD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold)
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text("DropDroid", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
          if (state.running) "Ready for local transfers" else "Receiver is offline",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      StatusPill(if (state.running) "Receiver online" else "Receiver offline")
      StatusPill(if (state.isPaired) "Portal paired" else "Pairing needed")
    }
  }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
  Column {
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(4.dp))
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
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
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    PanelTitle("Connection", "Local reachability")
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
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    PanelTitle("Storage", "Save destination")
    Spacer(Modifier.height(8.dp))
    Text(state.destinationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = onPickFolder) {
        IconText(R.drawable.ic_lucide_folder, "Choose folder")
      }
      TextButton(onClick = onUseDefault) {
        IconText(R.drawable.ic_lucide_refresh, "Default")
      }
    }
  }
}

@Composable
private fun ThemePanel() {
  val context = LocalContext.current
  val themePreference by AppThemeController.theme.collectAsStateWithLifecycle()
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    PanelTitle("Theme", "Keep the app aligned with the web portal.")
    Spacer(Modifier.height(12.dp))
    AppThemePreference.entries.forEach { preference ->
      SettingChoice(
        title = preference.label,
        selected = themePreference == preference,
        onClick = { AppThemeController.set(context, preference) },
      )
    }
  }
}

@Composable
private fun PairingPanel(state: ReceiverState, onScanQr: () -> Unit) {
  FeaturePanel(
    title = if (state.isPaired) "Secure portal paired" else "Pair secure portal",
    body = if (state.isPaired) "Only the paired portal can send files to this phone." else "Scan the QR shown on the desktop portal before receiving files.",
    action = {
      Button(onClick = onScanQr) {
        IconText(R.drawable.ic_lucide_qr_code, if (state.isPaired) "Rescan" else "Scan QR")
      }
    },
  )
}

@Composable
private fun ApkHelperPanel(state: ReceiverState, onAllowApkInstalls: () -> Unit) {
  val context = LocalContext.current
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
      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
      IconText(R.drawable.ic_lucide_external_link, "Allow APK installs")
    }
  }
}

@Composable
private fun AboutPanel() {
  val context = LocalContext.current
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    PanelTitle("About", "Open-source local-only sharing.")
    Spacer(Modifier.height(12.dp))
    InfoRow("Version", BuildConfig.VERSION_NAME)
    Spacer(Modifier.height(12.dp))
    InfoRow("Build", BuildConfig.VERSION_CODE.toString())
    Spacer(Modifier.height(14.dp))
    OutlinedButton(
      onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tjdam007/DropDroid")))
      },
      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
      IconText(R.drawable.ic_lucide_external_link, "View GitHub repository")
    }
  }
}

@Composable
private fun SettingChoice(title: String, selected: Boolean, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, fontWeight = FontWeight.SemiBold)
    Box(
      modifier =
        Modifier
          .size(22.dp)
          .clip(RoundedCornerShape(999.dp))
          .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
          .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
    )
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
    Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
  }
}

@Composable
private fun IconText(iconRes: Int, text: String) {
  Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
  Spacer(Modifier.width(8.dp))
  Text(text)
}

@Composable
private fun FeaturePanel(title: String, body: String, action: @Composable () -> Unit) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
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
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    PanelTitle("Receiving", if (state.isReceiving) "Transfer in progress" else "Waiting for files")
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
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .padding(14.dp),
  ) {
    PanelTitle("Files", "Recent received items")
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
            .padding(vertical = 10.dp),
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
private fun PanelTitle(title: String, subtitle: String) {
  Column {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun StatusPill(text: String) {
  Box(
    modifier =
      Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
        .padding(horizontal = 14.dp, vertical = 7.dp),
  ) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
