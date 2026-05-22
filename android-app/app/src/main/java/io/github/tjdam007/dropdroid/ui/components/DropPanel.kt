package io.github.tjdam007.dropdroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tjdam007.dropdroid.design.DropElevation
import io.github.tjdam007.dropdroid.design.DropRadius
import io.github.tjdam007.dropdroid.design.DropSpacing

enum class DropPanelVariant {
  Outlined,
  Elevated,
}

@Composable
fun DropPanel(
  modifier: Modifier = Modifier,
  variant: DropPanelVariant = DropPanelVariant.Outlined,
  contentPadding: PaddingValues = PaddingValues(DropSpacing.lg),
  content: @Composable ColumnScope.() -> Unit,
) {
  val border =
    if (variant == DropPanelVariant.Outlined) {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
    } else {
      null
    }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(DropRadius.lg),
    color = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = if (variant == DropPanelVariant.Elevated) DropElevation.raised else DropElevation.flat,
    shadowElevation = if (variant == DropPanelVariant.Elevated) DropElevation.raised else DropElevation.flat,
    border = border,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(contentPadding), content = content)
  }
}
