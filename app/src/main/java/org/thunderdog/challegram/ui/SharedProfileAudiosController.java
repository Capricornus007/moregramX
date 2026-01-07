/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.thunderdog.challegram.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.core.LangUtils;
import org.thunderdog.challegram.data.InlineResult;
import org.thunderdog.challegram.data.InlineResultCommon;
import org.thunderdog.challegram.player.TGPlayerController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.v.MediaRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SharedProfileAudiosController extends SharedBaseController<InlineResult<?>> implements View.OnClickListener, TGPlayerController.TrackChangeListener, TGPlayerController.PlayListBuilder {

  private long userId;

  public SharedProfileAudiosController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  public void setUserId (long userId) {
    this.userId = userId;
  }

  @Override
  protected void onCreateView (Context context, MediaRecyclerView recyclerView, SettingsAdapter adapter) {
    super.onCreateView(context, recyclerView, adapter);
    tdlib.context().player().addTrackChangeListener(this);
  }

  @Override
  public void destroy () {
    super.destroy();
    tdlib.context().player().removeTrackChangeListener(this);
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.TabProfileAudios);
  }

  @Override
  public int getIcon () {
    return R.drawable.baseline_queue_music_24;
  }

  @Override
  protected String getExplainedTitle () {
    return Lang.getString(R.string.TabProfileAudios);
  }

  @Override
  protected TdApi.Function<?> buildRequest (long chatId, TdApi.MessageTopic topicId, String query, long offset, String secretOffset, int limit) {
    return new TdApi.GetUserProfileAudios(userId, (int) offset, limit);
  }

  @Override
  protected boolean supportsLoadingMore (boolean isMore) {
    return true;
  }

  @Override
  protected boolean canSearch () {
    return false;
  }

  @Override
  protected boolean supportsMessageContent () {
    return false;
  }

  @Override
  protected boolean probablyHasEmoji () {
    return true;
  }

  @Override
  protected boolean needDateSectionSplitting () {
    return false;
  }

  @Override
  protected InlineResult<?> parseObject (TdApi.Object object) {
    if (object instanceof TdApi.Audio) {
      TdApi.Audio audio = (TdApi.Audio) object;
      // Create a fake message to wrap the audio
      TdApi.Message message = new TdApi.Message();
      message.id = 0;
      message.senderId = new TdApi.MessageSenderUser(userId);
      message.chatId = chatId;
      message.content = new TdApi.MessageAudio(audio, null);
      message.date = 0;

      InlineResult<?> result = InlineResult.valueOf(context, tdlib, message);
      if (result != null) {
        if (result instanceof InlineResultCommon) {
          ((InlineResultCommon) result).setIsTrack(true);
        }
      }
      return result;
    }
    return null;
  }

  @Override
  public void onClick (View v) {
    ListItem item = (ListItem) v.getTag();
    if (item != null && item.getViewType() == ListItem.TYPE_CUSTOM_INLINE) {
      if (adapter.isInSelectMode()) {
        toggleSelected(item);
        return;
      }

      InlineResult<?> result = (InlineResult<?>) item.getData();
      if (result.getType() == InlineResult.TYPE_AUDIO) {
        tdlib.context().player().playPauseMessage(tdlib, result.getMessage(), this);
      }
    }
  }

  @Override
  protected CharSequence buildTotalCount (ArrayList<InlineResult<?>> data) {
    return Lang.pluralBold(R.string.xProfileAudios, data.size());
  }

  @Override
  protected int provideViewType () {
    return ListItem.TYPE_CUSTOM_INLINE;
  }

  @Override
  protected long getCurrentOffset (ArrayList<InlineResult<?>> data, long emptyValue) {
    return data == null || data.isEmpty() ? emptyValue : data.size();
  }

  // Playback

  @Override
  public void onTrackChanged (Tdlib tdlib, @Nullable TdApi.Message newTrack, int fileId, int state, float progress, boolean byUser) {
    setCurrentTrack(data, newTrack);
    if (isSearching()) {
      setCurrentTrack(searchData, newTrack);
    }
  }

  private static void setCurrentTrack (ArrayList<InlineResult<?>> results, TdApi.Message newTrack) {
    if (results == null || results.isEmpty()) {
      return;
    }
    if (newTrack == null) {
      for (InlineResult<?> result : results) {
        if (result instanceof InlineResultCommon) {
          ((InlineResultCommon) result).setIsTrackCurrent(false);
        }
      }
    } else {
      for (InlineResult<?> result : results) {
        if (result instanceof InlineResultCommon) {
          ((InlineResultCommon) result).setIsTrackCurrent(TGPlayerController.compareTracks(result.getMessage(), newTrack));
        }
      }
    }
  }

  @Nullable
  @Override
  public TGPlayerController.PlayList buildPlayList (TdApi.Message fromMessage) {
    ArrayList<InlineResult<?>> data = isSearching() ? this.searchData : this.data;
    if (data == null || data.isEmpty()) {
      throw new IllegalStateException();
    }
    ArrayList<TdApi.Message> out = new ArrayList<>(data.size());

    int foundIndex = -1;

    for (int i = 0; i < data.size(); i++) {
      InlineResult<?> result = data.get(i);
      if (result.getType() == InlineResult.TYPE_AUDIO) {
        TdApi.Message message = result.getMessage();
        if (message != null) {
          out.add(message);
          if (TGPlayerController.compareTracks(message, fromMessage)) {
            foundIndex = out.size() - 1;
          }
        }
      }
    }

    if (foundIndex == -1) {
      return null;
    }

    return new TGPlayerController.PlayList(out, foundIndex);
  }

  @Override
  public boolean wouldReusePlayList(TdApi.Message fromMessage, boolean isReverse, boolean hasAltered, List<TdApi.Message> trackList, long playListChatId) {
    return false;
  }

  @Override
  protected boolean needsDefaultLongPress () {
    return false;
  }


  @Override
  protected int getItemCellHeight () {
    return Screen.dp(72f);
  }
}
