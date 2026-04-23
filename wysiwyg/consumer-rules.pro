# flexmark's BitFieldSet reflects over these enums and reconstructs constants using
# Enum.valueOf(field.getName()), so their constant names must survive obfuscation.
-keepclassmembernames class com.vladsch.flexmark.util.sequence.BasedOptionsHolder$Options {
    <fields>;
}
-keepclassmembernames class com.vladsch.flexmark.util.sequence.LineAppendable$Options {
    <fields>;
}
-keepclassmembernames class com.vladsch.flexmark.util.sequence.LineInfo$Flags {
    <fields>;
}
-keepclassmembernames class com.vladsch.flexmark.util.sequence.builder.ISegmentBuilder$Options {
    <fields>;
}
