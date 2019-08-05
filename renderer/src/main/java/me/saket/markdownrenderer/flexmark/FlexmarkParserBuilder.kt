package me.saket.markdownrenderer.flexmark

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.parser.LinkRefProcessorFactory
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataSet

/**
 * Wrapper around [Parser] because its builder doesn't allow updating [Parser.options].
 */
class FlexmarkParserBuilder {

  private val extensions = ArrayList<Extension>()
  private val options = MutableDataSet()
  private var delimiterProcessor: DelimiterProcessor? = null
  private var linkRefProcessorFactory: LinkRefProcessorFactory? = null

  init {
    // TODO: move this elsewhere.
    FlexmarkParserBuilder()
        .addExtension(StrikethroughExtension.create())
        .build()
  }

  fun addExtension(extension: Extension): FlexmarkParserBuilder {
    extensions += extension
    return this
  }

  fun <T> addOption(key: DataKey<T>, value: T) {
    options.set(key, value)
  }

  fun delimiterProcessor(processor: DelimiterProcessor?) {
    this.delimiterProcessor = processor
  }

  fun build(): Parser {
    return Parser.builder(options)
        .extensions(extensions)
        .apply {
          if (delimiterProcessor != null) {
            customDelimiterProcessor(delimiterProcessor)
          }
          if (linkRefProcessorFactory != null) {
            linkRefProcessorFactory(linkRefProcessorFactory)
          }
        }
        .build()
  }
}