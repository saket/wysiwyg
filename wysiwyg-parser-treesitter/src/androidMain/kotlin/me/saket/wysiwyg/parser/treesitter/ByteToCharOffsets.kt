package me.saket.wysiwyg.parser.treesitter

import de.cketti.codepoints.deluxe.CodePoint
import de.cketti.codepoints.deluxe.codePointSequence

/**
 * Pre-computed byte → char offset table for converting tree-sitter's UTF-8 byte offsets
 * into Kotlin `String` / Compose `AnnotatedString` UTF-16 char offsets.
 *
 * Tree-sitter only reports byte offsets at codepoint boundaries (the grammar never splits
 * a multi-byte character), so intermediate bytes inside a multi-byte codepoint don't need
 * unique entries, the table simply repeats the preceding codepoint's char index for them,
 * which also gives a graceful fallback if an unexpected offset lands mid-codepoint.
 */
internal class ByteToCharOffsets(text: String) {
  // null when [text] is pure ASCII.
  private val table: IntArray?

  init {
    val utf8Length = text.codePointSequence().sumOf { it.utf8ByteLength() }
    if (utf8Length == text.length) {
      table = null
    } else {
      val built = IntArray(utf8Length + 1)
      var byteIndex = 0
      var charIndex = 0
      for (codePoint in text.codePointSequence()) {
        val byteLength = codePoint.utf8ByteLength()
        built.fill(charIndex, fromIndex = byteIndex, toIndex = byteIndex + byteLength)
        byteIndex += byteLength
        charIndex += codePoint.charCount
      }
      built[byteIndex] = charIndex
      table = built
    }
  }

  fun byteToChar(byteOffset: Int): Int =
    table?.get(byteOffset) ?: byteOffset
}

/**
 * UTF-8 byte length of a single Unicode codepoint, per RFC 3629.
 *  - `U+0000..U+007F`    → 1 byte (ASCII)
 *  - `U+0080..U+07FF`    → 2 bytes
 *  - `U+0800..U+FFFF`    → 3 bytes (rest of BMP)
 *  - `U+10000..U+10FFFF` → 4 bytes (supplementary planes)
 */
private fun CodePoint.utf8ByteLength(): Int = when {
  isSupplementary -> 4
  value < 0x80 -> 1
  value < 0x800 -> 2
  else -> 3
}
