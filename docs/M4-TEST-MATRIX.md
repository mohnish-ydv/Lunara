# M4 Test Matrix

| Area | Case | Expected result |
|---|---|---|
| Inbox | Search display name, username, preview, label or draft | Matching conversation remains visible |
| Filters | Select unread, pinned, muted or archived | Only matching conversations appear |
| Pinning | Pin multiple conversations | All pinned items stay grouped and sort by latest activity |
| Archive | Archive a conversation | Main inbox hides it; archived filter shows it; history remains intact |
| Mute | Toggle mute | Personal preference persists without changing message history |
| Labels | Add duplicates, blanks or more than four labels | Values normalize, de-duplicate and cap at four |
| Drafts | Type, leave and reopen a conversation | Device-local draft restores in composer and inbox preview |
| Drafts | Send a draft | Stored draft clears after optimistic send starts |
| Search | Search active conversation case-insensitively | Matching non-deleted messages appear |
| Saved | Save and remove a message bookmark | Private saved collection updates immediately |
| Saved | Delete a previously saved message | Deleted message no longer appears in saved collection |
| Links | Share `https://` or `www.` URL | Details sheet includes it in shared links |
| Details | Open conversation details | Loaded message, saved and shared-link metrics render |
| Realtime | Preference/bookmark changes arrive | Realtime refresh updates active UI |
| Security | Another user reads preferences/bookmarks directly | RLS denies access |
| Conversation | Open connected profile twice | Same direct conversation ID |
| Authorization | Remove connection or block either direction | Conversation access becomes unavailable |
| Send | Repeat same client ID | One server message only |
| Failure | Network error | Failed state and retry affordance remain available |
| Reply/Edit/Delete | Act on synchronized owned message | Server reference and ownership rules are enforced |
| Receipt | Recipient opens thread | Delivery state advances without rollback |
| Paging | Load earlier messages | Older page prepends without duplicate rows |
