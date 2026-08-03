package com.mohnishraj.lunara.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mohnishraj.lunara.ui.screens.AuthWelcomeScreen
import com.mohnishraj.lunara.ui.screens.ConfirmEmailScreen
import com.mohnishraj.lunara.ui.screens.HomeScreen
import com.mohnishraj.lunara.ui.screens.OnboardingScreen
import com.mohnishraj.lunara.ui.screens.ProfileSetupScreen
import com.mohnishraj.lunara.ui.screens.SignInScreen
import com.mohnishraj.lunara.ui.screens.SignUpScreen
import com.mohnishraj.lunara.ui.screens.SplashScreen

@Composable
fun LunaraApp(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    AnimatedContent(
        targetState = state.screen,
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val slide = if (forward) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(330))
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(330))
            }
            val slideOut = if (forward) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(260))
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(260))
            }
            (slide + fadeIn(tween(250))) togetherWith (slideOut + fadeOut(tween(180)))
        },
        label = "screenTransition",
    ) { screen ->
        when (screen) {
            AppScreen.Splash -> SplashScreen()
            AppScreen.Onboarding -> OnboardingScreen(onFinished = viewModel::finishOnboarding)
            AppScreen.Welcome -> AuthWelcomeScreen(
                cloudReady = state.cloudReady,
                onSignIn = { viewModel.open(AppScreen.SignIn) },
                onCreateAccount = { viewModel.open(AppScreen.SignUp) },
            )
            AppScreen.SignIn -> SignInScreen(
                state = state,
                onBack = { viewModel.open(AppScreen.Welcome) },
                onSubmit = viewModel::signIn,
                onCreateAccount = { viewModel.open(AppScreen.SignUp) },
                onDismissMessage = viewModel::dismissMessage,
            )
            AppScreen.SignUp -> SignUpScreen(
                state = state,
                onBack = { viewModel.open(AppScreen.Welcome) },
                onSubmit = viewModel::signUp,
                onSignIn = { viewModel.open(AppScreen.SignIn) },
                onDismissMessage = viewModel::dismissMessage,
            )
            AppScreen.ConfirmEmail -> ConfirmEmailScreen(
                email = state.confirmationEmail,
                onContinue = { viewModel.open(AppScreen.SignIn) },
            )
            AppScreen.ProfileSetup -> ProfileSetupScreen(
                state = state,
                onSubmit = viewModel::completeProfile,
                onDismissMessage = viewModel::dismissMessage,
            )
            AppScreen.Home -> HomeScreen(
                state = state,
                onTabSelected = viewModel::selectHomeTab,
                onChatQueryChange = viewModel::updateChatQuery,
                onConversationFilterChange = viewModel::selectConversationFilter,
                onChatRefresh = { viewModel.refreshChats() },
                onOpenConversationOrganizer = viewModel::openConversationOrganizer,
                onSaveConversationSettings = viewModel::saveConversationSettings,
                onOpenConversation = viewModel::openConversation,
                onCloseConversation = viewModel::closeConversation,
                onComposerChange = viewModel::updateComposer,
                onSendMessage = viewModel::sendComposer,
                onLoadOlderMessages = viewModel::loadOlderMessages,
                onSelectMessage = viewModel::selectMessage,
                onRetryMessage = viewModel::retryMessage,
                onCancelComposerContext = viewModel::cancelComposerContext,
                onDismissChatNotice = viewModel::dismissChatNotice,
                onReplyMessage = viewModel::beginReply,
                onEditMessage = viewModel::beginEdit,
                onDeleteMessage = viewModel::deleteMessage,
                onReactMessage = viewModel::reactToMessage,
                onBookmarkMessage = viewModel::toggleBookmark,
                onShowMessageSearch = viewModel::showMessageSearch,
                onMessageSearchQueryChange = viewModel::searchConversationMessages,
                onShowBookmarks = viewModel::showBookmarks,
                onShowConversationDetails = viewModel::showConversationDetails,
                onShowModuleComposer = viewModel::showModuleComposer,
                onShowAttachmentPicker = viewModel::showAttachmentPicker,
                onCreateCameraCapture = viewModel::createCameraCapture,
                onMediaSelected = viewModel::selectMedia,
                onClearPendingMedia = viewModel::clearPendingMedia,
                onStartVoiceRecording = viewModel::startVoiceRecording,
                onPauseResumeVoice = viewModel::pauseOrResumeVoiceRecording,
                onCancelVoice = viewModel::cancelVoiceRecording,
                onFinishVoice = { viewModel.finishVoiceRecording(true) },
                onDownloadMedia = viewModel::downloadMedia,
                onShowMediaGallery = viewModel::showMediaGallery,
                onSendModule = viewModel::sendModule,
                onUpdateModule = viewModel::updateModule,
                onSpaceQueryChange = viewModel::updateSpaceQuery,
                onFavoriteSpacesOnlyChange = viewModel::setFavoriteSpacesOnly,
                onSpaceRefresh = { viewModel.refreshSpaces() },
                onOpenSpace = viewModel::openSpace,
                onCloseSpace = viewModel::closeSpace,
                onOpenSpaceChannel = viewModel::openSpaceChannel,
                onCloseSpaceChannel = viewModel::closeSpaceChannel,
                onSpaceComposerChange = viewModel::updateSpaceComposer,
                onSendSpaceMessage = viewModel::sendSpaceComposer,
                onReplySpaceMessage = viewModel::replyToSpaceMessage,
                onReactSpaceMessage = viewModel::reactToSpaceMessage,
                onShowCreateSpace = viewModel::showCreateSpace,
                onCreateSpace = viewModel::createSpace,
                onShowCreateSpaceChannel = viewModel::showCreateChannel,
                onCreateSpaceChannel = viewModel::createSpaceChannel,
                onShowSpaceInfo = viewModel::showSpaceInfo,
                onToggleSpaceFavorite = viewModel::toggleSpaceFavorite,
                onToggleSpaceMute = viewModel::toggleSpaceMute,
                onLeaveSpace = viewModel::leaveSpace,
                onDismissSpaceNotice = viewModel::dismissSpaceNotice,
                onStartChat = viewModel::startChat,
                onPeopleTabSelected = viewModel::selectPeopleTab,
                onPeopleQueryChange = viewModel::searchPeople,
                onPeopleRefresh = viewModel::refreshPeople,
                onOpenPerson = viewModel::openPerson,
                onClosePerson = viewModel::closePerson,
                onSendRequest = viewModel::sendRequest,
                onAccept = viewModel::acceptRequest,
                onReject = viewModel::rejectRequest,
                onCancel = viewModel::cancelRequest,
                onRemove = viewModel::removeConnection,
                onBlock = viewModel::blockPerson,
                onUnblock = viewModel::unblockPerson,
                onShowShare = viewModel::showShareCard,
                onShowPrivacy = viewModel::showPrivacy,
                onShowBlocked = viewModel::showBlocked,
                onUpdatePrivacy = viewModel::updatePrivacy,
                onDismissPeopleNotice = viewModel::dismissPeopleNotice,
                onShowMediaStorage = viewModel::showMediaStorage,
                onClearDownloadedMedia = viewModel::clearDownloadedMedia,
                onSignOut = viewModel::signOut,
            )
        }
    }
}
