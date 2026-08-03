# M6 Test Matrix

| Area | Case | Expected result |
|---|---|---|
| M5 regression | Open each interactive card and update it | Task, checklist, poll and RSVP behavior remains revision-safe |
| M5 regression | Open an ISO countdown card and wait | Remaining seconds update without another user action |
| Navigation | Use Share profile, privacy and blocked actions from You | Correct People destination opens immediately |
| Header | Open chat in light and dark themes | Name, status and actions remain readable above media content |
| Photo | Select a large image | Image is decoded off the UI thread, scaled within 2048 px and compressed before send |
| Camera | Capture a photo | Temporary camera file is imported, cleaned and shown as a pending attachment |
| Document | Select a supported document | File is copied into private app storage and guarded at 25 MB |
| Voice | Start, pause, resume and finish recording | Timer/waveform remain consistent and a playable AAC/M4A attachment is created |
| Voice | Cancel recording | Recorder releases and temporary recording is removed |
| Voice | Reach 20-minute limit | Recording safely finishes instead of continuing indefinitely |
| Composer | Add caption and reply context to media | Caption and reply target are sent with one idempotent client ID |
| Upload | Network fails mid-upload | Failed state remains visible and retry reuses the same client ID/path |
| Download | Download a remote attachment | Data streams to a temporary file, enforces 25 MB and atomically promotes on success |
| Download | App restarts after a successful download | Cached file hydrates into the message without downloading again |
| Gallery | Open Shared media | Only non-deleted photo/document/voice messages appear |
| Storage | Clear downloaded cache | Downloaded copies are removed; sent originals and messages remain |
| Playback | Play, seek and change voice speed | Player remains local to the card and releases when the card leaves composition |
| Photo viewer | Open a downloaded photo | Full-screen viewer uses correct aspect ratio and dismisses safely |
| Security | Non-participant requests a storage object | Storage RLS denies access |
| Security | User uploads into another sender/conversation path | Storage policy and message trigger reject it |
| Security | Client changes media metadata after insert | Message integrity trigger rejects the mutation |
| Migration | Re-run schema after M5 | Media columns, constraints, RPC, bucket and policies remain rerunnable |
| Theme | Review media cards in light/dark | Text, actions, transfer states and surfaces keep accessible contrast |


## Earlier build-regression gates

- `ModuleCards.kt` must not import `androidx.compose.foundation.layout.weight`; Row/Column scope supplies the correct modifier extension.
- `ModuleComposerSheet` must retain `@OptIn(ExperimentalMaterial3Api::class)` before using `ModalBottomSheet`.
- The clean workflow must run `clean testDebugUnitTest lintDebug assembleDebug` and publish only the current delivery artifact names.

## 0.6.4 lint-regression gates

- No `produceState` remains in `MediaCards.kt` or `ModuleCards.kt`.
- Actionable lint warnings are fatal via `warningsAsErrors = true`.
- Activity orientation is not locked.
- Backup/data-extraction resources are valid and referenced.
- Both manifest-resolved adaptive icons live in `mipmap-anydpi` and include the monochrome layer directly.
- Obsolete `mipmap-anydpi-v26` XML and redundant v33 copies are rejected.
