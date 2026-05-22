package io.github.tjdam007.dropdroid.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import io.github.tjdam007.dropdroid.design.DropRadius
import io.github.tjdam007.dropdroid.design.DropSpacing
import io.github.tjdam007.dropdroid.theme.MyApplicationTheme
import io.github.tjdam007.dropdroid.ui.components.DropPanel
import io.github.tjdam007.dropdroid.ui.components.DropPanelVariant
import io.github.tjdam007.dropdroid.ui.components.EmptyState
import io.github.tjdam007.dropdroid.ui.components.StatusPill
import io.github.tjdam007.dropdroid.ui.components.StatusVariant
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by ApkDropServer.state.collectAsStateWithLifecycle()
  MainScreen(state = state, modifier = modifier)
}

private enum class AppTab(val label: String, val iconRes: Int) {
  Home("Home", R.drawable.ic_lucide_smartphone),
  Files("Files", R.drawable.ic_lucide_files),
  Settings("Settings", R.drawable.ic_lucide_settings),
}

@Composable
internal fun MainScreen(state: ReceiverState, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home.name) }
  val currentTab = runCatching { AppTab.valueOf(selectedTab) }.getOrElse { AppTab.Home }
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
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        AppTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = selectedTab == tab.name,
            onClick = { selectedTab = tab.name },
            icon = {
              Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.label,
                modifier = Modifier.size(24.dp),
              )
            },
            label = { Text(tab.label) },
          )
        }
      }
    },
  ) { paddingValues ->
    when (currentTab) {
      AppTab.Files ->
        FilesTab(
          files = state.receivedFiles,
          onOpen = { ApkDropServer.openReceivedFile(context, it) },
          modifier =
            Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(paddingValues),
        )
      else ->
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .verticalScroll(rememberScrollState())
              .padding(paddingValues)
              .padding(horizontal = DropSpacing.lg, vertical = DropSpacing.md),
        ) {
          when (currentTab) {
            AppTab.Home ->
              HomeTab(
                state = state,
                onScanQr = { context.startActivity(Intent(context, QrPairingActivity::class.java)) },
                onCopyIp = { ip ->
                  clipboardManager.setText(AnnotatedString(ip))
                  scope.launch { snackbarHostState.showSnackbar("IP copied: $ip") }
                },
              )
            AppTab.Settings ->
              SettingsTab(
                state = state,
                onPickFolder = { folderPicker.launch(null) },
                onUseDefault = { ApkDropServer.resetDestination(context) },
                onAllowApkInstalls = { ApkDropServer.openInstallPermissionSettings(context) },
              )
            AppTab.Files -> Unit
          }
          Spacer(Modifier.height(DropSpacing.bottomNavClearance))
        }
    }
  }
}

@Composable
private fun HomeTab(state: ReceiverState, onScanQr: () -> Unit, onCopyIp: (String) -> Unit) {
  HeroPanel(state)
  Spacer(Modifier.height(DropSpacing.md))
  StatusPanel(state)
  Spacer(Modifier.height(DropSpacing.md))
  PairingPanel(state, onScanQr)
  Spacer(Modifier.height(DropSpacing.md))
  InfoPanel(state, onCopyIp)
}

@Composable
private fun SettingsTab(
  state: ReceiverState,
  onPickFolder: () -> Unit,
  onUseDefault: () -> Unit,
  onAllowApkInstalls: () -> Unit,
) {
  ScreenTitle("Settings", "Match the portal, tune receiving, and view project details.")
  Spacer(Modifier.height(DropSpacing.md))
  ThemePanel()
  Spacer(Modifier.height(DropSpacing.md))
  DestinationPanel(state, onPickFolder, onUseDefault)
  Spacer(Modifier.height(DropSpacing.md))
  ApkHelperPanel(state, onAllowApkInstalls)
  Spacer(Modifier.height(DropSpacing.md))
  AboutPanel()
}

@Composable
private fun FilesTab(files: List<ReceivedFile>, onOpen: (ReceivedFile) -> Unit, modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier,
    contentPadding =
      PaddingValues(
        start = DropSpacing.lg,
        top = DropSpacing.md,
        end = DropSpacing.lg,
        bottom = DropSpacing.bottomNavClearance + DropSpacing.xl,
      ),
    verticalArrangement = Arrangement.spacedBy(DropSpacing.md),
  ) {
    item {
      ScreenTitle("Files", "Open recent transfers with Android apps.")
    }

    if (files.isEmpty()) {
      item {
        DropPanel(variant = DropPanelVariant.Elevated) {
          EmptyState(
            title = "No files yet",
            body = "Received files will appear here as soon as your paired desktop sends them.",
            iconRes = R.drawable.ic_lucide_files,
          )
        }
      }
    } else {
      item {
        Text(
          "${files.size} recent ${if (files.size == 1) "item" else "items"}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      items(files, key = { "${it.uri}-${it.name}-${it.savedAtMillis}" }) { file ->
        ReceivedFileRow(file = file, onOpen = { onOpen(file) })
      }
    }
  }
}

@Composable
private fun HeroPanel(state: ReceiverState) {
  DropPanel(variant = DropPanelVariant.Elevated) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(DropRadius.md))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Text("DD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold)
      }
      Spacer(Modifier.width(DropSpacing.md))
      Column(modifier = Modifier.weight(1f)) {
        Text("DropDroid", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
          if (state.running) "Ready for local transfers" else "Receiver is offline",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    Spacer(Modifier.height(DropSpacing.md))
    Row(horizontalArrangement = Arrangement.spacedBy(DropSpacing.sm)) {
      StatusPill(
        text = if (state.running) "Receiver online" else "Receiver offline",
        variant = if (state.running) StatusVariant.Success else StatusVariant.Error,
      )
      StatusPill(
        text = if (state.isPaired) "Portal paired" else "Pairing needed",
        variant = if (state.isPaired) StatusVariant.Success else StatusVariant.Warning,
      )
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
private fun InfoPanel(state: ReceiverState, onCopyIp: (String) -> Unit) {
  DropPanel {
    PanelTitle("Connection", "Local reachability")
    Spacer(Modifier.height(DropSpacing.lg))
    InfoRow("Device IP", state.ipAddress)
    Spacer(Modifier.height(DropSpacing.md))
    InfoRow("Port", state.port.toString())
    Spacer(Modifier.height(DropSpacing.md))
    InfoRow("Mode", "Local connection")
    Spacer(Modifier.height(DropSpacing.md))
    InfoRow("Pairing", if (state.isPaired) "Linked to your computer" else "Not linked yet")
    Spacer(Modifier.height(DropSpacing.md))
    AddressBlock(state.ipAddresses, onCopyIp)
    Spacer(Modifier.height(DropSpacing.md))
    Text(
      "If desktop discovery picks a bad address or times out, type one of these IPs into Manual IP on the portal.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AddressBlock(addresses: List<String>, onCopyIp: (String) -> Unit) {
  Column {
    Text("Reachable IPs", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(DropSpacing.sm))
    if (addresses.isEmpty()) {
      Text("No local address found", fontWeight = FontWeight.SemiBold)
    } else {
      addresses.forEach { address ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(address, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
          IconButton(onClick = { onCopyIp(address) }) {
            Icon(
              painter = painterResource(R.drawable.ic_lucide_copy),
              contentDescription = "Copy IP address $address",
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DestinationPanel(state: ReceiverState, onPickFolder: () -> Unit, onUseDefault: () -> Unit) {
  DropPanel {
    PanelTitle("Storage", "Save destination")
    Spacer(Modifier.height(DropSpacing.sm))
    Text(state.destinationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(DropSpacing.lg))
    Row(horizontalArrangement = Arrangement.spacedBy(DropSpacing.md)) {
      OutlinedButton(onClick = onPickFolder) {
        IconText(R.drawable.ic_lucide_folder, "Choose folder")
      }
      TextButton(onClick = onUseDefault) {
        IconText(R.drawable.ic_lucide_refresh, "Default")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePanel() {
  val context = LocalContext.current
  val themePreference by AppThemeController.theme.collectAsStateWithLifecycle()
  DropPanel {
    PanelTitle("Theme", "Keep the app aligned with the web portal.")
    Spacer(Modifier.height(DropSpacing.md))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      AppThemePreference.entries.forEachIndexed { index, preference ->
        SegmentedButton(
          selected = themePreference == preference,
          onClick = { AppThemeController.set(context, preference) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = AppThemePreference.entries.size),
          label = { Text(preference.label) },
          icon = {},
        )
      }
    }
    if (themePreference == AppThemePreference.System) {
      Spacer(Modifier.height(DropSpacing.sm))
      Text(
        "System follows your device appearance.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Spacer(Modifier.height(DropSpacing.md))
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
  DropPanel {
    PanelTitle("About", "Open-source local-only sharing.")
    Spacer(Modifier.height(DropSpacing.md))
    InfoRow("Version", BuildConfig.VERSION_NAME)
    Spacer(Modifier.height(DropSpacing.md))
    InfoRow("Build", BuildConfig.VERSION_CODE.toString())
    Spacer(Modifier.height(DropSpacing.lg))
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
  Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
  Spacer(Modifier.width(DropSpacing.sm))
  Text(text)
}

@Composable
private fun FeaturePanel(title: String, body: String, action: @Composable () -> Unit) {
  DropPanel {
    Row(verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(DropSpacing.sm))
      Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.width(DropSpacing.lg))
    action()
    }
  }
}

@Composable
private fun StatusPanel(state: ReceiverState) {
  DropPanel(variant = if (state.isReceiving) DropPanelVariant.Elevated else DropPanelVariant.Outlined) {
    PanelTitle("Receiving", if (state.isReceiving) "Transfer in progress" else "Ready")
    Spacer(Modifier.height(DropSpacing.md))
    if (state.isReceiving) {
      val progressPercent = (state.progress * 100).toInt().coerceIn(0, 100)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(state.receivingFileName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text("$progressPercent%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.height(DropSpacing.md))
      LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(DropSpacing.sm))
      Text(
        "${state.receivingBytes.toReadableSize()} / ${state.receivingTotalBytes.toReadableSize()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(DropSpacing.lg))
      Text(state.lastMessage, style = MaterialTheme.typography.bodyLarge)
    } else {
      EmptyState(
        title = "Ready for files",
        body = "Drop a file in the desktop portal and it will appear here.",
        iconRes = R.drawable.ic_lucide_download,
      )
      if (state.lastFileName.isNotBlank()) {
        Spacer(Modifier.height(DropSpacing.md))
        Text("Last received", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(DropSpacing.xs))
        Text(state.lastFileName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
      }
    }
    Spacer(Modifier.height(DropSpacing.lg))
    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    Spacer(Modifier.height(DropSpacing.md))
    Text(
      "Android will always ask before installing an APK.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ReceivedFileRow(file: ReceivedFile, onOpen: () -> Unit) {
  DropPanel(
    modifier = Modifier.clickable(onClick = onOpen),
    variant = DropPanelVariant.Elevated,
    contentPadding = PaddingValues(DropSpacing.md),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(DropRadius.md))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(fileIconRes(file)),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Spacer(Modifier.width(DropSpacing.md))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            file.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
          )
          Spacer(Modifier.width(DropSpacing.sm))
          StatusPill(text = fileTypeLabel(file), variant = StatusVariant.Neutral)
        }
        Spacer(Modifier.height(DropSpacing.xs))
        Text(
          "${file.sizeBytes.toReadableSize()} • ${file.savedAtLabel()}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(DropSpacing.sm))
      IconButton(onClick = onOpen) {
        Icon(
          painter = painterResource(R.drawable.ic_lucide_external_link),
          contentDescription = "Open ${file.name}",
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

private fun fileIconRes(file: ReceivedFile): Int =
  if (file.mimeType == "application/vnd.android.package-archive" || file.name.endsWith(".apk", ignoreCase = true)) {
    R.drawable.ic_lucide_download
  } else {
    R.drawable.ic_lucide_files
  }

private fun fileTypeLabel(file: ReceivedFile): String =
  when {
    file.mimeType == "application/vnd.android.package-archive" || file.name.endsWith(".apk", ignoreCase = true) -> "APK"
    file.mimeType.startsWith("image/") -> "Image"
    file.mimeType.startsWith("video/") -> "Video"
    file.mimeType.startsWith("audio/") -> "Audio"
    file.mimeType == "application/pdf" || file.name.endsWith(".pdf", ignoreCase = true) -> "PDF"
    else -> "File"
  }

private fun ReceivedFile.savedAtLabel(): String =
  if (savedAtMillis <= 0L) {
    "Just now"
  } else {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(savedAtMillis))
  }

@Composable
private fun PanelTitle(title: String, subtitle: String) {
  Column {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(DropSpacing.xs))
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
