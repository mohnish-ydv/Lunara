# Lunara Architecture

## Online-first source of truth

Supabase is authoritative for accounts, profiles, relationships, conversations and messages. The Android client does not treat local storage as the permanent message database. Room currently caches only the signed-in profile for quick restoration.

## Repository selection

`AppConfig.isCloudReady` selects one complete repository set:

- Cloud: `SupabaseAuthRepository`, `SupabasePeopleRepository`, `SupabaseChatRepository`, `SupabaseSpaceRepository`
- Showcase: `DemoAuthRepository`, `DemoPeopleRepository`, `DemoChatRepository`, `DemoSpaceRepository`

Both paths use the same domain models, `AppViewModel` state transitions and Compose UI.

## Direct-conversation model

A conversation stores two participants in deterministic UUID order and has a unique pair constraint. The `ensure_direct_conversation` RPC verifies an accepted connection, verifies that neither direction is blocked, then returns the existing conversation or safely creates one.

Removing a connection or creating a block makes `private.is_conversation_participant` return false. Existing conversation rows remain for referential consistency but become inaccessible through RLS and RPCs.

## Message delivery path

1. The client generates a UUID `client_id`.
2. A local optimistic message appears with `Sending` state.
3. The message is inserted with a unique `(sender_id, client_id)` constraint.
4. A retry reuses the same `client_id`, so the server returns the same row instead of duplicating it.
5. The optimistic item is replaced by the returned server message.
6. Failure keeps the local item with `Failed` state and exposes retry.

## Receipts

The receiving participant records one receipt row per message. `delivered_at` and `read_at` use guarded upserts, so opening the same chat repeatedly does not generate endless no-op real-time updates.

## Realtime and presence

`SupabaseChatRepository` opens one Phoenix/Supabase Realtime WebSocket for the signed-in session and subscribes to messaging tables. Events are coalesced in the view model before refreshing conversations and the active thread.

The socket sends a heartbeat and reconnects after failure. A separate 45-second presence heartbeat updates `presence_states`; SQL only presents a profile as online when its record is newer than 90 seconds.

## Pagination and ordering

`get_conversation_messages` returns at most 80 messages, ordered oldest to newest within a page. The UI requests the first page with a size of 40 and uses the oldest loaded timestamp as the cursor for earlier pages.

## Database protection

- Row Level Security is enabled for every messaging table.
- Access requires an accepted, unblocked relationship.
- Only a sender can update their own message.
- A trigger prevents changing message ID, client ID, conversation, sender, reply target or creation time.
- A removed message cannot be restored or edited.
- Replies must reference a message in the same conversation.
- Public RPC execution is revoked, then explicitly granted only to `authenticated`.
- Realtime publication setup is rerunnable.

## UI state

`AppViewModel` owns:

- conversation list and query
- active conversation and paginated messages
- optimistic and failed messages
- reply/edit context
- selected message actions
- loading, action and notice states
- people discovery and relationship state
- shared-space list, detail, channel, composer and role-aware actions

The thread screen is displayed above the four-part home shell, preserving a focused, edge-safe conversation layout.

## M4 organization layer

Conversation organization remains private to each account. Supabase stores pin, archive, mute and labels in `conversation_preferences`; bookmarks live in `message_bookmarks`. Row Level Security requires both ownership and current conversation participation. Drafts intentionally remain device-local through `ConversationDraftStore`, keeping unfinished text fast and private while the backend remains authoritative for synchronized conversation state.

The UI consumes these capabilities through `ChatRepository`, so showcase mode and Supabase mode share the same state transitions. `AppViewModel` merges device drafts into server conversations, while the inbox and chat surfaces remain unaware of the active data source.

## M5 structured-message layer

A structured card is stored in the existing `messages` table, preserving one conversation timeline and one realtime stream. `module_type` identifies the card kind, `module_payload` stores its validated JSON state and `module_revision` provides optimistic concurrency. The normal `body` column keeps a short accessible summary such as `Poll · Which accent feels calmer?`, so notifications, replies, inbox previews and basic assistive surfaces never depend on rendering JSON.

The Android domain uses `MessageModule`, while `SupabaseChatRepository` owns JSON serialization. `DemoChatRepository` implements the same send/update contract for credential-free testing. Interactive changes call `update_message_module` with the expected revision; the database locks the row, confirms participation, validates the payload and rejects stale writes before incrementing the revision.

Type-specific validation exists in both Kotlin and PostgreSQL. This rejects incomplete polls/checklists, unsafe coordinates, malformed RSVP values, oversized code and duplicate option identifiers before the state reaches another participant. Search and shared-link queries include structured payload text as well as the accessible summary.

## Media delivery

Media is online-first like the rest of Lunara. The client prepares a private local copy, uploads it to the non-public `chat-media` bucket, then inserts an idempotent message row containing validated metadata and the object path. Object paths include conversation, sender and client IDs; Storage RLS and the message insert trigger independently verify that identity.

Photos are bounded and compressed before upload. Documents are streamed with a 25 MB limit. Voice notes use AAC in an MP4 container, capped at 20 minutes, with a compact normalized waveform. Downloads stream into a `.part` file and are atomically renamed only after successful completion. Cached downloads are rehydrated by attachment ID after app restarts.

Supabase remains authoritative for message/media metadata. Sent originals and downloaded copies are local retry/cache assets, not a second message database.

## Shared-space layer

Shared spaces use a separate `SpaceRepository` so group collaboration does not weaken the deterministic one-to-one conversation model. `spaces`, `space_members`, `space_channels`, `space_messages`, reactions, read markers and personal preferences remain server-authoritative. Showcase mode uses `DemoSpaceRepository`; cloud mode uses `SupabaseSpaceRepository` with the same domain contracts and UI state.

Space access requires current membership. Channel posting checks both membership and channel kind; announcement channels accept posts only from owners and admins. Membership inserts are limited to accepted connections, admins cannot elevate themselves through direct table access, owners cannot be deleted, and space messages expose no direct update policy. Public RPC access is revoked before authenticated grants are applied.

The Realtime socket subscribes to all space tables, emits coalesced refresh signals, heartbeats every 25 seconds and reconnects after failure. The view model refreshes the space list, active detail and active channel independently, preserving the user's current navigation context.

Profile sheets are rendered at the Home root rather than inside the People destination. Their state is mutually exclusive and does not mutate `homeTab`, so Share profile, Discovery & privacy and Blocked profiles remain anchored over You.
