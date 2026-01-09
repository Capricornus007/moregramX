package ni.shikatu.rex;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.navigation.SettingsWrapBuilder;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

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
                if (item.getId() == ID_HIDE_BUTTONS) {
                    view.setData(getHiddenButtonsSummary());
                }
                if(item.getId() == ID_ENABLE_MESSAGE_ANIMATOR){
                    view.getToggler().setRadioEnabled(ReXConfig.isMessageAnimatorEnabled(), isUpdate);
                }
            }
        };

        final ListItem[] rawItems = {
                new ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.reXSettings),
                new ListItem(ListItem.TYPE_SHADOW_TOP),
                new ListItem(ListItem.TYPE_VALUED_SETTING, ID_HIDE_BUTTONS, 0, R.string.HideButtonInInputField),
                new ListItem(ListItem.TYPE_SHADOW_TOP),
                new ListItem(ListItem.TYPE_RADIO_SETTING, ID_ENABLE_MESSAGE_ANIMATOR, 0, R.string.EnableMessageAnimator)
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
        }
        if(id == ID_ENABLE_MESSAGE_ANIMATOR){
            ReXConfig.setIsMessageAnimatorEnabled(this.context, adapter.toggleView(v));
        }
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
