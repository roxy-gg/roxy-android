# Model selector: assessment and roadmap

Assessment dated September 7, 2026, based on the local Android code and `../roxy` (desktop). The production version and relay implementation have not been verified.

## Decision

**A complete implementation has medium-to-high complexity.** The desktop already has a model catalog and per-session configuration, but neither is exposed to mobile clients. Implementation requires extending the remote protocol, adding desktop and Android support, checking the relay, and validating synchronization between clients.

The Android demo has been removed: its hardcoded catalog, local selection state, search, bottom sheet, and placeholder models. The composer now explains that messages use the model configured for the session on the desktop. Prompt submission behavior is unchanged.

## Code findings

| Location | Observed behavior |
| --- | --- |
| `app/src/main/java/gg/roxy/chatFullscreen/components/ChatComposer.kt` (before this cleanup) | `DesktopRoxyModels` was hardcoded and `currentModel` existed only in `rememberSaveable`. There was no callback to the ViewModel. |
| `app/src/main/java/gg/roxy/shared/data/RemoteWorkspaceClient.kt` | `sendPrompt` sends only `{ "t": "prompt", "text": "..." }`. It does not receive catalogs or model-change acknowledgments. |
| `../roxy/src/main/services/remote-protocol.ts` | `GuestFrame` supports `prompt`, `abort`, `list`, `switch`, and `dequeue`; there are no model operations. |
| `../roxy/src/main/services/remote.ts` | `onFrame` does not handle model selection. `runTurn` resolves session configuration and selects the provider/model on the PC. This host would ignore a model field added to an Android prompt. |
| `../roxy/src/main/services/models.ts` | `listModels(providerId)` already retrieves provider catalogs, including authenticated catalogs and proxies. Reuse this implementation. |
| `../roxy/src/shared/session-config.ts` | `resolveSessionConfig` treats provider and model as a pair and maintains per-session configuration. Context and reasoning-effort rules also exist. |
| `../roxy/src/main/db/repo.ts` | `listConnectedProviders` and `setChatConfig` already query connected providers and persist configuration. |

A connected provider's catalog does not always prove access to every listed model: some paths use public catalogs. The UI should reflect restrictions known to the host and surface actual provider rejections without claiming permissions it cannot verify.

## Implementation sequence

### 1. Define the protocol and compatibility behavior

- Advertise an optional capability, such as `model-selection-v1`. New clients hide the selector when the host does not advertise it; older clients must continue working.
- Define catalog queries, selection requests, and acknowledgments. Example request: `{ "t": "select-model", "requestId": "r1", "sessionId": "s1", "providerId": "p1", "modelId": "m1", "expectedRevision": 3 }`.
- Respond with `requestId`, `sessionId`, the effective provider/model pair, and a revision, or an explicit error. Message names in this document are proposals, not existing APIs.
- Include stable identifiers, display names, providers, capabilities, and known availability in the catalog. Never send credentials.
- Check the relay's allowed message types, role validation, and size limits. Host comments describe it as a JSON intermediary, but support for new types still needs verification. Update the web client's protocol definition as well.

Acceptance criterion: a documented protocol and a test proving that messages traverse the relay in both directions.

### 2. Implement desktop host support

- Build the catalog from `listConnectedProviders()` and `listModels(providerId)`, reusing their caches and filters. Distinguish a provider with no models from a failed query; support partial results and retries.
- Resolve and publish the effective selection using the same logic that executes turns, including defaults. Avoid a separate resolver that could display a different model from the one being executed.
- Validate the session, connected provider, model, and revision; persist the pair with `setChatConfig`. Align defaults for new sessions with the desktop selector's policy.
- Acknowledge changes only after saving. Notify desktop and mobile clients when configuration changes, including changes initiated on the PC.
- Publish state on connection, session switches, and reconnection. Discard query responses belonging to an earlier connection or session.
- For the initial scope, reject changes while the session has an active turn or pending queue. This prevents unexpected model changes for messages already submitted. Apply the same rule in both clients and enforce it on the host.

Acceptance criterion: an acknowledged selection determines the next turn's model without modifying other sessions.

### 3. Connect Android

- Add catalog/configuration DTOs and events in `RemoteModels.kt` and `RemoteWorkspaceClient.kt`.
- Keep the catalog, confirmed selection, and pending request in `RoxyAppViewModel`, keyed by connection and session. Clear data when switching PCs or disconnecting.
- Extend `ChatFullScreenUiState` and wire callbacks through `MainActivity`, `RoxyApp`, and `ChatFullScreen` to `ChatComposer`.
- Restore the selector using host data: search, provider groups, loading, empty catalog, errors, and retries. Use `(providerId, modelId)` as the identity because multiple providers may use the same model name.
- Keep the confirmed selection marked while another selection is being saved. Block prompt submission while a change is pending so a race cannot send the message with the previous model. On error or timeout, retain the confirmed selection and query again.
- Reflect changes made on the PC and keep state isolated between sessions. Defer favorites and visual extras until synchronization works.

Acceptance criterion: the mobile client displays the received catalog and never presents a local selection as already applied on the PC.

### 4. Validate and release

- Protocol tests: empty/partial catalogs, errors, acknowledgment, timeout, unknown types, and compatibility with older hosts.
- Host tests: disconnected provider, invalid model, missing session, stale revision, persistence, and isolation between sessions.
- Android tests: open A, switch to B before receiving A's response, switch PCs, reconnect, and receive desktop configuration changes.
- Integration test: select a model on Android and verify the `providerId` and `model` arguments received by `runSessionTurn`; checking the label alone is insufficient.
- Test with a real PC and phone: multiple providers, selection changes from both ends, reconnection, and rejection during execution or while prompts are queued.
- Release compatible relay support first if needed, then the host, and finally Android. Keep the selector hidden for unsupported versions.

## Rough estimate

**2-4 working sessions** for protocol/relay work, the host, Android, and joint validation, assuming repository access and a test environment. This is not a delivery guarantee: timing depends particularly on relay restrictions and how desktop configuration changes are propagated.

Recommended first milestone: a real catalog and effective model in read-only mode, gated by capability support. Second milestone: persisted, acknowledged selection with an execution test. Enable the interactive selector once both are complete.
