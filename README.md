# telegram-boot

## Publishing

Publishing to Maven Central relies on the Gradle Maven Publish plugin that is configured in the GitHub Actions workflow at [`.github/workflows/publish.yml`](.github/workflows/publish.yml). To run the `publishToMavenCentral` task successfully you must provide valid credentials via repository secrets:

| Secret | Purpose |
| --- | --- |
| `SONATYPE_USERNAME` | The username of your Sonatype OSSRH account. |
| `SONATYPE_PASSWORD` | The API token (a.k.a. "password") generated in the Sonatype Central Portal. |
| `GPG_KEY_ID` | The public key ID used for signing publications. |
| `GPG_PRIVATE_KEY` | The ASCII-armoured private key contents used for signing. |
| `GPG_PASSPHRASE` | The passphrase for the private signing key. |

If the workflow fails with `Upload failed: {"error":{"message":"Invalid token"}}`, refresh the token backing the `SONATYPE_PASSWORD` secret in the Sonatype Central Portal and update the secret value so that the build service can authenticate successfully.
