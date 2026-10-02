# CallVault

Clean-room Android call-recording project. No Cube ACR source code, assets, package names, or proprietary implementation are reused.

## Current checkpoint
CP10 — Samsung S25 Ultra physical-device validation.

## Design goals
- Modular capture engines.
- Local-first recording library.
- Recoverable Trash before permanent deletion.
- Separate Play-compliant and direct/full release paths.
- GitHub commits/tags are the source of truth.

See docs/ROADMAP.md and docs/ARCHITECTURE.md.

## Device validation
Samsung Galaxy S25 Ultra (SM-S938B), Android 16 / API 36 is the first physically validated target. See docs/DEVICE_SUPPORT.md for the exact support matrix and remaining audio-path limitations.
