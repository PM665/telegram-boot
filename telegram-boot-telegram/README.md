# telegram-boot-telegram

Telegram platform integration for bots and messages.

Set `telegram-boot.telegram.api-url` to an HTTP(S) origin (for example,
`http://localhost:18790`) to use a local Bot API server for outgoing requests,
token validation, and long polling. It defaults to `https://api.telegram.org`.
The admin bot uses the same origin by default; set
`telegram-boot.admin.telegram-api-url` to give it a separate server.
