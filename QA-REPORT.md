# QA Report — Lunara 0.7.1

## Functional coverage

- Profile sheets open immediately and remain on You.
- Profile text/icons retain contrast in light and dark themes.
- Space list search, favorites and refresh.
- Create a space with selected accepted connections.
- Open space, browse channels and members.
- Owner/admin channel creation and announcement restrictions.
- Send/reply/react with idempotent client IDs.
- Mark channel read, favorite, mute and leave rules.
- Realtime reconnect refresh path.
- Existing direct chat, live cards and media regressions.

## Security coverage

- Space membership required for all reads.
- Owners/admins manage channels; announcements require elevated role.
- Admins cannot promote themselves or delete owners through direct table access.
- Initial members are restricted to accepted connections.
- Space messages have no direct UPDATE policy.
- Public RPC execution is revoked and authenticated execution explicitly granted.


## Build-failure regression coverage

- Deprecated directional icon imports/usages are rejected source-wide.
- Both Shared Spaces chevrons use `AutoMirrored` icons.
- Kotlin source is parsed across the complete app/test tree.
- All 48 packaged domain/repository tests pass with warnings treated as errors.
- Media image rendering contains no production null assertion and disposes only the captured bitmap instance.

## Delivery coverage

- XML/YAML/shell/static SQL validation
- Credential and generated-output scan
- ZIP integrity, duplicate/path safety and fresh-extraction verification
- Git executable-mode preservation and wrong-repository protection

## Executed checks

- 48 packaged tests passed on the final source
- M7 domain/demo, Supabase repository, AppViewModel and Compose callback surfaces passed isolated compilation
- Full Kotlin syntax sweep passed
- Complete Android Gradle lint/APK execution is delegated to the included clean GitHub Actions runner
