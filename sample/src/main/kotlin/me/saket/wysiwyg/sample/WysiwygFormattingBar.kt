package me.saket.wysiwyg.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FormatBold
import androidx.compose.material.icons.twotone.FormatItalic
import androidx.compose.material.icons.twotone.FormatQuote
import androidx.compose.material.icons.twotone.Tag
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import me.saket.wysiwyg.Wysiwyg
import me.saket.wysiwyg.format.insertBlockQuoteSyntax
import me.saket.wysiwyg.format.insertBoldSyntax
import me.saket.wysiwyg.format.insertHeadingSyntax
import me.saket.wysiwyg.format.insertItalicSyntax

@Composable
fun MarkdownFormattingBar(
  wysiwyg: Wysiwyg,
  modifier: Modifier = Modifier
) {
  Row(
    modifier
      .clipToBounds()
      .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
      .horizontalScroll(rememberScrollState())
  ) {
    FormatButton(
      icon = Icons.TwoTone.FormatBold,
      contentDescription = "Insert bold markdown",
      onClick = { wysiwyg.insertBoldSyntax() }
    )
    FormatButton(
      icon = Icons.TwoTone.FormatItalic,
      contentDescription = "Insert italic markdown",
      onClick = { wysiwyg.insertItalicSyntax() }
    )
    FormatButton(
      icon = Icons.TwoTone.FormatQuote,
      contentDescription = "Insert block quote",
      onClick = { wysiwyg.insertBlockQuoteSyntax() }
    )
    FormatButton(
      icon = Icons.TwoTone.Tag,
      contentDescription = "Insert heading markdown",
      onClick = { wysiwyg.insertHeadingSyntax() }
    )
  }
}

@Composable
fun FormatButton(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String
) {
  Box(
    Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = false),
        onClick = onClick
      )
      .padding(12.dp)
      .size(24.dp)
  ) {
    Image(
      modifier = Modifier.matchParentSize(),
      painter = rememberVectorPainter(icon),
      contentDescription = contentDescription,
      colorFilter = ColorFilter.tint(LocalContentColor.current)
    )
  }
}
