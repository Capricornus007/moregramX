# Forum Topics Implementation Tasks

| Waiting | In Progress | Completed |
|---------|-------------|-----------|
| | | TDLib bindings available |
| | | ForumTopicInfoListener infrastructure |
| | | Topic info caching in Tdlib.java |
| | | Service message handling |
| | | Message topic ID filtering |
| | | Permission checks |
| | | ForumTopicsController - main topics list screen |
| | | ForumTopicView - topic list item view |
| | | Navigation integration (TdlibUi.java) |
| | | Pin/Unpin topics UI |
| | | Close/Reopen topics UI |
| | | Build successful (arm64 + x64 debug APK)
| | | Topic message filtering fix (GetForumTopicHistory)
| | | Topic-specific unread counter fix
| | | Mark messages as read fix (onForumTopicUpdated)
| | | Open topic at first unread position
| | | Cross-device read state sync (GetForumTopic refresh)
| | | Fix server-side read marking (needForceRead delay)
| | | Unread topics count in chat list (forum supergroups)
| | | Fix: Per-chat unread topic count refresh (BetterChatView/VerticalChatView)
| | | Fix: Main chat list unread topic count refresh (ChatsController/TGChat)
| | | Topic icon/avatar display (custom emoji + colored circle fallback)
| | | Transparent background for loaded custom emoji icons
| | | Topic Creation Dialog (CreateForumTopic with FAB button)
| | | Topic Editing Dialog (EditForumTopic - via long-press menu)
| | | Topic Header in Chat (ChatHeaderView shows topic name + chat name)
| | | Topic Notifications Settings (per-topic mute/unmute via long-press menu)
| | | Topic-specific typing indicator (per-topic send/receive)
| | | Tabs layout support (ForumTopicTabsController + hasForumTabs check)
| | | Forum Toggle in Group Settings (ToggleSupergroupIsForum via ProfileController)
| | | Search topics functionality (client-side name filtering + highlighting)
| | | Notification topic separation (shows "Chat > Topic" in notification title)
| | | Fix: Tab-style forums (hasForumTabs) always showing tabs
| | | Fix: "Topic icon changed" message instead of "Topic created" for icon edits
| | | "View as chat" option in ForumTopicsController (ToggleChatViewAsTopics)
| | | Fix: ForumTopicTabsController tabs and menu display (loading placeholder + more menu)
| | | Fix: ForumTopicTabsController "View as chat" navigation (destroyStackItemAt pattern)
| | | Fix: ForumTopicTabsController tabs layout margin (getMenuButtonsWidth override)
| | | "View as topics" option in MessagesController (for switching back from unified chat view)
| | | Fix: "View as topics" direct navigation (avoid stale chat.viewAsTopics issue)
| | | Default forum navigation to topics view (TdlibUi.java - matches official Telegram behavior)
| | | Fix: Topic-specific pinned messages in tabs mode (pass topicId to MessageListManager)
| | | Fix: Search button in ForumTopicTabsController (added search/clear mode support)
| | | Topic actions in tabs mode (mute, close, pin, edit via 3 dots menu)
| | | Fix: New messages appearing in wrong topic tab (updateNewMessage topic filtering)
| | | Fix: Topic mention/reaction counters not updating (onForumTopicUpdated extended)
| | | Fix: Chat list preview showing "Topic created" for non-forum service messages (switch fallthrough bug)
| | | Create topic option in tabs mode (ForumTopicTabsController - via 3 dots menu)
| | | Permission checks for topic actions UI (hide create/edit/pin/close/delete based on user rights)
| | | Group Info access from tabs mode (ForumTopicTabsController - admin-only menu option)
| | | Forum layout toggle (tabs vs list) in ProfileController (ToggleSupergroupIsForum with hasForumTabs)
| | | Fix: Forum layout toggle instant apply (wasForumTabsChanged check in processEditContentChanged)
| | | Fix: Visual flash when entering forum tabs (LoadingController placeholder instead of MessagesController)
| | | Fix: External forum toggle detection (onSupergroupUpdated handler for non-admin users)
| | | Change topic icon feature (GetForumTopicDefaultIcons + EditForumTopic with iconCustomEmojiId)
| | | Fix: Muted topic notifications (client-side filter in TdlibNotificationHelper.updateGroup)
| | | Fix: Closed topic input disabled (isTopicClosedForUser check in MessagesController.updateBottomBar)
| | | Search messages in topics (toggle Topics/Messages search in ForumTopicsController)
| | | Flat message search results with sender avatar and topic icon in corner (ForumTopicView)
| | | Message search pagination (infinite scroll with nextFromMessageId in ForumTopicsController)
| | | Filter message search results by topic (FAB button with multi-select checkboxes)
| | | Fix: Preserve search results when navigating back from topic (allowLeavingSearchMode override)
| | | Fix: Topic filter missing old messages (multi-page preloading + auto-retry)
| | | Topic filter dialog with proper icons (TopicIconModifier with colored circles + custom emoji)
| | | Fix: Settings popup Done/Cancel buttons ripple effect (use ?android:attr/colorControlHighlight for theme-adaptive ripple)
| | | Message search loading indicator (ClearButton spinner in search bar instead of centered ProgressComponentView)

## Implementation Notes

### Files Created
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicsController.java` - Main controller for forum topics list
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicView.java` - Custom view for topic items
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicTabsController.java` - ViewPager-based tabs controller for forum topics (used when hasForumTabs is enabled)
- `app/src/main/java/org/thunderdog/challegram/util/TopicIconModifier.java` - DrawModifier for rendering topic icons (colored circles + custom emoji) in list items

### Files Modified
- `app/src/main/java/org/thunderdog/challegram/telegram/TdlibUi.java` - Added forum navigation hook (lines 2117-2134)
- `app/src/main/java/org/thunderdog/challegram/component/chat/MessagesLoader.java` - Added forum topic history loading using GetForumTopicHistory (lines 1154-1158)
- `app/src/main/java/org/thunderdog/challegram/ui/MessagesController.java` - Added forumTopic field, unread counter fix (updateCounters), and onForumTopicUpdated override for read state handling
- `app/src/main/java/org/thunderdog/challegram/component/chat/MessagesManager.java` - Fixed needForceRead to delay read marking for forum topics until user scrolls (lines 782-792)
- `app/src/main/java/org/thunderdog/challegram/telegram/Tdlib.java` - Added forum unread topic count caching and methods (forumUnreadTopicCount, fetchForumUnreadTopicCount, updateForumTopicUnreadCount)
- `app/src/main/java/org/thunderdog/challegram/telegram/TdlibListeners.java` - Added updateForumUnreadTopicCount listener method
- `app/src/main/java/org/thunderdog/challegram/telegram/ChatListener.java` - Added onForumUnreadTopicCountChanged callback
- `app/src/main/java/org/thunderdog/challegram/data/TGChat.java` - Modified getUnreadCount() to return unread topic count for forum chats
- `app/src/main/java/org/thunderdog/challegram/widget/BetterChatView.java` - Added onForumUnreadTopicCountChanged handler
- `app/src/main/java/org/thunderdog/challegram/widget/VerticalChatView.java` - Added onForumUnreadTopicCountChanged handler
- `app/src/main/java/org/thunderdog/challegram/data/TGChat.java` - Added updateForumUnreadTopicCount() method
- `app/src/main/java/org/thunderdog/challegram/component/dialogs/ChatsAdapter.java` - Added updateForumUnreadTopicCount() method
- `app/src/main/java/org/thunderdog/challegram/v/ChatsRecyclerView.java` - Added updateForumUnreadTopicCount() method
- `app/src/main/java/org/thunderdog/challegram/ui/ChatsController.java` - Added onForumUnreadTopicCountChanged callback handler
- `app/src/main/res/values/ids.xml` - Added controller_forumTopics and button IDs
- `app/src/main/res/values/strings.xml` - Added topic-related strings
- `app/src/main/java/org/thunderdog/challegram/component/chat/ChatHeaderView.java` - Added forum topic header support (topic name as title, chat name as subtitle)
- `app/src/main/java/org/thunderdog/challegram/ui/ProfileController.java` - Added forum toggle for supergroup owners (ToggleSupergroupIsForum)
- `app/src/main/java/org/thunderdog/challegram/telegram/ForumTopicInfoListener.java` - Extended onForumTopicUpdated to include unreadMentionCount and unreadReactionCount

### Current Status: Build Successful + Tested
- arm64 APK: `app/build/outputs/apk/arm64/debug/TGX-Example-0.28.2.1778-arm64-v8a-debug.apk`
- x64 APK: `app/build/outputs/apk/x64/debug/TGX-Example-0.28.2.1778-x64-debug.apk`

Running on emulator (Medium_Phone_API_36.1). Each topic now shows only its own messages.

### TDLib Functions Used
- `GetForumTopics` - Fetch topics list (implemented in loadTopics/loadMoreTopics)
- `GetForumTopicHistory` - Load messages for a specific topic (fixed in MessagesLoader.java)
- `ToggleForumTopicIsClosed` - Close/reopen (implemented)
- `ToggleForumTopicIsPinned` - Pin/unpin (implemented)
- `DeleteForumTopic` - Delete topic (implemented)
- `CreateForumTopic` - Create new topic (FAB button + dialog in ForumTopicsController)
- `EditForumTopic` - Edit topic name (long-press menu in ForumTopicsController)
- `SetForumTopicNotificationSettings` - Per-topic mute/unmute (long-press menu in ForumTopicsController)
- `ToggleSupergroupIsForum` - Enable/disable forum topics mode (toggle in ProfileController group settings)
- `ToggleChatViewAsTopics` - Toggle between topics view and unified chat view (more menu in ForumTopicsController)
- `SearchChatMessages` - Search messages in forum chat, group by topicId for message search mode

### Future Enhancements (TODO)
- [x] Tabs layout support (`hasForumTabs`) - Show topics as horizontal tabs when admin enables "Tabs" layout
- [x] User typing in topics - Show typing indicator per-topic instead of per-chat

All major forum topics features have been implemented.

---

# Stories Implementation Tasks

## Overview
Full stories feature implementation for Telegram-X with complete feature parity.

## ✅ ALL TASKS COMPLETE

### Phase 1: Story Viewer Foundation
- [x] **StoryViewController.java** - Full-screen story viewer
  - PopupLayout.AnimatedPopupProvider for overlay
  - Segmented progress bars at top
  - Left/right tap navigation
  - Swipe down to close
  - Story content display (photo/video via ImageReceiver)
  - Caption overlay with gradient
  - TdApi.OpenStory/CloseStory calls

### Phase 2: Story Bar in Chat List
- [x] **StoryBarView.java** - Horizontal RecyclerView with:
  - StoryAvatarItemView with gradient ring for unread
  - Gray ring for read stories
  - Name truncation below avatar
  - Click handling to open StoryViewController
  - Respects hideStories setting

### Phase 2.3: ChatsController Integration
- [x] Added storyBarView as header in ChatsController
- [x] Fetches active stories from TdApi
- [x] Updates visibility based on settings

### Phase 3: Avatar Story Rings
- [x] **AvatarView.java** - Added story ring support:
  - STORY_NONE, STORY_READ, STORY_UNREAD states
  - Gradient ring (blue/turquoise/green) for unread
  - Gray ring for read
  - setStoryState() method
  - drawStoryRing() in onDraw()
  - Respects hideStories setting

### Phase 4: Story Posting UI
- [x] **StoryComposeController.java** - Entry point for posting stories
  - Shows camera/gallery options
  - Uses TdApi.PostStory to send
  - Checks TdApi.CanPostStory permission
- [x] Add "Add Story" button to StoryBarView
  - AddStoryItemView with plus icon and gradient ring
  - setCanPostStory() to control visibility
  - checkCanPostStory() in ChatsController
  - Opens StoryComposeController on click

### Phase 5: Story Interactions
- [x] Reply input in StoryViewController
  - StoryReplyInputView with EditText, send, and heart buttons
  - sendReply() via TdApi.SendMessage with MessageReplyToStory
  - Pauses story progress when input is focused
- [x] Reactions (double-tap for heart)
  - sendHeartReaction() via TdApi.SetStoryReaction
  - Double-tap detection in center of screen
- [x] **StoryViewersController.java** - Show who viewed your story
  - Uses TdApi.GetStoryViewers with pagination
  - Displays viewers with reaction emoji
  - Viewers button in StoryViewController header (own stories only)

### Phase 6: Supporting Features
- [x] **ReplyComponent.setStory()** - Story preview in replies
- [x] **Settings.hideStories()** - Toggle to hide stories
- [x] **String resources** - All story-related strings added

### Phase 7: Polish and Edge Cases
- [x] **Caption display** - TextView with semi-transparent background and text shadow
- [x] **Expired story handling** - Shows "Story expired" toast and auto-navigates to next
- [x] **Loading states** - ProgressBar indicator while loading stories

### Phase 8: Story Options Menu
- [x] **Three dots menu** - More button in story viewer header
- [x] **Own story options:**
  - Edit caption (TdApi.EditStory)
  - Story privacy settings (TdApi.SetStoryPrivacySettings)
  - Pin/unpin to profile (TdApi.ToggleStoryIsPostedToChatPage)
  - Delete story (TdApi.DeleteStory)
  - **Story Statistics** (TdApi.GetStoryStatistics) - Shows interaction/reaction graphs
  - **Add to Album** (TdApi.GetChatStoryAlbums, CreateStoryAlbum, AddStoryAlbumStories)
- [x] **Other's story options:**
  - Report story (TdApi.ReportStory)
  - Stealth mode (TdApi.ActivateStoryStealthMode) - Premium feature

### Phase 9: Story Albums (Highlights)
- [x] **Album picker** - Shows existing albums or create new
- [x] **Create album** - Dialog to name new album + add current story
- [x] **Add to album** - Add story to existing album
- [x] Uses TdApi: GetChatStoryAlbums, CreateStoryAlbum, AddStoryAlbumStories

---

## TDLib API Reference

**Viewing:**
- `getStory(chatId, storyId)` - Fetch single story
- `getChatActiveStories(chatId)` - Get active stories for chat
- `loadActiveStories(storyList)` - Load story list
- `openStory(chatId, storyId)` - Mark as viewed
- `closeStory(chatId, storyId)` - Finished viewing

**Posting:**
- `canPostStory(chatId)` - Check permission
- `postStory(chatId, content, ...)` - Create story

**Interactions:**
- `setStoryReaction(chatId, storyId, reactionType, updateRecentReactions)` - React to story
- `getStoryViewers(storyId, ...)` - Get who viewed

**Content Types:**
- `InputStoryContentPhoto(photo, stickerIds)`
- `InputStoryContentVideo(video, stickerIds, duration, coverTimestamp, isAnimation)`
- `StoryPrivacySettings*` - Various privacy options

---

## Git Commits

1. `00b6ea2` - Add Stories feature - viewing, story bar, and avatar rings
2. `8d746c5b` - Add Stories posting and interactions
2. `8d746c5` - Add Stories posting and interactions
3. (pending) - Add story viewers, caption, expired handling, loading states

---

## Files Created/Modified

### New Files
- `StoryViewController.java` - Full-screen story viewer
- `StoryBarView.java` - Horizontal story bar
- `StoryViewersController.java` - Story viewers list

### Modified Files
- `Settings.java` - hideStories flag
- `SettingsThemeController.java` - Settings toggle
- `ChatsController.java` - Story bar integration
- `AvatarView.java` - Story ring support
- `ReplyComponent.java` - Story preview
- `strings.xml` - Story-related strings
- `ids.xml` - Story-related IDs

---

# Forum Topics Implementation Tasks

| Waiting | In Progress | Completed |
|---------|-------------|-----------|
| | | TDLib bindings available |
| | | ForumTopicInfoListener infrastructure |
| | | Topic info caching in Tdlib.java |
| | | Service message handling |
| | | Message topic ID filtering |
| | | Permission checks |
| | | ForumTopicsController - main topics list screen |
| | | ForumTopicView - topic list item view |
| | | Navigation integration (TdlibUi.java) |
| | | Pin/Unpin topics UI |
| | | Close/Reopen topics UI |
| | | Build successful (arm64 + x64 debug APK)
| | | Topic message filtering fix (GetForumTopicHistory)
| | | Topic-specific unread counter fix
| | | Mark messages as read fix (onForumTopicUpdated)
| | | Open topic at first unread position
| | | Cross-device read state sync (GetForumTopic refresh)
| | | Fix server-side read marking (needForceRead delay)
| | | Unread topics count in chat list (forum supergroups)
| | | Fix: Per-chat unread topic count refresh (BetterChatView/VerticalChatView)
| | | Fix: Main chat list unread topic count refresh (ChatsController/TGChat)
| | | Topic icon/avatar display (custom emoji + colored circle fallback)
| | | Transparent background for loaded custom emoji icons
| | | Topic Creation Dialog (CreateForumTopic with FAB button)
| | | Topic Editing Dialog (EditForumTopic - via long-press menu)
| | | Topic Header in Chat (ChatHeaderView shows topic name + chat name)
| | | Topic Notifications Settings (per-topic mute/unmute via long-press menu)
| | | Topic-specific typing indicator (per-topic send/receive)
| | | Tabs layout support (ForumTopicTabsController + hasForumTabs check)
| | | Forum Toggle in Group Settings (ToggleSupergroupIsForum via ProfileController)
| | | Search topics functionality (client-side name filtering + highlighting)
| | | Notification topic separation (shows "Chat > Topic" in notification title)
| | | Fix: Tab-style forums (hasForumTabs) always showing tabs
| | | Fix: "Topic icon changed" message instead of "Topic created" for icon edits
| | | "View as chat" option in ForumTopicsController (ToggleChatViewAsTopics)
| | | Fix: ForumTopicTabsController tabs and menu display (loading placeholder + more menu)
| | | Fix: ForumTopicTabsController "View as chat" navigation (destroyStackItemAt pattern)
| | | Fix: ForumTopicTabsController tabs layout margin (getMenuButtonsWidth override)
| | | "View as topics" option in MessagesController (for switching back from unified chat view)
| | | Fix: "View as topics" direct navigation (avoid stale chat.viewAsTopics issue)
| | | Default forum navigation to topics view (TdlibUi.java - matches official Telegram behavior)
| | | Fix: Topic-specific pinned messages in tabs mode (pass topicId to MessageListManager)
| | | Fix: Search button in ForumTopicTabsController (added search/clear mode support)
| | | Topic actions in tabs mode (mute, close, pin, edit via 3 dots menu)
| | | Fix: New messages appearing in wrong topic tab (updateNewMessage topic filtering)
| | | Fix: Topic mention/reaction counters not updating (onForumTopicUpdated extended)
| | | Fix: Chat list preview showing "Topic created" for non-forum service messages (switch fallthrough bug)
| | | Create topic option in tabs mode (ForumTopicTabsController - via 3 dots menu)
| | | Permission checks for topic actions UI (hide create/edit/pin/close/delete based on user rights)
| | | Group Info access from tabs mode (ForumTopicTabsController - admin-only menu option)
| | | Forum layout toggle (tabs vs list) in ProfileController (ToggleSupergroupIsForum with hasForumTabs)
| | | Fix: Forum layout toggle instant apply (wasForumTabsChanged check in processEditContentChanged)
| | | Fix: Visual flash when entering forum tabs (LoadingController placeholder instead of MessagesController)
| | | Fix: External forum toggle detection (onSupergroupUpdated handler for non-admin users)
| | | Change topic icon feature (GetForumTopicDefaultIcons + EditForumTopic with iconCustomEmojiId)
| | | Fix: Muted topic notifications (client-side filter in TdlibNotificationHelper.updateGroup)
| | | Fix: Closed topic input disabled (isTopicClosedForUser check in MessagesController.updateBottomBar)
| | | Search messages in topics (toggle Topics/Messages search in ForumTopicsController)
| | | Flat message search results with sender avatar and topic icon in corner (ForumTopicView)
| | | Message search pagination (infinite scroll with nextFromMessageId in ForumTopicsController)
| | | Filter message search results by topic (FAB button with multi-select checkboxes)
| | | Fix: Preserve search results when navigating back from topic (allowLeavingSearchMode override)
| | | Fix: Topic filter missing old messages (multi-page preloading + auto-retry)
| | | Topic filter dialog with proper icons (TopicIconModifier with colored circles + custom emoji)
| | | Fix: Settings popup Done/Cancel buttons ripple effect (use ?android:attr/colorControlHighlight for theme-adaptive ripple)
| | | Message search loading indicator (ClearButton spinner in search bar instead of centered ProgressComponentView)
| | | Fix: Topic filter dialog icon positioning (LEFT_OFFSET_DP 68→18dp to place icons in left padding area)
| | | Fix: ForumTopicView emoji rendering (use Text class instead of canvas.drawText for proper emoji support)
| | | Star/Paid reactions support (TdExt.kt, TGStickerObj, TGReaction, TGReactions, Tdlib.java)
| | | Fix: Premium bot crash on Buy button (TGInlineKeyboard null check + payment form handling)
| | | Fix: Windows file lock issue (kotlin.compiler.execution.strategy=in-process in gradle.properties)
| | | View Forum navigation from topic (btn_viewForum menu option when viewing topic via message link)
| | | Stories Settings screen (SettingsStoriesController - new settings section)
| | | Customizable story ring colors (StoryColorPickerController - 1-3 color gradient picker)
| | | Optional "Add Story" button border (SETTING_FLAG_SHOW_ADD_STORY_BORDER)
| | | Story bar as RecyclerView item (scrolls with chat list instead of overlay)

## Stories Settings Implementation

### New Files
- `app/src/main/java/org/thunderdog/challegram/ui/SettingsStoriesController.java` - Main stories settings screen with Visibility, Appearance, Behavior sections
- `app/src/main/java/org/thunderdog/challegram/ui/StoryColorPickerController.java` - Color picker for story ring gradient (1-3 colors, visual HSV picker, live preview)

### Modified Files (Stories Settings)
- `app/src/main/java/org/thunderdog/challegram/unsorted/Settings.java` - Added SETTING_FLAG_SHOW_ADD_STORY_BORDER, getStoryRingColors(), setStoryRingColors(), DEFAULT_STORY_RING_COLORS
- `app/src/main/java/org/thunderdog/challegram/ui/SettingsController.java` - Added Stories menu item
- `app/src/main/java/org/thunderdog/challegram/ui/SettingsThemeController.java` - Removed story settings (moved to new screen)
- `app/src/main/java/org/thunderdog/challegram/widget/StoryBarView.java` - Uses Settings for border toggle and ring colors
- `app/src/main/java/org/thunderdog/challegram/widget/AvatarView.java` - Uses Settings for ring colors
- `app/src/main/res/values/strings.xml` - Added StoriesSettings, Appearance, Behavior, etc.
- `app/src/main/res/values/ids.xml` - Added story settings IDs

### Story Bar as List Item (scrolls with chat list)

Refactored story bar from overlay to RecyclerView item so it scrolls naturally with the chat list.

#### Modified Files
- `app/src/main/java/org/thunderdog/challegram/component/dialogs/ChatsAdapter.java`:
  - Added VIEW_TYPE_STORY_BAR = 4
  - Added hasStoryBar(), setShowStoryBar(), setActiveStories(), setCanPostStory() methods
  - Updated getItemCount(), getItemViewType(), position calculation methods
- `app/src/main/java/org/thunderdog/challegram/component/dialogs/ChatsViewHolder.java`:
  - Added VIEW_TYPE_STORY_BAR case in measureHeightForType() and create()
- `app/src/main/java/org/thunderdog/challegram/ui/ChatsController.java`:
  - Added setStoryBarViewFromAdapter() to receive view from adapter
  - Replaced overlay creation with adapter.setShowStoryBar(true)
  - Removed padding updates and scroll translation logic
  - Updated loadActiveStories(), checkCanPostStory() to use adapter methods

## Forum Topics Implementation Notes

### Files Created
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicsController.java` - Main controller for forum topics list
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicView.java` - Custom view for topic items
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicTabsController.java` - ViewPager-based tabs controller for forum topics (used when hasForumTabs is enabled)
- `app/src/main/java/org/thunderdog/challegram/util/TopicIconModifier.java` - DrawModifier for rendering topic icons (colored circles + custom emoji) in list items

### Files Modified
- `app/src/main/java/org/thunderdog/challegram/telegram/TdlibUi.java` - Added forum navigation hook (lines 2117-2134)
- `app/src/main/java/org/thunderdog/challegram/component/chat/MessagesLoader.java` - Added forum topic history loading using GetForumTopicHistory (lines 1154-1158)
- `app/src/main/java/org/thunderdog/challegram/ui/MessagesController.java` - Added forumTopic field, unread counter fix (updateCounters), and onForumTopicUpdated override for read state handling
- `app/src/main/java/org/thunderdog/challegram/component/chat/MessagesManager.java` - Fixed needForceRead to delay read marking for forum topics until user scrolls (lines 782-792)
- `app/src/main/java/org/thunderdog/challegram/telegram/Tdlib.java` - Added forum unread topic count caching and methods (forumUnreadTopicCount, fetchForumUnreadTopicCount, updateForumTopicUnreadCount)
- `app/src/main/java/org/thunderdog/challegram/telegram/TdlibListeners.java` - Added updateForumUnreadTopicCount listener method
- `app/src/main/java/org/thunderdog/challegram/telegram/ChatListener.java` - Added onForumUnreadTopicCountChanged callback
- `app/src/main/java/org/thunderdog/challegram/data/TGChat.java` - Modified getUnreadCount() to return unread topic count for forum chats
- `app/src/main/java/org/thunderdog/challegram/widget/BetterChatView.java` - Added onForumUnreadTopicCountChanged handler
- `app/src/main/java/org/thunderdog/challegram/widget/VerticalChatView.java` - Added onForumUnreadTopicCountChanged handler
- `app/src/main/java/org/thunderdog/challegram/data/TGChat.java` - Added updateForumUnreadTopicCount() method
- `app/src/main/java/org/thunderdog/challegram/component/dialogs/ChatsAdapter.java` - Added updateForumUnreadTopicCount() method
- `app/src/main/java/org/thunderdog/challegram/v/ChatsRecyclerView.java` - Added updateForumUnreadTopicCount() method
- `app/src/main/java/org/thunderdog/challegram/ui/ChatsController.java` - Added onForumUnreadTopicCountChanged callback handler
- `app/src/main/res/values/ids.xml` - Added controller_forumTopics and button IDs
- `app/src/main/res/values/strings.xml` - Added topic-related strings
- `app/src/main/java/org/thunderdog/challegram/component/chat/ChatHeaderView.java` - Added forum topic header support (topic name as title, chat name as subtitle)
- `app/src/main/java/org/thunderdog/challegram/ui/ProfileController.java` - Added forum toggle for supergroup owners (ToggleSupergroupIsForum)
- `app/src/main/java/org/thunderdog/challegram/telegram/ForumTopicInfoListener.java` - Extended onForumTopicUpdated to include unreadMentionCount and unreadReactionCount
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicView.java` - Updated to use Text class with FormattedText for proper emoji rendering (custom emoji support)
- `app/src/main/kotlin/tgx/td/TdExt.kt` - Enabled paid reactions (isUnsupported returns false)
- `app/src/main/java/org/thunderdog/challegram/component/sticker/TGStickerObj.java` - Added makePaidReactionStar() factory method
- `app/src/main/java/org/thunderdog/challegram/data/TGReaction.java` - Added paid reaction constructor and initializePaid() method
- `app/src/main/java/org/thunderdog/challegram/data/TGReactions.java` - Added paid reaction drawable support
- `app/src/main/java/org/thunderdog/challegram/data/TGInlineKeyboard.java` - Fixed MessageInvoice cast crash, implemented Buy button handler
- `app/src/main/java/org/thunderdog/challegram/telegram/TdlibUi.java` - Added openPaymentForm() and stars payment methods
- `gradle.properties` - Added `kotlin.compiler.execution.strategy=in-process` to fix Windows file lock issue

### Windows Build Fix
The Kotlin compiler daemon was causing persistent "user-mapped section open" errors on Windows. The Kotlin daemon uses memory-mapped files for caching compiled code, and when builds fail or are interrupted, these locks aren't properly released.

**Solution**: Added `kotlin.compiler.execution.strategy=in-process` to `gradle.properties`. This runs the Kotlin compiler in the same JVM as Gradle instead of a separate daemon process, avoiding the memory-mapped file locking issues.

### Forum Topics TDLib Functions Used
- `GetForumTopics` - Fetch topics list (implemented in loadTopics/loadMoreTopics)
- `GetForumTopicHistory` - Load messages for a specific topic (fixed in MessagesLoader.java)
- `ToggleForumTopicIsClosed` - Close/reopen (implemented)
- `ToggleForumTopicIsPinned` - Pin/unpin (implemented)
- `DeleteForumTopic` - Delete topic (implemented)
- `CreateForumTopic` - Create new topic (FAB button + dialog in ForumTopicsController)
- `EditForumTopic` - Edit topic name (long-press menu in ForumTopicsController)
- `SetForumTopicNotificationSettings` - Per-topic mute/unmute (long-press menu in ForumTopicsController)
- `ToggleSupergroupIsForum` - Enable/disable forum topics mode (toggle in ProfileController group settings)
- `ToggleChatViewAsTopics` - Toggle between topics view and unified chat view (more menu in ForumTopicsController)
- `SearchChatMessages` - Search messages in forum chat, group by topicId for message search mode

All major forum topics features have been implemented.

---

## Bug Fixes

### InternalLinkTypeInvoice Crash Fix
Fixed ClassCastException when opening invoice links. The crash occurred because `InternalLinkTypeInvoice` was falling through to `InternalLinkTypeBuyStars` case in the switch statement, causing an invalid cast.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/telegram/TdlibUi.java`:
  - Separated `InternalLinkTypeInvoice` from `InternalLinkTypeBuyStars` cases
  - Added new `openPaymentForm(TdlibDelegate, String invoiceName, ...)` method
  - Invoice links now properly open payment forms via `GetPaymentForm` API

### MessageGift Support
Added support for the `MessageGift` message type which was previously showing as "Unsupported message".

**Files Created:**
- `app/src/main/java/org/thunderdog/challegram/data/TGMessageGiftRegular.java` - Handler for regular gift messages (extends TGMessageGiveawayBase)

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/data/TGMessage.java`:
  - Added case for `MessageGift.CONSTRUCTOR` → `TGMessageGiftRegular`
  - Removed `MessageGift` from unsupported message types list
- `app/src/main/res/values/strings.xml` - Added gift-related strings:
  - GiftReceived, GiftSent, GiftConverted, GiftUpgraded, GiftRefunded
  - ViewGift, xGiftValue, xGiftCanBeSold

### Visual HSV Color Picker
Replaced hex keyboard input with visual HSV color picker for story ring color customization.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/ui/StoryColorPickerController.java`:
  - Added `ColorPickerPopupView` inner class with:
    - Saturation/Value gradient square
    - Hue rainbow bar
    - Color preview circle
    - Cancel/Done buttons
  - Touch handling for dragging color selection

### Choose Gift Recipient Button Fix
Fixed "Choose Gift Recipient" keyboard button (and similar bot buttons) doing nothing on click.

**Root Cause:** `CommandKeyboardLayout.onClick` was missing handlers for `KeyboardButtonTypeRequestUsers` and `KeyboardButtonTypeRequestChat` button types.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/component/chat/CommandKeyboardLayout.java`:
  - Added cases for `KeyboardButtonTypeRequestUsers` and `KeyboardButtonTypeRequestChat`
  - Added `onRequestUsers()` and `onRequestChat()` to `Callback` interface
- `app/src/main/java/org/thunderdog/challegram/ui/MessagesController.java`:
  - Implemented `onRequestUsers()` - opens contact picker, then calls `ShareUsersWithBot` API
  - Implemented `onRequestChat()` - shows "not yet supported" (chat picker not implemented)

**TDLib Function Used:**
- `ShareUsersWithBot(chatId, messageId, buttonId, sharedUserIds, onlyCheck)` - shares selected users with the bot after pressing a `KeyboardButtonTypeRequestUsers` button

### Contact Picker Navigation Fix
Fixed contact picker not navigating back after selecting a contact for "Choose Gift Recipient" button.

**Root Cause:** `ContactsController.onFoundChatClick()` wasn't calling `navigateBack()` after delegate callback.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/ui/ContactsController.java`:
  - Added `navigateBack()` call after `delegate.onSenderPick()` returns true

### MessageUsersShared / MessageChatShared Support
Added support for `MessageUsersShared` and `MessageChatShared` service message types which were showing as "Unsupported message".

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/data/TGMessageService.java`:
  - Added constructor for `MessageUsersShared` - shows "You shared [user name]"
  - Added constructor for `MessageChatShared` - shows "You shared [chat name]"
- `app/src/main/java/org/thunderdog/challegram/data/TGMessage.java`:
  - Added cases for `MessageUsersShared` and `MessageChatShared`
  - Removed from unsupported message types list
- `app/src/main/res/values/strings.xml`:
  - Added `YouSharedUser`, `YouSharedUsers`, `YouSharedChat` strings

### User Sharing Confirmation Toast
Added toast notification when sharing user with bot to provide UX feedback.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/ui/MessagesController.java`:
  - Added toast showing "You shared [user name]" after successful ShareUsersWithBot call

### Payment Card Input Validation & Formatting
Fixed payment card input fields lacking proper validation and formatting.

**Issues Fixed:**
- Card number, expiry, CVC fields now show numeric keyboard
- Card number auto-formats as `XXXX XXXX XXXX XXXX`
- Expiry date auto-formats as `MM/YY`
- CVC limited to 3-4 digits
- Card holder shows text keyboard with auto-capitalization
- Cannot type letters/symbols in numeric fields

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/ui/PaymentFormController.java`:
  - Added imports for `Editable`, `InputFilter`, `InputType`, `TextWatcher`
  - Overrode `modifyEditText()` in adapter to configure each field:
    - Card number: `TYPE_CLASS_PHONE` + custom filter (digits/spaces) + formatting TextWatcher
    - Expiry: `TYPE_CLASS_PHONE` + custom filter (digits/slash) + formatting TextWatcher
    - CVC: `TYPE_CLASS_NUMBER` + max length 4
    - Card holder: `TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_CAP_CHARACTERS`

### Paid Reaction Crash Fix
Fixed crash when opening reactions selector with paid (star) reactions.

**Root Cause:** `TGReaction.newCenterAnimationSicker()` and `newStaticIconSicker()` didn't handle paid reactions - they fell through to code that accessed null `customReaction` field.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/data/TGReaction.java`:
  - Added `isPaid` check to `newStaticIconSicker()` - returns cached or new paid star sticker
  - Added `isPaid` check to `newCenterAnimationSicker()` - returns cached or new paid star sticker

### Archive Pin/Unpin Overlap with Stories Fix
Fixed archive row scroll handling using hardcoded positions that didn't account for story bar.

**Root Cause:** The archive collapse/expand scroll listener used hardcoded positions (0, 1) assuming archive was always at position 0. With story bar at position 0, archive is at position 1, causing incorrect scroll behavior and visual overlap.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/ui/ChatsController.java`:
  - Updated `onScrollStateChanged` to use dynamic `archivePosition` from `adapter.getArchiveItemPosition()`
  - Updated `onScrolled` to check against dynamic archive position instead of hardcoded 0
  - Updated `getLiveLocationPosition()` to account for story bar offset
  - Fixed ItemDecoration to not apply negative collapse offset when story bar is present
  - Changed story bar loading to only add to adapter when content is available
  - Added `scrollToPosition(0)` when story bar is first added to ensure visibility on app start
- `app/src/main/java/org/thunderdog/challegram/widget/StoryBarView.java`:
  - Changed initial visibility from GONE to VISIBLE (adapter now controls presence)
  - Removed GONE state from updateVisibility() - adapter handles add/remove

### Paid Reaction Empty Icon Fix
Fixed paid/star reactions showing as empty (no icon visible) on channels.

**Root Cause:** `StickerSmallView.setSticker()` didn't handle stickers with `isDefaultPremiumStar()` flag. When a paid reaction sticker was set, `getImage()` and `getPreviewAnimation()` returned null (no actual sticker file), so nothing was drawn.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/component/sticker/StickerSmallView.java`:
  - Added check for `sticker.isDefaultPremiumStar()` in `setSticker()`
  - When true, sets `premiumStarDrawable` from `R.drawable.baseline_premium_star_28`
  - Clears `premiumStarDrawable` for normal stickers to avoid stale state
- `app/src/main/java/org/thunderdog/challegram/data/TD.java`:
  - Added error translation for "BALANCE_TOO_LOW" and "not enough stars" → `PaidReactionInsufficientStars`
- `app/src/main/res/values/strings.xml`:
  - Added `PaidReactionInsufficientStars` string with user-friendly message

### ForumTopicView Custom Emoji Crash Fix
Fixed crash when opening forum topics with custom emoji in message preview.

**Root Cause:** `ForumTopicView.buildTextLayouts()` was passing `FormattedText` (which may contain custom emoji) to `Text.Builder` without a `TextMediaListener`. When `Text.newOrExistingMedia()` is called without a listener, it throws `IllegalStateException`.

**Solution:** Implemented proper custom emoji support in ForumTopicView:
- Made ForumTopicView implement `Text.TextMediaListener`
- Added `textMediaReceiver` (ComplexReceiver) for loading custom emoji
- Pass `this` as textMediaListener when building Text with FormattedText
- Call `requestTextMedia()` after building displayPreview
- Pass textMediaReceiver to `displayPreview.draw()` for rendering custom emoji

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/ui/ForumTopicView.java`:
  - Implemented `Text.TextMediaListener` interface
  - Added `textMediaReceiver` field initialized in constructor
  - Added `onInvalidateTextMedia()` callback for view invalidation
  - Added `requestTextMedia()` helper method
  - Updated `buildTextLayouts()` to pass `this` as listener
  - Updated `displayPreview.draw()` to pass textMediaReceiver
  - Updated attach/detach/destroy to handle textMediaReceiver lifecycle

### Forum Preview Swipe-Up Fix
Fixed long-pressing forum chat in chat list and swiping up opening old chat interface instead of ForumTopicsController.

**Root Cause:** `BaseView.openChatPreviewAsync()` always created `MessagesController` for preview, even for forums.

**Solution:** Added check for forum chats - skip preview and fall through to normal long-press menu behavior.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/widget/BaseView.java`:
  - Added `tdlib.isForum(chat.id)` check in `onLongPressRequestedAt()`
  - Forums now skip preview mode

### Star Reaction Icon Size Fix
Fixed star icon in reaction bubbles being too large (overflowing its border).

**Root Cause:** `Drawables.draw()` ignores `setBounds()` and draws at the drawable's intrinsic size. The star was drawn at 24dp regardless of the reaction bubble bounds.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/data/TGReactions.java`:
  - Changed `drawReceiver()` to use `drawable.draw(canvas)` directly instead of `Drawables.draw()`
  - Now properly respects the bounds set with `setBounds(l, t, r, b)`
  - Star icon scales to fit the reaction bubble correctly

### Emoji/Sticker Panel to Keyboard Switching Performance Fix
Fixed 1-second freeze when switching from emoji/sticker panel to keyboard.

**Root Cause:** `KeyboardFrameLayout.onPreDraw()` was intentionally dropping 45-55 frames (750ms-1s freeze at 60 FPS) when showing keyboard. This was an old workaround for pre-Android N devices but caused noticeable lag on modern devices.

**Solution:** Reduced frame dropping from 45-55 to 3-5 frames (~50-83ms), making the transition nearly imperceptible.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/widget/KeyboardFrameLayout.java`:
  - Line 120: Changed `framesDropped = 45 : 55` → `framesDropped = 3 : 5`
  - Line 133: Changed safety limit from `>= 60` → `>= 10` frames

### Quote Creation and Highlighting Implementation (REFACTORED)
Complete reimplementation of quote functionality with proper highlighting and long-press text selection.

**Features Implemented:**

1. **Quote Highlighting on Click (FULLY WORKING)**
   - Added quote highlight rendering directly in Text.java
   - Quote info extraction from `MessageReplyToMessage.quote` when clicking on replies
   - Semi-transparent blue highlight with 2-second fade-in/fade-out animation
   - Quote info passed through: TGMessage → MessagesController → MessagesManager → TGMessageText

2. **Two-Step Long-Press Quote Creation with Visual Selection (NEW)**
   - First long-press on message → selects message (enters selection mode)
   - Second long-press on selected message text → shows **floating ActionMode** with visual selection
   - **Visual features**:
     - Blue highlight over selected text (like EditText)
     - Draggable circular handles at start and end of selection
     - Floating menu bar above selection with "Quote" button
   - Touch events handled for dragging handles to change selection
   - Automatically opens reply with quote and shows keyboard
   - Works seamlessly with message selection mode

3. **Quote Button Removed**
   - Removed quote button from message selection menu (as requested)
   - Quoting now only available via long-press on message text

**Files Modified:**

- `app/src/main/java/org/thunderdog/challegram/util/text/Text.java`:
  - Lines 273-276: Added quote highlight fields (quoteHighlightStart, quoteHighlightEnd, quoteHighlightAlpha)
  - Lines 2828-2839: Added setQuoteHighlight() and clearQuoteHighlight() public methods
  - Lines 2444-2473: Added drawQuoteHighlight() method for rendering
  - Line 2636: Added drawQuoteHighlight call in main draw() method

- `app/src/main/java/org/thunderdog/challegram/data/TGMessageText.java`:
  - Lines 675-725: Refactored performLongPress to show quote ActionMode
  - Lines 692-724: Added canBeQuoted(), inSelectionMode(), showQuoteActionMode() methods
  - Lines 768-812: Updated setTextHighlight and animateHighlight to use Text.setQuoteHighlight()
  - Removed enableTextSelection/disableTextSelection methods (no longer needed)

- `app/src/main/java/org/thunderdog/challegram/ui/MessagesController.java`:
  - Lines 3853-3862: Removed menu_btn_quote handler (deleted)
  - Lines 3684-3686: Quote button in menu already commented out
  - Removed canCreateQuoteFromSelectedMessage() method
  - Removed enterTextSelectionMode(), exitTextSelectionMode(), onQuoteCreated() methods
  - Removed messageInTextSelection field

- `app/src/main/java/org/thunderdog/challegram/data/TGMessage.java`:
  - Lines 213-221: TextQuoteInfo class (kept for quote click handling)
  - Lines 2964-2983: Quote info extraction on reply click (unchanged)
  - Lines 3001-3003: highlightOtherMessage with quoteInfo (unchanged)

- `app/src/main/java/org/thunderdog/challegram/component/chat/MessagesManager.java`:
  - Lines 3094: Quote info parameter in highlightMessage (unchanged)
  - Lines 3117-3119: setTextHighlight call on TGMessageText (unchanged)

**Files Kept (Still Used):**
- `app/src/main/java/org/thunderdog/challegram/util/text/TextSelectionHelper.java` - Used for ActionMode in long-press
- `app/src/main/res/menu/text_selection_quote.xml` - ActionMode menu resource
- `app/src/main/res/values/ids.xml` - menu_btn_create_quote ID

**Implementation Details:**

1. **Quote Highlight Rendering:**
   - Uses existing Text.java infrastructure (similar to pressHighlight and spoilers)
   - Finds TextParts that overlap with UTF-16 quote range
   - Draws semi-transparent blue background (0x4000A0FF)
   - Alpha animated from 0 → 1 → 0 over 2 seconds

2. **Two-Step Long-Press Quote Flow:**
   - **First press**: User long-presses message → super.performLongPress() selects message
   - **Second press**: User long-presses selected message → performLongPress() in TGMessageText
   - Check: not already handled, in selection mode, current message is selected, message has text
   - Create TextSelectionHelper with full message text
   - Show ActionMode with "Quote" button
   - On "Quote" click → create InputTextQuote → showReply() → open keyboard
   - On cancel → clear TextSelectionHelper via CancelCallback

3. **UTF-16 Position Handling:**
   - Java String uses UTF-16 internally
   - convertUtf16ToCharIndex handles surrogate pairs correctly
   - Quote position calculated at selection start (0 for full message)

**Current Status:**
- ✅ Quote highlighting on click: WORKING
- ✅ Two-step quote creation: WORKING (1st press selects, 2nd shows floating ActionMode)
- ✅ Floating ActionMode menu: WORKING (TYPE_FLOATING like EditText)
- ✅ Visual text selection highlight: WORKING (blue highlight)
- ✅ Selection handles: WORKING (draggable circles like EditText)
- ✅ Touch event handling: WORKING (drag handles to change selection)
- ✅ Quote button removed from selection menu: DONE
- ✅ Repeated long-press fix: WORKING (ActionMode cancel callback implemented)
- ✅ Partial text selection: IMPLEMENTED (touch handling and character detection)
- ✅ Multi-line selection support: IMPLEMENTED (getCharIndexAt supports multi-line)

**Implementation Details:**

1. **Repeated Long-Press Fix:**
   - Added CancelCallback interface to TextSelectionHelper
   - TextSelectionHelper now notifies TGMessageText when ActionMode is cancelled
   - TGMessageText properly resets textSelectionHelper on cancel
   - Always recreates TextSelectionHelper for clean state

2. **Floating ActionMode (like EditText):**
   - Uses ActionMode.Callback2 for floating menu support
   - ActionMode.TYPE_FLOATING on Android 6.0+ (API 23+)
   - onGetContentRect() positions menu above selection
   - View invalidation on show/hide for selection rendering

3. **Visual Selection Rendering:**
   - drawSelection() renders blue highlight using quote highlight system
   - drawHandles() draws circular handles with stems at selection edges
   - Rendered in TGMessageText.drawContent() after text drawing
   - Handle color: #2196F3 (Material Blue), radius: 12dp

4. **Touch Event Handling:**
   - TGMessageText.onTouchEvent() delegates to TextSelectionHelper first
   - updateSelectionFromTouch() updates selection based on drag
   - isDraggingStart/isDraggingEnd flags track which handle is being moved
   - View invalidation on touch for smooth visual updates

5. **Partial Text Selection:**
   - Added getCharIndexAt() method in Text.java for coordinate-to-character mapping
   - Implemented touch handling in TextSelectionHelper with drag support
   - Selection updates based on touch position

6. **Multi-Line Selection:**
   - getCharIndexAt() finds line by Y coordinate
   - Finds character within line by X coordinate
   - Handles text measurement with proper Paint widths

**Future Enhancements (Optional):**
1. Custom selection handle drawables (drag circles/pins)
2. Selection persistence across view recycling
3. Word/paragraph selection on double-tap/triple-tap

### Message Input Cursor Thickness
Made the message input cursor more visible by increasing its width.

**Files Created:**
- `app/src/main/res/drawable/cursor_input_message.xml` - Custom cursor drawable with 2dp width

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/component/chat/InputView.java`:
  - Line 213: Changed from `Views.clearCursorDrawable(this)` to `Views.setCursorDrawable(this, R.drawable.cursor_input_message)`
  - Now uses custom cursor instead of system default

### Paid Reaction Double Star Icon Fix
Fixed paid (star) reactions displaying two overlapping star icons instead of one.

**Root Cause:** `TGReactions.drawReceiver()` was calling both `drawable.draw(c)` and `Drawables.draw()` on the same star drawable, causing duplicate rendering.

**Files Modified:**
- `app/src/main/java/org/thunderdog/challegram/data/TGReactions.java`:
  - Line 981: Removed duplicate `Drawables.draw()` call
  - Now only draws star icon once via `drawable.draw(c)` after setting bounds/alpha/color filter

---

### Text Selection System Improvements & Bug Fixes
Comprehensive improvements to the quote creation text selection system with bug fixes and unification.

**Bugs Fixed:**

1. **ActionMode Memory Leak:**
   - Fixed memory leak when MessagesController is destroyed without closing ActionMode
   - Added cleanup in `onMessageContainerDestroyed()` and `finishTextSelection()`
   - ActionMode now properly closes when fragment is destroyed

2. **Selection Handles Not Responding:**
   - Increased touch hit radius from 40dp to 50dp
   - Improved hit detection to account for actual handle drawable height
   - Handles now properly respond to dragging by their lower portions
   - Fixed touch events passing through to underlying messages

3. **Selection Offset on Multiline Text:**
   - Fixed handles and highlight "shifting" on second and subsequent lines
   - Added dynamic layoutOffset updates in both `drawContent()` and `onTouchEvent()`
   - Properly accounts for linkPreview position when it's above text
   - Coordinates now stay synchronized during animations

4. **Click Outside Closes ActionMode:**
   - Click/tap anywhere outside selection handles now closes quote mode
   - Implemented in `MessageView.onTouchEvent()` - detects unhandled ACTION_DOWN events

5. **Back Button Handling:**
   - System back button now closes ActionMode when text selection is active
   - Added `performOnBackPressed()` override in `MessagesController`
   - Added `hasActiveTextSelection()` static method in `TGMessage`

6. **System Unification:**
   - Moved base text selection logic to `TGMessage` base class
   - Added documentation comments explaining how to implement in other message types
   - Uses existing `getMessageText()` public final method (extracts text via Td.textOrCaption)
   - System now ready for use in `TGMessageMedia` and other message types

**Files Modified:**

- `app/src/main/java/org/thunderdog/challegram/util/text/TextSelectionHelper.java`:
  - Enhanced `checkHandleTouch()` with larger hit radius (50dp) and proper height calculation
  - Improved touch detection logic for handle drawables

- `app/src/main/java/org/thunderdog/challegram/data/TGMessageText.java`:
  - Added `finishTextSelection()` override to properly clean up TextSelectionHelper
  - Fixed callback cleanup in `showQuoteActionMode()` to avoid duplicate finishTextSelection calls
  - Added cleanup in `onMessageContainerDestroyed()` to prevent memory leaks
  - Fixed layoutOffset updates in `drawContent()` to account for linkPreview animation
  - Fixed layoutOffset updates in `onTouchEvent()` for proper touch handling

- `app/src/main/java/org/thunderdog/challegram/component/chat/MessageView.java`:
  - Added logic to close ActionMode when clicking outside selection handles
  - Touch events now properly consumed when selection is active

- `app/src/main/java/org/thunderdog/challegram/ui/MessagesController.java`:
  - Added `performOnBackPressed()` override to handle back button
  - Back button closes text selection before falling back to default behavior

- `app/src/main/java/org/thunderdog/challegram/data/TGMessage.java`:
  - Added comprehensive documentation block explaining text selection system
  - Added `hasActiveTextSelection()` static method for global state checking
  - Uses existing `getMessageText()` public final method that extracts text via Td.textOrCaption
  - System now provides clear API for implementing in subclasses

**Implementation Notes:**

The text selection system is now properly unified and documented. Other message types (like TGMessageMedia) can easily implement quote functionality by:
1. Overriding `canTextSelection()` if custom logic is needed (base implementation uses getMessageText())
2. Overriding `processTextSelection()` to show quote ActionMode
3. Overriding `finishTextSelection()` to clean up resources
4. Calling `registerAsActiveSelection()` and `unregisterAsActiveSelection()` appropriately

The base `getMessageText()` method (public final) already extracts text from msg.content using Td.textOrCaption(), so it works for most message types without modification.

**Current Status:**
- ✅ Memory leak fixed
- ✅ Touch handling improved
- ✅ Multiline coordinate sync fixed (removed duplicate offset addition in drawHandles)
- ✅ Click outside closes selection
- ✅ Back button closes selection
- ✅ System unified and documented
- ✅ System highlight color used (replaced hardcoded blue with ColorId.textSelectionHighlight)

**Additional Fixes (Round 2):**

1. **Multiline Selection Offset Fixed (Final):**
   - Root cause identified: `drawHandles()` was overwriting `layoutX/layoutY` and then adding `startX/startY` again
   - This caused double offset calculation, making handles shift on second+ lines
   - Fixed by removing redundant `layoutX/layoutY` update in `drawHandles()` (lines 273-275 removed)
   - layoutX/layoutY now set only once via `setLayoutOffset()` before drawing
   - Handles now render at correct positions on all lines

2. **System Highlight Color:**
   - Replaced hardcoded `0x4000A0FF` (semi-transparent blue) with system color
   - Now uses `Theme.getColor(ColorId.textSelectionHighlight)` for proper theme support
   - Selection highlight color adapts to user's theme (dark/light mode)
   - File: `Text.java` line 2448-2450
