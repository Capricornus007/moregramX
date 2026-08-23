package ni.shikatu.rex;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class ReXConfig {
    private static final String PREFS_NAME = "ReXSettings";
    private static final String KEY_HIDDEN_INPUT_BUTTONS = "hidden_input_buttons";
    private static final String KEY_MESSAGE_ANIMATOR_ENABLED = "message_animator_enabled";
    private static final String KEY_WHISPER_MODEL = "whisper_model";
    private static final String KEY_WHISPER_MODEL_PATH = "whisper_model_path";

    // Whisper model download URL template
    public static final String WHISPER_MODEL_URL_TEMPLATE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-%s.bin";

    // Public pre-converted models provided by whisper.cpp. Larger models may
    // need more RAM than a mobile device can provide; keeping them in the
    // picker still lets capable devices opt in without sideloading a model.
    public static final String[][] WHISPER_MODELS = {
        // {id, display name, size in MB}
        {"tiny", "Tiny (75 MB)", "75"},
        {"tiny-q5_1", "Tiny Q5 (31 MB)", "31"},
        {"tiny-q8_0", "Tiny Q8 (42 MB)", "42"},
        {"tiny.en", "Tiny English-only (75 MB)", "75"},
        {"tiny.en-q5_1", "Tiny English-only Q5 (31 MB)", "31"},
        {"tiny.en-q8_0", "Tiny English-only Q8 (42 MB)", "42"},
        {"base", "Base (142 MB)", "142"},
        {"base-q5_1", "Base Q5 (57 MB)", "57"},
        {"base-q8_0", "Base Q8 (78 MB)", "78"},
        {"base.en", "Base English-only (142 MB)", "142"},
        {"base.en-q5_1", "Base English-only Q5 (57 MB)", "57"},
        {"base.en-q8_0", "Base English-only Q8 (78 MB)", "78"},
        {"small", "Small (466 MB)", "466"},
        {"small-q5_1", "Small Q5 (181 MB)", "181"},
        {"small-q8_0", "Small Q8 (252 MB)", "252"},
        {"small.en", "Small English-only (466 MB)", "466"},
        {"small.en-q5_1", "Small English-only Q5 (181 MB)", "181"},
        {"small.en-q8_0", "Small English-only Q8 (252 MB)", "252"},
        {"medium", "Medium (1.5 GB)", "1536"},
        {"medium-q5_0", "Medium Q5 (514 MB)", "514"},
        {"medium-q8_0", "Medium Q8 (785 MB)", "785"},
        {"medium.en", "Medium English-only (1.5 GB)", "1536"},
        {"medium.en-q5_0", "Medium English-only Q5 (514 MB)", "514"},
        {"medium.en-q8_0", "Medium English-only Q8 (785 MB)", "785"},
        {"large-v1", "Large v1 (2.9 GB)", "2969"},
        {"large-v2", "Large v2 (2.9 GB)", "2969"},
        {"large-v2-q5_0", "Large v2 Q5 (1.1 GB)", "1126"},
        {"large-v2-q8_0", "Large v2 Q8 (1.6 GB)", "1579"},
        {"large-v3", "Large v3 (2.9 GB)", "2969"},
        {"large-v3-q5_0", "Large v3 Q5 (1.1 GB)", "1126"},
        {"large-v3-turbo", "Large v3 Turbo (1.5 GB)", "1536"},
        {"large-v3-turbo-q5_0", "Large v3 Turbo Q5 (547 MB)", "547"},
        {"large-v3-turbo-q8_0", "Large v3 Turbo Q8 (834 MB)", "834"},
    };

    private static Set<String> hiddenInputButtons = new HashSet<>();
    private static boolean isMessageAnimatorEnabled = false;
    private static String whisperModel = "";
    private static String whisperModelPath = "";

    public static void load(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        hiddenInputButtons = new HashSet<>(prefs.getStringSet(KEY_HIDDEN_INPUT_BUTTONS, Collections.emptySet()));
        isMessageAnimatorEnabled = prefs.getBoolean(KEY_MESSAGE_ANIMATOR_ENABLED, false);
        whisperModel = prefs.getString(KEY_WHISPER_MODEL, "");
        whisperModelPath = prefs.getString(KEY_WHISPER_MODEL_PATH, "");
    }

    public static void save(Context context) {
        if (context == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(KEY_HIDDEN_INPUT_BUTTONS, hiddenInputButtons);
        editor.putBoolean(KEY_MESSAGE_ANIMATOR_ENABLED, isMessageAnimatorEnabled);
        editor.putString(KEY_WHISPER_MODEL, whisperModel);
        editor.putString(KEY_WHISPER_MODEL_PATH, whisperModelPath);
        editor.apply();
    }

    public static Set<String> getHiddenInputButtons() {
        return hiddenInputButtons;
    }

    public static void setHiddenInputButtons(Context context, Set<String> newSet) {
        hiddenInputButtons = newSet;
        save(context);
    }

    public static void setIsMessageAnimatorEnabled(Context context, boolean enabled) {
        isMessageAnimatorEnabled = enabled;
        save(context);
    }

    public static boolean isMessageAnimatorEnabled() {
        return isMessageAnimatorEnabled;
    }


    public static boolean isCommandsButtonHidden() {
        return hiddenInputButtons.contains("commands");
    }

    public static boolean isCameraButtonHidden() {
        return hiddenInputButtons.contains("camera");
    }

    public static boolean isSendAsButtonHidden() { return hiddenInputButtons.contains("sendAs"); }

    // Whisper model settings
    public static String getWhisperModel() {
        return whisperModel;
    }

    public static void setWhisperModel(Context context, String model) {
        whisperModel = model;
        save(context);
    }

    public static String getWhisperModelPath() {
        return whisperModelPath;
    }

    public static void setWhisperModelPath(Context context, String path) {
        whisperModelPath = path;
        save(context);
    }

    public static boolean isWhisperModelDownloaded() {
        if (whisperModelPath == null || whisperModelPath.isEmpty()) {
            return false;
        }
        File file = new File(whisperModelPath);
        return file.exists() && file.length() > 0;
    }

    public static String getWhisperModelDisplayName(String modelId) {
        for (String[] model : WHISPER_MODELS) {
            if (model[0].equals(modelId)) {
                return model[1];
            }
        }
        return modelId;
    }

    public static String getWhisperModelUrl(String modelId) {
        return String.format(WHISPER_MODEL_URL_TEMPLATE, modelId);
    }

    public static File getWhisperModelsDir(Context context) {
        File dir = new File(context.getFilesDir(), "whisper_models");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String getWhisperModelFileName(String modelId) {
        return "ggml-" + modelId + ".bin";
    }
}
