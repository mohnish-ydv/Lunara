# Lunara — Online Communication, Reimagined

Lunara is an online-first native Android communication app built with Kotlin and Jetpack Compose. It combines calm direct messaging, collaborative live cards, private media and shared spaces in one polished mobile-first experience.

The project supports a phone-only, zero-cost portfolio workflow. Without Supabase credentials it runs in interactive showcase mode. When credentials are added later, the same screens switch to authenticated PostgreSQL, Realtime and Storage repositories without a UI rewrite.

## Current experience

### Accounts and people

- Animated onboarding, sign-up, sign-in and profile setup
- Username search, QR profile sharing and deep links
- Connection requests, accepted connections and blocking
- Discovery and request privacy controls
- Profile actions open over the **You** tab without redirecting to People
- Theme-aware text and icon contrast across light and dark surfaces

### Direct conversations

- Optimistic send, retry and duplicate prevention
- Sent, delivered and read states with typing and presence
- Replies, edits, reactions, removal and saved messages
- Search, pin, archive, mute, labels, drafts and details
- Tasks, checklists, polls, events, reminders, notes, countdowns, code, locations and contacts
- Photos, documents and voice notes with progress, retry, viewing and playback

### Shared spaces

- Create focused spaces with up to 24 accepted connections
- Owner, admin and member roles
- Chat, announcement and planning channels
- Announcement posting restricted to owners and admins
- Space and channel search, unread counts, favorite and mute controls
- Realtime messages, replies and emoji reactions
- Member roster, online state, invite code and role visibility
- Idempotent sends, channel read state and reconnect refresh
- Participant-aware Row Level Security and protected RPCs

## Visual system

Every milestone includes a complete visual and usability audit. M7 strengthens global content contrast, keeps profile sheets anchored to the You destination, gives Spaces a distinct visual identity, adds rich space/channel cards, focused empty and loading states, immersive channel threads, responsive sheets and consistent one-handed touch targets.

## Free stack

- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Android minSdk 26, compileSdk/targetSdk 35
- Supabase Auth, PostgreSQL, RLS, Realtime and private Storage
- Room profile cache and DataStore drafts
- OkHttp HTTP/WebSocket client
- GitHub Actions + Gradle 8.9

## Showcase mode

Supabase is optional during development. Build without secrets and Lunara opens with sample profiles, conversations, media and shared spaces. Cloud credentials can be added after the complete app is ready.

## Connect Supabase later

1. Create a free Supabase project.
2. Run the complete `supabase/schema.sql` in SQL Editor.
3. Add GitHub Actions secrets `SUPABASE_URL` and `SUPABASE_ANON_KEY`.
4. Run **Build and verify Android app**.

Use only the publishable/anon key. Never add a service-role key. Detailed steps are in `docs/SUPABASE-SETUP.md`.

## Phone-only upload from Termux

Create an empty repository named `Lunara` or beginning with `Lunara-`, extract this ZIP, then run:

```bash
termux-setup-storage
pkg update -y
pkg install git unzip curl coreutils -y
cd ~/storage/downloads/Lunara-M7.1-Final-Verified-GitHub-Termux-Ready
bash scripts/push-to-github.sh https://github.com/YOUR_USERNAME/Lunara.git
```

The uploader verifies project identity, stages a clean copy, preserves executable modes and rejects unrelated repositories.

## Verification

```bash
bash scripts/verify-project.sh
bash gradlew clean testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions publishes `Lunara-M7.1-Debug-APK` after unit tests, strict lint and debug assembly complete.

## Security boundary

Lunara uses authenticated participant checks, blocking rules, Row Level Security, integrity triggers, revision-checked cards, private media and role-aware spaces. It does **not** claim end-to-end encryption; use portfolio/test data until a separately reviewed encryption layer is implemented.

## Developer

Mohnish Raj
