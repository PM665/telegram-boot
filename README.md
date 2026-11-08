# telegram-boot

## Modules

- [telegram-boot-core](telegram-boot-core/README.md)
- [telegram-boot-telegram](telegram-boot-telegram/README.md)
- [telegram-boot-max](telegram-boot-max/README.md)
- [telegram-boot-gmail](telegram-boot-gmail/README.md)
- [telegram-boot-ai](telegram-boot-ai/README.md)
- [telegram-boot-autoconfigure](telegram-boot-autoconfigure/README.md)
- [telegram-boot-spring-data](telegram-boot-spring-data/README.md)
- [telegram-boot-spring-boot-starter](telegram-boot-spring-boot-starter/README.md)
- [telegram-boot-admin](telegram-boot-admin/README.md)
- [telegram-boot-sample-app](telegram-boot-sample-app/README.md)

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
