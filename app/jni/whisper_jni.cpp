#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cmath>
#include <cstring>
#include <algorithm>
#include "whisper.h"
#include "opusfile.h"

#define TAG "WhisperNative"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

void whisper_log_callback(ggml_log_level level, const char * text, void * user_data) {
    if (level == GGML_LOG_LEVEL_ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, "WhisperInternal", "%s", text);
    } else if (level == GGML_LOG_LEVEL_WARN) {
        __android_log_print(ANDROID_LOG_WARN, "WhisperInternal", "%s", text);
    } else {
        __android_log_print(ANDROID_LOG_INFO, "WhisperInternal", "%s", text);
    }
}

extern "C" {



JNIEXPORT jlong JNICALL
Java_ni_shikatu_rex_Whisper_initContext(JNIEnv *env, jclass clazz, jstring model_path_str, jboolean use_gpu) {
    whisper_log_set(whisper_log_callback, nullptr);

    const char *model_path = env->GetStringUTFChars(model_path_str, nullptr);

    struct whisper_context *ctx = nullptr;

    if (use_gpu) {
        struct whisper_context_params cparams = whisper_context_default_params();
        cparams.use_gpu = true;
        cparams.flash_attn = false;
        cparams.dtw_token_timestamps = false;
        cparams.dtw_aheads_preset = WHISPER_AHEADS_NONE;
        ctx = whisper_init_from_file_with_params(model_path, cparams);
    } else {
        ctx = whisper_init_from_file(model_path);
    }

    env->ReleaseStringUTFChars(model_path_str, model_path);

    if (ctx == nullptr) {
        LOGE("Failed to initialize Whisper context from: %s", model_path);
        return 0;
    }

    LOGD("Whisper context initialized successfully");
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jlong JNICALL
Java_ni_shikatu_rex_Whisper_initContextFromBuffer(JNIEnv *env, jclass clazz, jbyteArray model_data, jboolean use_gpu) {
    whisper_log_set(whisper_log_callback, nullptr);

    jsize len = env->GetArrayLength(model_data);
    void *buffer = env->GetPrimitiveArrayCritical(model_data, nullptr);
    if (!buffer) return 0;

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = (bool)use_gpu;
    cparams.flash_attn = false;

    struct whisper_context *ctx = whisper_init_from_buffer_with_params(buffer, len, cparams);

    env->ReleasePrimitiveArrayCritical(model_data, buffer, 0);

    if (ctx == nullptr) {
        LOGE("Failed to initialize Whisper context from buffer");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_ni_shikatu_rex_Whisper_freeContext(JNIEnv *env, jclass clazz, jlong context_ptr) {
    if (context_ptr != 0) {
        whisper_free(reinterpret_cast<struct whisper_context *>(context_ptr));
        LOGD("Whisper context freed");
    }
}



JNIEXPORT jstring JNICALL
Java_ni_shikatu_rex_Whisper_transcribe(
        JNIEnv *env,
        jclass clazz,
        jlong context_ptr,
        jfloatArray samples,
        jint num_threads,
        jstring language,
        jboolean translate
) {
    if (context_ptr == 0) return nullptr;
    auto *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);

    jsize n_samples_input = env->GetArrayLength(samples);
    jfloat *audio_raw = env->GetFloatArrayElements(samples, nullptr);
    if (!audio_raw) return nullptr;

    std::vector<float> pcm_data;
    int min_samples = 16000 * 3;
    if (n_samples_input < min_samples) {
        pcm_data.resize(min_samples, 0.0f);
        for(int i=0; i<n_samples_input; i++) pcm_data[i] = audio_raw[i];
    } else {
        pcm_data.resize(n_samples_input);
        for(int i=0; i<n_samples_input; i++) pcm_data[i] = audio_raw[i];
    }
    env->ReleaseFloatArrayElements(samples, audio_raw, JNI_ABORT);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.translate        = (bool)translate;
    params.n_threads        = num_threads;
    params.offset_ms        = 0;
    params.no_context       = true;
    params.single_segment   = false;

    std::string lang_str;
    bool need_auto_detect = true;

    if (language != nullptr) {
        const char *l = env->GetStringUTFChars(language, nullptr);
        if (l && strlen(l) > 0) {
            if (strcmp(l, "auto") != 0) {
                lang_str = l;
                need_auto_detect = false;
            }
        }
        env->ReleaseStringUTFChars(language, l);
    }

    if (need_auto_detect) {
        if (whisper_pcm_to_mel(ctx, pcm_data.data(), pcm_data.size(), num_threads) == 0) {
            std::vector<float> lang_probs(whisper_lang_max_id() + 1, 0.0f);
            int detected_lang_id = whisper_lang_auto_detect(ctx, 0, num_threads, lang_probs.data());

            if (detected_lang_id >= 0) {
                const char* detected = whisper_lang_str(detected_lang_id);
                if (detected) {
                    lang_str = detected;
                    LOGD("Auto-detected language: %s (id=%d, prob=%.2f%%)",
                         detected, detected_lang_id, lang_probs[detected_lang_id] * 100.0f);
                }
            }
        }

        if (lang_str.empty()) {
            lang_str = "en";
            LOGD("Language detection failed, using default: en");
        }
    }

    params.language = lang_str.c_str();
    params.detect_language = false;

    LOGD("Using language for transcription: %s", params.language);

    whisper_reset_timings(ctx);

    LOGD("Starting transcription: samples=%zu, threads=%d, lang=%s",
         pcm_data.size(), num_threads, params.language);

    int result = whisper_full(ctx, params, pcm_data.data(), pcm_data.size());

    if (result != 0) {
        LOGE("Transcription failed with code: %d", result);
        return nullptr;
    }

    int n_segments = whisper_full_n_segments(ctx);
    std::string full_text;

    for (int i = 0; i < n_segments; ++i) {
        const char* seg_text = whisper_full_get_segment_text(ctx, i);
        if (seg_text) {
            full_text += seg_text;
        }
    }

    LOGD("Transcription complete: %d segments, %zu chars", n_segments, full_text.length());
    return env->NewStringUTF(full_text.c_str());
}



JNIEXPORT jstring JNICALL Java_ni_shikatu_rex_Whisper_getFullText(JNIEnv *env, jclass, jlong ctx) {
    if (ctx == 0) return nullptr;
    auto *wctx = (struct whisper_context *)ctx;
    int n = whisper_full_n_segments(wctx);
    std::string text;
    for (int i = 0; i < n; ++i) {
        text += whisper_full_get_segment_text(wctx, i);
    }
    return env->NewStringUTF(text.c_str());
}

JNIEXPORT jint JNICALL Java_ni_shikatu_rex_Whisper_getSegmentCount(JNIEnv *, jclass, jlong ctx) {
    return whisper_full_n_segments((struct whisper_context *)ctx);
}

JNIEXPORT jstring JNICALL Java_ni_shikatu_rex_Whisper_getSegmentText(JNIEnv *env, jclass, jlong ctx, jint i) {
    const char* txt = whisper_full_get_segment_text((struct whisper_context *)ctx, i);
    return env->NewStringUTF(txt ? txt : "");
}

JNIEXPORT jlong JNICALL Java_ni_shikatu_rex_Whisper_getSegmentStartTime(JNIEnv *, jclass, jlong ctx, jint i) {
    return whisper_full_get_segment_t0((struct whisper_context *)ctx, i) * 10;
}

JNIEXPORT jlong JNICALL Java_ni_shikatu_rex_Whisper_getSegmentEndTime(JNIEnv *, jclass, jlong ctx, jint i) {
    return whisper_full_get_segment_t1((struct whisper_context *)ctx, i) * 10;
}

JNIEXPORT jstring JNICALL Java_ni_shikatu_rex_Whisper_getDetectedLanguage(JNIEnv *env, jclass, jlong ctx) {
    const char* lang = whisper_lang_str(whisper_full_lang_id((struct whisper_context *)ctx));
    return env->NewStringUTF(lang ? lang : "");
}

JNIEXPORT jboolean JNICALL Java_ni_shikatu_rex_Whisper_isMultilingual(JNIEnv *, jclass, jlong ctx) {
    return (jboolean)whisper_is_multilingual((struct whisper_context *)ctx);
}

JNIEXPORT jint JNICALL Java_ni_shikatu_rex_Whisper_getSegmentTokenCount(JNIEnv *, jclass, jlong ctx, jint i) {
    return whisper_full_n_tokens((struct whisper_context *)ctx, i);
}

JNIEXPORT jstring JNICALL Java_ni_shikatu_rex_Whisper_getTokenText(JNIEnv *env, jclass, jlong ctx, jint i, jint j) {
    const char* txt = whisper_full_get_token_text((struct whisper_context *)ctx, i, j);
    return env->NewStringUTF(txt ? txt : "");
}

JNIEXPORT jfloat JNICALL Java_ni_shikatu_rex_Whisper_getTokenProbability(JNIEnv *, jclass, jlong ctx, jint i, jint j) {
    return whisper_full_get_token_p((struct whisper_context *)ctx, i, j);
}

JNIEXPORT jint JNICALL Java_ni_shikatu_rex_Whisper_getSampleRate(JNIEnv *, jclass) {
    return WHISPER_SAMPLE_RATE;
}

JNIEXPORT jstring JNICALL Java_ni_shikatu_rex_Whisper_getVersion(JNIEnv *env, jclass) {
    return env->NewStringUTF(whisper_print_system_info());
}

JNIEXPORT jstring JNICALL Java_ni_shikatu_rex_Whisper_getSystemInfo(JNIEnv *env, jclass) {
    return env->NewStringUTF(whisper_print_system_info());
}


static bool save_wav_file(const char* path, const float* data, size_t num_samples, int sample_rate = 16000) {
    FILE* f = fopen(path, "wb");
    if (!f) {
        LOGE("Cannot create WAV file: %s", path);
        return false;
    }

    int32_t data_size = num_samples * 2;
    int32_t file_size = 36 + data_size;

    fwrite("RIFF", 1, 4, f);
    fwrite(&file_size, 4, 1, f);
    fwrite("WAVE", 1, 4, f);

    fwrite("fmt ", 1, 4, f);
    int32_t fmt_size = 16;
    int16_t audio_format = 1;
    int16_t num_channels = 1;
    int32_t byte_rate = sample_rate * 2;
    int16_t block_align = 2;
    int16_t bits_per_sample = 16;

    fwrite(&fmt_size, 4, 1, f);
    fwrite(&audio_format, 2, 1, f);
    fwrite(&num_channels, 2, 1, f);
    fwrite(&sample_rate, 4, 1, f);
    fwrite(&byte_rate, 4, 1, f);
    fwrite(&block_align, 2, 1, f);
    fwrite(&bits_per_sample, 2, 1, f);

    fwrite("data", 1, 4, f);
    fwrite(&data_size, 4, 1, f);

    for (size_t i = 0; i < num_samples; ++i) {
        float val = data[i];
        if (val > 1.0f) val = 1.0f;
        if (val < -1.0f) val = -1.0f;
        int16_t sample = (int16_t)(val * 32767.0f);
        fwrite(&sample, 2, 1, f);
    }

    fclose(f);
    LOGD("Saved WAV file: %s (%zu samples)", path, num_samples);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_ni_shikatu_rex_Whisper_saveDebugWavNative(JNIEnv *env, jclass clazz, jfloatArray samples, jstring output_path) {
    const char *path = env->GetStringUTFChars(output_path, nullptr);
    jsize len = env->GetArrayLength(samples);
    jfloat *data = env->GetFloatArrayElements(samples, nullptr);

    bool result = save_wav_file(path, data, len);

    env->ReleaseFloatArrayElements(samples, data, JNI_ABORT);
    env->ReleaseStringUTFChars(output_path, path);

    return (jboolean)result;
}



static void apply_lowpass_filter(std::vector<float>& data, int kernel_size = 5) {
    if (data.size() < (size_t)kernel_size) return;

    std::vector<float> filtered(data.size());
    int half = kernel_size / 2;

    for (size_t i = 0; i < data.size(); ++i) {
        float sum = 0.0f;
        int count = 0;
        for (int j = -half; j <= half; ++j) {
            int idx = (int)i + j;
            if (idx >= 0 && idx < (int)data.size()) {
                sum += data[idx];
                count++;
            }
        }
        filtered[i] = sum / count;
    }

    data = std::move(filtered);
}

JNIEXPORT jfloatArray JNICALL
Java_ni_shikatu_rex_Whisper_convertOggOpusToSamples(JNIEnv *env, jclass clazz, jstring file_path) {
    const char *path = env->GetStringUTFChars(file_path, nullptr);
    if (!path) return nullptr;

    LOGD("Opening Opus file: %s", path);

    int error = OPUS_OK;
    OggOpusFile *opusFile = op_open_file(path, &error);
    env->ReleaseStringUTFChars(file_path, path);

    if (!opusFile || error != OPUS_OK) {
        LOGE("Opus open error: %d", error);
        return nullptr;
    }

    ogg_int64_t totalSamples48k = op_pcm_total(opusFile, -1);
    LOGD("Total samples at 48kHz: %lld", (long long)totalSamples48k);

    const OpusHead *head = op_head(opusFile, -1);
    int channels = head ? head->channel_count : 1;
    LOGD("Channels: %d", channels);

    std::vector<opus_int16> pcm48k(totalSamples48k * channels);
    int samplesRead = 0;
    while (samplesRead < totalSamples48k) {
        int remaining = (totalSamples48k - samplesRead) * channels;
        int n = op_read(opusFile, pcm48k.data() + samplesRead * channels, remaining, nullptr);
        if (n <= 0) break;
        samplesRead += n;
    }
    op_free(opusFile);

    LOGD("Samples read: %d", samplesRead);

    if (samplesRead == 0) {
        LOGE("No samples read from Opus file");
        return nullptr;
    }

    std::vector<float> mono48k(samplesRead);
    for (int i = 0; i < samplesRead; ++i) {
        float sum = 0.0f;
        for (int c = 0; c < channels; ++c) {
            sum += pcm48k[i * channels + c];
        }
        mono48k[i] = sum / (32768.0f * channels);
    }

    apply_lowpass_filter(mono48k, 5);

    size_t frames16 = samplesRead / 3;
    std::vector<float> pcm16k(frames16);

    for (size_t i = 0; i < frames16; ++i) {
        float src_pos = i * 3.0f;
        size_t idx = (size_t)src_pos;
        float frac = src_pos - idx;

        float val;
        if (idx + 1 < mono48k.size()) {
            val = mono48k[idx] * (1.0f - frac) + mono48k[idx + 1] * frac;
        } else {
            val = mono48k[idx];
        }

        val = std::max(-1.0f, std::min(1.0f, val));
        pcm16k[i] = val;
    }

    float max_val = 0.0f, sum_sq = 0.0f;
    for (size_t i = 0; i < pcm16k.size(); ++i) {
        float abs_val = std::abs(pcm16k[i]);
        if (abs_val > max_val) max_val = abs_val;
        sum_sq += pcm16k[i] * pcm16k[i];
    }
    float rms = std::sqrt(sum_sq / pcm16k.size());
    LOGD("Audio stats: samples=%zu, duration=%.2fs, max=%.4f, RMS=%.4f",
         pcm16k.size(), (float)pcm16k.size() / 16000.0f, max_val, rms);

    jfloatArray result = env->NewFloatArray(pcm16k.size());
    env->SetFloatArrayRegion(result, 0, pcm16k.size(), pcm16k.data());
    return result;
}

} // extern "C"