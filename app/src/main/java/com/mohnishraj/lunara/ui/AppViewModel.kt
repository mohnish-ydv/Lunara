package com.mohnishraj.lunara.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mohnishraj.lunara.LunaraApplication
import com.mohnishraj.lunara.core.AppConfig
import com.mohnishraj.lunara.data.cache.toCache
import com.mohnishraj.lunara.data.cache.toDomain
import com.mohnishraj.lunara.domain.AuthResult
import com.mohnishraj.lunara.domain.ChatConversation
import com.mohnishraj.lunara.domain.ChatMessage
import com.mohnishraj.lunara.domain.ChatSignal
import com.mohnishraj.lunara.domain.ConversationFilter
import com.mohnishraj.lunara.domain.ConversationInsight
import com.mohnishraj.lunara.domain.ConversationSettings
import com.mohnishraj.lunara.domain.MessageDeliveryState
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MediaAttachment
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.domain.MediaStorageSnapshot
import com.mohnishraj.lunara.domain.MediaTransferState
import com.mohnishraj.lunara.domain.VoiceRecordingState
import com.mohnishraj.lunara.domain.SpaceChannel
import com.mohnishraj.lunara.domain.SpaceChannelKind
import com.mohnishraj.lunara.domain.SpaceDetail
import com.mohnishraj.lunara.domain.SpaceMessage
import com.mohnishraj.lunara.domain.SpaceSignal
import com.mohnishraj.lunara.domain.SpaceSummary
import com.mohnishraj.lunara.data.media.MediaAttachmentStore
import com.mohnishraj.lunara.domain.PeopleSnapshot
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

enum class AppScreen {
    Splash,
    Onboarding,
    Welcome,
    SignIn,
    SignUp,
    ConfirmEmail,
    ProfileSetup,
    Home,
}

enum class HomeTab {
    Chats,
    People,
    Spaces,
    You,
}

enum class PeopleTab {
    Discover,
    Requests,
    Connections,
}

data class PeopleUiState(
    val tab: PeopleTab = PeopleTab.Discover,
    val query: String = "",
    val results: List<UserProfile> = emptyList(),
    val snapshot: PeopleSnapshot = PeopleSnapshot(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val actionId: String? = null,
    val selectedPerson: UserProfile? = null,
    val showShareCard: Boolean = false,
    val showPrivacy: Boolean = false,
    val showBlocked: Boolean = false,
    val notice: String? = null,
)

data class ChatUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val query: String = "",
    val filter: ConversationFilter = ConversationFilter.All,
    val activeConversation: ChatConversation? = null,
    val organizingConversation: ChatConversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val composer: String = "",
    val replyTo: ChatMessage? = null,
    val editTarget: ChatMessage? = null,
    val selectedMessage: ChatMessage? = null,
    val showMessageSearch: Boolean = false,
    val messageSearchQuery: String = "",
    val messageSearchResults: List<ChatMessage> = emptyList(),
    val isSearchingMessages: Boolean = false,
    val showBookmarks: Boolean = false,
    val bookmarkedMessages: List<ChatMessage> = emptyList(),
    val isLoadingBookmarks: Boolean = false,
    val showDetails: Boolean = false,
    val showModuleComposer: Boolean = false,
    val showAttachmentPicker: Boolean = false,
    val pendingAttachment: MediaAttachment? = null,
    val isPreparingMedia: Boolean = false,
    val voiceRecording: VoiceRecordingState = VoiceRecordingState(),
    val showMediaGallery: Boolean = false,
    val mediaMessages: List<ChatMessage> = emptyList(),
    val isLoadingMedia: Boolean = false,
    val sharedLinks: List<ChatMessage> = emptyList(),
    val insight: ConversationInsight = ConversationInsight(),
    val isLoadingDetails: Boolean = false,
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val actionMessageId: String? = null,
    val notice: String? = null,
)

data class MediaUiState(
    val showStorage: Boolean = false,
    val snapshot: MediaStorageSnapshot = MediaStorageSnapshot(),
    val isClearing: Boolean = false,
)

data class SpaceUiState(
    val spaces: List<SpaceSummary> = emptyList(),
    val query: String = "",
    val favoriteOnly: Boolean = false,
    val activeSpace: SpaceDetail? = null,
    val activeChannel: SpaceChannel? = null,
    val messages: List<SpaceMessage> = emptyList(),
    val composer: String = "",
    val replyTo: SpaceMessage? = null,
    val showCreateSpace: Boolean = false,
    val showCreateChannel: Boolean = false,
    val showInfo: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val actionId: String? = null,
    val notice: String? = null,
)

data class AppUiState(
    val screen: AppScreen = AppScreen.Splash,
    val session: UserSession? = null,
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val cloudReady: Boolean = AppConfig.isCloudReady,
    val message: String? = null,
    val confirmationEmail: String = "",
    val homeTab: HomeTab = HomeTab.Chats,
    val people: PeopleUiState = PeopleUiState(),
    val chat: ChatUiState = ChatUiState(),
    val media: MediaUiState = MediaUiState(),
    val spaces: SpaceUiState = SpaceUiState(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LunaraApplication
    private val prefs = application.getSharedPreferences("lunara_preferences", 0)
    private val cache = app.database.profileCacheDao()
    private val authRepository = app.authRepository
    private val peopleRepository = app.peopleRepository
    private val chatRepository = app.chatRepository
    private val draftStore = app.draftStore
    private val mediaStore = app.mediaStore
    private val voiceRecorder = app.voiceRecorder
    private val spaceRepository = app.spaceRepository
    private val sessionStore = app.sessionStore
    private var peopleSearchJob: Job? = null
    private var messageSearchJob: Job? = null
    private var chatSyncJob: Job? = null
    private var presenceJob: Job? = null
    private var typingJob: Job? = null
    private var realtimeRefreshJob: Job? = null
    private var recordingTickerJob: Job? = null
    private var spaceSyncJob: Job? = null
    private var spaceRefreshJob: Job? = null
    private var pendingSharedProfile: String? = null

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        restoreApp()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            app.networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
                if (online && _uiState.value.screen == AppScreen.Home) refreshChats(silent = true)
            }
        }
    }

    private fun restoreApp() {
        viewModelScope.launch {
            delay(850)
            val session = sessionStore.load()
            if (session == null) {
                val seenOnboarding = prefs.getBoolean("seen_onboarding", false)
                _uiState.update { it.copy(screen = if (seenOnboarding) AppScreen.Welcome else AppScreen.Onboarding) }
                return@launch
            }

            val cached = cache.get(session.userId)?.toDomain()
            _uiState.update { it.copy(session = session, profile = cached) }
            val remote = authRepository.loadProfile(session).getOrNull()
            if (remote != null) cache.upsert(remote.toCache())
            val profile = remote ?: cached
            _uiState.update {
                it.copy(
                    profile = profile,
                    screen = if (profile == null || profile.username.isBlank()) AppScreen.ProfileSetup else AppScreen.Home,
                )
            }
            if (profile != null && profile.username.isNotBlank()) beginOnlineSession(session)
            tryOpenPendingSharedProfile()
        }
    }

    fun finishOnboarding() {
        prefs.edit().putBoolean("seen_onboarding", true).apply()
        _uiState.update { it.copy(screen = AppScreen.Welcome) }
    }

    fun open(screen: AppScreen) {
        _uiState.update { it.copy(screen = screen, message = null) }
    }

    fun signIn(email: String, password: String) = authenticate(email, password, isSignUp = false)

    fun signUp(email: String, password: String) = authenticate(email, password, isSignUp = true)

    private fun authenticate(email: String, password: String, isSignUp: Boolean) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = if (isSignUp) {
                authRepository.signUp(email.trim(), password)
            } else {
                authRepository.signIn(email.trim(), password)
            }
            when (result) {
                is AuthResult.SignedIn -> {
                    sessionStore.save(result.session)
                    val profile = authRepository.loadProfile(result.session).getOrNull()
                    if (profile != null) cache.upsert(profile.toCache())
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = result.session,
                            profile = profile,
                            screen = if (profile == null || profile.username.isBlank()) AppScreen.ProfileSetup else AppScreen.Home,
                        )
                    }
                    if (profile != null && profile.username.isNotBlank()) beginOnlineSession(result.session)
                    tryOpenPendingSharedProfile()
                }
                is AuthResult.ConfirmationRequired -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        confirmationEmail = result.email.ifBlank { email },
                        screen = AppScreen.ConfirmEmail,
                    )
                }
                is AuthResult.Failure -> _uiState.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    fun completeProfile(displayName: String, username: String, bio: String, avatarSeed: Int) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val profile = UserProfile(
                id = session.userId,
                email = session.email,
                username = username.trim().lowercase(),
                displayName = displayName.trim(),
                bio = bio.trim(),
                avatarSeed = avatarSeed,
            )
            authRepository.saveProfile(session, profile)
                .onSuccess { saved ->
                    cache.upsert(saved.toCache())
                    _uiState.update { state ->
                        state.copy(isLoading = false, profile = saved, screen = AppScreen.Home)
                    }
                    beginOnlineSession(session)
                    tryOpenPendingSharedProfile()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, message = error.message ?: "Could not save your profile") }
                }
        }
    }

    private fun beginOnlineSession(session: UserSession) {
        chatSyncJob?.cancel()
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            while (true) {
                chatRepository.setPresence(session, true)
                delay(45_000)
            }
        }
        refreshChats(silent = false)
        refreshSpaces(silent = false)
        chatSyncJob = viewModelScope.launch {
            chatRepository.observe(session).collect { signal ->
                when (signal) {
                    ChatSignal.Refresh, ChatSignal.Reconnected -> scheduleRealtimeRefresh()
                }
            }
        }
        spaceSyncJob?.cancel()
        spaceSyncJob = viewModelScope.launch {
            spaceRepository.observe(session).collect { signal ->
                when (signal) {
                    SpaceSignal.Refresh, SpaceSignal.Reconnected -> scheduleSpaceRefresh()
                }
            }
        }
    }

    private fun scheduleRealtimeRefresh() {
        realtimeRefreshJob?.cancel()
        realtimeRefreshJob = viewModelScope.launch {
            delay(180)
            refreshChats(silent = true)
            val active = _uiState.value.chat.activeConversation
            if (active != null) loadActiveConversation(active, showLoader = false)
        }
    }

    fun selectHomeTab(tab: HomeTab) {
        if (_uiState.value.chat.activeConversation != null) return
        _uiState.update { it.copy(homeTab = tab) }
        when (tab) {
            HomeTab.Chats -> refreshChats(silent = _uiState.value.chat.conversations.isNotEmpty())
            HomeTab.People -> if (_uiState.value.people.results.isEmpty()) refreshPeople()
            HomeTab.Spaces -> refreshSpaces(silent = _uiState.value.spaces.spaces.isNotEmpty())
            HomeTab.You -> Unit
        }
    }

    private fun scheduleSpaceRefresh() {
        spaceRefreshJob?.cancel()
        spaceRefreshJob = viewModelScope.launch {
            delay(220)
            refreshSpaces(silent = true)
            val activeSpaceId = _uiState.value.spaces.activeSpace?.space?.id
            if (activeSpaceId != null) loadSpaceDetail(activeSpaceId, showLoader = false)
            val activeChannel = _uiState.value.spaces.activeChannel
            if (activeChannel != null) loadSpaceMessages(activeChannel, showLoader = false)
        }
    }

    fun updateSpaceQuery(query: String) {
        _uiState.update { it.copy(spaces = it.spaces.copy(query = query.take(80))) }
    }

    fun setFavoriteSpacesOnly(enabled: Boolean) {
        _uiState.update { it.copy(spaces = it.spaces.copy(favoriteOnly = enabled)) }
    }

    fun refreshSpaces(silent: Boolean = false) {
        val session = _uiState.value.session ?: return
        if (!silent && _uiState.value.spaces.isLoading) return
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(spaces = it.spaces.copy(isLoading = true, notice = null)) }
            spaceRepository.spaces(session)
                .onSuccess { spaces ->
                    val activeId = _uiState.value.spaces.activeSpace?.space?.id
                    val active = _uiState.value.spaces.activeSpace?.let { detail ->
                        spaces.firstOrNull { it.id == activeId }?.let { detail.copy(space = it) } ?: detail
                    }
                    _uiState.update { it.copy(spaces = it.spaces.copy(spaces = spaces, activeSpace = active, isLoading = false)) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(spaces = it.spaces.copy(isLoading = false, notice = error.message ?: "Could not refresh spaces")) }
                }
        }
    }

    fun openSpace(space: SpaceSummary) {
        _uiState.update {
            it.copy(
                homeTab = HomeTab.Spaces,
                spaces = it.spaces.copy(
                    activeSpace = SpaceDetail(space),
                    activeChannel = null,
                    messages = emptyList(),
                    composer = "",
                    replyTo = null,
                    showInfo = false,
                    notice = null,
                ),
            )
        }
        loadSpaceDetail(space.id, showLoader = true)
    }

    private fun loadSpaceDetail(spaceId: String, showLoader: Boolean) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            if (showLoader) _uiState.update { it.copy(spaces = it.spaces.copy(isLoadingDetail = true, notice = null)) }
            spaceRepository.detail(session, spaceId)
                .onSuccess { detail ->
                    _uiState.update { state ->
                        if (state.spaces.activeSpace?.space?.id != spaceId) state else state.copy(
                            spaces = state.spaces.copy(activeSpace = detail, isLoadingDetail = false)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(spaces = it.spaces.copy(isLoadingDetail = false, notice = error.message ?: "Could not open this space")) }
                }
        }
    }

    fun closeSpace() {
        _uiState.update {
            it.copy(spaces = it.spaces.copy(activeSpace = null, activeChannel = null, messages = emptyList(), composer = "", replyTo = null, showInfo = false, showCreateChannel = false))
        }
        refreshSpaces(silent = true)
    }

    fun openSpaceChannel(channel: SpaceChannel) {
        _uiState.update {
            it.copy(spaces = it.spaces.copy(activeChannel = channel, messages = emptyList(), composer = "", replyTo = null, notice = null))
        }
        loadSpaceMessages(channel, showLoader = true)
    }

    private fun loadSpaceMessages(channel: SpaceChannel, showLoader: Boolean) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            if (showLoader) _uiState.update { it.copy(spaces = it.spaces.copy(isLoadingMessages = true, notice = null)) }
            spaceRepository.messages(session, channel.id, limit = 60)
                .onSuccess { messages ->
                    _uiState.update { state ->
                        if (state.spaces.activeChannel?.id != channel.id) state else state.copy(
                            spaces = state.spaces.copy(messages = messages.sortedBy(SpaceMessage::createdAt), isLoadingMessages = false)
                        )
                    }
                    spaceRepository.markChannelRead(session, channel.id)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(spaces = it.spaces.copy(isLoadingMessages = false, notice = error.message ?: "Could not load channel messages")) }
                }
        }
    }

    fun closeSpaceChannel() {
        val spaceId = _uiState.value.spaces.activeSpace?.space?.id
        _uiState.update { it.copy(spaces = it.spaces.copy(activeChannel = null, messages = emptyList(), composer = "", replyTo = null, notice = null)) }
        if (spaceId != null) loadSpaceDetail(spaceId, showLoader = false)
    }

    fun updateSpaceComposer(value: String) {
        _uiState.update { it.copy(spaces = it.spaces.copy(composer = value.take(4000))) }
    }

    fun replyToSpaceMessage(message: SpaceMessage?) {
        _uiState.update { it.copy(spaces = it.spaces.copy(replyTo = message)) }
    }

    fun sendSpaceComposer() {
        val session = _uiState.value.session ?: return
        val channel = _uiState.value.spaces.activeChannel ?: return
        val body = _uiState.value.spaces.composer.trim()
        if (body.isBlank() || _uiState.value.spaces.actionId == "send-space-message") return
        val reply = _uiState.value.spaces.replyTo
        val clientId = UUID.randomUUID().toString()
        _uiState.update { it.copy(spaces = it.spaces.copy(actionId = "send-space-message", notice = null)) }
        viewModelScope.launch {
            spaceRepository.sendMessage(session, channel.id, clientId, body, reply?.id)
                .onSuccess { sent ->
                    _uiState.update {
                        it.copy(spaces = it.spaces.copy(
                            messages = (it.spaces.messages + sent).distinctBy(SpaceMessage::id).sortedBy(SpaceMessage::createdAt),
                            composer = "",
                            replyTo = null,
                            actionId = null,
                        ))
                    }
                    loadSpaceDetail(channel.spaceId, showLoader = false)
                    refreshSpaces(silent = true)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(spaces = it.spaces.copy(actionId = null, notice = error.message ?: "Message was not sent")) }
                }
        }
    }

    fun reactToSpaceMessage(message: SpaceMessage, emoji: String?) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            spaceRepository.react(session, message.id, emoji)
                .onSuccess { _uiState.value.spaces.activeChannel?.let { loadSpaceMessages(it, showLoader = false) } }
                .onFailure { error -> _uiState.update { it.copy(spaces = it.spaces.copy(notice = error.message ?: "Could not update reaction")) } }
        }
    }

    fun showCreateSpace(show: Boolean) {
        _uiState.update { it.copy(spaces = it.spaces.copy(showCreateSpace = show, showCreateChannel = false, showInfo = false, notice = null)) }
    }

    fun createSpace(name: String, description: String, emoji: String, memberIds: List<String>) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.spaces.actionId == "create-space") return
        _uiState.update { it.copy(spaces = it.spaces.copy(actionId = "create-space", notice = null)) }
        viewModelScope.launch {
            spaceRepository.createSpace(session, name, description, emoji, memberIds)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(homeTab = HomeTab.Spaces, spaces = it.spaces.copy(activeSpace = detail, showCreateSpace = false, actionId = null))
                    }
                    refreshSpaces(silent = true)
                }
                .onFailure { error -> _uiState.update { it.copy(spaces = it.spaces.copy(actionId = null, notice = error.message ?: "Could not create space")) } }
        }
    }

    fun showCreateChannel(show: Boolean) {
        _uiState.update { it.copy(spaces = it.spaces.copy(showCreateChannel = show, showCreateSpace = false, showInfo = false, notice = null)) }
    }

    fun createSpaceChannel(name: String, description: String, kind: SpaceChannelKind) {
        val session = _uiState.value.session ?: return
        val spaceId = _uiState.value.spaces.activeSpace?.space?.id ?: return
        if (_uiState.value.spaces.actionId == "create-channel") return
        _uiState.update { it.copy(spaces = it.spaces.copy(actionId = "create-channel", notice = null)) }
        viewModelScope.launch {
            spaceRepository.createChannel(session, spaceId, name, description, kind)
                .onSuccess { channel ->
                    _uiState.update { it.copy(spaces = it.spaces.copy(showCreateChannel = false, actionId = null, activeChannel = channel, messages = emptyList())) }
                    loadSpaceMessages(channel, showLoader = true)
                    loadSpaceDetail(spaceId, showLoader = false)
                }
                .onFailure { error -> _uiState.update { it.copy(spaces = it.spaces.copy(actionId = null, notice = error.message ?: "Could not create channel")) } }
        }
    }

    fun showSpaceInfo(show: Boolean) {
        _uiState.update { it.copy(spaces = it.spaces.copy(showInfo = show, showCreateSpace = false, showCreateChannel = false)) }
    }

    fun toggleSpaceFavorite(space: SpaceSummary) = updateSpacePreferences(space, favorite = !space.isFavorite, muted = space.isMuted)
    fun toggleSpaceMute(space: SpaceSummary) = updateSpacePreferences(space, favorite = space.isFavorite, muted = !space.isMuted)

    private fun updateSpacePreferences(space: SpaceSummary, favorite: Boolean, muted: Boolean) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            spaceRepository.setPreferences(session, space.id, favorite, muted)
                .onSuccess {
                    _uiState.update { state ->
                        val updated = space.copy(isFavorite = favorite, isMuted = muted)
                        state.copy(spaces = state.spaces.copy(
                            spaces = state.spaces.spaces.map { if (it.id == space.id) updated else it },
                            activeSpace = state.spaces.activeSpace?.takeIf { it.space.id == space.id }?.copy(space = updated) ?: state.spaces.activeSpace,
                        ))
                    }
                    refreshSpaces(silent = true)
                }
                .onFailure { error -> _uiState.update { it.copy(spaces = it.spaces.copy(notice = error.message ?: "Could not update space")) } }
        }
    }

    fun leaveSpace(space: SpaceSummary) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.spaces.actionId == "leave-space") return
        _uiState.update { it.copy(spaces = it.spaces.copy(actionId = "leave-space", notice = null)) }
        viewModelScope.launch {
            spaceRepository.leaveSpace(session, space.id)
                .onSuccess {
                    _uiState.update { it.copy(spaces = it.spaces.copy(activeSpace = null, activeChannel = null, showInfo = false, actionId = null, messages = emptyList())) }
                    refreshSpaces(silent = false)
                }
                .onFailure { error -> _uiState.update { it.copy(spaces = it.spaces.copy(actionId = null, notice = error.message ?: "Could not leave space")) } }
        }
    }

    fun dismissSpaceNotice() {
        _uiState.update { it.copy(spaces = it.spaces.copy(notice = null)) }
    }

    fun updateChatQuery(query: String) {
        _uiState.update { it.copy(chat = it.chat.copy(query = query.take(80))) }
    }

    fun selectConversationFilter(filter: ConversationFilter) {
        _uiState.update { it.copy(chat = it.chat.copy(filter = filter, organizingConversation = null)) }
    }

    fun openConversationOrganizer(conversation: ChatConversation?) {
        _uiState.update { it.copy(chat = it.chat.copy(organizingConversation = conversation)) }
    }

    fun refreshChats(silent: Boolean = false) {
        val session = _uiState.value.session ?: return
        if (!silent && _uiState.value.chat.isLoadingConversations) return
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(chat = it.chat.copy(isLoadingConversations = true, notice = null)) }
            chatRepository.conversations(session)
                .onSuccess { conversations ->
                    val drafts = draftStore.all()
                    val organized = conversations.map { conversation ->
                        conversation.copy(draft = drafts[conversation.id].orEmpty())
                    }
                    val activeId = _uiState.value.chat.activeConversation?.id
                    val updatedActive = activeId?.let { id -> organized.firstOrNull { it.id == id } }
                        ?: _uiState.value.chat.activeConversation
                    _uiState.update {
                        it.copy(
                            chat = it.chat.copy(
                                conversations = organized,
                                activeConversation = updatedActive,
                                isLoadingConversations = false,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            chat = it.chat.copy(
                                isLoadingConversations = false,
                                notice = error.message ?: "Could not refresh conversations",
                            )
                        )
                    }
                }
        }
    }

    fun startChat(person: UserProfile) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.chat.actionMessageId == "conversation") return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    homeTab = HomeTab.Chats,
                    people = it.people.copy(selectedPerson = null),
                    chat = it.chat.copy(actionMessageId = "conversation", notice = null),
                )
            }
            chatRepository.ensureConversation(session, person)
                .onSuccess { conversation ->
                    val withDraft = conversation.copy(draft = draftStore.load(conversation.id))
                    _uiState.update {
                        it.copy(chat = it.chat.copy(actionMessageId = null, activeConversation = withDraft, composer = withDraft.draft))
                    }
                    loadActiveConversation(withDraft, showLoader = true)
                    refreshChats(silent = true)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(chat = it.chat.copy(actionMessageId = null, notice = error.message ?: "Could not start the conversation"))
                    }
                }
        }
    }

    fun openConversation(conversation: ChatConversation) {
        val withDraft = conversation.copy(draft = draftStore.load(conversation.id))
        _uiState.update {
            it.copy(
                homeTab = HomeTab.Chats,
                chat = it.chat.copy(
                    activeConversation = withDraft,
                    messages = emptyList(),
                    composer = withDraft.draft,
                    replyTo = null,
                    editTarget = null,
                    selectedMessage = null,
                    organizingConversation = null,
                    showMessageSearch = false,
                    messageSearchQuery = "",
                    messageSearchResults = emptyList(),
                    showBookmarks = false,
                    bookmarkedMessages = emptyList(),
                    showDetails = false,
                    showModuleComposer = false,
                    showAttachmentPicker = false,
                    pendingAttachment = null,
                    voiceRecording = VoiceRecordingState(),
                    showMediaGallery = false,
                    mediaMessages = emptyList(),
                    sharedLinks = emptyList(),
                    insight = ConversationInsight(),
                    hasMore = true,
                ),
            )
        }
        loadActiveConversation(withDraft, showLoader = true)
    }

    private fun loadActiveConversation(conversation: ChatConversation, showLoader: Boolean) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            if (showLoader) _uiState.update { it.copy(chat = it.chat.copy(isLoadingMessages = true, notice = null)) }
            chatRepository.messages(session, conversation.id, limit = 40)
                .onSuccess { remote ->
                    val optimistic = _uiState.value.chat.messages.filter {
                        it.deliveryState == MessageDeliveryState.Sending || it.deliveryState == MessageDeliveryState.Failed
                    }
                    val remoteClientIds = remote.mapTo(mutableSetOf(), ChatMessage::clientId)
                    val merged = (remote + optimistic.filterNot { it.clientId in remoteClientIds })
                        .distinctBy(ChatMessage::id)
                        .sortedBy(ChatMessage::createdAt)
                    _uiState.update {
                        if (it.chat.activeConversation?.id != conversation.id) it else it.copy(
                            chat = it.chat.copy(
                                messages = merged,
                                isLoadingMessages = false,
                                hasMore = remote.size >= 40,
                            )
                        )
                    }
                    chatRepository.markDelivered(session, conversation.id)
                    chatRepository.markRead(session, conversation.id)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(chat = it.chat.copy(isLoadingMessages = false, notice = error.message ?: "Could not load messages"))
                    }
                }
        }
    }

    fun loadOlderMessages() {
        val session = _uiState.value.session ?: return
        val state = _uiState.value.chat
        val conversation = state.activeConversation ?: return
        val before = state.messages.firstOrNull { it.createdAt.isNotBlank() }?.createdAt ?: return
        if (state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(chat = it.chat.copy(isLoadingMore = true)) }
            chatRepository.messages(session, conversation.id, before = before, limit = 40)
                .onSuccess { older ->
                    _uiState.update {
                        it.copy(
                            chat = it.chat.copy(
                                messages = (older + it.chat.messages).distinctBy(ChatMessage::id).sortedBy(ChatMessage::createdAt),
                                isLoadingMore = false,
                                hasMore = older.size >= 40,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(chat = it.chat.copy(isLoadingMore = false, notice = error.message ?: "Could not load earlier messages"))
                    }
                }
        }
    }

    fun closeConversation() {
        val session = _uiState.value.session
        val conversation = _uiState.value.chat.activeConversation
        typingJob?.cancel()
        recordingTickerJob?.cancel()
        if (voiceRecorder.isRecording) voiceRecorder.cancel()
        if (session != null && conversation != null) {
            viewModelScope.launch { chatRepository.setTyping(session, conversation.id, false) }
        }
        _uiState.update {
            it.copy(
                chat = it.chat.copy(
                    activeConversation = null,
                    messages = emptyList(),
                    composer = "",
                    replyTo = null,
                    editTarget = null,
                    selectedMessage = null,
                    organizingConversation = null,
                    showMessageSearch = false,
                    messageSearchQuery = "",
                    messageSearchResults = emptyList(),
                    showBookmarks = false,
                    bookmarkedMessages = emptyList(),
                    showDetails = false,
                    showModuleComposer = false,
                    showAttachmentPicker = false,
                    pendingAttachment = null,
                    voiceRecording = VoiceRecordingState(),
                    showMediaGallery = false,
                    mediaMessages = emptyList(),
                    sharedLinks = emptyList(),
                    insight = ConversationInsight(),
                    isLoadingMessages = false,
                )
            )
        }
        refreshChats(silent = true)
    }

    fun updateComposer(text: String) {
        val clean = text.take(4000)
        val current = _uiState.value.chat
        _uiState.update { it.copy(chat = it.chat.copy(composer = clean)) }
        val conversation = current.activeConversation ?: return
        if (current.editTarget == null) draftStore.save(conversation.id, clean)
        val session = _uiState.value.session ?: return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            chatRepository.setTyping(session, conversation.id, clean.isNotBlank())
            if (clean.isNotBlank()) {
                delay(1_800)
                chatRepository.setTyping(session, conversation.id, false)
            }
        }
    }

    fun sendComposer() {
        val state = _uiState.value.chat
        val body = state.composer.trim()
        val edit = state.editTarget
        if (edit != null) {
            if (body.isNotBlank()) editMessage(edit, body)
            return
        }
        val pendingMedia = state.pendingAttachment
        if (pendingMedia != null) {
            sendNewMedia(pendingMedia.copy(caption = body), state.replyTo)
            return
        }
        if (body.isBlank()) return
        sendNewMessage(body, state.replyTo)
    }

    private fun sendNewMessage(body: String, replyTo: ChatMessage?, existingClientId: String? = null) {
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        val clientId = existingClientId ?: UUID.randomUUID().toString()
        draftStore.clear(conversation.id)
        val optimistic = ChatMessage(
            id = "local-$clientId",
            clientId = clientId,
            conversationId = conversation.id,
            senderId = session.userId,
            body = body,
            createdAt = Instant.now().toString(),
            replyToId = replyTo?.id.orEmpty(),
            replyPreview = replyTo?.previewText.orEmpty(),
            deliveryState = MessageDeliveryState.Sending,
        )
        _uiState.update {
            val withoutPrevious = it.chat.messages.filterNot { message -> message.clientId == clientId }
            it.copy(
                chat = it.chat.copy(
                    messages = (withoutPrevious + optimistic).sortedBy(ChatMessage::createdAt),
                    composer = "",
                    replyTo = null,
                    editTarget = null,
                    selectedMessage = null,
                    actionMessageId = optimistic.id,
                    notice = null,
                )
            )
        }
        typingJob?.cancel()
        viewModelScope.launch {
            chatRepository.setTyping(session, conversation.id, false)
            chatRepository.sendMessage(
                session = session,
                conversationId = conversation.id,
                clientId = clientId,
                body = body,
                replyToId = replyTo?.id,
            ).onSuccess { sent ->
                _uiState.update {
                    it.copy(
                        chat = it.chat.copy(
                            messages = it.chat.messages.map { message -> if (message.clientId == clientId) sent else message },
                            actionMessageId = null,
                        )
                    )
                }
                refreshChats(silent = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        chat = it.chat.copy(
                            messages = it.chat.messages.map { message ->
                                if (message.clientId == clientId) message.copy(deliveryState = MessageDeliveryState.Failed) else message
                            },
                            actionMessageId = null,
                            notice = error.message ?: "Message was not sent",
                        )
                    )
                }
            }
        }
    }

    fun showAttachmentPicker(show: Boolean) {
        _uiState.update {
            it.copy(chat = it.chat.copy(showAttachmentPicker = show, selectedMessage = null, showModuleComposer = false))
        }
    }

    fun createCameraCapture(): MediaAttachmentStore.CameraCapture = mediaStore.createCameraCapture()

    fun selectMedia(uri: String, kind: MediaKind, cameraPath: String? = null) {
        if (_uiState.value.chat.isPreparingMedia || _uiState.value.chat.activeConversation == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(chat = it.chat.copy(isPreparingMedia = true, showAttachmentPicker = false, notice = null))
            }
            val result = if (cameraPath != null) mediaStore.importCamera(cameraPath) else mediaStore.import(uri, kind)
            result.onSuccess { attachment ->
                _uiState.update {
                    it.copy(chat = it.chat.copy(
                        pendingAttachment = attachment,
                        isPreparingMedia = false,
                        composer = "",
                        editTarget = null,
                    ))
                }
                refreshMediaStorage()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(chat = it.chat.copy(isPreparingMedia = false, notice = error.message ?: "Could not prepare this file"))
                }
            }
        }
    }

    fun clearPendingMedia() {
        val pending = _uiState.value.chat.pendingAttachment
        if (pending != null) mediaStore.deleteOutgoing(pending)
        _uiState.update { it.copy(chat = it.chat.copy(pendingAttachment = null)) }
        refreshMediaStorage()
    }

    private fun sendNewMedia(
        attachment: MediaAttachment,
        replyTo: ChatMessage?,
        existingClientId: String? = null,
    ) {
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        val clean = attachment.validateForSend().getOrElse { error ->
            _uiState.update { it.copy(chat = it.chat.copy(notice = error.message ?: "This attachment is not ready")) }
            return
        }
        val clientId = existingClientId ?: UUID.randomUUID().toString()
        draftStore.clear(conversation.id)
        val optimisticAttachment = clean.copy(
            transferState = MediaTransferState.Uploading,
            progress = 0,
            errorMessage = "",
        )
        val optimistic = ChatMessage(
            id = "local-$clientId",
            clientId = clientId,
            conversationId = conversation.id,
            senderId = session.userId,
            body = clean.previewText(),
            createdAt = Instant.now().toString(),
            replyToId = replyTo?.id.orEmpty(),
            replyPreview = replyTo?.previewText.orEmpty(),
            deliveryState = MessageDeliveryState.Sending,
            attachment = optimisticAttachment,
        )
        _uiState.update { state ->
            val withoutPrevious = state.chat.messages.filterNot { it.clientId == clientId }
            state.copy(chat = state.chat.copy(
                messages = (withoutPrevious + optimistic).sortedBy(ChatMessage::createdAt),
                composer = "",
                pendingAttachment = null,
                replyTo = null,
                selectedMessage = null,
                actionMessageId = optimistic.id,
                notice = null,
            ))
        }
        viewModelScope.launch {
            chatRepository.sendMediaMessage(
                session = session,
                conversationId = conversation.id,
                clientId = clientId,
                attachment = clean,
                replyToId = replyTo?.id,
                onProgress = { progress ->
                    _uiState.update { state ->
                        state.copy(chat = state.chat.copy(messages = state.chat.messages.map { message ->
                            if (message.clientId == clientId && message.attachment != null) {
                                message.copy(attachment = message.attachment.copy(progress = progress.coerceIn(0, 100)))
                            } else message
                        }))
                    }
                },
            ).onSuccess { sent ->
                _uiState.update { state ->
                    state.copy(chat = state.chat.copy(
                        messages = state.chat.messages.map { if (it.clientId == clientId) sent else it },
                        actionMessageId = null,
                    ))
                }
                refreshChats(silent = true)
                refreshMediaStorage()
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(chat = state.chat.copy(
                        messages = state.chat.messages.map { message ->
                            if (message.clientId == clientId && message.attachment != null) {
                                message.copy(
                                    deliveryState = MessageDeliveryState.Failed,
                                    attachment = message.attachment.copy(
                                        transferState = MediaTransferState.Failed,
                                        errorMessage = error.message ?: "Upload failed",
                                    ),
                                )
                            } else message
                        },
                        actionMessageId = null,
                        notice = error.message ?: "Media was not sent",
                    ))
                }
            }
        }
    }

    fun startVoiceRecording() {
        if (voiceRecorder.isRecording || _uiState.value.chat.activeConversation == null) return
        voiceRecorder.start().onFailure { error ->
            _uiState.update { it.copy(chat = it.chat.copy(notice = error.message ?: "Could not start recording")) }
        }.onSuccess {
            _uiState.update { it.copy(chat = it.chat.copy(
                showAttachmentPicker = false,
                pendingAttachment = null,
                voiceRecording = VoiceRecordingState(isRecording = true),
                notice = null,
            )) }
            recordingTickerJob?.cancel()
            recordingTickerJob = viewModelScope.launch {
                val waveform = mutableListOf<Int>()
                while (voiceRecorder.isRecording) {
                    if (!voiceRecorder.isPaused) waveform += voiceRecorder.currentAmplitude()
                    val compressed = if (waveform.size <= 90) waveform.toList() else waveform.filterIndexed { index, _ -> index % 2 == 0 }.takeLast(90)
                    _uiState.update { state ->
                        state.copy(chat = state.chat.copy(voiceRecording = VoiceRecordingState(
                            isRecording = true,
                            isPaused = voiceRecorder.isPaused,
                            elapsedMs = voiceRecorder.elapsedMs(),
                            waveform = compressed,
                        )))
                    }
                    if (voiceRecorder.elapsedMs() >= MediaAttachment.MAX_VOICE_DURATION_MS) {
                        finishVoiceRecording(send = true)
                        break
                    }
                    delay(120)
                }
            }
        }
    }

    fun pauseOrResumeVoiceRecording() {
        val result = if (voiceRecorder.isPaused) voiceRecorder.resume() else voiceRecorder.pause()
        result.onFailure { error -> _uiState.update { it.copy(chat = it.chat.copy(notice = error.message ?: "Recording control failed")) } }
    }

    fun cancelVoiceRecording() {
        recordingTickerJob?.cancel()
        voiceRecorder.cancel()
        _uiState.update { it.copy(chat = it.chat.copy(voiceRecording = VoiceRecordingState())) }
    }

    fun finishVoiceRecording(send: Boolean = true) {
        if (!voiceRecorder.isRecording) return
        val waveform = _uiState.value.chat.voiceRecording.waveform
        recordingTickerJob?.cancel()
        voiceRecorder.stop().onFailure { error ->
            _uiState.update { it.copy(chat = it.chat.copy(voiceRecording = VoiceRecordingState(), notice = error.message ?: "Could not finish recording")) }
        }.onSuccess { recording ->
            viewModelScope.launch {
                mediaStore.voiceAttachment(recording.file, recording.durationMs, waveform)
                    .onSuccess { attachment ->
                        _uiState.update { it.copy(chat = it.chat.copy(voiceRecording = VoiceRecordingState())) }
                        if (send) sendNewMedia(attachment, _uiState.value.chat.replyTo)
                        else _uiState.update { it.copy(chat = it.chat.copy(pendingAttachment = attachment)) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(chat = it.chat.copy(voiceRecording = VoiceRecordingState(), notice = error.message ?: "Voice note is not ready")) }
                    }
            }
        }
    }

    fun downloadMedia(message: ChatMessage) {
        val session = _uiState.value.session ?: return
        val attachment = message.attachment ?: return
        if (attachment.localUri.isNotBlank() && mediaStore.fileFor(attachment) != null) return
        _uiState.update { state ->
            state.copy(chat = state.chat.copy(messages = state.chat.messages.map {
                if (it.id == message.id && it.attachment != null) it.copy(attachment = it.attachment.copy(transferState = MediaTransferState.Downloading, progress = 0)) else it
            }))
        }
        viewModelScope.launch {
            chatRepository.downloadMedia(session, attachment) { progress ->
                _uiState.update { state -> state.copy(chat = state.chat.copy(messages = state.chat.messages.map {
                    if (it.id == message.id && it.attachment != null) it.copy(attachment = it.attachment.copy(progress = progress)) else it
                })) }
            }.onSuccess { downloaded ->
                _uiState.update { state -> state.copy(chat = state.chat.copy(
                    messages = state.chat.messages.map { if (it.id == message.id) it.copy(attachment = downloaded) else it },
                    mediaMessages = state.chat.mediaMessages.map { if (it.id == message.id) it.copy(attachment = downloaded) else it },
                )) }
                refreshMediaStorage()
            }.onFailure { error ->
                _uiState.update { state -> state.copy(chat = state.chat.copy(
                    messages = state.chat.messages.map {
                        if (it.id == message.id && it.attachment != null) it.copy(attachment = it.attachment.copy(transferState = MediaTransferState.Failed, errorMessage = error.message ?: "Download failed")) else it
                    },
                    notice = error.message ?: "Could not download this file",
                )) }
            }
        }
    }

    fun showMediaGallery(show: Boolean) {
        _uiState.update { it.copy(chat = it.chat.copy(showMediaGallery = show, isLoadingMedia = show)) }
        if (!show) return
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        viewModelScope.launch {
            chatRepository.mediaMessages(session, conversation.id)
                .onSuccess { media -> _uiState.update { it.copy(chat = it.chat.copy(mediaMessages = media, isLoadingMedia = false)) } }
                .onFailure { error -> _uiState.update { it.copy(chat = it.chat.copy(isLoadingMedia = false, notice = error.message ?: "Could not load shared media")) } }
        }
    }

    fun showMediaStorage(show: Boolean) {
        _uiState.update { it.copy(media = it.media.copy(showStorage = show, snapshot = mediaStore.storageSnapshot())) }
    }

    fun clearDownloadedMedia() {
        _uiState.update { it.copy(media = it.media.copy(isClearing = true)) }
        viewModelScope.launch {
            val snapshot = mediaStore.clearDownloadedCache()
            _uiState.update { it.copy(media = it.media.copy(isClearing = false, snapshot = snapshot)) }
        }
    }

    private fun refreshMediaStorage() {
        _uiState.update { it.copy(media = it.media.copy(snapshot = mediaStore.storageSnapshot())) }
    }

    fun showModuleComposer(show: Boolean) {
        _uiState.update { it.copy(chat = it.chat.copy(showModuleComposer = show, selectedMessage = null)) }
    }

    fun sendModule(module: MessageModule) {
        val validated = module.validate()
        validated.onFailure { error ->
            _uiState.update { it.copy(chat = it.chat.copy(notice = error.message ?: "Card is not ready")) }
        }.onSuccess { clean ->
            sendNewModule(clean, _uiState.value.chat.replyTo)
        }
    }

    private fun sendNewModule(
        module: MessageModule,
        replyTo: ChatMessage?,
        existingClientId: String? = null,
    ) {
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        val clientId = existingClientId ?: UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = "local-$clientId",
            clientId = clientId,
            conversationId = conversation.id,
            senderId = session.userId,
            body = module.previewText(),
            createdAt = Instant.now().toString(),
            replyToId = replyTo?.id.orEmpty(),
            replyPreview = replyTo?.previewText.orEmpty(),
            deliveryState = MessageDeliveryState.Sending,
            module = module,
        )
        _uiState.update {
            val withoutPrevious = it.chat.messages.filterNot { message -> message.clientId == clientId }
            it.copy(
                chat = it.chat.copy(
                    messages = (withoutPrevious + optimistic).sortedBy(ChatMessage::createdAt),
                    replyTo = null,
                    selectedMessage = null,
                    showModuleComposer = false,
                    actionMessageId = optimistic.id,
                    notice = null,
                )
            )
        }
        viewModelScope.launch {
            chatRepository.sendModuleMessage(
                session = session,
                conversationId = conversation.id,
                clientId = clientId,
                module = module,
                replyToId = replyTo?.id,
            ).onSuccess { sent ->
                _uiState.update {
                    it.copy(
                        chat = it.chat.copy(
                            messages = it.chat.messages.map { message -> if (message.clientId == clientId) sent else message },
                            actionMessageId = null,
                        )
                    )
                }
                refreshChats(silent = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        chat = it.chat.copy(
                            messages = it.chat.messages.map { message ->
                                if (message.clientId == clientId) message.copy(deliveryState = MessageDeliveryState.Failed) else message
                            },
                            actionMessageId = null,
                            notice = error.message ?: "Card was not sent",
                        )
                    )
                }
            }
        }
    }

    fun updateModule(message: ChatMessage, module: MessageModule) {
        val session = _uiState.value.session ?: return
        if (!message.canUseAsServerReference() || message.module == null) return
        val clean = module.validate().getOrElse { error ->
            _uiState.update { it.copy(chat = it.chat.copy(notice = error.message ?: "Card update is invalid")) }
            return
        }
        val optimistic = message.copy(module = clean, body = clean.previewText(), moduleRevision = message.moduleRevision + 1)
        _uiState.update { state ->
            state.copy(
                chat = state.chat.copy(
                    messages = state.chat.messages.map { if (it.id == message.id) optimistic else it },
                    actionMessageId = message.id,
                    selectedMessage = null,
                    notice = null,
                )
            )
        }
        viewModelScope.launch {
            chatRepository.updateModule(session, message.id, message.moduleRevision, clean)
                .onSuccess {
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null)) }
                    _uiState.value.chat.activeConversation?.let { active -> loadActiveConversation(active, showLoader = false) }
                    refreshChats(silent = true)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = state.chat.messages.map { if (it.id == message.id) message else it },
                                actionMessageId = null,
                                notice = error.message ?: "Could not update this card",
                            )
                        )
                    }
                    _uiState.value.chat.activeConversation?.let { active -> loadActiveConversation(active, showLoader = false) }
                }
        }
    }

    fun retryMessage(message: ChatMessage) {
        if (message.deliveryState != MessageDeliveryState.Failed) return
        val reply = _uiState.value.chat.messages.firstOrNull { it.id == message.replyToId }
        val attachment = message.attachment
        val module = message.module
        when {
            attachment != null -> sendNewMedia(attachment.copy(transferState = MediaTransferState.Ready, progress = 100), reply, existingClientId = message.clientId)
            module != null -> sendNewModule(module, reply, existingClientId = message.clientId)
            else -> sendNewMessage(message.body, reply, existingClientId = message.clientId)
        }
    }

    fun selectMessage(message: ChatMessage?) {
        _uiState.update { it.copy(chat = it.chat.copy(selectedMessage = message)) }
    }

    fun beginReply(message: ChatMessage) {
        if (!message.canUseAsServerReference()) return
        _uiState.update {
            it.copy(chat = it.chat.copy(replyTo = message, editTarget = null, selectedMessage = null))
        }
    }

    fun beginEdit(message: ChatMessage) {
        val session = _uiState.value.session ?: return
        if (!message.isMine(session.userId) || message.isDeleted || (message.module != null || message.attachment != null) || !message.canUseAsServerReference()) return
        _uiState.update {
            it.copy(
                chat = it.chat.copy(
                    composer = message.body,
                    editTarget = message,
                    replyTo = null,
                    selectedMessage = null,
                )
            )
        }
    }

    fun cancelComposerContext() {
        _uiState.update { state ->
            val conversationId = state.chat.activeConversation?.id
            state.copy(
                chat = state.chat.copy(
                    replyTo = null,
                    editTarget = null,
                    composer = if (state.chat.editTarget != null && conversationId != null) draftStore.load(conversationId) else state.chat.composer,
                )
            )
        }
    }

    private fun editMessage(message: ChatMessage, body: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = message.id, selectedMessage = null)) }
            chatRepository.editMessage(session, message.id, body)
                .onSuccess {
                    val draft = _uiState.value.chat.activeConversation?.id?.let(draftStore::load).orEmpty()
                    _uiState.update { it.copy(chat = it.chat.copy(composer = draft, editTarget = null, actionMessageId = null)) }
                    _uiState.value.chat.activeConversation?.let { active -> loadActiveConversation(active, showLoader = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null, notice = error.message ?: "Could not edit the message")) }
                }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        val session = _uiState.value.session ?: return
        if (!message.isMine(session.userId) || !message.canUseAsServerReference()) return
        viewModelScope.launch {
            _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = message.id, selectedMessage = null)) }
            chatRepository.deleteMessage(session, message.id)
                .onSuccess {
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null)) }
                    _uiState.value.chat.activeConversation?.let { active -> loadActiveConversation(active, showLoader = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null, notice = error.message ?: "Could not remove the message")) }
                }
        }
    }

    fun reactToMessage(message: ChatMessage, emoji: String?) {
        val session = _uiState.value.session ?: return
        if (!message.canUseAsServerReference()) return
        viewModelScope.launch {
            _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = message.id, selectedMessage = null)) }
            chatRepository.react(session, message.id, emoji)
                .onSuccess {
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null)) }
                    _uiState.value.chat.activeConversation?.let { active -> loadActiveConversation(active, showLoader = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null, notice = error.message ?: "Could not add that reaction")) }
                }
        }
    }

    fun saveConversationSettings(
        conversation: ChatConversation,
        pinned: Boolean,
        archived: Boolean,
        muted: Boolean,
        labels: List<String>,
    ) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.chat.actionMessageId != null) return
        val settings = ConversationSettings(pinned, archived, muted, labels)
        viewModelScope.launch {
            _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = "settings:${conversation.id}", notice = null)) }
            chatRepository.updateConversationSettings(session, conversation.id, settings)
                .onSuccess {
                    val updated = conversation.copy(
                        isPinned = pinned,
                        isArchived = archived,
                        isMuted = muted,
                        labels = labels.map(String::trim).filter(String::isNotBlank).distinct().take(4),
                    )
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                conversations = state.chat.conversations.map { if (it.id == updated.id) updated.copy(draft = it.draft) else it },
                                activeConversation = state.chat.activeConversation?.let { if (it.id == updated.id) updated.copy(draft = it.draft) else it },
                                organizingConversation = null,
                                actionMessageId = null,
                                notice = "Conversation updated",
                            )
                        )
                    }
                    refreshChats(silent = true)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null, notice = error.message ?: "Could not update conversation")) }
                }
        }
    }

    fun toggleBookmark(message: ChatMessage) {
        val session = _uiState.value.session ?: return
        if (!message.canUseAsServerReference() || message.isDeleted) return
        viewModelScope.launch {
            _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = message.id, selectedMessage = null)) }
            chatRepository.setBookmark(session, message.id, !message.isBookmarked)
                .onSuccess {
                    val bookmarked = !message.isBookmarked
                    _uiState.update { state ->
                        state.copy(
                            chat = state.chat.copy(
                                messages = state.chat.messages.map { if (it.id == message.id) it.copy(isBookmarked = bookmarked) else it },
                                messageSearchResults = state.chat.messageSearchResults.map { if (it.id == message.id) it.copy(isBookmarked = bookmarked) else it },
                                bookmarkedMessages = if (bookmarked)
                                    (state.chat.bookmarkedMessages + message.copy(isBookmarked = true)).distinctBy(ChatMessage::id).sortedBy(ChatMessage::createdAt)
                                else state.chat.bookmarkedMessages.filterNot { it.id == message.id },
                                actionMessageId = null,
                                notice = if (bookmarked) "Message saved" else "Removed from saved messages",
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(actionMessageId = null, notice = error.message ?: "Could not update saved messages")) }
                }
        }
    }

    fun showMessageSearch(show: Boolean) {
        messageSearchJob?.cancel()
        _uiState.update {
            it.copy(
                chat = it.chat.copy(
                    showMessageSearch = show,
                    messageSearchQuery = if (show) it.chat.messageSearchQuery else "",
                    messageSearchResults = if (show) it.chat.messageSearchResults else emptyList(),
                    isSearchingMessages = false,
                )
            )
        }
    }

    fun searchConversationMessages(query: String) {
        val clean = query.take(120)
        _uiState.update { it.copy(chat = it.chat.copy(messageSearchQuery = clean, isSearchingMessages = clean.isNotBlank())) }
        messageSearchJob?.cancel()
        if (clean.isBlank()) {
            _uiState.update { it.copy(chat = it.chat.copy(messageSearchResults = emptyList(), isSearchingMessages = false)) }
            return
        }
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        messageSearchJob = viewModelScope.launch {
            delay(220)
            chatRepository.searchMessages(session, conversation.id, clean)
                .onSuccess { results ->
                    _uiState.update { state ->
                        if (state.chat.activeConversation?.id != conversation.id || state.chat.messageSearchQuery != clean) state
                        else state.copy(chat = state.chat.copy(messageSearchResults = results, isSearchingMessages = false))
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(isSearchingMessages = false, notice = error.message ?: "Could not search this conversation")) }
                }
        }
    }

    fun showBookmarks(show: Boolean) {
        _uiState.update { it.copy(chat = it.chat.copy(showBookmarks = show, isLoadingBookmarks = show)) }
        if (!show) return
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        viewModelScope.launch {
            chatRepository.bookmarkedMessages(session, conversation.id)
                .onSuccess { bookmarks ->
                    _uiState.update { it.copy(chat = it.chat.copy(bookmarkedMessages = bookmarks, isLoadingBookmarks = false)) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(chat = it.chat.copy(isLoadingBookmarks = false, notice = error.message ?: "Could not load saved messages")) }
                }
        }
    }

    fun showConversationDetails(show: Boolean) {
        _uiState.update { it.copy(chat = it.chat.copy(showDetails = show, isLoadingDetails = show)) }
        if (!show) return
        val session = _uiState.value.session ?: return
        val conversation = _uiState.value.chat.activeConversation ?: return
        viewModelScope.launch {
            val bookmarks = chatRepository.bookmarkedMessages(session, conversation.id).getOrDefault(emptyList())
            val links = chatRepository.sharedLinks(session, conversation.id, 120).getOrDefault(emptyList())
            val media = chatRepository.mediaMessages(session, conversation.id, 120).getOrDefault(emptyList())
            val loaded = _uiState.value.chat.messages
            val insight = ConversationInsight(
                totalMessages = maxOf(loaded.size, bookmarks.size + links.size + media.size),
                bookmarkedMessages = bookmarks.size,
                sharedLinks = links.size,
                sharedMedia = media.size,
                firstMessageAt = loaded.firstOrNull()?.createdAt.orEmpty(),
            )
            _uiState.update {
                it.copy(chat = it.chat.copy(bookmarkedMessages = bookmarks, sharedLinks = links, mediaMessages = media, insight = insight, isLoadingDetails = false))
            }
        }
    }

    private fun ChatMessage.canUseAsServerReference(): Boolean =
        id.isNotBlank() &&
            !id.startsWith("local-") &&
            deliveryState != MessageDeliveryState.Sending &&
            deliveryState != MessageDeliveryState.Failed

    fun dismissChatNotice() {
        _uiState.update { it.copy(chat = it.chat.copy(notice = null)) }
    }

    fun openSharedProfile(identifier: String) {
        val clean = identifier.trim().removePrefix("@").take(40)
        if (clean.isBlank()) return
        pendingSharedProfile = clean
        tryOpenPendingSharedProfile()
    }

    private fun tryOpenPendingSharedProfile() {
        val clean = pendingSharedProfile ?: return
        val state = _uiState.value
        if (state.session == null || state.profile == null || state.screen != AppScreen.Home) return

        pendingSharedProfile = null
        _uiState.update {
            it.copy(
                homeTab = HomeTab.People,
                people = it.people.copy(
                    tab = PeopleTab.Discover,
                    query = clean,
                    isSearching = true,
                    selectedPerson = null,
                ),
            )
        }
        peopleSearchJob?.cancel()
        peopleSearchJob = viewModelScope.launch { runSearch(clean) }
    }

    fun selectPeopleTab(tab: PeopleTab) {
        _uiState.update { it.copy(people = it.people.copy(tab = tab, notice = null)) }
        if (tab != PeopleTab.Discover) refreshSnapshot()
    }

    fun searchPeople(query: String) {
        _uiState.update { it.copy(people = it.people.copy(query = query, isSearching = true, notice = null)) }
        peopleSearchJob?.cancel()
        peopleSearchJob = viewModelScope.launch {
            delay(250)
            runSearch(query)
        }
    }

    fun refreshPeople() {
        refreshSnapshot()
        peopleSearchJob?.cancel()
        peopleSearchJob = viewModelScope.launch { runSearch(_uiState.value.people.query) }
    }

    private suspend fun runSearch(query: String) {
        val session = _uiState.value.session ?: return
        peopleRepository.search(session, query)
            .onSuccess { results ->
                val blockedIds = _uiState.value.people.snapshot.blocked.mapTo(mutableSetOf(), UserProfile::id)
                _uiState.update {
                    it.copy(
                        people = it.people.copy(
                            results = results.filterNot { person -> person.id in blockedIds },
                            isSearching = false,
                        )
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(people = it.people.copy(isSearching = false, notice = error.message ?: "Search failed"))
                }
            }
    }

    private fun refreshSnapshot() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(people = it.people.copy(isLoading = true)) }
            peopleRepository.snapshot(session)
                .onSuccess { snapshot ->
                    val blockedIds = snapshot.blocked.mapTo(mutableSetOf(), UserProfile::id)
                    _uiState.update {
                        it.copy(
                            people = it.people.copy(
                                snapshot = snapshot,
                                results = it.people.results.filterNot { person -> person.id in blockedIds },
                                isLoading = false,
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            people = it.people.copy(
                                isLoading = false,
                                notice = error.message ?: "Could not refresh people",
                            )
                        )
                    }
                }
        }
    }

    fun sendRequest(person: UserProfile) = peopleAction(person.id, "Request sent") { session ->
        peopleRepository.sendRequest(session, person.id)
    }

    fun acceptRequest(requestId: String, personId: String) = peopleAction(personId, "You are now connected") { session ->
        peopleRepository.acceptRequest(session, requestId)
    }

    fun rejectRequest(requestId: String, personId: String) = peopleAction(personId, "Request declined") { session ->
        peopleRepository.rejectRequest(session, requestId)
    }

    fun cancelRequest(requestId: String, personId: String) = peopleAction(personId, "Request cancelled") { session ->
        peopleRepository.cancelRequest(session, requestId)
    }

    fun removeConnection(connectionId: String, personId: String) = peopleAction(personId, "Connection removed") { session ->
        peopleRepository.removeConnection(session, connectionId)
    }

    fun blockPerson(person: UserProfile) = peopleAction(person.id, "@${person.username} blocked") { session ->
        peopleRepository.block(session, person.id)
    }

    fun unblockPerson(person: UserProfile) = peopleAction(person.id, "@${person.username} unblocked") { session ->
        peopleRepository.unblock(session, person.id)
    }

    private fun peopleAction(
        actionId: String,
        successMessage: String,
        action: suspend (UserSession) -> Result<Unit>,
    ) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.people.actionId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(people = it.people.copy(actionId = actionId, notice = null)) }
            action(session)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            people = it.people.copy(
                                actionId = null,
                                selectedPerson = null,
                                notice = successMessage,
                            )
                        )
                    }
                    refreshSnapshot()
                    runSearch(_uiState.value.people.query)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            people = it.people.copy(
                                actionId = null,
                                notice = error.message ?: "Could not complete that action",
                            )
                        )
                    }
                }
        }
    }

    fun updatePrivacy(discoverable: Boolean, allowRequests: Boolean) {
        val session = _uiState.value.session ?: return
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.people.actionId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(people = it.people.copy(actionId = "privacy", notice = null)) }
            peopleRepository.updatePrivacy(session, discoverable, allowRequests)
                .onSuccess {
                    val updated = profile.copy(isDiscoverable = discoverable, allowRequests = allowRequests)
                    cache.upsert(updated.toCache())
                    _uiState.update {
                        it.copy(
                            profile = updated,
                            people = it.people.copy(
                                actionId = null,
                                showPrivacy = false,
                                notice = "Privacy updated",
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            people = it.people.copy(
                                actionId = null,
                                notice = error.message ?: "Could not update privacy",
                            )
                        )
                    }
                }
        }
    }

    fun openPerson(person: UserProfile) {
        _uiState.update { it.copy(people = it.people.copy(selectedPerson = person)) }
    }

    fun closePerson() {
        _uiState.update { it.copy(people = it.people.copy(selectedPerson = null)) }
    }

    fun showShareCard(show: Boolean) {
        _uiState.update {
            it.copy(
                people = it.people.copy(
                    showShareCard = show,
                    showPrivacy = if (show) false else it.people.showPrivacy,
                    showBlocked = if (show) false else it.people.showBlocked,
                ),
            )
        }
    }

    fun showPrivacy(show: Boolean) {
        _uiState.update {
            it.copy(
                people = it.people.copy(
                    showShareCard = if (show) false else it.people.showShareCard,
                    showPrivacy = show,
                    showBlocked = if (show) false else it.people.showBlocked,
                ),
            )
        }
    }

    fun showBlocked(show: Boolean) {
        _uiState.update {
            it.copy(
                people = it.people.copy(
                    showShareCard = if (show) false else it.people.showShareCard,
                    showPrivacy = if (show) false else it.people.showPrivacy,
                    showBlocked = show,
                ),
            )
        }
        if (show) refreshSnapshot()
    }

    fun dismissPeopleNotice() {
        _uiState.update { it.copy(people = it.people.copy(notice = null)) }
    }

    fun signOut() {
        val session = _uiState.value.session
        viewModelScope.launch {
            if (session != null) chatRepository.setPresence(session, false)
            sessionStore.clear()
            cache.clear()
            draftStore.clearAll()
            peopleSearchJob?.cancel()
            messageSearchJob?.cancel()
            chatSyncJob?.cancel()
            presenceJob?.cancel()
            typingJob?.cancel()
            realtimeRefreshJob?.cancel()
            recordingTickerJob?.cancel()
            spaceSyncJob?.cancel()
            spaceRefreshJob?.cancel()
            if (voiceRecorder.isRecording) voiceRecorder.cancel()
            _uiState.update { AppUiState(screen = AppScreen.Welcome, cloudReady = AppConfig.isCloudReady) }
        }
    }

    override fun onCleared() {
        recordingTickerJob?.cancel()
        spaceSyncJob?.cancel()
        spaceRefreshJob?.cancel()
        if (voiceRecorder.isRecording) voiceRecorder.cancel()
        super.onCleared()
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
