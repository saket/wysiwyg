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
Last measured: 2026-05-01.

Numbers below were taken on a Pixel 10 Pro (Android 16), Compose UI 1.11.0-rc01, against the first 50,000 characters of the CommonMark spec.
The scenario is: launch the editor, jump the cursor to end-of-doc, type 26 characters with no inter-char delay.

**Frame timing during the typing burst** (lower is better; 8.3 ms is the 120 Hz frame budget):

|                   | Plain `BasicTextField` | `WsyiwygTextField` | Ratio |
|-------------------|------------------------|--------------------|-------|
| P50 frame CPU     | 4.2 ms                 | 4.6 ms             | 1.1×  |
| P90 frame CPU     | 21.9 ms                | 55.3 ms            | 2.5×  |
| P95 frame CPU     | 26.7 ms                | 59.9 ms            | 2.2×  |
| P99 frame CPU     | 47.4 ms                | 70.7 ms            | 1.5×  |
| P99 frame overrun | 55.4 ms                | 110.6 ms           | 2.0×  |

**Per-iteration trace section sums** (wysiwyg only):

| Section                        | Median   | Calls / iteration |
|--------------------------------|----------|-------------------|
| `Wysiwyg:parse`                | 174.8 ms | 3                 |
| ↳ `Wysiwyg:flexmarkParse`      | 94.6 ms  | 3                 |
| ↳ `Wysiwyg:convertFlexmarkAst` | 2.3 ms   | 3                 |
| `Wysiwyg:drawBehind`           | 4.2 ms   | 8                 |
| `Wysiwyg:transformOutput`      | 11.7 ms  | 17                |
| ↳ `Wysiwyg:render`             | 11.4 ms  | 17                |
| `Wysiwyg:inputTransformation`  | 0.2 ms   | 5                 |
| `Wysiwyg:edited` (overlay)     | < 0.1 ms | 3                 |

`Wysiwyg:parse` runs on `Dispatchers.Default`, so it doesn't block frames directly — but the main-thread work it triggers (`transformOutput` → `render`) explains most of the gap above plain `BasicTextField`.

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
