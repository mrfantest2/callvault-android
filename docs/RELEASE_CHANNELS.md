# Release channels

CallVault has two distribution channels from CP11 onward.

## Play channel

Package:
win.fantest.callvault

Purpose:
- Google Play compatible distribution.
- Uses only Android APIs and behaviors intended to remain compatible with Play policy.
- Must not depend on accessibility-based remote-party call capture.
- Must not include privileged, root-only, OEM-private, or sideload-only capture mechanisms.

Build tasks:
- assemblePlayDebug
- assemblePlayRelease

## Full channel

Package:
win.fantest.callvault.full

Purpose:
- Direct GitHub/sideload distribution.
- Isolated package identity so experimental device-specific capture engines can be added without changing the Play package.
- Future OEM-specific or privileged modules must remain behind the Full source set or runtime capability boundary.

Build tasks:
- assembleFullDebug
- assembleFullRelease

## Release gate

Every checkpoint intended for distribution must:
1. Pass unit tests.
2. Build both Play and Full debug variants.
3. Pass Android lint for both variants.
4. Preserve existing Play-safe behavior.
5. Document any Full-only capability before release.

The existence of a Full build does not imply that Android platform restrictions can be bypassed. Device-specific capture must still be technically supported by the device and lawfully used.
