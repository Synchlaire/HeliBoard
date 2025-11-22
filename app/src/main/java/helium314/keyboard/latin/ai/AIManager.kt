// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.ai

import android.content.Context
import android.content.SharedPreferences

/**
 * Central manager for AI features (Whisper STT and Llama LLM)
 */
object AIManager {
    private var whisperSTT: WhisperSTT? = null
    private var llamaLLM: LlamaLLM? = null
    private var isInitialized = false

    fun init(context: Context, prefs: SharedPreferences) {
        if (isInitialized) return

        whisperSTT = WhisperSTT(context)
        llamaLLM = LlamaLLM(context)
        isInitialized = true
    }

    fun getWhisperSTT(): WhisperSTT? = whisperSTT

    fun getLlamaLLM(): LlamaLLM? = llamaLLM

    fun isWhisperAvailable(): Boolean = whisperSTT?.isModelLoaded() == true

    fun isLlamaAvailable(): Boolean = llamaLLM?.isModelLoaded() == true
}
