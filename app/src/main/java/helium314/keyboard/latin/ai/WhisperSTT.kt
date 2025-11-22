// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.ai

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Whisper speech-to-text implementation
 * Uses whisper.cpp for on-device inference
 */
class WhisperSTT(private val context: Context) {
    private var modelPath: String? = null
    private var isModelReady = false

    companion object {
        private const val TAG = "WhisperSTT"
        private const val SAMPLE_RATE = 16000
        private const val RECORDING_BUFFER_SIZE = 16000 * 30 // 30 seconds max
    }

    init {
        // Check if whisper model exists
        val modelFile = File(context.filesDir, "models/whisper-tiny.bin")
        if (modelFile.exists()) {
            modelPath = modelFile.absolutePath
            isModelReady = true
            Log.d(TAG, "Whisper model loaded from: ${modelPath}")
        } else {
            Log.w(TAG, "Whisper model not found at: ${modelFile.absolutePath}")
        }
    }

    fun isModelLoaded(): Boolean = isModelReady

    /**
     * Start recording audio for speech-to-text
     * Returns a callback that can be used to stop recording and get transcription
     */
    suspend fun startRecording(onResult: (String) -> Unit, onError: (String) -> Unit): RecordingSession {
        if (!isModelReady) {
            onError("Whisper model not loaded. Please download the model first.")
            return RecordingSession(null)
        }

        return withContext(Dispatchers.IO) {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                audioRecord.startRecording()
                Log.d(TAG, "Recording started")

                RecordingSession(audioRecord)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                onError("Failed to start recording: ${e.message}")
                RecordingSession(null)
            }
        }
    }

    /**
     * Transcribe audio to text using Whisper
     * This is a placeholder - will integrate whisper.cpp JNI bindings
     */
    private suspend fun transcribe(audioData: ShortArray): String {
        return withContext(Dispatchers.Default) {
            // TODO: Integrate whisper.cpp
            // For now, return a placeholder
            "Whisper transcription placeholder - model integration pending"
        }
    }

    /**
     * Recording session that handles audio capture and transcription
     */
    inner class RecordingSession(private val audioRecord: AudioRecord?) {
        private val audioBuffer = mutableListOf<Short>()
        private var isRecording = true

        suspend fun stopAndTranscribe(onResult: (String) -> Unit, onError: (String) -> Unit) {
            withContext(Dispatchers.IO) {
                try {
                    audioRecord?.let {
                        // Read remaining audio
                        val buffer = ShortArray(1024)
                        while (isRecording && audioBuffer.size < RECORDING_BUFFER_SIZE) {
                            val read = it.read(buffer, 0, buffer.size)
                            if (read > 0) {
                                audioBuffer.addAll(buffer.take(read))
                            } else {
                                break
                            }
                        }

                        it.stop()
                        it.release()
                        isRecording = false

                        Log.d(TAG, "Recording stopped, audio length: ${audioBuffer.size}")

                        // Transcribe
                        val result = transcribe(audioBuffer.toShortArray())
                        withContext(Dispatchers.Main) {
                            onResult(result)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop recording", e)
                    withContext(Dispatchers.Main) {
                        onError("Failed to process audio: ${e.message}")
                    }
                }
            }
        }

        fun isValid(): Boolean = audioRecord != null
    }

    /**
     * Download whisper model
     * This should download whisper-tiny.bin or similar small model
     */
    suspend fun downloadModel(onProgress: (Int) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // TODO: Implement model download
                // For now, just notify that manual download is needed
                withContext(Dispatchers.Main) {
                    onError("Please download whisper-tiny.bin model manually to: ${context.filesDir}/models/")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Failed to download model: ${e.message}")
                }
            }
        }
    }
}
