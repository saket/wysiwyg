package me.saket.markdownrenderer.flexmark.stylers

import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.ThematicBreakSpan
import me.saket.markdownrenderer.spans.ThematicBreakSpan.Mode
import me.saket.markdownrenderer.spans.ThematicBreakSpan.Mode.ASTERISKS
import me.saket.markdownrenderer.spans.ThematicBreakSpan.Mode.HYPHENS
import me.saket.markdownrenderer.spans.ThematicBreakSpan.Mode.UNDERSCORES
import me.saket.markdownrenderer.spans.pool.Recycler
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

class ThematicBreakVisitor : NodeVisitor<ThematicBreak> {

  private val thematicBreakSpansPool = ThematicSpanPool()

  override fun visit(
    node: ThematicBreak,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.foregroundColor(theme.syntaxColor), node.startOffset, node.endOffset)

    val thematicBreakSyntax = node.chars
    val clashesWithBoldSyntax = FOUR_ASTERISKS_HORIZONTAL_RULE == thematicBreakSyntax
    if (clashesWithBoldSyntax) {
      return
    }

    val ruleMode = when (thematicBreakSyntax[0]) {
      '*' -> ASTERISKS
      '-' -> HYPHENS
      '_' -> UNDERSCORES
      else -> throw UnsupportedOperationException(
          "Unknown thematic break mode: $thematicBreakSyntax"
      )
    }

    // Caching mutable BasedSequence isn't a good idea.
    val immutableThematicBreakChars = thematicBreakSyntax.toString()

    val hrSpan = thematicBreakSpansPool.get(
        theme = theme,
        syntax = immutableThematicBreakChars,
        mode = ruleMode
    )
    writer.add(hrSpan, node.startOffset, node.endOffset)
  }

  companion object {
    private val FOUR_ASTERISKS_HORIZONTAL_RULE = SubSequence.of("****")
  }
}

internal class ThematicSpanPool {
  private val pool = mutableMapOf<String, ThematicBreakSpan>()

  private val recycler: Recycler = { span ->
    require(span is ThematicBreakSpan)
    pool[recyclingKey(span)] = span
  }

  /**
   * @param syntax See [ThematicBreakSpan.syntax].
   */
  internal fun get(
    theme: WysiwygTheme,
    syntax: CharSequence,
    mode: Mode
  ): ThematicBreakSpan {
    val key = recyclingKey(syntax, mode)
    return pool.remove(key) ?: ThematicBreakSpan(theme, syntax, mode, recycler)
  }

  private fun recyclingKey(span: ThematicBreakSpan) = recyclingKey(span.syntax, span.mode)

  private fun recyclingKey(syntax: CharSequence, mode: Mode) = "${syntax}_$mode"
}