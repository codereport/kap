package array

import kotlin.test.Test

class OutputFormatterTest : APLTest() {
    @Test
    fun outputFormatter() {
        val src =
            """
            |isArray ⇐ 'array≡typeof
            |isChar ⇐ 'char≡typeof
            |
            |∇ isAplString (v) {
            |  if (1≢≢⍴v) { →0 }
            |  ~0∊'char=typeof¨v
            |}
            |
            |stringToGraphemes ⇐ (1,≢)⍛⍴ unicode:toGraphemes
            |
            |∇ alignCells (v) {
            |  colWidths ← ⌈⌿ (↑↓⍴)¨v
            |  xAligned ← ((⍴v) ⍴ colWidths) {((≢⍵) (⍺-↑↓⍴⍵) ⍴ @\s),⍵}¨ v
            |
            |  rowHeights ← ⌈/ ≢¨xAligned
            |  yAligned ← (⍉ (⌽ ⍴ xAligned) ⍴ rowHeights) {⍵ ,[0] ((⍺-≢⍵) (↑↓⍴⍵)) ⍴ @\s}¨ xAligned
            |}
            |
            |∇ renderArray (v) {
            |  r ← ¯1 (↑¯1↑⍴v) ⍴ v
            |  alignCells (stringToGraphemes⍕)¨ r
            |}
            |
            |∇ prettyPrintToChars (v) {
            |  when {
            |    (isChar(v))      { renderChar v }
            |    (isAplString(v)) { renderString v }
            |    (isArray(v))     { renderArray v }
            |    (1)              { stringToGraphemes ⍕v }
            |  }
            |}
            |
            |prettyPrintToChars 2 3 3 ⍴ ⍳100            
            """.trimMargin()
        parseAPLExpression(src, withStandardLib = true)
    }
}
