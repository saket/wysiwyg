# Simplest Parser-Agnostic Partial Reparsing Plan

## Summary

Add partial reparsing as a `MarkdownParser` decorator, not as Flexmark-specific logic. The wrapper keeps the last parsed document, computes a conservative line-based dirty window for each edit, asks the delegate parser to parse only that substring, splices the parsed nodes into the previous document, and falls back to a full delegate parse whenever the window is unsafe.

No public API changes are required. `FlexmarkMarkdownParser` remains a normal full-document parser; partial reparsing sits above any parser that already implements `MarkdownParser`.

## Key Changes

- Add an internal wrapper, tentatively `PartialMarkdownParser(delegate: MarkdownParser) : MarkdownParser`.
- Wrap user-provided parsers as `IncrementalMarkdownParser(PartialMarkdownParser(parser))`.
- Keep a snapshot after every accepted parse:
  - previous full text
  - previous `MarkdownDocument`
  - top-level child ranges in absolute coordinates
- Extract reusable helpers for:
  - building top-level absolute ranges from `MarkdownDocument.children`
  - computing a dirty window from `TextChangeListSnapshot`
  - shifting `MarkdownChildNode.offsetInParent` for spliced nodes
  - validating the rebuilt document
- Keep the delegate parser as the only Markdown parser. For partial parsing, call `delegate.parse(substring, changes = TextChangeListSnapshot.Empty)` and treat the returned document as local to the substring.

## Dirty Window Rules

- Accept only one contiguous edit. Multiple disjoint edits fall back to full parse.
- Expand both old and new edit ranges to whole line boundaries.
- Find previous top-level nodes whose absolute ranges overlap the old dirty lines.
- The dirty window is the union of:
  - touched full lines
  - touched previous top-level nodes
  - one neighboring top-level node before and after the touched region
- The neighboring-node padding is the simple mechanism for Markdown spillover. It handles common paragraph split/merge cases without trying to fully understand Markdown grammar.
- Fall back to full parse when:
  - old/new window anchors cannot be rebased cleanly
  - the edit touches a fenced-code marker line
  - the computed window exceeds a fixed threshold, such as 25% of the document or 20,000 chars
  - spliced children fail validation

## Splicing

- Reuse previous top-level children fully before `oldWindow`.
- Drop previous top-level children overlapping `oldWindow`.
- Parse `newText.substring(newWindow.start, newWindow.end)` using the delegate parser.
- Insert parsed substring children after shifting each top-level `offsetInParent` by `newWindow.start`.
- Reuse previous top-level children fully after `oldWindow`, shifting their offsets by the total edit delta.
- Build a new `MarkdownDocument(range = 0..newText.length, children = splicedChildren)`.
- Refresh the snapshot from the spliced document. If any step fails, run a full delegate parse and refresh the snapshot from that result instead.

## Validation

- Child offsets must be sorted.
- Child absolute ranges must not overlap.
- Every child absolute range must be inside `0..newText.length`.
- The rebuilt document range must be `0..newText.length`.
- In tests, compare partial-parser output with direct full delegate output by rendering both to the existing test HTML/debug representation.

## Test Plan

- Dirty-window planner tests:
  - edit inside one paragraph includes that paragraph plus neighbors
  - deleting blank line between paragraphs includes both paragraphs
  - inserting blank line inside a paragraph includes the split area plus neighbors
  - edit inside a list includes the list block plus neighbors
  - edit inside a block quote includes the block quote plus neighbors
  - edit touching a fenced-code marker falls back
  - multiple disjoint edits fall back
- Parser equivalence tests:
  - paragraph text edit matches full parse
  - paragraph merge/split matches full parse
  - list item text edit matches full parse
  - heading text edit matches full parse
  - inline bold/link text edit matches full parse
  - fallback refreshes the snapshot and later partial edits still work
- Benchmark:
  - keep the existing long-document typing benchmark
  - add trace sections or counters for partial parse hit, fallback, and parsed character count
  - compare `Wysiwyg:parse` and parser-specific parse sections before and after

## Assumptions

- Correctness is more important than partial-parse hit rate.
- The wrapper can parse substrings because `MarkdownParser` returns local ranges relative to its input text.
- Parser implementations should not need to know whether they are parsing a whole document or a dirty substring.
- No render caching, stable node identity, or Flexmark-specific incremental behavior is included.
