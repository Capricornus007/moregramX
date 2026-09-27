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

    // {model id, sha256 of the .bin, exact size in bytes}
    // 取自 HuggingFace ggerganov/whisper.cpp 的 LFS oid（型別 sha256），2026-09-27 用
    // `curl "https://huggingface.co/api/models/ggerganov/whisper.cpp/tree/main?recursive=true"`
    // 產生，33 個模型與 WHISPER_MODELS 一一對齊。下載完成後邊寫邊算的雜湊要跟這裡對得上，
    // 否則刪掉重抓——只比內容長度的話， truncated/被中转层换掉的档案会被当成可用模型。
    public static final String[][] WHISPER_MODEL_CHECKSUMS = {
        {"tiny", "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21", "77691713"},
        {"tiny-q5_1", "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7", "32152673"},
        {"tiny-q8_0", "c2085835d3f50733e2ff6e4b41ae8a2b8d8110461e18821b09a15c40c42d1cca", "43537433"},
        {"tiny.en", "921e4cf8686fdd993dcd081a5da5b6c365bfde1162e72b08d75ac75289920b1f", "77704715"},
        {"tiny.en-q5_1", "c77c5766f1cef09b6b7d47f21b546cbddd4157886b3b5d6d4f709e91e66c7c2b", "32166155"},
        {"tiny.en-q8_0", "5bc2b3860aa151a4c6e7bb095e1fcce7cf12c7b020ca08dcec0c6d018bb7dd94", "43550795"},
        {"base", "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe", "147951465"},
        {"base-q5_1", "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898", "59707625"},
        {"base-q8_0", "c577b9a86e7e048a0b7eada054f4dd79a56bbfa911fbdacf900ac5b567cbb7d9", "81768585"},
        {"base.en", "a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002", "147964211"},
        {"base.en-q5_1", "4baf70dd0d7c4247ba2b81fafd9c01005ac77c2f9ef064e00dcf195d0e2fdd2f", "59721011"},
        {"base.en-q8_0", "a4d4a0768075e13cfd7e19df3ae2dbc4a68d37d36a7dad45e8410c9a34f8c87e", "81781811"},
        {"small", "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b", "487601967"},
        {"small-q5_1", "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb", "190085487"},
        {"small-q8_0", "49c8fb02b65e6049d5fa6c04f81f53b867b5ec9540406812c643f177317f779f", "264464607"},
        {"small.en", "c6138d6d58ecc8322097e0f987c32f1be8bb0a18532a3f88f734d1bbf9c41e5d", "487614201"},
        {"small.en-q5_1", "bfdff4894dcb76bbf647d56263ea2a96645423f1669176f4844a1bf8e478ad30", "190098681"},
        {"small.en-q8_0", "67a179f608ea6114bd3fdb9060e762b588a3fb3bd00c4387971be4d177958067", "264477561"},
        {"medium", "6c14d5adee5f86394037b4e4e8b59f1673b6cee10e3cf0b11bbdbee79c156208", "1533763059"},
        {"medium-q5_0", "19fea4b380c3a618ec4723c3eef2eb785ffba0d0538cf43f8f235e7b3b34220f", "539212467"},
        {"medium-q8_0", "42a1ffcbe4167d224232443396968db4d02d4e8e87e213d3ee2e03095dea6502", "823369779"},
        {"medium.en", "cc37e93478338ec7700281a7ac30a10128929eb8f427dda2e865faa8f6da4356", "1533774781"},
        {"medium.en-q5_0", "76733e26ad8fe1c7a5bf7531a9d41917b2adc0f20f2e4f5531688a8c6cd88eb0", "539225533"},
        {"medium.en-q8_0", "43fa2cd084de5a04399a896a9a7a786064e221365c01700cea4666005218f11c", "823382461"},
        {"large-v1", "7d99f41a10525d0206bddadd86760181fa920438b6b33237e3118ff6c83bb53d", "3094623691"},
        {"large-v2", "9a423fe4d40c82774b6af34115b8b935f34152246eb19e80e376071d3f999487", "3094623691"},
        {"large-v2-q5_0", "3a214837221e4530dbc1fe8d734f302af393eb30bd0ed046042ebf4baf70f6f2", "1080732091"},
        {"large-v2-q8_0", "fef54e6d898246a65c8285bfa83bd1807e27fadf54d5d4e81754c47634737e8c", "1656129691"},
        {"large-v3", "64d182b440b98d5203c4f9bd541544d84c605196c4f7b845dfa11fb23594d1e2", "3095033483"},
        {"large-v3-q5_0", "d75795ecff3f83b5faa89d1900604ad8c780abd5739fae406de19f23ecd98ad1", "1081140203"},
        {"large-v3-turbo", "1fc70f774d38eb169993ac391eea357ef47c88757ef72ee5943879b7e8e2bc69", "1624555275"},
        {"large-v3-turbo-q5_0", "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2", "574041195"},
        {"large-v3-turbo-q8_0", "317eb69c11673c9de1e1f0d459b253999804ec71ac4c23c17ecf5fbe24e259a1", "874188075"},
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
        if (!file.exists() || file.length() <= 0) {
            return false;
        }
        // 被抓到一半斷線、或被中轉層截短的檔案，光看「存在且長度>0」會被當成可用模型，
        // 然後在 whisper_init 那邊炸成難以理解的失敗。這裡比一次正確位元組數（常数時間，
        // 完整 SHA-256 只在下載完成時算一次，不在每次啟動時重算幾 GB）。
        long expectedBytes = getWhisperModelBytes(whisperModel);
        return expectedBytes <= 0 || file.length() == expectedBytes;
    }

    public static String getWhisperModelDisplayName(String modelId) {
        for (String[] model : WHISPER_MODELS) {
            if (model[0].equals(modelId)) {
                return model[1];
            }
        }
        return modelId;
    }

    /** 回傳該模型的 sha256（小寫 hex）；清單裡沒有這個 id 時回傳空字串。 */
    public static String getWhisperModelSha256(String modelId) {
        for (String[] c : WHISPER_MODEL_CHECKSUMS) {
            if (c[0].equals(modelId)) {
                return c[1];
            }
        }
        return "";
    }

    /** 回傳該模型的正確位元組數；未知 id 回傳 -1。 */
    public static long getWhisperModelBytes(String modelId) {
        for (String[] c : WHISPER_MODEL_CHECKSUMS) {
            if (c[0].equals(modelId)) {
                return Long.parseLong(c[2]);
            }
        }
        return -1L;
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
