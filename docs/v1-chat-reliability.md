# V1 chat reliability implementation

Current branch: `jair/fixbugs1.0` (changed by the user during the pause). Started on `codex/v1-chat-reliability`, based on the chat/PIN fixes plus the model selector cleanup. All implementation, documentation, and commit messages are in English.

## Scope and checkpoints

Each module is committed separately. Do not publish a release until the verification checklist passes.

- [x] Module 1: report transport send failures, block offline sends, preserve drafts, expose connection recovery in chat.
- [x] Module 2: display remote errors, wait for authoritative session synchronization, allow one turn at a time, preserve stream event ordering.
- [x] Module 3: connect Stop for mobile-started turns, wait for host confirmation, remove attachment placeholders.
- [ ] Module 4: regression tests, debug/release compilation, and final review.

## Design constraints

- The desktop protocol remains unchanged. A successful WebSocket send only confirms local enqueueing, not host receipt. Never automatically replay prompts after a disconnect.
- Keep session drafts and transcripts across accidental disconnects; explicit disconnect or pairing a different PC clears them.
- Reconnect to the saved PC and resynchronize the selected session before enabling Send.
- Keep the host authoritative for running/idle state. Do not claim a turn stopped just because an abort frame was queued.
- No model picker, attachments, new-session creation, or queue UI in this release.
- Existing IDE and Google Services changes belong to the user and are excluded from commits.

## Verification

- [ ] Unit tests cover offline/rejected sends, reconnect, session isolation, errors, duplicate submission, stream ordering, and Stop.
- [ ] Debug APK builds.
- [ ] Release build completes (distribution signing is separate).
- [ ] Device smoke test: pairing, incorrect PIN, text response, session switch, airplane mode/reconnect, background/resume, Stop.

Initial environment issue: Gradle fails before running tasks with `Unable to establish loopback connection`. Investigate locally; do not mark tests passed based on older reports. No Android device was attached during the assessment.

## Progress

- Baseline saved in commit `0b01e02`; includes the already-reviewed model selector cleanup and English roadmap.
- Module 1 implemented and validated: 48 unit tests passed and the debug APK built. Drafts survive rejected sends, accidental disconnects, and session navigation; explicit disconnect clears them. Reconnect requests the previous session without replaying prompts.
- Local Windows workaround discovered: run Gradle with `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/nonexistent-roxy-unix-sockets`. The directory must not exist; Java falls back to TCP for its internal selector wakeup pipe. This is a process-local workaround, not a project setting or Android runtime change.
- Module 2: 56 unit tests passed and the debug APK built. Session readiness requires snapshot + turn; prompts remain pending until the host starts them; desktop queues block additional sends. Errors appear inside chat with a refresh action. Events are processed serially and filtered by connection generation, including replayed events.
- The user saved in-progress module 2 changes in `72ae0ab` during the pause. The module 2 checkpoint fixes the incomplete edits in that commit and adds regression coverage.
- Module 3: 62 unit tests passed and the debug APK built. Stop is exposed only for a confirmed mobile-started turn; repeated clicks, offline/rejected aborts, and timeouts are covered. The UI waits for host idle. Attachment controls and image placeholder text have been removed.
- Next: final review, release compilation, and device-test availability.
