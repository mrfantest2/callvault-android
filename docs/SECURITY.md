# Security and backup

CallVault uses two different key strategies because they solve different problems:

1. Device-local secret material is generated in Android Keystore under the alias callvault_local_vault_v1. It is intended for future local-vault encryption features and never needs to leave the phone.
2. Portable backups use AES-256-GCM with a key derived from a user passphrase using PBKDF2-HMAC-SHA256 (210,000 iterations), with a random 128-bit salt and 96-bit GCM nonce per file.

Portable backup passphrases are not stored by CallVault. Losing the passphrase means the backup cannot be decrypted.

Current CP09 portable backups encrypt the audio copies stored in the backup package. Existing live library recordings remain in their recorder-native files; transparent at-rest vaulting of the active library remains a separate hardening task.
