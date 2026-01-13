package ni.shikatu.rex

import androidx.annotation.Keep
import java.io.File

/**
 * Whisper speech recognition wrapper.
 */
@Keep
class Whisper {

    private var contextPtr: Long = 0
    private val lock = Any()

    /**
     * Check if model is loaded
     */
    val isLoaded: Boolean
        get() = contextPtr != 0L

    fun loadModel(modelPath: String, useGpu: Boolean = false): Boolean {
        synchronized(lock) {
            release()
            checkModelFormat(modelPath)
            contextPtr = initContext(modelPath, useGpu)
            return contextPtr != 0L
        }
    }

    fun checkModelFormat(path: String) {
        val file = File(path)
        if (!file.exists()) {
            android.util.Log.e("WhisperCheck", "Файл не найден: $path")
            return
        }

        file.inputStream().use { stream ->
            val header = ByteArray(4)
            stream.read(header)
            val magic = String(header, Charsets.US_ASCII)

            android.util.Log.d("WhisperCheck", "magic: '$magic'")

            if (magic == "GGUF") {
                android.util.Log.d("WhisperCheck", "Format: GGUF")
            } else if (magic == "ggml") {
                android.util.Log.e("WhisperCheck", "Format: GGML (deprecated)")
            } else {
                android.util.Log.e("WhisperCheck", "Format: Unknown")
            }
        }
    }

    fun loadModel(modelData: ByteArray, useGpu: Boolean = false): Boolean {
        synchronized(lock) {
            release()
            contextPtr = initContextFromBuffer(modelData, useGpu)
            return contextPtr != 0L
        }
    }

    /**
     * Transcribe audio samples
     */
    fun transcribe(
        samples: FloatArray,
        numThreads: Int = 4,
        language: String? = null,
        translate: Boolean = false
    ): TranscriptionResult? {
        synchronized(lock) {
            if (contextPtr == 0L) {
                return null
            }

            // ИСПРАВЛЕНИЕ: Получаем текст (String?) напрямую из C++
            // Если вернулся null, значит произошла ошибка в whisper_full
            val text = transcribe(contextPtr, samples, numThreads, language, translate)

            if (text == null) {
                return null
            }

            return buildResult(text)
        }
    }

    fun getDetectedLanguage(): String? {
        synchronized(lock) {
            if (contextPtr == 0L) return null
            return getDetectedLanguage(contextPtr)
        }
    }

    fun isMultilingual(): Boolean {
        synchronized(lock) {
            if (contextPtr == 0L) return false
            return isMultilingual(contextPtr)
        }
    }

    fun release() {
        synchronized(lock) {
            if (contextPtr != 0L) {
                freeContext(contextPtr)
                contextPtr = 0
            }
        }
    }

    // ИСПРАВЛЕНИЕ: Принимаем fullText как аргумент
    private fun buildResult(fullText: String): TranscriptionResult {
        val segmentCount = getSegmentCount(contextPtr)
        val segments = mutableListOf<TranscriptionSegment>()

        for (i in 0 until segmentCount) {
            val text = getSegmentText(contextPtr, i) ?: ""
            val startTime = getSegmentStartTime(contextPtr, i)
            val endTime = getSegmentEndTime(contextPtr, i)

            // Get tokens for this segment
            val tokenCount = getSegmentTokenCount(contextPtr, i)
            val tokens = mutableListOf<TranscriptionToken>()

            for (j in 0 until tokenCount) {
                val tokenText = getTokenText(contextPtr, i, j) ?: ""
                val probability = getTokenProbability(contextPtr, i, j)
                tokens.add(TranscriptionToken(tokenText, probability))
            }

            segments.add(TranscriptionSegment(text, startTime, endTime, tokens))
        }

        val language = getDetectedLanguage(contextPtr)

        return TranscriptionResult(fullText, segments, language)
    }

    protected fun finalize() {
        release()
    }

    companion object {
        @JvmStatic
        val SAMPLE_RATE: Int
            get() = getSampleRate()

        @JvmStatic
        fun version(): String? = getVersion()

        @JvmStatic
        fun systemInfo(): String? = getSystemInfo()

        @JvmStatic
        fun convertVoiceMessage(filePath: String): FloatArray? {
            return convertOggOpusToSamples(filePath)
        }

        /**
         * Save audio samples to WAV file for debugging
         * @param samples Float array of audio samples (16kHz mono)
         * @param outputPath Path where to save WAV file
         * @return true if saved successfully
         */
        @JvmStatic
        fun saveDebugWav(samples: FloatArray, outputPath: String): Boolean {
            return saveDebugWavNative(samples, outputPath)
        }

        // Native methods
        @JvmStatic
        private external fun initContext(modelPath: String, useGpu: Boolean): Long

        @JvmStatic
        private external fun initContextFromBuffer(modelData: ByteArray, useGpu: Boolean): Long

        @JvmStatic
        private external fun freeContext(contextPtr: Long)

        // ИСПРАВЛЕНИЕ: Возвращает String? (текст или null), а не Int
        @JvmStatic
        private external fun transcribe(
            contextPtr: Long,
            samples: FloatArray,
            numThreads: Int,
            language: String?,
            translate: Boolean
        ): String?

        @JvmStatic
        private external fun getSegmentCount(contextPtr: Long): Int

        @JvmStatic
        private external fun getSegmentText(contextPtr: Long, segmentIndex: Int): String?

        @JvmStatic
        private external fun getSegmentStartTime(contextPtr: Long, segmentIndex: Int): Long

        @JvmStatic
        private external fun getSegmentEndTime(contextPtr: Long, segmentIndex: Int): Long

        @JvmStatic
        private external fun getFullText(contextPtr: Long): String?

        @JvmStatic
        private external fun getDetectedLanguage(contextPtr: Long): String?

        @JvmStatic
        private external fun isMultilingual(contextPtr: Long): Boolean

        @JvmStatic
        private external fun getSegmentTokenCount(contextPtr: Long, segmentIndex: Int): Int

        @JvmStatic
        private external fun getTokenText(
            contextPtr: Long,
            segmentIndex: Int,
            tokenIndex: Int
        ): String?

        @JvmStatic
        private external fun getTokenProbability(
            contextPtr: Long,
            segmentIndex: Int,
            tokenIndex: Int
        ): Float

        @JvmStatic
        private external fun getSampleRate(): Int

        @JvmStatic
        private external fun getVersion(): String?

        @JvmStatic
        private external fun getSystemInfo(): String?

        @JvmStatic
        private external fun convertOggOpusToSamples(filePath: String): FloatArray?

        @JvmStatic
        private external fun saveDebugWavNative(samples: FloatArray, outputPath: String): Boolean
    }
}

@Keep
data class TranscriptionResult(
    val fullText: String,
    val segments: List<TranscriptionSegment>,
    val detectedLanguage: String?
)

@Keep
data class TranscriptionSegment(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val tokens: List<TranscriptionToken> = emptyList()
) {
    val duration: Long
        get() = endTime - startTime
}

@Keep
data class TranscriptionToken(
    val text: String,
    val probability: Float
)