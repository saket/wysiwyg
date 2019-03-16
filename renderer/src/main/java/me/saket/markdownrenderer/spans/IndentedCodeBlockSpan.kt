package me.saket.markdownrenderer.spans

import ru.noties.markwon.spans.CodeSpan
import ru.noties.markwon.spans.SpannableTheme

class IndentedCodeBlockSpan(theme: SpannableTheme) : CodeSpan(theme, true)
