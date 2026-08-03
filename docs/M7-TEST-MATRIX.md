# M7 Test Matrix — Shared Spaces

| Area | Verification |
|---|---|
| You contrast | Global and explicit theme-aware foreground checks |
| Profile navigation | Sheets render at Home root; no forced People tab state |
| Space validation | Name, channel slug and 4000-character message limits |
| Space discovery | Name, description and invite-code search |
| Creation | Owner and default general channel are created |
| Roles | Member announcements denied; managers can create channels |
| Messaging | Idempotent send, reply validation and preview updates |
| Reactions | Current user's reaction is replaced deterministically |
| Preferences | Favorite, mute and channel read state persist |
| Cloud repository | RPC parsing, auth headers and Realtime reconnect compile |
| Database | RLS, policy reruns, helper revokes and RPC grants |
| UI | Space list, detail, thread, creation and info surfaces compile |
