# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 2.5.x   | :white_check_mark: |
| < 2.5   | :x:                |

## Security Architecture Highlights

* **Zero Network Surface:** Zentra operates entirely offline without `android.permission.INTERNET`.
* **Encrypted Preferences:** User settings and app categorizations are encrypted using `EncryptedSharedPreferences` (`AES256_SIV` keys and `AES256_GCM` values) backed by the hardware Android Keystore, with automatic corruption recovery and cloud-backup exclusion rules (`res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml`).
* **Scoped Package Visibility:** Zentra uses scoped `<queries>` intent filters rather than broad `QUERY_ALL_PACKAGES` permissions.
* **Release Obfuscation & Signing:** Production builds enable R8 code shrinking, obfuscation, and resource shrinking (`minifyEnabled true`, `shrinkResources true`) with externalized signing credentials.

## Reporting a Vulnerability

If you discover a security vulnerability in Zentra, please report it privately via GitHub Security Advisories rather than opening a public issue. Include steps to reproduce, affected Android API levels, and potential impact.
