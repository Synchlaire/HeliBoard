// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.vim

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants

/**
 * Manages vim editing mode functionality
 */
class VimModeManager(private val modeChangeCallback: (VimMode) -> Unit) {

    private val state = VimState()
    private var pendingG = false  // For 'gg' motion
    private var pendingD = false  // For 'dd' delete line

    /**
     * Check if vim mode is enabled
     */
    var isEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                state.reset()
                modeChangeCallback(VimMode.INSERT)
            }
        }

    /**
     * Get current vim mode
     */
    fun getCurrentMode(): VimMode = state.mode

    /**
     * Enter normal mode from insert mode
     */
    fun enterNormalMode() {
        if (state.mode == VimMode.INSERT) {
            state.mode = VimMode.NORMAL
            state.pendingOperation = null
            modeChangeCallback(VimMode.NORMAL)
        }
    }

    /**
     * Enter insert mode from normal mode
     */
    fun enterInsertMode() {
        if (state.mode != VimMode.INSERT) {
            state.mode = VimMode.INSERT
            state.visualStartOffset = -1
            modeChangeCallback(VimMode.INSERT)
        }
    }

    /**
     * Toggle visual mode
     */
    fun toggleVisualMode(isLinewise: Boolean = false) {
        state.mode = if (isLinewise) VimMode.VISUAL_LINE else VimMode.VISUAL
        modeChangeCallback(state.mode)
    }

    /**
     * Process a key in vim mode
     * @return true if the key was handled by vim, false to pass through normally
     */
    fun processKey(primaryCode: Int, connection: InputConnection): Boolean {
        if (!isEnabled || state.isInInsertMode()) {
            return false  // Pass through in insert mode
        }

        // Handle escape to return to normal mode
        if (primaryCode == KeyCode.DELETE && state.isInVisualMode()) {
            // ESC in visual mode returns to normal
            enterNormalMode()
            return true
        }

        when (state.mode) {
            VimMode.NORMAL -> return processNormalModeKey(primaryCode, connection)
            VimMode.VISUAL, VimMode.VISUAL_LINE -> return processVisualModeKey(primaryCode, connection)
            else -> return false
        }
    }

    private fun processNormalModeKey(primaryCode: Int, connection: InputConnection): Boolean {
        val char = primaryCode.toChar().lowercaseChar()

        // Check for repeat count (0-9)
        if (char in '1'..'9' && state.pendingOperation == null) {
            state.repeatCount = state.repeatCount * 10 + (char - '0')
            return true
        }

        val count = if (state.repeatCount > 0) state.repeatCount else 1

        // Handle 'gg' for document start
        if (pendingG && char == 'g') {
            pendingG = false
            moveToDocumentStart(connection)
            state.repeatCount = 0
            return true
        } else if (char == 'g' && !pendingG) {
            pendingG = true
            return true
        } else {
            pendingG = false
        }

        // Handle 'dd' for delete line
        if (pendingD && char == 'd') {
            pendingD = false
            deleteLine(connection)
            state.repeatCount = 0
            return true
        }

        // Check for operations (d, c, y, r)
        when (char) {
            'd' -> {
                if (state.pendingOperation == VimOperation.DELETE) {
                    // dd - delete line
                    deleteLine(connection)
                    state.pendingOperation = null
                    state.repeatCount = 0
                } else {
                    state.pendingOperation = VimOperation.DELETE
                }
                return true
            }
            'c' -> {
                state.pendingOperation = VimOperation.CHANGE
                return true
            }
            'y' -> {
                state.pendingOperation = VimOperation.YANK
                return true
            }
            'r' -> {
                state.pendingOperation = VimOperation.REPLACE
                return true
            }
            'p' -> {
                paste(connection, false)
                state.repeatCount = 0
                return true
            }
            'x' -> {
                deleteChar(connection, count)
                state.repeatCount = 0
                return true
            }
            'i' -> {
                enterInsertMode()
                return true
            }
            'a' -> {
                moveRight(connection, 1)
                enterInsertMode()
                return true
            }
            'v' -> {
                toggleVisualMode(false)
                return true
            }
            'V' -> {
                toggleVisualMode(true)
                return true
            }
        }

        // Handle motions
        return processMotion(char, count, connection)
    }

    private fun processVisualModeKey(primaryCode: Int, connection: InputConnection): Boolean {
        val char = primaryCode.toChar().lowercaseChar()
        val count = if (state.repeatCount > 0) state.repeatCount else 1

        // Operations in visual mode
        when (char) {
            'd', 'x' -> {
                deleteSelection(connection)
                enterNormalMode()
                return true
            }
            'y' -> {
                yankSelection(connection)
                enterNormalMode()
                return true
            }
            'c' -> {
                deleteSelection(connection)
                enterInsertMode()
                return true
            }
        }

        // Motions extend selection
        return processMotion(char, count, connection)
    }

    private fun processMotion(char: Char, count: Int, connection: InputConnection): Boolean {
        val operation = state.pendingOperation

        val motion = when (char) {
            'h' -> VimMotion.LEFT
            'j' -> VimMotion.DOWN
            'k' -> VimMotion.UP
            'l' -> VimMotion.RIGHT
            'w' -> VimMotion.WORD_FORWARD
            'b' -> VimMotion.WORD_BACKWARD
            'e' -> VimMotion.WORD_END
            '0' -> VimMotion.LINE_START
            '$' -> VimMotion.LINE_END
            '^' -> VimMotion.LINE_FIRST_CHAR
            'G' -> VimMotion.DOCUMENT_END
            '{' -> VimMotion.PARAGRAPH_BACKWARD
            '}' -> VimMotion.PARAGRAPH_FORWARD
            else -> null
        }

        if (motion != null) {
            executeMotion(motion, count, operation, connection)
            if (operation != null) {
                state.pendingOperation = null
            }
            state.repeatCount = 0
            return true
        }

        return false
    }

    private fun executeMotion(motion: VimMotion, count: Int, operation: VimOperation?, connection: InputConnection) {
        when (motion) {
            VimMotion.LEFT -> repeat(count) { moveLeft(connection, 1) }
            VimMotion.DOWN -> repeat(count) { moveDown(connection) }
            VimMotion.UP -> repeat(count) { moveUp(connection) }
            VimMotion.RIGHT -> repeat(count) { moveRight(connection, 1) }
            VimMotion.WORD_FORWARD -> repeat(count) { moveWordForward(connection) }
            VimMotion.WORD_BACKWARD -> repeat(count) { moveWordBackward(connection) }
            VimMotion.LINE_START -> moveToLineStart(connection)
            VimMotion.LINE_END -> moveToLineEnd(connection)
            VimMotion.DOCUMENT_START -> moveToDocumentStart(connection)
            VimMotion.DOCUMENT_END -> moveToDocumentEnd(connection)
            else -> {}
        }
    }

    // Motion implementations
    private fun moveLeft(connection: InputConnection, count: Int) {
        sendArrowKey(connection, KeyEvent.KEYCODE_DPAD_LEFT, count)
    }

    private fun moveRight(connection: InputConnection, count: Int) {
        sendArrowKey(connection, KeyEvent.KEYCODE_DPAD_RIGHT, count)
    }

    private fun moveUp(connection: InputConnection) {
        sendArrowKey(connection, KeyEvent.KEYCODE_DPAD_UP, 1)
    }

    private fun moveDown(connection: InputConnection) {
        sendArrowKey(connection, KeyEvent.KEYCODE_DPAD_DOWN, 1)
    }

    private fun moveWordForward(connection: InputConnection) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON))
    }

    private fun moveWordBackward(connection: InputConnection) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON))
    }

    private fun moveToLineStart(connection: InputConnection) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME))
    }

    private fun moveToLineEnd(connection: InputConnection) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
    }

    private fun moveToDocumentStart(connection: InputConnection) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_CTRL_ON))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME, 0, KeyEvent.META_CTRL_ON))
    }

    private fun moveToDocumentEnd(connection: InputConnection) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END, 0, KeyEvent.META_CTRL_ON))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END, 0, KeyEvent.META_CTRL_ON))
    }

    // Operation implementations
    private fun deleteChar(connection: InputConnection, count: Int) {
        repeat(count) {
            connection.deleteSurroundingText(0, 1)
        }
    }

    private fun deleteLine(connection: InputConnection) {
        moveToLineStart(connection)
        val text = connection.getTextAfterCursor(10000, 0) ?: ""
        val lineEnd = text.indexOf('\n')
        if (lineEnd >= 0) {
            connection.deleteSurroundingText(0, lineEnd + 1)
        } else {
            connection.deleteSurroundingText(0, text.length)
        }
    }

    private fun deleteSelection(connection: InputConnection) {
        val selectedText = connection.getSelectedText(0)
        if (selectedText != null) {
            state.yankBuffer = selectedText.toString()
            connection.commitText("", 1)
        }
    }

    private fun yankSelection(connection: InputConnection) {
        val selectedText = connection.getSelectedText(0)
        if (selectedText != null) {
            state.yankBuffer = selectedText.toString()
        }
    }

    private fun paste(connection: InputConnection, before: Boolean) {
        if (state.yankBuffer.isNotEmpty()) {
            if (!before) {
                moveRight(connection, 1)
            }
            connection.commitText(state.yankBuffer, 1)
        }
    }

    private fun sendArrowKey(connection: InputConnection, keyCode: Int, count: Int) {
        repeat(count) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }
}
