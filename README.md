![Screenshot](/screenshots/wysiwyg_github.png)

A markdown editor that highlights syntax in real-time.

```
TODO: upload to maven
```

### Usage

```kotlin
val parser = FlexmarkMarkdownParser(MarkdownHintStyles(context), MarkdownSpanPool())
val markdownHints = MarkdownHints(editText, parser)
editText.addTextChangedListener(markdownHints.textWatcher())
```

**Supported syntax**

* Bold
* Italic
* Strikethrough
* Headings
* Quote
* Horizontal rule
* Lists
* Links

### How do I...

**Customize colors and dimensions?**

`MarkdownHintStyles` contains default values for colors and dimensions that I felt were sensible. You can override them to change the feel of your editor. It's a Kotlin data class so overriding default values with Java may not be pretty.

**Use a different markdown parser?**

TODO

**Extend support for more markdown syntax?**

TODO

**Use custom spans?**

TODO

### Performance
Last measured: 2026-05-02.

Numbers below were taken on a Pixel 10 Pro (Android 16), Compose UI 1.11.0-rc01.
The scenario is: launch the editor, jump the cursor to end-of-doc, type 26 characters with no inter-char delay.
Two fixtures: the first 50,000 characters of the CommonMark spec (long document) and the ~400-char sample fixture (short note).

#### Long document — first 50,000 characters of the CommonMark spec

**Frame timing during the typing burst** (lower is better; 8.3 ms is the 120 Hz frame budget):

|                   | Plain `BasicTextField` | `WsyiwygTextField` | Ratio |
|-------------------|------------------------|--------------------|-------|
| P50 frame CPU     | 4.1 ms                 | 4.3 ms             | 1.0×  |
| P90 frame CPU     | 13.5 ms                | 32.9 ms            | 2.4×  |
| P95 frame CPU     | 30.9 ms                | 40.8 ms            | 1.3×  |
| P99 frame CPU     | 53.0 ms                | 53.9 ms            | 1.0×  |
| P99 frame overrun | 61.1 ms                | 72.8 ms            | 1.2×  |

**Per-iteration trace section sums** (wysiwyg only):

| Section                             | Median   | Calls / iteration |
|-------------------------------------|----------|-------------------|
| `Wysiwyg:parse`                     | 116.3 ms | 3                 |
| ↳ `Wysiwyg:flexmarkParse`           | 92.4 ms  | 3                 |
| ↳ `Wysiwyg:convertFlexmarkAst`      | 2.2 ms   | 3                 |
| `Wysiwyg:drawBehind`                | 3.8 ms   | 8                 |
| `Wysiwyg:transformOutput`           | 14.9 ms  | 22                |
| ↳ `Wysiwyg:render`                  | 14.5 ms  | 22                |
| ↳↳ `Wysiwyg:render:addStyle`        | 2.9 ms   | 1,402             |
| `Wysiwyg:inputTransformation`       | 0.2 ms   | 4                 |
| `Wysiwyg:edited` (overlay)          | 0.06 ms  | 3                 |

`Wysiwyg:parse` runs on `Dispatchers.Default`, so it doesn't block frames directly — but the main-thread work it triggers (`transformOutput` → `render`) explains most of the gap above plain `BasicTextField`.

Two signals stand out under the granular traces. **Render scales with the visible viewport, not the document:** every `OutputTransformation` query still re-walks the AST, but the resolve()-side viewport filter drops offscreen ranges so emissions stay proportional to what's on screen. With Compose querying `OutputTransformation` 22× per typing burst, that's 1,402 `addStyle` calls into the `TextFieldBuffer` span list (~64 per walk). **Draw is paint-bound:** of the 3.8 ms `drawBehind` median, 3.1 ms is actual painter draws across 65 visible spans; the painter-loop overhead is ~0.2 ms (`paintVisible` − `paint`).

#### Short note — ~400-char sample fixture

**Frame timing during the typing burst:**

| Metric            | `WsyiwygTextField` |
|-------------------|--------------------|
| P50 frame CPU     | 3.6 ms             |
| P90 frame CPU     | 9.8 ms             |
| P95 frame CPU     | 13.8 ms            |
| P99 frame CPU     | 36.0 ms            |
| P99 frame overrun | 56.2 ms            |

**Per-iteration trace section sums:**

| Section                             | Median  | Calls / iteration |
|-------------------------------------|---------|-------------------|
| `Wysiwyg:parse`                     | 25.1 ms | 3                 |
| ↳ `Wysiwyg:flexmarkParse`           | 7.0 ms  | 3                 |
| ↳ `Wysiwyg:convertFlexmarkAst`      | 0.2 ms  | 3                 |
| `Wysiwyg:drawBehind`                | 2.1 ms  | 5                 |
| `Wysiwyg:transformOutput`           | 3.7 ms  | 13                |
| ↳ `Wysiwyg:render`                  | 3.4 ms  | 13                |
| ↳↳ `Wysiwyg:render:addStyle`        | 0.7 ms  | 247               |
| `Wysiwyg:inputTransformation`       | 0.1 ms  | 3                 |
| `Wysiwyg:edited` (overlay)          | 0.06 ms | 3                 |

Render is small: `addStyle` (247 calls) and `transformOutput` (3.7 ms) clear the 8.3 ms budget at P50 with room to spare. Most of the document fits in the viewport, so the offscreen-filter is bypassed and emissions are bounded by the doc itself. The P95+ tail is driven by the off-thread `Wysiwyg:parse` triggering the next `transformOutput`, not render itself.

### License

```
Copyright 2019 Saket Narayan.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
