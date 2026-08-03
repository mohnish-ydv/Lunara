# Changelog

## 0.7.1

### Build stabilization

- Replaced both deprecated `KeyboardArrowRight` usages in Shared Spaces with their `AutoMirrored` variants so strict Kotlin compilation no longer fails under `-Werror`.
- Added source-wide guards that reject non-auto-mirrored directional icon imports and usages before Gradle compilation.
- Removed unsafe bitmap null assertions and fixed image-preview disposal so an old composition cannot recycle a newly loaded bitmap.
- Updated build metadata and GitHub artifacts for the repaired 0.7.1 delivery.
- CI now captures the complete Gradle console output as a verification artifact even when compilation stops before lint/test reports are created.

### Fixed

- Fixed unreadable dark text and icons on the You/profile surface by establishing a theme-aware global content color and explicit surface foreground colors.
- Fixed Share profile, Discovery & privacy and Blocked profiles opening only after switching tabs.
- Fixed those profile actions redirecting to People after opening; sheets now remain anchored over You.
- Preserved the previous chat-header contrast and navigation fixes.

### Shared spaces

- Added a complete Spaces destination instead of the placeholder screen.
- Added space creation with accepted connections, owner/admin/member roles and invite codes.
- Added chat, announcements and planning channels.
- Added role-aware announcement posting, channel creation and leave restrictions.
- Added realtime space refresh, message replies, reactions, unread counts and read state.
- Added favorite, mute, search, member roster and space information flows.
- Added showcase data and full Supabase repositories, RPCs, RLS and Realtime publication setup.
- Hardened membership policies against admin privilege escalation, owner deletion and unrestricted invitations.
- Removed direct space-message updates so immutable identity fields cannot be changed through the REST table endpoint.

### Visual polish

- Added premium space pulse, space cards, channel cards and immersive channel threads.
- Improved loading, empty, notice and creation states.
- Re-audited light/dark contrast, spacing, hierarchy and touch targets.
