package com.mohnishraj.lunara

import android.app.Application
import androidx.room.Room
import com.mohnishraj.lunara.core.AppConfig
import com.mohnishraj.lunara.core.network.NetworkMonitor
import com.mohnishraj.lunara.core.security.SecureSessionStore
import com.mohnishraj.lunara.data.auth.AuthRepository
import com.mohnishraj.lunara.data.auth.DemoAuthRepository
import com.mohnishraj.lunara.data.auth.SupabaseAuthRepository
import com.mohnishraj.lunara.data.cache.LunaraDatabase
import com.mohnishraj.lunara.data.chat.ChatRepository
import com.mohnishraj.lunara.data.chat.ConversationDraftStore
import com.mohnishraj.lunara.data.chat.DemoChatRepository
import com.mohnishraj.lunara.data.chat.SupabaseChatRepository
import com.mohnishraj.lunara.data.people.DemoPeopleRepository
import com.mohnishraj.lunara.data.people.PeopleRepository
import com.mohnishraj.lunara.data.people.SupabasePeopleRepository
import com.mohnishraj.lunara.data.media.MediaAttachmentStore
import com.mohnishraj.lunara.data.media.VoiceNoteRecorder
import com.mohnishraj.lunara.data.spaces.DemoSpaceRepository
import com.mohnishraj.lunara.data.spaces.SpaceRepository
import com.mohnishraj.lunara.data.spaces.SupabaseSpaceRepository

class LunaraApplication : Application() {
    lateinit var database: LunaraDatabase
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var peopleRepository: PeopleRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var draftStore: ConversationDraftStore
    lateinit var spaceRepository: SpaceRepository
        private set
    lateinit var sessionStore: SecureSessionStore
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var mediaStore: MediaAttachmentStore
        private set
    lateinit var voiceRecorder: VoiceNoteRecorder
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, LunaraDatabase::class.java, "lunara-cache.db")
            .fallbackToDestructiveMigration()
            .build()
        authRepository = if (AppConfig.isCloudReady) SupabaseAuthRepository() else DemoAuthRepository()
        peopleRepository = if (AppConfig.isCloudReady) SupabasePeopleRepository() else DemoPeopleRepository()
        mediaStore = MediaAttachmentStore(this)
        voiceRecorder = VoiceNoteRecorder(this)
        chatRepository = if (AppConfig.isCloudReady) SupabaseChatRepository(mediaStore) else DemoChatRepository()
        draftStore = ConversationDraftStore(this)
        spaceRepository = if (AppConfig.isCloudReady) SupabaseSpaceRepository() else DemoSpaceRepository()
        sessionStore = SecureSessionStore(this)
        networkMonitor = NetworkMonitor(this)
    }
}
