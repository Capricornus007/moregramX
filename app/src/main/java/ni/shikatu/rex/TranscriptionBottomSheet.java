package ni.shikatu.rex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.charts.LayoutHelper;
import org.thunderdog.challegram.core.Background;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TGMessage;
import org.thunderdog.challegram.data.TGSource;
import org.thunderdog.challegram.loader.AvatarReceiver;
import org.thunderdog.challegram.loader.ComplexReceiver;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.ToggleHeaderView2;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.ColorState;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Fonts;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.BottomSheetViewController;
import org.thunderdog.challegram.unsorted.Settings;
import org.thunderdog.challegram.util.ScrollJumpCompensator;
import org.thunderdog.challegram.util.text.Text;
import org.thunderdog.challegram.util.text.TextColorSet;
import org.thunderdog.challegram.util.text.TextEntity;
import org.thunderdog.challegram.util.text.TextStyleProvider;
import org.thunderdog.challegram.util.text.TextWrapper;
import org.thunderdog.challegram.v.CustomRecyclerView;
import org.thunderdog.challegram.widget.BottomInsetFrameLayout;
import org.thunderdog.challegram.widget.PopupLayout;
import org.thunderdog.challegram.widget.TextView;
import org.thunderdog.challegram.widget.ViewPager;

import java.util.concurrent.TimeUnit;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.ListAnimator;
import me.vkryl.android.animator.ReplaceAnimator;
import me.vkryl.android.widget.FrameLayoutFix;

public class TranscriptionBottomSheet extends BottomSheetViewController.BottomSheetBaseRecyclerViewController<TranscriptionBottomSheet.Args>
        implements BottomSheetViewController.BottomSheetBaseControllerPage {

  private final ReplaceAnimator<TextWrapper> textAnimator;
  private ComplexReceiver textMediaReceiver;
  private final Wrapper parent;

  private TdApi.VoiceNote voiceNote;
  private TGMessage message;
  private boolean isTranscribing = false;
  private String transcribedText; // Для копирования

  private FrameLayout wrapView;
  private CustomRecyclerView recyclerView;
  private TranscriptionTextView transcriptionTextView;
  private ToggleHeaderView2 headerCell;
  private @Nullable View senderAvatarView;
  private @Nullable LinearLayout linearLayout;
  private @Nullable AvatarReceiver avatarReceiver;
  private @Nullable SenderTextView senderTextView;
  private @Nullable TextView dateTextView;

  private int currentHeight = -1;
  private int prevHeight = -1;

  private TranscriptionBottomSheet(Context context, Tdlib tdlib, Wrapper parent) {
    super(context, tdlib);
    this.parent = parent;
    textAnimator = new ReplaceAnimator<>(this::updateTexts, AnimatorUtils.DECELERATE_INTERPOLATOR, 300L);
  }

  private void updateTexts(ReplaceAnimator<?> animator) {
    if (transcriptionTextView != null) {
      transcriptionTextView.invalidate();
    }
    if (prevHeight <= 0) {
      return;
    }

    int newHeight = getTextAnimatedHeight();
    // Принудительно запрашиваем перерисовку, если высота изменилась
    if (newHeight != transcriptionTextView.getMeasuredHeight()) {
      transcriptionTextView.requestLayout();
    }

    // Компенсация скролла, чтобы текст не прыгал при обновлении
    int heightDiff = newHeight - currentHeight;
    int contentOffset = parent.getContentOffset();
    int topEdge = parent.getTopEdge();

    if (heightDiff > 0 && (topEdge > contentOffset)) {
      scrollCompensation(heightDiff);
    }

    currentHeight = newHeight;
    prevHeight = currentHeight;
  }

  @SuppressWarnings("deprecation")
  private void scrollCompensation(int heightDiff) {
    if (recyclerView != null && transcriptionTextView != null) {
      ScrollJumpCompensator listener = new ScrollJumpCompensator(recyclerView, transcriptionTextView, heightDiff);
      listener.add();
    }
  }

  @Override
  public boolean supportsBottomInset() {
    return true;
  }

  @Override
  protected void onBottomInsetChanged(int extraBottomInset, int extraBottomInsetWithoutIme, boolean isImeInset) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset);
    if (wrapView instanceof BottomInsetFrameLayout) {
      ((BottomInsetFrameLayout) wrapView).setBottomInset(extraBottomInsetWithoutIme);
    }
  }

  @Override
  protected boolean needRecyclerBottomInset() {
    return false;
  }

  @Override
  protected FrameLayout createFrameLayout(Context context) {
    if (Settings.instance().useEdgeToEdge()) {
      return new BottomInsetFrameLayout(context);
    } else {
      return super.createFrameLayout(context);
    }
  }

  protected View onCreateView(Context context) {
    headerView = new HeaderView(context);

    headerCell = new ToggleHeaderView2(context);
    headerCell.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(67f), Gravity.TOP, Screen.dp(56), 0, Screen.dp(60), 0));
    headerCell.setTitle(Lang.getString(R.string.Transcription), false);
    headerCell.setSubtitle(Lang.getString(R.string.TranscriptionProcessing), false);
    headerCell.setTranslationY(Screen.dp(7.5f));
    addThemeInvalidateListener(headerCell);

    headerView.initWithSingleController(this, false);
    headerView.getFilling().setShadowAlpha(0f);
    headerView.getBackButton().setIsReverse(true);
    headerView.setBackgroundHeight(Screen.dp(67));
    headerView.setWillNotDraw(false);
    addThemeInvalidateListener(headerView);

    wrapView = (FrameLayout) super.onCreateView(context);
    wrapView.setBackgroundColor(0);
    wrapView.setBackground(null);
    // Важно: не клипать детей, чтобы тень хедера и контент корректно отображались
    wrapView.setClipChildren(false);
    wrapView.setClipToPadding(false);

    if (message != null) {
      senderAvatarView = new View(context) {
        @Override
        protected void onAttachedToWindow() {
          super.onAttachedToWindow();
          avatarReceiver.attach();
        }

        @Override
        protected void onDetachedFromWindow() {
          super.onDetachedFromWindow();
          avatarReceiver.detach();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
          super.onMeasure(widthMeasureSpec, heightMeasureSpec);
          avatarReceiver.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }

        @Override
        protected void onDraw(Canvas canvas) {
          super.onDraw(canvas);
          if (avatarReceiver.needPlaceholder())
            avatarReceiver.drawPlaceholder(canvas);
          avatarReceiver.draw(canvas);
        }
      };
      avatarReceiver = new AvatarReceiver(senderAvatarView);
      message.requestAvatar(avatarReceiver, true);
      wrapView.addView(senderAvatarView, FrameLayoutFix.newParams(Screen.dp(20), Screen.dp(20), Gravity.LEFT | Gravity.BOTTOM, Screen.dp(18), 0, 0, Screen.dp(16)));

      linearLayout = new LinearLayout(context);
      linearLayout.setOrientation(LinearLayout.HORIZONTAL);

      senderTextView = new SenderTextView(context);
      TGSource forwardInfo = message.getForwardInfo();
      if (forwardInfo != null) {
        senderTextView.setText(forwardInfo.getAuthorName());
      } else {
        senderTextView.setText(message.getSender().getName());
      }
      linearLayout.addView(senderTextView, LayoutHelper.createLinear(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 2, Gravity.LEFT | Gravity.CENTER_VERTICAL));

      if (!message.isFakeMessage() && !message.isSponsoredMessage()) {
        dateTextView = new TextView(context);
        dateTextView.setTextColor(Theme.getColor(ColorId.textLight));
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        dateTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        dateTextView.setText(Lang.dateYearShortTime(message.getComparingDate(), TimeUnit.SECONDS));
        dateTextView.setMaxLines(1);
        linearLayout.addView(dateTextView, LayoutHelper.createLinear(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, Gravity.RIGHT | Gravity.CENTER_VERTICAL, Screen.dp(12), 0, 0, 0));
      }
      wrapView.addView(linearLayout, FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(20), Gravity.BOTTOM, Screen.dp(44), 0, Screen.dp(18), Screen.dp(16)));
    }

    transcriptionTextView = new TranscriptionTextView(context);
    textMediaReceiver = new ComplexReceiver(transcriptionTextView);

    recyclerView.setItemAnimator(null);
    recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
    recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
      @NonNull
      @Override
      public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(transcriptionTextView) {};
      }

      @Override
      public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}

      @Override
      public int getItemCount() {
        return 1;
      }
    });

    // Отступ снизу для RecyclerView, чтобы текст не налипал на аватарку
    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) recyclerView.getLayoutParams();
    if (message != null) {
      layoutParams.bottomMargin = Screen.dp(48 - 6);
    }

    setTranscriptionText(Lang.getString(R.string.TranscriptionProcessing), false);
    startTranscription();

    return wrapView;
  }

  private void startTranscription() {
    if (voiceNote == null || voiceNote.voice == null || isTranscribing) {
      return;
    }

    TdApi.File file = voiceNote.voice;

    // Check cache first
    TranscriptionCache cache = TranscriptionCache.getInstance(context());
    TranscriptionCache.CachedTranscription cached = cache.get(file.id);
    if (cached != null) {
      showResult(cached.text, cached.language);
      return;
    }

    isTranscribing = true;

    if (!file.local.isDownloadingCompleted) {
      updateStatus(Lang.getString(R.string.TranscriptionDownloading), false);
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
    updateStatus(Lang.getString(R.string.TranscriptionProcessing), false);

    String modelPath = ReXConfig.getWhisperModelPath();
    if (modelPath == null || modelPath.isEmpty() || !ReXConfig.isWhisperModelDownloaded()) {
      showError(Lang.getString(R.string.TranscriptionErrorNoModel));
      return;
    }

    final int fileId = voiceNote.voice.id;
    final Context ctx = context();

    Background.instance().post(() -> {
      try {
        float[] samples = Whisper.convertVoiceMessage(filePath);
        if (samples == null || samples.length == 0) {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorConvert)));
          return;
        }

        Whisper whisper = new Whisper();
        boolean loaded = whisper.loadModel(modelPath, false);
        if (!loaded) {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorModel)));
          return;
        }

        // ВАЖНО: Тут можно добавить стриминг токенов, если Whisper поддерживает коллбэк,
        // тогда можно обновлять UI чаще.
        TranscriptionResult result = whisper.transcribe(samples, 4, "auto", false);
        whisper.release();

        if (result != null && result.getFullText() != null) {
          String text = result.getFullText().trim();
          String lang = result.getDetectedLanguage();
          if (text.isEmpty()) {
            UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorEmpty)));
          } else {
            // Save to cache
            TranscriptionCache.getInstance(ctx).put(fileId, text, lang);
            UI.post(() -> showResult(text, lang));
          }
        } else {
          UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorFailed)));
        }

      } catch (Exception e) {
        Log.e("Transcription error", e);
        UI.post(() -> showError(Lang.getString(R.string.TranscriptionErrorFailed)));
      } finally {
        isTranscribing = false;
      }
    });
  }

  private void updateStatus(String status, boolean animated) {
    if (headerCell != null) {
      headerCell.setSubtitle(status, animated);
    }
  }

  private void showResult(String text, @Nullable String language) {
    this.transcribedText = text;
    this.isTranscribing = false;

    if (headerCell != null) {
      headerCell.setTitle(Lang.getString(R.string.Transcription), true);
      if (language != null && !language.isEmpty()) {
        String langName = Lang.getLanguageName(language, language);
        headerCell.setSubtitle(Lang.getString(R.string.TranscriptionDetectedLanguage, langName), true);
      } else {
        headerCell.setSubtitle(Lang.getString(R.string.TranscriptionComplete), true);
      }
    }

    setTranscriptionText(text, true);
  }

  private void showError(String error) {
    isTranscribing = false;
    if (headerCell != null) {
      headerCell.setTitle(Lang.getString(R.string.TranscriptionError), true);
      headerCell.setSubtitle(error, true);
    }
    setTranscriptionText(error, true);
  }

  private void setTranscriptionText(String text, boolean animate) {
    TdApi.FormattedText formattedText = new TdApi.FormattedText(text, new TdApi.TextEntity[0]);
    TextWrapper wrapper = new TextWrapper(formattedText.text, TGMessage.getTextStyleProvider(), parent.textColorSet)
            .setEntities(TextEntity.valueOf(tdlib, formattedText, null), (w, t, m) -> {
              if (transcriptionTextView != null) transcriptionTextView.invalidate();
            })
            .addTextFlags(Text.FLAG_BIG_EMOJI);

    if (currentTextWidth > 0) {
      wrapper.prepare(currentTextWidth);
    }

    textAnimator.replace(wrapper, animate);
    updateTexts(textAnimator);
  }

  public void setHeaderPosition(float y) {
    float y2 = parent.getTargetHeight() - Screen.dp(48);
    float y3 = y + parent.getHeaderHeight();
    float translation = Math.max(y3 - y2, 0);

    if (senderAvatarView != null) senderAvatarView.setTranslationY(translation);
    if (linearLayout != null) linearLayout.setTranslationY(translation);
  }

  @Override
  public boolean needsTempUpdates() {
    return true;
  }

  @Override
  public void onThemeColorsChanged(boolean areTemp, ColorState state) {
    super.onThemeColorsChanged(areTemp, state);
    if (headerView != null) {
      headerView.resetColors(this, null);
    }
  }

  @Override
  protected void onCreateView(Context context, CustomRecyclerView recyclerView) {
    this.recyclerView = recyclerView;
  }

  private int currentTextWidth = -1;

  private void measureText(int width) {
    currentTextWidth = width;
    for (ListAnimator.Entry<TextWrapper> entry : textAnimator) {
      entry.item.prepare(width);
      entry.item.requestMedia(textMediaReceiver, 0, Integer.MAX_VALUE);
    }
  }

  private int getTextAnimatedHeight() {
    float height = 0;
    for (ListAnimator.Entry<TextWrapper> entry : textAnimator) {
      height += entry.item.getHeight() * entry.getVisibility();
    }
    return Math.max((int) height, Screen.dp(48));
  }

  @Override
  public void onScrollToTopRequested() {
    if (recyclerView.getAdapter() != null) {
      try {
        LinearLayoutManager manager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        getRecyclerView().stopScroll();
        int firstVisiblePosition = manager.findFirstVisibleItemPosition();
        if (firstVisiblePosition == RecyclerView.NO_POSITION) {
          return;
        }
        int scrollTop = 0;
        View view = manager.findViewByPosition(firstVisiblePosition);
        if (view != null) {
          scrollTop -= view.getTop();
        }
        getRecyclerView().smoothScrollBy(0, -scrollTop);
      } catch (Throwable t) {
        Log.w("Cannot scroll to top", t);
      }
    }
  }

  @Override
  public int getItemsHeight(RecyclerView parent) {
    return -1;
  }

  public HeaderView getHeaderView() {
    return headerView;
  }

  @Override
  public View getCustomHeaderCell() {
    return headerCell;
  }

  @Override
  public boolean needBottomDecorationOffsets(RecyclerView parent) {
    return false;
  }

  @Override
  public CustomRecyclerView getRecyclerView() {
    return recyclerView;
  }

  @Override
  protected int getHeaderTextColorId() {
    return ColorId.text;
  }

  @Override
  protected int getHeaderColorId() {
    return ColorId.filling;
  }

  @Override
  protected int getHeaderIconColorId() {
    return ColorId.icon;
  }

  @Override
  protected int getBackButton() {
    return BackHeaderButton.TYPE_CLOSE;
  }

  @Override
  public int getId() {
    return R.id.controller_whisperTranscription;
  }

  @Override
  public void setArguments(Args args) {
    super.setArguments(args);
    this.voiceNote = args.voiceNote;
    this.message = args.message;
  }

  public void copyText() {
    if (transcribedText != null && !transcribedText.isEmpty()) {
      UI.copyText(transcribedText, R.string.CopiedText);
    }
  }

  // --- Inner Classes ---

  public static class Args {
    final TdApi.VoiceNote voiceNote;
    final @Nullable TGMessage message;

    public Args(TdApi.VoiceNote voiceNote, @Nullable TGMessage message) {
      this.voiceNote = voiceNote;
      this.message = message;
    }
  }

  public static class Wrapper extends BottomSheetViewController<Args> {
    private final TranscriptionBottomSheet controller;
    private final ViewController<?> parentController;
    public TextColorSet textColorSet;

    public Wrapper(Context context, Tdlib tdlib, ViewController<?> parent) {
      super(context, tdlib);
      this.parentController = parent;
      this.controller = new TranscriptionBottomSheet(context, tdlib, this);
      this.textColorSet = () -> Theme.getColor(ColorId.text);
    }

    @Override
    public boolean supportsBottomInset() {
      return controller.supportsBottomInset();
    }

    @Override
    public void dispatchSystemInsets(View parentView, ViewGroup.MarginLayoutParams originalParams, Rect legacyInsets, Rect insets, Rect insetsWithoutIme, Rect systemInsets, Rect systemInsetsWithoutIme, boolean fitsSystemWindows) {
      super.dispatchSystemInsets(parentView, originalParams, legacyInsets, insets, insetsWithoutIme, systemInsets, systemInsetsWithoutIme, fitsSystemWindows);
      controller.dispatchSystemInsets(parentView, originalParams, legacyInsets, insets, insetsWithoutIme, systemInsets, systemInsetsWithoutIme, fitsSystemWindows);
    }

    @Override
    protected void onBeforeCreateView() {
      controller.setArguments(getArguments());
      controller.getValue();
    }

    @Override
    protected HeaderView onCreateHeaderView() {
      return controller.getHeaderView();
    }

    @Override
    protected void onCreateView(Context context, FrameLayoutFix contentView, ViewPager pager) {
      pager.setOffscreenPageLimit(1);
      tdlib.ui().post(this::launchOpenAnimation);
    }

    @Override
    protected void onAfterCreateView() {
      setLickViewColor(Theme.getColor(ColorId.headerLightBackground));
    }

    @Override
    public void onThemeColorsChanged(boolean areTemp, ColorState state) {
      super.onThemeColorsChanged(areTemp, state);
      setLickViewColor(Theme.getColor(ColorId.headerLightBackground));
    }

    @Override
    protected void setupPopupLayout(PopupLayout popupLayout) {
      popupLayout.setBoundController(controller);
      if (Settings.instance().useEdgeToEdge()) {
        popupLayout.setNeedRootInsets();
      }
      popupLayout.setPopupHeightProvider(this);
      popupLayout.init(true);
      popupLayout.setTouchProvider(this);
      popupLayout.setTag(parentController);
    }

    @Override
    protected void setHeaderPosition(float y) {
      super.setHeaderPosition(y);
      controller.setHeaderPosition(y);
    }

    @Override
    public int getId() {
      return controller.getId();
    }

    @Override
    protected int getPagerItemCount() {
      return 1;
    }

    @Override
    protected ViewController<?> onCreatePagerItemForPosition(Context context, int position) {
      if (position != 0) return null;
      setHeaderPosition(getContentOffset() + HeaderView.getTopOffset());
      setDefaultListenersAndDecorators(controller);
      return controller;
    }

    @Override
    protected int getContentOffset() {
      return (getTargetHeight() - getHeaderHeight(true)) / 3;
    }

    @Override
    protected int getHeaderHeight() {
      return Screen.dp(67);
    }

    @Override
    protected boolean canHideByScroll() {
      return true;
    }

    @Override
    protected int getHideByScrollBorder() {
      return Math.min(controller.getTextAnimatedHeight() / 2 + Screen.dp(48), getTargetHeight() / 3);
    }

    @Override
    protected int getBackgroundColorId() {
      return ColorId.filling;
    }

    @Override
    public int getTopEdge() {
      return super.getTopEdge();
    }
  }

  private class TranscriptionTextView extends View {
    public TranscriptionTextView(Context context) {
      super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      // Screen.dp(36) = 18dp left padding + 18dp right padding
      measureText(MeasureSpec.getSize(widthMeasureSpec) - Screen.dp(36));

      int textHeight = getTextAnimatedHeight();
      if (prevHeight <= 0) {
        currentHeight = textHeight;
        prevHeight = currentHeight;
      }
      // Screen.dp(12) - небольшой отступ снизу
      super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(textHeight + Screen.dp(12), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
      for (ListAnimator.Entry<TextWrapper> entry : textAnimator) {
        // Отрисовка текста с отступами как в оригинальном мессенджере
        entry.item.draw(canvas, Screen.dp(18), getMeasuredWidth() - Screen.dp(18), 0, Screen.dp(6), null, entry.getVisibility(), textMediaReceiver);
      }
    }

    @Override
    protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      textMediaReceiver.attach();
    }

    @Override
    protected void onDetachedFromWindow() {
      super.onDetachedFromWindow();
      textMediaReceiver.detach();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      if (super.onTouchEvent(event)) return true;
      for (ListAnimator.Entry<TextWrapper> entry : textAnimator) {
        if (entry.getVisibility() == 1f && entry.item.onTouchEvent(this, event)) {
          return true;
        }
      }

      if (event.getAction() == MotionEvent.ACTION_UP) {
        copyText();
      }

      return true;
    }
  }

  // Custom View для имени отправителя, идентичный TranslationControllerV2
  private static class SenderTextView extends View {
    private String senderString;
    private Text senderText;

    public SenderTextView(Context context) {
      super(context);
    }

    public void setText(String text) {
      senderString = text;
      requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      updateSenderText(getMeasuredWidth());
    }

    protected void updateSenderText(int maxWidth) {
      TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
      textPaint.setTypeface(Fonts.getRobotoMedium());
      TextStyleProvider textStyleProvider = new TextStyleProvider(textPaint);
      textStyleProvider.setTextSize(12);
      senderText = new Text.Builder(senderString, maxWidth, textStyleProvider, () -> Theme.getColor(ColorId.textLight))
              .singleLine()
              .clipTextArea()
              .view(this)
              .build();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      if (senderText != null) {
        senderText.draw(canvas, 0, (getMeasuredHeight() - Screen.dp(12)) / 2 - Screen.dp(1));
      }
    }
  }

  public static void show(ViewController<?> parent, TdApi.VoiceNote voiceNote, @Nullable TGMessage message) {
    Wrapper wrapper = new Wrapper(parent.context(), parent.tdlib(), parent);
    wrapper.setArguments(new Args(voiceNote, message));
    wrapper.show();
  }
}