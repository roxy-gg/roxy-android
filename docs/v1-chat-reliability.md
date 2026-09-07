# V1 chat reliability implementation

Branch: `codex/v1-chat-reliability`. Based on the current chat/PIN fixes plus the model selector cleanup. All implementation, documentation, and commit messages are in English.

## Scope and checkpoints

Each module is committed separately. Do not publish a release until the verification checklist passes.

- [ ] Module 1: report transport send failures, block offline sends, preserve drafts, expose connection recovery in chat.
- [ ] Module 2: display remote errors, wait for authoritative session synchronization, allow one turn at a time, preserve stream event ordering.
- [ ] Module 3: connect Stop for mobile-started turns, wait for host confirmation, remove attachment placeholders.
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
- Implementation pending.
