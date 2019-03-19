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
