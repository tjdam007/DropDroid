package io.github.tjdam007.dropdroid.ui.main

import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
  ) {
    Column(
      modifier =
        Modifier
          .verticalScroll(rememberScrollState())
          .padding(bottom = 18.dp),
    ) {
      HeroPanel(state)

      Spacer(Modifier.height(18.dp))
      InfoPanel(state)

      Spacer(Modifier.height(18.dp))
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
          onClick = { ApkDropServer.openInstallPermissionSettings(context) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Allow installs from DropDroid")
        }
      }

      Spacer(Modifier.height(18.dp))
      StatusPanel(state)
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
        Text("Wi-Fi file sharing", color = Color.White.copy(alpha = 0.78f))
      }
    }

    Spacer(Modifier.height(22.dp))
    Text(
      "Keep this phone open and drop any file into the desktop sender.",
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
    InfoRow("Mode", "Local Wi-Fi")
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  MyApplicationTheme {
    MainScreen(
      ReceiverState(
        running = true,
        ipAddress = "192.168.1.42",
        autoOpenApkInstaller = true,
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
