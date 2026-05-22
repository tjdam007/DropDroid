package io.github.tjdam007.dropdroid.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import io.github.tjdam007.dropdroid.design.DropRadius
import io.github.tjdam007.dropdroid.design.DropSpacing

enum class StatusVariant {
  Success,
  Warning,
  Error,
  Neutral,
}

@Composable
fun StatusPill(
  text: String,
  variant: StatusVariant,
  modifier: Modifier = Modifier,
  @DrawableRes iconRes: Int? = null,
) {
  val colorScheme = MaterialTheme.colorScheme
  val container =
    when (variant) {
      StatusVariant.Success -> colorScheme.primary.copy(alpha = 0.14f)
      StatusVariant.Warning -> colorScheme.secondary.copy(alpha = 0.18f)
      StatusVariant.Error -> colorScheme.errorContainer
      StatusVariant.Neutral -> colorScheme.surfaceVariant
    }
  val content =
    when (variant) {
      StatusVariant.Success -> colorScheme.primary
      StatusVariant.Warning -> colorScheme.secondary
      StatusVariant.Error -> colorScheme.onErrorContainer
      StatusVariant.Neutral -> colorScheme.onSurfaceVariant
    }

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(DropRadius.pill))
        .background(container)
        .padding(horizontal = DropSpacing.lg, vertical = DropSpacing.sm),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (iconRes != null) {
        Icon(
          painter = painterResource(iconRes),
          contentDescription = null,
          modifier = Modifier.size(DropSpacing.lg),
          tint = content,
        )
        Spacer(Modifier.width(DropSpacing.sm))
      }
      Text(text, color = content, fontWeight = FontWeight.Bold)
    }
  }
}
