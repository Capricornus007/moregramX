package ni.shikatu.rex;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class ReXConfig {
    private static final String PREFS_NAME = "ReXSettings";
    private static final String KEY_HIDDEN_INPUT_BUTTONS = "hidden_input_buttons";
    private static final String KEY_MESSAGE_ANIMATOR_ENABLED = "message_animator_enabled";


    private static Set<String> hiddenInputButtons = new HashSet<>();
    private static boolean isMessageAnimatorEnabled = false;

    public static void load(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        hiddenInputButtons = new HashSet<>(prefs.getStringSet(KEY_HIDDEN_INPUT_BUTTONS, Collections.emptySet()));
        isMessageAnimatorEnabled = prefs.getBoolean(KEY_MESSAGE_ANIMATOR_ENABLED, false);
    }

    public static void save(Context context) {
        if (context == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(KEY_HIDDEN_INPUT_BUTTONS, hiddenInputButtons);
        editor.putBoolean(KEY_MESSAGE_ANIMATOR_ENABLED, isMessageAnimatorEnabled);
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
}
