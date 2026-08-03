# Lunara 0.7.1 Final Verification

This delivery fixes the two reported You-screen regressions and adds M7 Shared Spaces. No Supabase credentials are embedded.

## Uploaded CI failure repaired

The supplied run reached Android resource processing and KSP, then stopped at `compileDebugKotlin` because strict warning handling detected two deprecated `Icons.Rounded.KeyboardArrowRight` usages in `SpaceScreens.kt`. Both now use `Icons.AutoMirrored.Rounded.KeyboardArrowRight`, and the verifier rejects the deprecated import or usage anywhere in application source.

The media preview was also hardened during the full audit: bitmap rendering no longer uses `!!`, and `DisposableEffect` captures the exact bitmap instance it owns instead of reading a newer state value during disposal.

The workflow now writes the full Gradle console stream to `app/build/verification/gradle.log`, so failed runs still upload actionable diagnostics instead of producing an empty-report warning.

## Reported regressions

- `LunaraBackdrop` supplies `onBackground` as the default content color.
- Profile header, cards, labels and actions use explicit theme-aware foreground colors.
- Profile sheets are rendered at the Home root and no profile-sheet action changes `homeTab`.
- Share, privacy and blocked states are mutually exclusive and remain over the You tab.

## M7 gates

- Space domain validation and search
- Demo repository creation, roles, channels, messages, idempotency, reactions, preferences and read state
- Supabase REST and Realtime repository semantic compilation
- AppViewModel space state/callback compilation
- Space Compose surface compilation
- Home and LunaraApp callback wiring checks
- PostgreSQL structure, policy, RPC permission and publication checks
- Existing M1–M6 regression suite

## Executed on the final source

- 48 packaged domain/repository behavior tests passed
- Pure Kotlin source compilation passed with warnings treated as errors
- Supabase space repository semantic compilation passed
- AppViewModel, Space UI, Home routing, LunaraApp callbacks and application composition semantic compilation passed
- Kotlin parser sweep passed across `app/src`
- XML, workflow YAML, shell and static SQL checks passed
- Credential, generated-output and app-visible internal-label scans passed

## Android gate

GitHub Actions runs:

```text
clean testDebugUnitTest lintDebug assembleDebug
```

The workflow uploads the APK only after the build tasks complete. The local sandbox does not include a complete Android SDK/dependency cache, so GitHub Actions remains the authoritative AGP lint and APK assembly gate.
