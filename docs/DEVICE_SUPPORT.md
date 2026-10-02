# Device support

## Samsung Galaxy S25 Ultra

Verified device:
- Model: SM-S938B
- Android: 16
- API level: 36
- Validation date: 2026-10-03

Verified on physical hardware:
- APK install/update through the existing remote ADB path.
- RECORD_AUDIO permission.
- READ_PHONE_STATE permission.
- POST_NOTIFICATIONS permission.
- Microphone hardware available.
- Telephony/calling/subscription features available.
- Dual-SIM inventory detected both active subscriptions.
- Manual microphone recording created a valid non-empty M4A file.
- Recording was registered immediately in the persistent library.
- Foreground automatic-recording service starts successfully as microphone foreground-service type.
- Foreground notification channel is created.
- Automatic-recording preference enables and disables correctly.
- Service stops cleanly after disabling.
- Existing phone rotation configuration remained unchanged throughout validation.

Observed physical-device test recording:
- File size: 47,829 bytes after approximately three seconds.
- Storage path: app-specific external Music/recordings directory.

Not yet verified:
- Remote-party audio quality during a real cellular call.
- Incoming versus outgoing real-call behavior across both SIMs.
- OEM/background behavior after long idle periods or reboot.
- Battery-optimization behavior.
- Bluetooth/headset/carkit call audio.
- VoIP apps such as WhatsApp, Teams, Telegram or Messenger.

CallVault must not claim two-sided cellular-call capture until an actual consented test call verifies it on the target device.
