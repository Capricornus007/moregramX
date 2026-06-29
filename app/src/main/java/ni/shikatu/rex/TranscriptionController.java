package ni.shikatu.rex;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Background;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;

public class TranscriptionController {

  public interface Callback {
    void onTranscriptionComplete(String text, @Nullable String language);
    void onTranscriptionError(String error);
  }

  private final Context context;
  private final Tdlib tdlib;
  private final TdApi.VoiceNote voiceNote;
  private AlertDialog dialog;
  private android.widget.TextView statusText;
  private android.widget.TextView resultText;
  private ProgressBar progressBar;
  private boolean isTranscribing = false;

  public TranscriptionController(Context context, Tdlib tdlib, TdApi.VoiceNote voiceNote) {
    this.context = context;
    this.tdlib = tdlib;
    this.voiceNote = voiceNote;
  }

  public void show() {
    // Create custom view
    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(Screen.dp(24f), Screen.dp(16f), Screen.dp(24f), Screen.dp(8f));

    // Progress bar
    progressBar = new ProgressBar(context);
    LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    );
    progressParams.gravity = Gravity.CENTER_HORIZONTAL;
    progressParams.bottomMargin = Screen.dp(16f);
    progressBar.setLayoutParams(progressParams);
    layout.addView(progressBar);

    // Status text
    statusText = new android.widget.TextView(context);
    statusText.setTextColor(Theme.textDecentColor());
    statusText.setTextSize(14f);
    statusText.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    );
    statusParams.bottomMargin = Screen.dp(16f);
    statusText.setLayoutParams(statusParams);
    statusText.setText(Lang.getString(R.string.TranscriptionLoading));
    layout.addView(statusText);

    // Result text (scrollable)
    ScrollView scrollView = new ScrollView(context);
    scrollView.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      Screen.dp(200f)
    ));

    resultText = new android.widget.TextView(context);
    resultText.setTextColor(Theme.textAccentColor());
    resultText.setTextSize(16f);
    resultText.setTextIsSelectable(true);
    resultText.setPadding(0, 0, 0, Screen.dp(8f));
    resultText.setVisibility(View.GONE);
    scrollView.addView(resultText);
    layout.addView(scrollView);

    // Create dialog
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.Transcription);
    builder.setView(layout);
    builder.setNegativeButton(R.string.Close, null);
    builder.setPositiveButton(R.string.Copy, (d, which) -> {
      if (resultText.getText() != null && resultText.getText().length() > 0) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("transcription", resultText.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, R.string.CopiedText, Toast.LENGTH_SHORT).show();
      }
    });

    dialog = builder.create();
    dialog.show();

    // Disable copy button until we have text
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

    // Start transcription
    startTranscription();
  }

  private void startTranscription() {
    if (voiceNote == null || voiceNote.voice == null) {
      showError(Lang.getString(R.string.TranscriptionErrorNoFile));
      return;
    }

    TdApi.File file = voiceNote.voice;

    // Check if file is downloaded
    if (!file.local.isDownloadingCompleted) {
      statusText.setText(Lang.getString(R.string.TranscriptionDownloading));

      // Download file first
      tdlib.client().send(new TdApi.DownloadFile(file.id, 1, 0, 0, true), result -> {
        if (result instanceof TdApi.File) {
          TdApi.File downloadedFile = (TdApi.File) result;
          if (downloadedFile.local.isDownloadingCompleted) {
            UI.post(() -> performTranscription(downloadedFile.local.path));
          } else {
            UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorDownload)));
          }
        } else {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorDownload)));
        }
      });
    } else {
      performTranscription(file.local.path);
    }
  }

  private void performTranscription(String filePath) {
    if (isTranscribing) return;
    isTranscribing = true;

    statusText.setText(Lang.getString(R.string.TranscriptionProcessing));

    // Check if model is loaded
    String modelPath = ReXConfig.getWhisperModelPath();
    if (modelPath == null || modelPath.isEmpty() || !ReXConfig.isWhisperModelDownloaded()) {
      showError(Lang.getString(R.string.TranscriptionErrorNoModel));
      return;
    }

    Background.instance().post(() -> {
      try {
        // Convert voice message to samples
        float[] samples = Whisper.convertVoiceMessage(filePath);
        if (samples == null || samples.length == 0) {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorConvert)));
          return;
        }

        // Load model and transcribe
        Whisper whisper = new Whisper();
        boolean loaded = whisper.loadModel(modelPath, false);
        if (!loaded) {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorModel)));
          return;
        }

        // TODO: Добавить выбор языка в UI
        // Пока используем "ru" для русского языка
        // "auto" пока не работает стабильно на ARM
        TranscriptionResult result = whisper.transcribe(samples, 4, "ru", false);
        whisper.release();

        if (result != null && result.getFullText() != null) {
          String text = result.getFullText().trim();
          if (text.isEmpty()) {
            UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorEmpty)));
          } else {
            UI.post(() -> showResult(text, result.getDetectedLanguage()));
          }
        } else {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorFailed)));
        }

      } catch (Exception e) {
        e.printStackTrace();
        UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorFailed) + ": " + e.getMessage()));
      } finally {
        isTranscribing = false;
      }
    });
  }

  private void showResult(String text, @Nullable String language) {
    progressBar.setVisibility(View.GONE);

    if (language != null && !language.isEmpty()) {
      String langName = Lang.getLanguageName(language, language);
      statusText.setText(Lang.getString(R.string.TranscriptionDetectedLanguage, langName));
    } else {
      statusText.setText(Lang.getString(R.string.TranscriptionComplete));
    }

    resultText.setText(text);
    resultText.setVisibility(View.VISIBLE);

    // Enable copy button
    if (dialog != null) {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }
  }

  private void showError(String error) {
    progressBar.setVisibility(View.GONE);
    statusText.setText(error);
    statusText.setTextColor(Theme.getColor(org.thunderdog.challegram.theme.ColorId.textNegative));
    isTranscribing = false;
  }
}
