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

    // Available Whisper models (recommended for mobile)
    public static final String[][] WHISPER_MODELS = {
        // {id, display name, size in MB}
        {"tiny", "Tiny (75 MB)", "75"},
        {"base", "Base (142 MB)", "142"},
        {"small", "Small (466 MB)", "466"},
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
