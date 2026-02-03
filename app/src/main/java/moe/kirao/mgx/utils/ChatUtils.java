package moe.kirao.mgx.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;

import tgx.td.Td;

import me.vkryl.core.StringUtils;
import me.vkryl.core.lambda.RunnableData;

public class ChatUtils {
  public record AuthorInfo(long id, @NonNull String name, @Nullable String username) {
    public @NonNull String displayText () {
      if (username != null) {
        return String.join("\n", String.valueOf(id), name, '@' + username);
      }
      return String.join("\n", String.valueOf(id), name);
    }
  }

  @Nullable
  public static AuthorInfo resolveUserLocal (@NonNull Tdlib tdlib, long userId) {
    TdApi.MessageSender sender = tdlib.sender(userId);
    if (tdlib.senderName(sender).startsWith("User#")) return null;
    String username = tdlib.senderUsername(sender);
    return new AuthorInfo(userId, tdlib.senderName(sender), StringUtils.isEmptyOrBlank(username) ? null : username);
  }

  public static void processAuthorRequest (@NonNull Tdlib tdlib, long botUserId, String query, RunnableData<AuthorInfo> after) {
    tdlib.client().send(new TdApi.GetInlineQueryResults(botUserId, tdlib.selfChatId(), null, query, null), article -> {
      if (article.getConstructor() == TdApi.InlineQueryResults.CONSTRUCTOR) {
        TdApi.InlineQueryResults results = (TdApi.InlineQueryResults) article;
        if (results.results.length == 0) return;
        TdApi.InlineQueryResult result = results.results[0];
        if (result.getConstructor() == TdApi.InlineQueryResultArticle.CONSTRUCTOR) {
          long queryId = results.inlineQueryId;
          String resultId = ((TdApi.InlineQueryResultArticle) result).id;
          tdlib.client().send(new TdApi.SendInlineQueryResultMessage(tdlib.selfChatId(), null, null, Td.newSendOptions(null, true), queryId, resultId, false), newMsg -> {
            if (newMsg.getConstructor() != TdApi.Message.CONSTRUCTOR) return;
            tdlib.deleteMessages(tdlib.selfChatId(), new long[] {((TdApi.Message) newMsg).id}, true);
            String text = ((TdApi.MessageText) ((TdApi.Message) newMsg).content).text.text;
            after.runWithData(parseAuthorResponse(text));
          });
        }
      }
    });
  }

  @Nullable
  private static AuthorInfo parseAuthorResponse (@NonNull String text) {
    long id = 0;
    String name = null;
    String username = null;
    for (String line : text.split("\n")) {
      if (line.contains("\uD83D\uDC64")) {
        id = Long.parseLong(line.replaceAll("\\D+", ""));
      } else if (line.contains("\uD83D\uDC66\uD83C\uDFFB")) {
        name = line.replaceAll("^.+? ", "");
      } else if (line.contains("\u2063\uD83C\uDF10")) {
        username = line.replaceAll("^.+? @?", "");
      }
    }
    if (name == null) return null;
    return new AuthorInfo(id, name, username);
  }

  public static long extractAuthorId (long setId) {
    long authorId = setId >> 32;

    if (((setId >> 16) & 0xff) == 0x3f) {
      authorId |= 0x800000000L;
    }
    if (((setId >> 24) & 0xff) != 0) {
      authorId += 0x100000000L;
    }
    return authorId;
  }

  public static String getDCName (int dc) {
    return switch (dc) {
      case 1, 3 -> "Miami FL, USA";
      case 2, 4 -> "Amsterdam, NL";
      case 5 -> "Singapore, SG";
      default -> null;
    };
  }
}