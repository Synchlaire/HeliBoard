// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Llama LLM text transformation implementation
 * Uses llama.cpp with Llama 3.2 1B for on-device inference
 */
class LlamaLLM(private val context: Context) {
    private var modelPath: String? = null
    private var isModelReady = false

    companion object {
        private const val TAG = "LlamaLLM"
        private const val MAX_TOKENS = 256
    }

    /**
     * Text transformation operations
     */
    enum class TransformOperation(val displayName: String, val prompt: String) {
        IMPROVE("Improve", "Improve the following text while maintaining its meaning:"),
        FIX_GRAMMAR("Fix Grammar", "Fix all grammar and spelling errors in the following text:"),
        MAKE_FORMAL("Make Formal", "Rewrite the following text in a formal tone:"),
        MAKE_CASUAL("Make Casual", "Rewrite the following text in a casual, friendly tone:"),
        SHORTEN("Shorten", "Make the following text more concise:"),
        EXPAND("Expand", "Expand and elaborate on the following text:"),
        SUMMARIZE("Summarize", "Summarize the following text:");

        fun buildPrompt(text: String): String {
            return "$prompt\n\n$text\n\nImproved text:"
        }
    }

    init {
        // Check if llama model exists
        val modelFile = File(context.filesDir, "models/llama-3.2-1b-q4.gguf")
        if (modelFile.exists()) {
            modelPath = modelFile.absolutePath
            isModelReady = true
            Log.d(TAG, "Llama model loaded from: $modelPath")
        } else {
            Log.w(TAG, "Llama model not found at: ${modelFile.absolutePath}")
        }
    }

    fun isModelLoaded(): Boolean = isModelReady

    /**
     * Transform text using the LLM
     */
    suspend fun transformText(
        text: String,
        operation: TransformOperation,
        onProgress: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isModelReady) {
            onError("Llama model not loaded. Please download the model first.")
            return
        }

        if (text.isBlank()) {
            onError("No text selected")
            return
        }

        withContext(Dispatchers.Default) {
            try {
                val prompt = operation.buildPrompt(text)
                Log.d(TAG, "Transforming text with operation: ${operation.displayName}")

                // TODO: Integrate llama.cpp
                // For now, return a placeholder
                val result = generatePlaceholder(text, operation)

                withContext(Dispatchers.Main) {
                    onComplete(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transform text", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to transform text: ${e.message}")
                }
            }
        }
    }

    /**
     * Placeholder generation until llama.cpp is integrated
     */
    private fun generatePlaceholder(text: String, operation: TransformOperation): String {
        return when (operation) {
            TransformOperation.IMPROVE -> "[Improved] $text"
            TransformOperation.FIX_GRAMMAR -> "[Grammar Fixed] $text"
            TransformOperation.MAKE_FORMAL -> "[Formalized] $text"
            TransformOperation.MAKE_CASUAL -> "[Casualized] $text"
            TransformOperation.SHORTEN -> text.take(text.length / 2) + "..."
            TransformOperation.EXPAND -> "$text (expanded with more details...)"
            TransformOperation.SUMMARIZE -> text.take(50) + "..."
        }
    }

    /**
     * Generate text with custom prompt
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = MAX_TOKENS,
        onToken: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isModelReady) {
            onError("Llama model not loaded")
            return
        }

        withContext(Dispatchers.Default) {
            try {
                // TODO: Integrate llama.cpp with streaming
                val result = "Generated response for: $prompt"

                withContext(Dispatchers.Main) {
                    onComplete(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate text", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to generate: ${e.message}")
                }
            }
        }
    }

    /**
     * Download llama model
     */
    suspend fun downloadModel(
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // TODO: Implement model download
                withContext(Dispatchers.Main) {
                    onError("Please download llama-3.2-1b-q4.gguf model manually to: ${context.filesDir}/models/")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Failed to download model: ${e.message}")
                }
            }
        }
    }

    /**
     * Get available transformation operations
     */
    fun getAvailableOperations(): List<TransformOperation> {
        return TransformOperation.values().toList()
    }
}
