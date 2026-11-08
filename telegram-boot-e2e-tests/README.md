# End-to-end tests

With Docker running, execute `./gradlew :telegram-boot-e2e-tests:test`.
The test starts tgfake in a container and runs the sample application against it.
tgfake records Markdown as sent; it does not parse formatting or verify how Telegram renders it.
