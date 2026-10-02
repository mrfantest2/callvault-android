# Architecture

CallVault is split into capture, session, storage and presentation layers.

The capture layer exposes a RecorderEngine contract. Implementations can include a standard microphone engine, speaker-assisted engine, manufacturer-specific engine, and privileged/direct-distribution engine.

The Play build must not depend on prohibited accessibility-based remote call audio capture. Device-specific capabilities stay isolated from the portable application core.
