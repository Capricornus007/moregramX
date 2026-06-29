package ni.shikatu.rex;

import android.app.AlertDialog;
import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Toast;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Background;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.SettingsWrapBuilder;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ReXSettingsController extends RecyclerViewController<Void> implements View.OnClickListener, RecyclerViewController.SettingsIntDelegate {
    private SettingsAdapter adapter;
    private Context context;

    private static final int ID_HIDE_BUTTONS = 1;
    private static final int ID_HIDE_BUTTONS_COMMANDS = 2;
    private static final int ID_HIDE_BUTTONS_CAMERA = 3;
    private static final int ID_HIDE_BUTTONS_SENDER = 4;
    private static final int ID_ENABLE_MESSAGE_ANIMATOR = 5;

    // Whisper settings
    private static final int ID_WHISPER_MODEL = 10;
    private static final int ID_WHISPER_DOWNLOAD = 11;
    private static final int ID_WHISPER_DELETE = 12;

    private boolean isDownloading = false;
    private int downloadProgress = 0;
    public ReXSettingsController(Context context, Tdlib tdlib) {
        super(context, tdlib);
        this.context = context;

    }

    @Override
    public CharSequence getName() {
        return Lang.getString(R.string.reXSettings);
    }

    @Override
    protected void onCreateView(Context context, CustomRecyclerView recyclerView) {
        adapter = new SettingsAdapter(this) {
            @Override
            public void setValuedSetting(ListItem item, SettingView view, boolean isUpdate) {
                int id = item.getId();
                if (id == ID_HIDE_BUTTONS) {
                    view.setData(getHiddenButtonsSummary());
                } else if (id == ID_ENABLE_MESSAGE_ANIMATOR) {
                    view.getToggler().setRadioEnabled(ReXConfig.isMessageAnimatorEnabled(), isUpdate);
                } else if (id == ID_WHISPER_MODEL) {
                    String model = ReXConfig.getWhisperModel();
                    if (model.isEmpty()) {
                        view.setData(Lang.getString(R.string.WhisperModelNotSelected));
                    } else {
                        view.setData(ReXConfig.getWhisperModelDisplayName(model));
                    }
                } else if (id == ID_WHISPER_DOWNLOAD) {
                    if (isDownloading) {
                        view.setData(Lang.getString(R.string.WhisperDownloading) + " " + downloadProgress + "%");
                    } else if (ReXConfig.isWhisperModelDownloaded()) {
                        view.setData(Lang.getString(R.string.WhisperModelReady));
                    } else {
                        String model = ReXConfig.getWhisperModel();
                        if (model.isEmpty()) {
                            view.setData(Lang.getString(R.string.WhisperSelectModelFirst));
                        } else {
                            view.setData(Lang.getString(R.string.WhisperClickToDownload));
                        }
                    }
                }
            }
        };

        final ListItem[] rawItems = {
                new ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.reXSettings),
                new ListItem(ListItem.TYPE_SHADOW_TOP),
                new ListItem(ListItem.TYPE_VALUED_SETTING, ID_HIDE_BUTTONS, 0, R.string.HideButtonInInputField),
                new ListItem(ListItem.TYPE_SHADOW_BOTTOM),
                new ListItem(ListItem.TYPE_RADIO_SETTING, ID_ENABLE_MESSAGE_ANIMATOR, 0, R.string.EnableMessageAnimator),

                // Whisper section
                new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.WhisperSettings),
                new ListItem(ListItem.TYPE_SHADOW_TOP),
                new ListItem(ListItem.TYPE_VALUED_SETTING, ID_WHISPER_MODEL, R.drawable.baseline_mic_24, R.string.WhisperModel),
                new ListItem(ListItem.TYPE_SEPARATOR),
                new ListItem(ListItem.TYPE_VALUED_SETTING, ID_WHISPER_DOWNLOAD, R.drawable.baseline_cloud_download_24, R.string.WhisperDownload),
                new ListItem(ListItem.TYPE_SHADOW_BOTTOM),
        };

        adapter.setItems(rawItems, false);
        recyclerView.setAdapter(adapter);
    }

    private String getHiddenButtonsSummary() {
        Set<String> hiddenButtons = ReXConfig.getHiddenInputButtons();
        if (hiddenButtons == null || hiddenButtons.isEmpty()) {
            return Lang.getString(R.string.NothingHid);
        }
        return hiddenButtons.stream().map(s -> {
            if ("commands".equals(s)) return Lang.getString(R.string.hid_Camera);
            if ("camera".equals(s)) return Lang.getString(R.string.hid_Commands);
            if ("sendAs".equals(s)) return Lang.getString(R.string.hid_SendAs);
            return "";
        }).collect(Collectors.joining(", "));
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == ID_HIDE_BUTTONS) {
            showSettings(new SettingsWrapBuilder(id)
                    .setRawItems(new ListItem[]{
                            new ListItem(ListItem.TYPE_CHECKBOX_OPTION, ID_HIDE_BUTTONS_COMMANDS, 0, R.string.hid_Camera, ReXConfig.isCommandsButtonHidden()),
                            new ListItem(ListItem.TYPE_CHECKBOX_OPTION, ID_HIDE_BUTTONS_CAMERA, 0, R.string.hid_Commands, ReXConfig.isCameraButtonHidden()),
                            new ListItem(ListItem.TYPE_CHECKBOX_OPTION, ID_HIDE_BUTTONS_SENDER, 0, R.string.hid_SendAs, ReXConfig.isSendAsButtonHidden())
                    })
                    .setIntDelegate(this)
            );
        } else if (id == ID_ENABLE_MESSAGE_ANIMATOR) {
            ReXConfig.setIsMessageAnimatorEnabled(this.context, adapter.toggleView(v));
        } else if (id == ID_WHISPER_MODEL) {
            showWhisperModelPicker();
        } else if (id == ID_WHISPER_DOWNLOAD) {
            if (isDownloading) {
                // Already downloading
                return;
            }
            if (ReXConfig.isWhisperModelDownloaded()) {
                showDeleteModelDialog();
            } else {
                String model = ReXConfig.getWhisperModel();
                if (model.isEmpty()) {
                    Toast.makeText(context, R.string.WhisperSelectModelFirst, Toast.LENGTH_SHORT).show();
                } else {
                    startModelDownload(model);
                }
            }
        }
    }

    private void showWhisperModelPicker() {
        String[][] models = ReXConfig.WHISPER_MODELS;
        String[] names = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            names[i] = models[i][1];
        }

        String currentModel = ReXConfig.getWhisperModel();
        int selectedIndex = -1;
        for (int i = 0; i < models.length; i++) {
            if (models[i][0].equals(currentModel)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.WhisperSelectModel)
                .setSingleChoiceItems(names, selectedIndex, (dialog, which) -> {
                    String selectedModel = models[which][0];
                    // Check if model changed and we have old model downloaded
                    if (!selectedModel.equals(currentModel) && ReXConfig.isWhisperModelDownloaded()) {
                        // Delete old model file
                        File oldFile = new File(ReXConfig.getWhisperModelPath());
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                        ReXConfig.setWhisperModelPath(context, "");
                    }
                    ReXConfig.setWhisperModel(context, selectedModel);
                    adapter.updateValuedSettingById(ID_WHISPER_MODEL);
                    adapter.updateValuedSettingById(ID_WHISPER_DOWNLOAD);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    private void showDeleteModelDialog() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.WhisperDeleteModel)
                .setMessage(R.string.WhisperDeleteModelConfirm)
                .setPositiveButton(R.string.Delete, (dialog, which) -> {
                    File file = new File(ReXConfig.getWhisperModelPath());
                    if (file.exists()) {
                        file.delete();
                    }
                    ReXConfig.setWhisperModelPath(context, "");
                    adapter.updateValuedSettingById(ID_WHISPER_DOWNLOAD);
                    Toast.makeText(context, R.string.WhisperModelDeleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    private void startModelDownload(String modelId) {
        isDownloading = true;
        downloadProgress = 0;
        adapter.updateValuedSettingById(ID_WHISPER_DOWNLOAD);

        Background.instance().post(() -> {
            try {
                String urlStr = ReXConfig.getWhisperModelUrl(modelId);
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error: " + responseCode);
                }

                int fileLength = connection.getContentLength();
                File outputDir = ReXConfig.getWhisperModelsDir(context);
                File outputFile = new File(outputDir, ReXConfig.getWhisperModelFileName(modelId));
                File tempFile = new File(outputDir, ReXConfig.getWhisperModelFileName(modelId) + ".tmp");

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(tempFile)) {

                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;
                    int lastProgress = 0;

                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        output.write(buffer, 0, count);

                        if (fileLength > 0) {
                            int progress = (int) (total * 100 / fileLength);
                            if (progress != lastProgress) {
                                lastProgress = progress;
                                final int p = progress;
                                UI.post(() -> {
                                    downloadProgress = p;
                                    adapter.updateValuedSettingById(ID_WHISPER_DOWNLOAD);
                                });
                            }
                        }
                    }
                }

                // Rename temp file to final
                tempFile.renameTo(outputFile);

                UI.post(() -> {
                    isDownloading = false;
                    ReXConfig.setWhisperModelPath(context, outputFile.getAbsolutePath());
                    adapter.updateValuedSettingById(ID_WHISPER_DOWNLOAD);
                    Toast.makeText(context, R.string.WhisperDownloadComplete, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                UI.post(() -> {
                    isDownloading = false;
                    adapter.updateValuedSettingById(ID_WHISPER_DOWNLOAD);
                    Toast.makeText(context, Lang.getString(R.string.WhisperDownloadFailed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onApplySettings(int id, SparseIntArray result) {
        if (id == ID_HIDE_BUTTONS) {
            Set<String> hiddenButtons = new HashSet<>();
            if (result.get(ID_HIDE_BUTTONS_COMMANDS, 0) != 0) {
                hiddenButtons.add("commands");
            }
            if (result.get(ID_HIDE_BUTTONS_CAMERA, 0) != 0) {
                hiddenButtons.add("camera");
            }
            if(result.get(ID_HIDE_BUTTONS_SENDER, 0) != 0) {
                hiddenButtons.add("sendAs");
            }
            ReXConfig.setHiddenInputButtons(context, hiddenButtons);
            adapter.updateValuedSettingById(ID_HIDE_BUTTONS);
        }

    }

    @Override
    public int getId() {
        return R.id.controller_reXSettings;
    }
}
