package me.saket.wysiwyg.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.highlight.LocalTextRange
import me.saket.wysiwyg.highlight.MarkdownChildNode
import me.saket.wysiwyg.highlight.MarkdownDocument
import me.saket.wysiwyg.highlight.TextChangeListSnapshot
import me.saket.wysiwyg.highlight.rebased

@JvmInline
internal value class MarkdownRenderer(
  private val theme: WysiwygTheme,
) {
  fun buildAnnotatedString(text: AnnotatedString, document: MarkdownDocument): AnnotatedString {
    val scope = RealMarkdownNodeRenderScope(
      theme = theme,
      unstyledText = text,
      changes = document.changes,
      offsetInRoot = 0,
      addTestTags = addTestTags,
    )
    return buildAnnotatedString {
      // Discard any previous styles that may have gotten restored after a config change.
      // This is slightly unfortunate because any spans added by user will also be discarded.
      append(text.text)
      with(document) {
        scope.render(text = this@buildAnnotatedString)
      }
    }
  }
}

// todo: kdoc
interface MarkdownNodeRenderScope {
  val theme: WysiwygTheme
  val unstyledText: AnnotatedString

  // todo: kdoc
  val changes: List<TextChangeListSnapshot>

  // todo: kdoc
  val offsetInRoot: Int

  // todo: kdoc
  fun childScope(child: MarkdownChildNode): MarkdownNodeRenderScope

  // todo: kdoc
  fun LocalTextRange.resolve(): TextRange? {
    val rangeInRoot = TextRange(
      start = textRange.start + offsetInRoot,
      end = textRange.end + offsetInRoot,
    )
    return rangeInRoot.rebased(changes)
  }

  // todo: kdoc
  fun MarkdownChildNode.render(text: AnnotatedString.Builder) {
    val childScope = childScope(this)
    with(node) {
      childScope.render(text)
    }
  }

  fun AnnotatedString.Builder.addStyle(style: SpanStyle, range: TextRange) {
    addStyle(
      style = style,
      start = range.start.coerceAtMost(unstyledText.lastIndex),
      end = range.end.coerceAtMost(length),
    )
  }

  fun AnnotatedString.Builder.addStyle(style: ParagraphStyle, range: TextRange) {
    addStyle(
      style = style,
      start = range.start.coerceAtMost(unstyledText.lastIndex),
      end = range.end.coerceAtMost(length),
    )
    // Compose UI adds a lot of vertical paddings around paragraphs.
    // Reduce the font size of line breaks to make them smaller.
    // https://issuetracker.google.com/u/1/issues/241426911
    if (unstyledText.getOrNull(range.start - 1) == '\n') {
      addStyle(SpanStyle(fontSize = 1.sp), start = range.start - 1, end = range.start)
    }
    if (unstyledText.getOrNull(range.end) == '\n') {
      addStyle(
        SpanStyle(fontSize = 1.sp),
        start = range.end - 1,
        end = range.end + 1,
      )
    }
  }
}

private data class RealMarkdownNodeRenderScope(
  override val theme: WysiwygTheme,
  override val unstyledText: AnnotatedString,
  override val changes: List<TextChangeListSnapshot>,
  override val offsetInRoot: Int,
) : MarkdownNodeRenderScope {

  override fun childScope(child: MarkdownChildNode): MarkdownNodeRenderScope {
    return RealMarkdownNodeRenderScope(
      theme = theme,
      unstyledText = unstyledText,
      changes = changes,
      offsetInRoot = offsetInRoot + child.offsetInParent,
    )
  }
}
