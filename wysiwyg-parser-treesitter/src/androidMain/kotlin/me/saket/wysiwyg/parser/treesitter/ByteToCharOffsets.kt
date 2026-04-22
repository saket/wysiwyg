package me.saket.wysiwyg.parser.treesitter

import de.cketti.codepoints.deluxe.CodePoint
import de.cketti.codepoints.deluxe.codePointSequence

/**
 * Pre-computed conversion tables between tree-sitter's UTF-8 byte offsets and Kotlin `String`
 * / Compose `AnnotatedString` UTF-16 char offsets. Exposes both [byteToChar] (used when mapping
 * parsed node ranges back into span coordinates) and [charToByte] (used when converting
 * Compose's `ChangeListSnapshot` into tree-sitter `InputEdit`s).
 *
 * Tree-sitter only reports byte offsets at codepoint boundaries — the grammar never splits a
 * multi-byte character — so intermediate bytes inside a multi-byte codepoint don't need unique
 * entries, the table simply repeats the preceding codepoint's char index for them. Same trick
 * for UTF-16 surrogate pairs: both halves of a pair map to the same byte index. This also
 * gives a graceful fallback if an unexpected offset lands mid-codepoint or mid-surrogate.
 *
 * Pure-ASCII text skips both tables entirely — byte and char offsets match 1:1, so lookups
 * pass the input through unchanged.
 */
internal class ByteToCharOffsets(text: String) {
  // Both null on the ASCII fast path.
  private val byteTable: IntArray?
  private val charTable: IntArray?

  init {
    if (text.isPureAscii()) {
      byteTable = null
      charTable = null
    } else {
      // Worst case: a 3-byte UTF-8 codepoint (e.g., CJK) occupies one UTF-16 code unit,
      // so max UTF-8 length is text.length * 3. Supplementary planes are 4 bytes / 2 chars
      // which is a looser ratio. We oversize by this worst case to fill in one pass, then
      // copyOf() to the right size at the end.
      val scratchByteTable = IntArray(text.length * 3 + 1)
      val charTableBuilt = IntArray(text.length + 1)
      var byteIndex = 0
      var charIndex = 0
      for (codePoint in text.codePointSequence()) {
        val byteLength = codePoint.utf8ByteLength()
        val charCount = codePoint.charCount
        // byte→char: every byte inside a multi-byte codepoint maps to the codepoint's char index.
        scratchByteTable.fill(charIndex, fromIndex = byteIndex, toIndex = byteIndex + byteLength)
        // char→byte: both halves of a surrogate pair map to the same starting byte index.
        charTableBuilt.fill(byteIndex, fromIndex = charIndex, toIndex = charIndex + charCount)
        byteIndex += byteLength
        charIndex += charCount
      }
      scratchByteTable[byteIndex] = charIndex
      charTableBuilt[charIndex] = byteIndex
      byteTable = scratchByteTable.copyOf(byteIndex + 1)
      charTable = charTableBuilt
    }
  }

  fun byteToChar(byteOffset: Int): Int =
    byteTable?.get(byteOffset) ?: byteOffset

  fun charToByte(charOffset: Int): Int =
    charTable?.get(charOffset) ?: charOffset
}

/**
 * Fast pre-check that avoids entering the full codepoint walk when every UTF-16 code unit
 * is below U+0080. Operates directly on the `String`'s char array — no codepoint decoding,
 * no allocation.
 */
private fun String.isPureAscii(): Boolean {
  for (i in 0 until length) {
    if (this[i].code >= 0x80) return false
  }
  return true
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
