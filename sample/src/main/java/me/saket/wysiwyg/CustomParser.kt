package me.saket.wysiwyg

//class CustomParser(
//  private val pool: CustomPool
//) : MarkdownParser {
//
//  override fun parseSpans(text: Spannable): SpanWriter {
//    //pool.inlineCode()
//    return SpanWriter()
//  }
//
//  override fun removeSpans(text: Spannable) = Unit
//}
//
//class CustomPool : MarkdownSpanPool() {
//
//  override fun italics(): Any {
//    return CustomSpan()
//  }
//
//  override fun indentedCodeBlock(markwonTheme: MarkwonTheme): Any {
//    return super.indentedCodeBlock(markwonTheme)
//  }
//
//  override fun recycle(span: Any) {
//    when (span) {
//      is CustomSpan -> {
//
//      }
//      else -> super.recycle(span)
//    }
//  }
//}
//
//class CustomSpan : LineHeightSpan {
//  override fun chooseHeight(
//    text: CharSequence?,
//    start: Int,
//    end: Int,
//    spanstartv: Int,
//    lineHeight: Int,
//    fm: FontMetricsInt?
//  ) {
//    TODO()
//  }
//}