# Release Keystore

Place your release keystore file (`release.jks`) in this directory.

## Environment Variables

Set the following environment variables for signing:

- `RELEASE_STORE_PASSWORD` - Keystore password
- `RELEASE_KEY_ALIAS` - Key alias
- `RELEASE_KEY_PASSWORD` - Key password

## Generating a Keystore

```bash
keytool -genkey -v -keystore release.jks -alias petservice -keyalg RSA -keysize 2048 -validity 10000
```
