# M5 Test Matrix

| Area | Case | Expected result |
|---|---|---|
| Header | Open chat in dark theme | Name, status and every action remain readable |
| Header | Tap name/status surface | Conversation details open |
| You | Tap Share profile | People destination opens with share sheet immediately |
| You | Tap Discovery & privacy | People destination opens with privacy sheet immediately |
| You | Tap Blocked profiles | People destination opens with blocked sheet immediately |
| Navigation | Open one You destination after another | Only the latest sheet remains active |
| Composer | Tap plus button | Structured-card creator opens |
| Validation | Submit blank title | Send is rejected with a clear message |
| Task | Toggle completion | Card updates and revision advances |
| Checklist | Toggle an item | Only selected item changes |
| Poll | Vote from a participant | Vote moves between options without duplication |
| Event | Select RSVP | Participant response updates or toggles off |
| Reminder | Omit target time | Validation rejects card |
| Countdown | Provide target time | Remaining time label renders safely |
| Code | Submit empty or oversized code | Validation rejects card |
| Location | Provide one coordinate or out-of-range coordinate | Validation rejects card |
| Contact | Omit name/detail | Validation rejects card |
| Send | Retry same client ID | One server card exists |
| Concurrency | Update stale revision | Server rejects and client refreshes latest state |
| Search | Search card title or payload content | Matching card is returned |
| Links | Store URL inside structured payload | Shared-links collection includes the card |
| Reply | Reply to a card | Accessible card summary appears in reply context |
| Delete | Remove a card | Content and interactions become unavailable |
| Security | Non-participant invokes update RPC | Access is denied |
| Migration | Re-run complete schema after M4 | Return-shape functions recreate safely |
| Theme | Review all card types in light/dark | Contrast and hierarchy remain consistent |
