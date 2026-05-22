package io.github.tjdam007.dropdroid.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun EmptyState(
  title: String,
  body: String,
  @DrawableRes iconRes: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(vertical = DropSpacing.sm),
    horizontalAlignment = Alignment.Start,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = null,
      modifier =
        Modifier
          .size(DropSpacing.xxl)
          .clip(RoundedCornerShape(DropRadius.md))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
          .padding(DropSpacing.sm),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(DropSpacing.md))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(DropSpacing.xs))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
