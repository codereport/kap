package array.keyboard

class ExtendedCharsKeyboardInput {
    val keymap: Map<KeyDescriptor, String>

    init {
        keymap = hashMapOf(
            // First row
            makeKeyDescriptor("`") to "⋄",
            makeKeyDescriptor("1") to "¨", makeKeyDescriptor("!", Flag.SHIFT) to "⌶",
            makeKeyDescriptor("2") to "¯", makeKeyDescriptor("@", Flag.SHIFT) to "⍫",
            makeKeyDescriptor("3") to "<", makeKeyDescriptor("#", Flag.SHIFT) to "⍒",
            makeKeyDescriptor("4") to "≤", makeKeyDescriptor("$", Flag.SHIFT) to "⍋",
            makeKeyDescriptor("5") to "=", makeKeyDescriptor("%", Flag.SHIFT) to "⌽",
            makeKeyDescriptor("6") to "≥", makeKeyDescriptor("^", Flag.SHIFT) to "⍉",
            makeKeyDescriptor("7") to ">", makeKeyDescriptor("&", Flag.SHIFT) to "⊖",
            makeKeyDescriptor("8") to "≠", makeKeyDescriptor("*", Flag.SHIFT) to "⍟",
            makeKeyDescriptor("9") to "∨", makeKeyDescriptor("(", Flag.SHIFT) to "⍱",
            makeKeyDescriptor("0") to "∧", makeKeyDescriptor(")", Flag.SHIFT) to "⍲",
            makeKeyDescriptor("-") to "×", makeKeyDescriptor("_", Flag.SHIFT) to "⍠",
            makeKeyDescriptor("=") to "÷", makeKeyDescriptor("+", Flag.SHIFT) to "⌹",
            // Second row
            // q is unassigned
            makeKeyDescriptor("w") to "⍵", makeKeyDescriptor("W", Flag.SHIFT) to "⍹",
            makeKeyDescriptor("e") to "∊", makeKeyDescriptor("E", Flag.SHIFT) to "⍷",
            makeKeyDescriptor("r") to "⍴", makeKeyDescriptor("R", Flag.SHIFT) to "√",
            makeKeyDescriptor("t") to "∼", makeKeyDescriptor("T", Flag.SHIFT) to "⍨",
            makeKeyDescriptor("y") to "↑",
            makeKeyDescriptor("u") to "↓", makeKeyDescriptor("U", Flag.SHIFT) to "⇐",
            makeKeyDescriptor("i") to "⍳", makeKeyDescriptor("I", Flag.SHIFT) to "⍸",
            makeKeyDescriptor("o") to "○", makeKeyDescriptor("O", Flag.SHIFT) to "⍥",
            makeKeyDescriptor("p") to "⋆", makeKeyDescriptor("P", Flag.SHIFT) to "⍣",
            makeKeyDescriptor("[") to "←", makeKeyDescriptor("{", Flag.SHIFT) to "⍞",
            makeKeyDescriptor("]") to "→", makeKeyDescriptor("}", Flag.SHIFT) to "⍬",
            makeKeyDescriptor("\\") to "⊢", makeKeyDescriptor("|", Flag.SHIFT) to "⊣",
            // Third row
            makeKeyDescriptor("a") to "⍺", makeKeyDescriptor("A", Flag.SHIFT) to "⍶",
            makeKeyDescriptor("s") to "⌈", makeKeyDescriptor("S", Flag.SHIFT) to "∵",
            makeKeyDescriptor("d") to "⌊", makeKeyDescriptor("D", Flag.SHIFT) to "˝",
            makeKeyDescriptor("f") to "_", makeKeyDescriptor("F", Flag.SHIFT) to "⍛",
            makeKeyDescriptor("g") to "∇", makeKeyDescriptor("G", Flag.SHIFT) to "⍢",
            makeKeyDescriptor("h") to "∆", makeKeyDescriptor("H", Flag.SHIFT) to "⍙",
            makeKeyDescriptor("j") to "∘", makeKeyDescriptor("J", Flag.SHIFT) to "⍤",
            makeKeyDescriptor("k") to "⍓", makeKeyDescriptor("K", Flag.SHIFT) to "⌻",
            makeKeyDescriptor("l") to "⎕", makeKeyDescriptor("L", Flag.SHIFT) to "⌷",
            makeKeyDescriptor(";") to "⍎", makeKeyDescriptor(":", Flag.SHIFT) to "≡",
            makeKeyDescriptor("'") to "⍕", makeKeyDescriptor("\"", Flag.SHIFT) to "≢",
            // Fourth row
            makeKeyDescriptor("z") to "⊂", makeKeyDescriptor("Z", Flag.SHIFT) to "⊆",
            makeKeyDescriptor("x") to "⊃", makeKeyDescriptor("X", Flag.SHIFT) to "⊇",
            makeKeyDescriptor("c") to "∩",
            makeKeyDescriptor("v") to "∪", makeKeyDescriptor("V", Flag.SHIFT) to "λ",
            makeKeyDescriptor("b") to "⊥", makeKeyDescriptor("B", Flag.SHIFT) to "«",
            makeKeyDescriptor("n") to "⊤", makeKeyDescriptor("N", Flag.SHIFT) to "»",
            makeKeyDescriptor("m") to "|", makeKeyDescriptor("M", Flag.SHIFT) to "∥",
            makeKeyDescriptor(",") to "⍝", makeKeyDescriptor("<", Flag.SHIFT) to "⍪",
            makeKeyDescriptor(".") to "⍀", makeKeyDescriptor(">", Flag.SHIFT) to "⑊",
            makeKeyDescriptor("/") to "⌿", makeKeyDescriptor("?", Flag.SHIFT) to "⫽")
    }

    enum class Flag {
        SHIFT
    }

    private fun makeKeyDescriptor(character: String, vararg flags: Flag): KeyDescriptor {
        return KeyDescriptor(character, flags.contains(Flag.SHIFT))
    }

    data class KeyDescriptor(val character: String, val shift: Boolean)
}
