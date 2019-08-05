package me.saket.markdownrenderer.spans

import com.vladsch.flexmark.ast.Heading
import me.saket.markdownrenderer.spans.HeadingLevel.H1
import me.saket.markdownrenderer.spans.HeadingLevel.H2
import me.saket.markdownrenderer.spans.HeadingLevel.H3
import me.saket.markdownrenderer.spans.HeadingLevel.H4
import me.saket.markdownrenderer.spans.HeadingLevel.H5
import me.saket.markdownrenderer.spans.HeadingLevel.H6

/**
 * @param textSizeRatio Taken from HTML5 spec: [http://zuga.net/articles/html-heading-elements/
 */
enum class HeadingLevel(val textSizeRatio: Float) {
  H1(2f),
  H2(1.5f),
  H3(1.17f),
  H4(1f),
  H5(.83f),
  H6(.75f);
}

val Heading.headingLevel
  get() = when (level) {
    1 -> H1
    2 -> H2
    3 -> H3
    4 -> H4
    5 -> H5
    6 -> H6
    else -> throw AssertionError("Unknown level: $level")
  }