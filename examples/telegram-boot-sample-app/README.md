# Telegram Boot sample

From the repository root, run with JDK 25 and a bot token from BotFather:

```sh
BOT_USERNAME=your_bot_username BOT_TOKEN=your_bot_token ./gradlew :examples:telegram-boot-sample-app:bootRun
```

Send `/hello` to the bot in a private chat. It replies `Hello @username` using the user supplied by the command context; if that user has no username, it replies `Hello there`. Bot and user data are kept in memory.
