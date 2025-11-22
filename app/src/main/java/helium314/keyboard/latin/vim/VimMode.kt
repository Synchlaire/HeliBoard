// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.vim

/**
 * Vim editing modes
 */
enum class VimMode {
    /** Normal mode - for navigation and commands */
    NORMAL,
    /** Insert mode - for text input */
    INSERT,
    /** Visual mode - for text selection (character-wise) */
    VISUAL,
    /** Visual Line mode - for text selection (line-wise) */
    VISUAL_LINE,
    /** Command mode - for ex commands (future) */
    COMMAND
}

/**
 * Vim motion types
 */
enum class VimMotion {
    // Character motions
    LEFT,      // h
    DOWN,      // j
    UP,        // k
    RIGHT,     // l

    // Word motions
    WORD_FORWARD,        // w
    WORD_BACKWARD,       // b
    WORD_END,            // e

    // Line motions
    LINE_START,          // 0
    LINE_END,            // $
    LINE_FIRST_CHAR,     // ^

    // Document motions
    DOCUMENT_START,      // gg
    DOCUMENT_END,        // G

    // Paragraph motions
    PARAGRAPH_FORWARD,   // }
    PARAGRAPH_BACKWARD,  // {
}

/**
 * Vim operations
 */
enum class VimOperation {
    DELETE,    // d
    CHANGE,    // c
    YANK,      // y
    REPLACE,   // r
    PASTE,     // p/P
}

/**
 * State machine for vim mode
 */
data class VimState(
    var mode: VimMode = VimMode.INSERT,
    var pendingOperation: VimOperation? = null,
    var yankBuffer: String = "",
    var visualStartOffset: Int = -1,
    var repeatCount: Int = 0,
    var lastCommand: String = ""
) {
    fun reset() {
        mode = VimMode.INSERT
        pendingOperation = null
        visualStartOffset = -1
        repeatCount = 0
    }

    fun isInNormalMode() = mode == VimMode.NORMAL
    fun isInInsertMode() = mode == VimMode.INSERT
    fun isInVisualMode() = mode == VimMode.VISUAL || mode == VimMode.VISUAL_LINE
}
