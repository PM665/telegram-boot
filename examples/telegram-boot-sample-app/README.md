# Telegram Boot sample

With Docker and JDK 25 installed, run from the repository root with the `tgfake` profile:

```sh
./gradlew :examples:telegram-boot-sample-app:bootRun --args='--spring.profiles.active=tgfake'
```

Spring Boot starts tgfake with Docker Compose and stops it when the application exits. If tgfake was already running, Spring Boot leaves it running. Open <http://localhost:18790/> and send `/hello`. The bot greets the user, or replies `Hello there` if the user has no username. Bot and user data are kept in memory.

The built-in `/start` command sends a rich menu of available commands. tgfake records rich HTML as text and does not render its embedded buttons; use Telegram to see and tap the rich menu.

To use a real bot, put its credentials in the ignored `src/main/resources/application-real.yml`:

```yaml
sample:
  bot:
    username: your_bot_username
    token: your_bot_token
```

This file is ignored by Git. Run with the `real` profile:

```sh
./gradlew :examples:telegram-boot-sample-app:bootRun --args='--spring.profiles.active=real'
```

Docker Compose is disabled by default, and the application uses the Telegram API unless another URL is configured.
