package me.saket.markdownrenderer.spans

import ru.noties.markwon.spans.HeadingSpan
import ru.noties.markwon.spans.SpannableTheme

class HeadingSpanWithLevel(theme: SpannableTheme, val level: Int) : HeadingSpan(theme, level)
