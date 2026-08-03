#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

required=(
  ".gitattributes"
  "settings.gradle.kts"
  "build.gradle.kts"
  "gradlew"
  "gradlew.bat"
  "app/build.gradle.kts"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/res/values/themes.xml"
  "app/src/main/res/values-v27/themes.xml"
  "app/src/main/res/values-v31/themes.xml"
  "app/src/main/java/com/mohnishraj/lunara/data/chat/ChatRepository.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/chat/ConversationDraftStore.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/chat/DemoChatRepository.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/chat/SupabaseChatRepository.kt"
  "app/src/main/java/com/mohnishraj/lunara/domain/ChatModels.kt"
  "app/src/main/java/com/mohnishraj/lunara/domain/MessageModules.kt"
  "app/src/main/java/com/mohnishraj/lunara/domain/MediaModels.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/media/MediaAttachmentStore.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/media/VoiceNoteRecorder.kt"
  "app/src/main/java/com/mohnishraj/lunara/domain/SpaceModels.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/spaces/SpaceRepository.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/spaces/DemoSpaceRepository.kt"
  "app/src/main/java/com/mohnishraj/lunara/data/spaces/SupabaseSpaceRepository.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/LunaraApp.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/screens/ChatScreens.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/screens/HomeScreen.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt"
  "app/src/main/java/com/mohnishraj/lunara/ui/screens/SpaceScreens.kt"
  "app/src/main/res/xml/file_paths.xml"
  "app/src/main/res/xml/backup_rules.xml"
  "app/src/main/res/xml/data_extraction_rules.xml"
  "app/src/main/res/drawable/ic_launcher_monochrome.xml"
  "app/src/main/res/mipmap-anydpi/ic_launcher.xml"
  "app/src/main/res/mipmap-anydpi/ic_launcher_round.xml"
  "app/src/test/java/com/mohnishraj/lunara/data/chat/DemoChatRepositoryTest.kt"
  "app/src/test/java/com/mohnishraj/lunara/domain/ConversationOrganizationTest.kt"
  "app/src/test/java/com/mohnishraj/lunara/domain/MessageModuleTest.kt"
  "app/src/test/java/com/mohnishraj/lunara/domain/MediaAttachmentTest.kt"
  "app/src/test/java/com/mohnishraj/lunara/domain/SpaceModelsTest.kt"
  "app/src/test/java/com/mohnishraj/lunara/data/spaces/DemoSpaceRepositoryTest.kt"
  ".github/workflows/build-apk.yml"
  "FINAL-VERIFICATION.md"
  "QA-REPORT.md"
  "docs/M5-TEST-MATRIX.md"
  "docs/M6-TEST-MATRIX.md"
  "docs/M7-TEST-MATRIX.md"
  "supabase/schema.sql"
)

for file in "${required[@]}"; do
  test -f "$file" || { echo "Missing: $file"; exit 1; }
done

# Android shared storage and some ZIP tools drop Unix executable bits.
chmod +x gradlew scripts/*.sh 2>/dev/null || true
grep -q '^#!/usr/bin/env sh' gradlew || { echo "gradlew has an invalid launcher header."; exit 1; }
grep -q '^#!/data/data/com.termux/files/usr/bin/bash' scripts/push-to-github.sh || {
  echo "Termux upload script has an invalid launcher header."; exit 1;
}
sh -n gradlew
bash -n scripts/push-to-github.sh
bash -n scripts/verify-project.sh

# Source identity prevents accidentally packaging a different project.
grep -q 'rootProject.name = "Lunara"' settings.gradle.kts
grep -q 'include(":app")' settings.gradle.kts
test ! -d engine-core
test ! -d engine-platform-android

if grep -R --line-number -E '(SUPABASE_SERVICE_ROLE|service_role|sb_secret_|postgres(ql)?://[^[:space:]]+:[^[:space:]]+@)' \
  app/src/main build.gradle.kts app/build.gradle.kts supabase 2>/dev/null; then
  echo "A privileged credential or database connection leaked into delivery source."
  exit 1
fi

missing_package="$(find app/src -type f -name '*.kt' -print0 | xargs -0 grep -L '^package ' || true)"
if [ -n "$missing_package" ]; then
  echo "$missing_package"
  echo "A Kotlin source file is missing a package declaration."
  exit 1
fi

python3 - <<'PY'
from pathlib import Path
import re
import xml.etree.ElementTree as ET

for path in Path('app/src/main').rglob('*.xml'):
    ET.parse(path)

base_theme = Path('app/src/main/res/values/themes.xml').read_text()
v27_theme = Path('app/src/main/res/values-v27/themes.xml').read_text()
v31_theme = Path('app/src/main/res/values-v31/themes.xml').read_text()
assert 'windowLayoutInDisplayCutoutMode' not in base_theme, 'API 27 cutout item leaked into base values'
assert 'windowSplashScreen' not in base_theme, 'API 31 splash item leaked into base values'
assert 'windowLayoutInDisplayCutoutMode' in v27_theme
assert 'windowSplashScreenBackground' in v31_theme
assert 'windowSplashScreenAnimatedIcon' in v31_theme

sql = Path('supabase/schema.sql').read_text()
assert sql.count('$$') % 2 == 0, 'Unbalanced PostgreSQL dollar quotes'
required_sql = [
    'create table if not exists public.conversations',
    'create table if not exists public.messages',
    'create table if not exists public.message_receipts',
    'create table if not exists public.message_reactions',
    'create table if not exists public.conversation_preferences',
    'create table if not exists public.message_bookmarks',
    'create table if not exists public.typing_states',
    'create table if not exists public.presence_states',
    "alter table public.messages add column if not exists module_type",
    "alter table public.messages add column if not exists module_payload",
    "alter table public.messages add column if not exists module_revision",
    "alter table public.messages add column if not exists media_type",
    "alter table public.messages add column if not exists media_payload",
    'create table if not exists public.spaces',
    'create table if not exists public.space_members',
    'create table if not exists public.space_channels',
    'create table if not exists public.space_messages',
    'create table if not exists public.space_message_reactions',
    'create table if not exists public.space_channel_reads',
    'create table if not exists public.space_preferences',
    'private.is_space_member',
    'private.can_manage_space',
    'private.is_space_owner',
    'private.can_post_space_channel',
    'public.list_my_spaces',
    'public.get_space_detail',
    'public.create_space',
    'public.create_space_channel',
    'public.get_space_messages',
    'public.send_space_message',
    'public.react_space_message',
    'public.set_space_preferences',
    'public.mark_space_channel_read',
    'public.leave_space',
    'private.is_conversation_participant',
    'private.is_reply_in_conversation',
    'private.valid_conversation_labels',
    'private.valid_message_module',
    'private.valid_message_module_transition',
    'private.valid_message_media',
    'public.ensure_direct_conversation',
    'public.get_conversation_messages',
    'public.search_conversation_messages',
    'public.list_conversation_bookmarks',
    'public.list_conversation_links',
    'public.list_conversation_media',
    'public.update_message_module',
    'messages_prepare_insert',
    'messages_enforce_update_integrity',
    'receipts_enforce_progress',
    'messages_valid_body',
    'messages_valid_module',
    'messages_valid_media',
    "'chat-media'",
    'Conversation participants can read chat media',
    'Senders can upload chat media',
    'allowed_mime_types = excluded.allowed_mime_types',
    "new.body := '';",
    'alter publication supabase_realtime add table',
]
for token in required_sql:
    assert token in sql, f'Missing SQL protection or M5 object: {token}'

for signature in [
    'public.get_conversation_messages(uuid, timestamptz, integer)',
    'public.search_conversation_messages(uuid, text, integer)',
    'public.list_conversation_bookmarks(uuid, integer)',
    'public.list_conversation_links(uuid, integer)',
    'public.list_conversation_media(uuid, integer)',
]:
    assert f'drop function if exists {signature};' in sql, f'Return-shape migration is not rerunnable: {signature}'

for policy in re.findall(r'create policy\s+"([^"]+)"', sql, flags=re.I):
    drop = f'drop policy if exists "{policy}"'
    assert drop.lower() in sql.lower(), f'Policy is not rerunnable: {policy}'

rpc_signatures = [
    'public.ensure_direct_conversation(uuid)',
    'public.list_my_conversations()',
    'public.get_conversation_messages(uuid, timestamptz, integer)',
    'public.search_conversation_messages(uuid, text, integer)',
    'public.list_conversation_bookmarks(uuid, integer)',
    'public.list_conversation_links(uuid, integer)',
    'public.list_conversation_media(uuid, integer)',
    'public.update_message_module(uuid, integer, jsonb)',
    'public.mark_conversation_delivered(uuid)',
    'public.mark_conversation_read(uuid)',
    'public.set_conversation_typing(uuid, boolean)',
    'public.set_my_presence(boolean)',
    'public.list_my_spaces()',
    'public.get_space_detail(uuid)',
    'public.create_space(text, text, text, uuid[])',
    'public.create_space_channel(uuid, text, text, text)',
    'public.get_space_messages(uuid, timestamptz, integer)',
    'public.send_space_message(uuid, uuid, text, uuid)',
    'public.react_space_message(uuid, text)',
    'public.set_space_preferences(uuid, boolean, boolean)',
    'public.mark_space_channel_read(uuid)',
    'public.leave_space(uuid)',
]
for signature in rpc_signatures:
    assert f'revoke all on function {signature} from public;' in sql
    assert f'grant execute on function {signature} to authenticated;' in sql

assert "message_page.module_payload::text" in sql, 'Structured card content is missing from search/link discovery'
assert "target.module_revision <> expected_revision" in sql, 'Optimistic concurrency guard is missing'
assert "module_revision = module_revision + 1" in sql, 'Structured card revision is not advanced'
assert "private.valid_message_module_transition(old.module_type, old.module_payload, new.module_payload, auth.uid())" in sql, 'Direct payload updates bypass transition security'
assert "Interactive card summary cannot be changed" in sql, 'Structured card summary integrity guard is missing'
assert "Media path does not match message identity" in sql, 'Media identity/path trigger is missing'
assert "new.media_payload is distinct from old.media_payload" in sql, 'Media metadata immutability guard is missing'
assert "bucket_id = 'chat-media'" in sql, 'Private media bucket policies are missing'
assert "private.is_conversation_participant(conversation.id, auth.uid())" in sql, 'Storage participant check is missing'
assert 'create policy "owners can update memberships"' in sql, 'Space role updates are not owner-only'
assert "role <> 'owner'" in sql, 'Space owner deletion protection is missing'
assert "connection.status = 'accepted'" in sql, 'Space invitations are not restricted to accepted connections'
assert 'Message editing is intentionally RPC-only' in sql, 'Direct space-message updates were exposed'
assert 'create policy "authors can edit space messages"' not in sql, 'Unsafe direct space-message update policy exists'
for signature in [
    'private.is_space_member(uuid, uuid)',
    'private.can_manage_space(uuid, uuid)',
    'private.is_space_owner(uuid, uuid)',
    'private.channel_space(uuid)',
    'private.can_access_space_channel(uuid, uuid)',
    'private.can_post_space_channel(uuid, uuid)',
]:
    assert f'revoke all on function {signature} from public;' in sql, f'Private helper remains publicly executable: {signature}'

workflow = Path('.github/workflows/build-apk.yml').read_text()
assert 'Confirm Lunara source' in workflow
assert 'Normalize executable permissions' in workflow
assert 'clean testDebugUnitTest lintDebug assembleDebug' in workflow
assert 'set -o pipefail' in workflow
assert 'tee app/build/verification/gradle.log' in workflow
assert 'app/build/verification/' in workflow
print('XML, workflow and SQL static checks passed.')
PY

# Version, artifacts and delivery gates.
grep -q 'versionCode = 12' app/build.gradle.kts
grep -q 'versionName = "0.7.1"' app/build.gradle.kts
grep -q 'Lunara-M7.1-Debug-APK' .github/workflows/build-apk.yml
grep -q 'Lunara-M7.1-Verification-Reports' .github/workflows/build-apk.yml
grep -q 'uses: actions/checkout@v6' .github/workflows/build-apk.yml
grep -q 'uses: actions/setup-java@v5' .github/workflows/build-apk.yml
grep -q 'uses: gradle/actions/setup-gradle@v6' .github/workflows/build-apk.yml
test "$(grep -c 'uses: actions/upload-artifact@v6' .github/workflows/build-apk.yml)" -eq 2
grep -q 'git update-index --chmod=+x gradlew scripts/\*.sh' scripts/push-to-github.sh
grep -q 'lunara-upload' scripts/push-to-github.sh
grep -q 'Safety stop:' scripts/push-to-github.sh

# Reported UI regressions must remain fixed.
grep -q 'color = MaterialTheme.colorScheme.onSurface' app/src/main/java/com/mohnishraj/lunara/ui/screens/ChatScreens.kt
grep -q 'tint = MaterialTheme.colorScheme.onSurface' app/src/main/java/com/mohnishraj/lunara/ui/screens/ChatScreens.kt
grep -q 'clickable(onClick = onDetails)' app/src/main/java/com/mohnishraj/lunara/ui/screens/ChatScreens.kt
grep -q 'CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground)' app/src/main/java/com/mohnishraj/lunara/ui/components/LunaraComponents.kt
grep -q 'Text(profile.displayName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)' app/src/main/java/com/mohnishraj/lunara/ui/screens/HomeScreen.kt
grep -q 'Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)' app/src/main/java/com/mohnishraj/lunara/ui/screens/HomeScreen.kt
! grep -q 'homeTab = if (show) HomeTab.People' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt
python3 - <<'PY_PROFILE_NAV'
from pathlib import Path
home = Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/HomeScreen.kt').read_text()
vm = Path('app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt').read_text()
profile_start = home.index('private fun ProfileOverview(')
profile_end = home.index('@Composable\nprivate fun ProfileAction', profile_start)
profile = home[profile_start:profile_end]
assert 'onTabSelected(HomeTab.People)' not in profile, 'You actions still redirect to People'
for sheet in ['ShareProfileSheet', 'PrivacySheet', 'BlockedSheet']:
    assert f'if (state.people.show' in home and sheet in home
for method in ['fun showShareCard', 'fun showPrivacy', 'fun showBlocked']:
    start = vm.index(method)
    end = vm.find('\n    fun ', start + 5)
    block = vm[start:end if end > 0 else len(vm)]
    assert 'homeTab' not in block, f'{method} changes the selected tab'
assert 'showShareCard = show' in vm
assert 'showPrivacy = show' in vm
assert 'showBlocked = show' in vm
print('Profile contrast and anchored-sheet navigation checks passed.')
PY_PROFILE_NAV


# Exact compiler and lint regression guards.
if grep -q '^import androidx.compose.foundation.layout.weight$' app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt; then
  echo "Invalid Compose weight import was reintroduced."
  exit 1
fi
python3 - <<'PY_MODULE_CARDS'
from pathlib import Path
import re
source = Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt').read_text()
assert 'import androidx.compose.material3.ExperimentalMaterial3Api' in source
assert re.search(
    r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*internal fun ModuleComposerSheet',
    source,
), 'ModuleComposerSheet is missing its Material3 opt-in'
PY_MODULE_CARDS

python3 - <<'PY_LINT_REPAIR'
from pathlib import Path
import xml.etree.ElementTree as ET

media = Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt').read_text()
modules = Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt').read_text()
manifest = Path('app/src/main/AndroidManifest.xml').read_text()
build = Path('app/build.gradle.kts').read_text()

assert 'produceState' not in media, 'MediaCards produceState lint regression returned'
assert 'produceState' not in modules, 'ModuleCards produceState lint regression returned'
assert 'var bitmap by remember(attachment.localUri)' in media
assert 'LaunchedEffect(attachment.localUri)' in media
assert 'var now by remember(target)' in modules
assert 'LaunchedEffect(target)' in modules
assert 'VERSION.SDK_INT >= 23' not in media
assert 'android:screenOrientation=' not in manifest
assert 'android:dataExtractionRules="@xml/data_extraction_rules"' in manifest
assert 'android:fullBackupContent="@xml/backup_rules"' in manifest
assert 'android:label="@string/app_name"' in manifest
assert 'warningsAsErrors = true' in build
assert 'abortOnError = true' in build
assert 'GradleDependency' in build and 'OldTargetApi' in build
assert not Path('app/lint-baseline.xml').exists(), 'Lint baseline must not hide failures'
# A -vNN resource directory is obsolete when NN is at or below minSdk.
for directory in Path('app/src/main/res').iterdir():
    if not directory.is_dir():
        continue
    match = __import__('re').search(r'-v(\d+)(?:$|-)', directory.name)
    if match and int(match.group(1)) <= 26:
        raise AssertionError(f'Obsolete SDK resource qualifier for minSdk 26: {directory.name}')
assert Path('app/src/main/res/mipmap-anydpi').is_dir(), 'Adaptive icons must be in mipmap-anydpi for minSdk 26'
assert not Path('app/src/main/res/mipmap-anydpi-v26').exists(), 'Obsolete v26 icon folder must not return'
assert not Path('app/src/main/res/mipmap-anydpi-v33').exists(), 'Duplicate v33 adaptive icons must not return'
for path in [
    Path('app/src/main/res/mipmap-anydpi/ic_launcher.xml'),
    Path('app/src/main/res/mipmap-anydpi/ic_launcher_round.xml'),
]:
    ET.parse(path)
    icon = path.read_text()
    assert '<adaptive-icon ' in icon
    assert '<background ' in icon
    assert '<foreground ' in icon
    assert '<monochrome android:drawable="@drawable/ic_launcher_monochrome"' in icon
assert '<full-backup-content>' in Path('app/src/main/res/xml/backup_rules.xml').read_text()
assert 'allWarningsAsErrors = true' in build
assert 'Icons.Rounded.Label' not in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/ChatScreens.kt').read_text()
assert 'Icons.Rounded.InsertDriveFile' not in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt').read_text()
assert 'Icons.Rounded.KeyboardArrowRight' not in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/SpaceScreens.kt').read_text()
assert 'import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight' in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/SpaceScreens.kt').read_text()
assert 'Icons.AutoMirrored.Rounded.KeyboardArrowRight' in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/SpaceScreens.kt').read_text()
assert 'bitmap!!' not in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt').read_text()
assert 'DisposableEffect(currentBitmap)' in Path('app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt').read_text()
print('Lint and compiler-warning regression checks passed.')
PY_LINT_REPAIR

# M5 structured-message implementation gates.
grep -q 'enum class MessageModuleType' app/src/main/java/com/mohnishraj/lunara/domain/MessageModules.kt
grep -q 'data class MessageModule' app/src/main/java/com/mohnishraj/lunara/domain/MessageModules.kt
grep -q 'sendModuleMessage' app/src/main/java/com/mohnishraj/lunara/data/chat/ChatRepository.kt
grep -q 'updateModule' app/src/main/java/com/mohnishraj/lunara/data/chat/ChatRepository.kt
grep -q 'module_revision' app/src/main/java/com/mohnishraj/lunara/data/chat/SupabaseChatRepository.kt
grep -q 'showModuleComposer' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt
grep -q 'MessageModuleCard' app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt
grep -q 'ModuleComposerSheet' app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt
grep -q 'MessageModuleType.entries' app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt
grep -q 'moduleSendIsIdempotentAndUpdatesUseRevision' app/src/test/java/com/mohnishraj/lunara/data/chat/DemoChatRepositoryTest.kt
grep -q 'structuredOptionIdentifiersMustBeUnique' app/src/test/java/com/mohnishraj/lunara/domain/MessageModuleTest.kt
grep -q 'var now by remember(target) { mutableStateOf(Instant.now()) }' app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt
grep -q 'LaunchedEffect(target)' app/src/main/java/com/mohnishraj/lunara/ui/screens/ModuleCards.kt


# M6 private media and voice-note gates.
grep -q 'enum class MediaKind' app/src/main/java/com/mohnishraj/lunara/domain/MediaModels.kt
grep -q 'MAX_FILE_BYTES = 25L' app/src/main/java/com/mohnishraj/lunara/domain/MediaModels.kt
grep -q 'class MediaAttachmentStore' app/src/main/java/com/mohnishraj/lunara/data/media/MediaAttachmentStore.kt
grep -q 'copyToLimited' app/src/main/java/com/mohnishraj/lunara/data/media/MediaAttachmentStore.kt
grep -q 'class VoiceNoteRecorder' app/src/main/java/com/mohnishraj/lunara/data/media/VoiceNoteRecorder.kt
grep -q 'sendMediaMessage' app/src/main/java/com/mohnishraj/lunara/data/chat/ChatRepository.kt
grep -q 'downloadMedia' app/src/main/java/com/mohnishraj/lunara/data/chat/ChatRepository.kt
grep -q 'list_conversation_media' app/src/main/java/com/mohnishraj/lunara/data/chat/SupabaseChatRepository.kt
grep -q 'deleteUploadedMedia' app/src/main/java/com/mohnishraj/lunara/data/chat/SupabaseChatRepository.kt
grep -q 'saveDownload' app/src/main/java/com/mohnishraj/lunara/data/chat/SupabaseChatRepository.kt
grep -q 'MediaAttachmentCard' app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt
grep -q 'VoiceRecorderBar' app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt
grep -q 'MediaGallerySheet' app/src/main/java/com/mohnishraj/lunara/ui/screens/MediaCards.kt
grep -q 'showAttachmentPicker' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt
grep -q 'finishVoiceRecording' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt
grep -q 'android.permission.RECORD_AUDIO' app/src/main/AndroidManifest.xml
grep -q 'androidx.core.content.FileProvider' app/src/main/AndroidManifest.xml
grep -q 'android.support.FILE_PROVIDER_PATHS' app/src/main/AndroidManifest.xml
grep -q '<files-path name="media_files" path="lunara_media/"' app/src/main/res/xml/file_paths.xml
grep -q 'imageAndDocumentValidationEnforcesLimits' app/src/test/java/com/mohnishraj/lunara/domain/MediaAttachmentTest.kt
grep -q 'mediaSendIsIdempotentAndAppearsInGallery' app/src/test/java/com/mohnishraj/lunara/data/chat/DemoChatRepositoryTest.kt

# M7 shared-space implementation gates.
grep -q 'enum class SpaceRole' app/src/main/java/com/mohnishraj/lunara/domain/SpaceModels.kt
grep -q 'enum class SpaceChannelKind' app/src/main/java/com/mohnishraj/lunara/domain/SpaceModels.kt
grep -q 'interface SpaceRepository' app/src/main/java/com/mohnishraj/lunara/data/spaces/SpaceRepository.kt
grep -q 'class DemoSpaceRepository' app/src/main/java/com/mohnishraj/lunara/data/spaces/DemoSpaceRepository.kt
grep -q 'class SupabaseSpaceRepository' app/src/main/java/com/mohnishraj/lunara/data/spaces/SupabaseSpaceRepository.kt
grep -q 'SpaceSignal.Reconnected' app/src/main/java/com/mohnishraj/lunara/data/spaces/SupabaseSpaceRepository.kt
grep -q 'fun SpaceHubScreen' app/src/main/java/com/mohnishraj/lunara/ui/screens/SpaceScreens.kt
grep -q 'HomeTab.Spaces -> SpaceHubScreen' app/src/main/java/com/mohnishraj/lunara/ui/screens/HomeScreen.kt
grep -q 'spaceRepository.observe' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt
grep -q 'sendSpaceComposer' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt
grep -q 'createSpaceAddsOwnerAndGeneralChannel' app/src/test/java/com/mohnishraj/lunara/data/spaces/DemoSpaceRepositoryTest.kt
grep -q 'managersCanCreateChannelsAndMembersCannotPostAnnouncements' app/src/test/java/com/mohnishraj/lunara/data/spaces/DemoSpaceRepositoryTest.kt

# Previously fixed Android and messaging regressions must stay fixed.
grep -q 'GRADLE_SHA256="d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"' gradlew
grep -q 'distributionSha256Sum=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab' gradle/wrapper/gradle-wrapper.properties
grep -q 'windowLayoutInDisplayCutoutMode' app/src/main/res/values-v27/themes.xml
! grep -q 'windowLayoutInDisplayCutoutMode' app/src/main/res/values/themes.xml
grep -q 'callbackFlow' app/src/main/java/com/mohnishraj/lunara/data/chat/SupabaseChatRepository.kt
grep -q 'MessageDeliveryState.Failed' app/src/main/java/com/mohnishraj/lunara/ui/AppViewModel.kt

if grep -R --line-number -E 'Icons\.Rounded\.(ArrowBack|ArrowForward|Send|KeyboardArrowLeft|KeyboardArrowRight)|statusBarColor[[:space:]]*=|navigationBarColor[[:space:]]*=' \
  app/src/main/java 2>/dev/null; then
  echo "A known deprecated Android/Compose API was reintroduced."
  exit 1
fi


if grep -R --line-number -E '^import androidx\.compose\.material\.icons\.rounded\.(ArrowBack|ArrowForward|Send|KeyboardArrowLeft|KeyboardArrowRight)$' \
  app/src/main/java 2>/dev/null; then
  echo "A non-auto-mirrored directional icon import was reintroduced."
  exit 1
fi

if grep -R --line-number -E 'TODO|FIXME|Lunara-M5-Debug-APK|Lunara-M5-Verification-Reports|Lunara-M6.1-Debug-APK|Lunara-M6.2-Debug-APK|Lunara-M6.2-Verification-Reports|Lunara-M6.3-Debug-APK|Lunara-M6.3-Verification-Reports|Lunara-M6.4-Debug-APK|Lunara-M6.4-Verification-Reports|versionName = "0\.[56]\.[0-9]+"' \
  app/src/main .github scripts/push-to-github.sh README.md 2>/dev/null; then
  echo "A stale marker remains in delivery-critical files."
  exit 1
fi

if find . -path './.git' -prune -o \( -name build -o -name .gradle -o -name local.properties \) -print | grep -q .; then
  echo "Generated build output or local credentials are present in the delivery tree."
  exit 1
fi

echo "Lunara 0.7.1 project checks passed."
