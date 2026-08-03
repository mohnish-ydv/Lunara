# Supabase Setup from a Phone

## Create the backend

1. Create a free project in the Supabase dashboard.
2. Open **SQL Editor**.
3. Paste the complete contents of `supabase/schema.sql`.
4. Run it once. The file is rerunnable for later updates.
5. Open **Project Settings → API**.
6. Copy the Project URL and anon public key.

The SQL script adds all messaging tables to the `supabase_realtime` publication automatically.

## Add GitHub secrets

In the GitHub repository:

1. Open **Settings → Secrets and variables → Actions**.
2. Create `SUPABASE_URL`.
3. Create `SUPABASE_ANON_KEY`.
4. Open **Actions** and run **Build and verify Android app**.

Do not use a service-role key in an Android app.

## Objects created

- `profiles`
- `connections`
- `blocks`
- `conversations`
- `messages` with structured-card and private-media metadata columns
- `message_receipts`
- `message_reactions`
- `typing_states`
- `presence_states`
- participant/block helper functions
- direct-conversation and message-list RPCs
- receipt, typing and presence RPCs
- revision-checked structured-card update RPC
- participant-scoped shared-media RPC
- private `chat-media` Storage bucket with participant/sender RLS
- update and integrity triggers
- indexes, constraints, RLS policies and Realtime publication entries

## Email confirmation

Supabase may require email confirmation depending on Auth settings:

- Confirmation disabled: profile setup opens after sign-up.
- Confirmation enabled: confirm the email, return to Lunara and sign in.

The default email service is sufficient for limited portfolio testing.

## Two-account test

Use two email accounts or two browser/app sessions:

1. Complete both profiles.
2. Search one username from the other account.
3. Send and accept a connection request.
4. Tap **Message** from the connected profile.
5. Keep both sessions open and test typing, receipts, replies, reactions, edits, removal and live task/poll/checklist/RSVP updates, photo/document delivery and voice-note playback.
6. Remove the connection and confirm the thread is no longer accessible.

## Apply future schema updates

Run the newest complete `supabase/schema.sql` again. Policies and triggers are dropped/recreated safely, tables and indexes use `if not exists`, and Realtime publication entries are checked before addition.
