# jyeet-bot

Discord Music Bot re-coded in Java.<br>
Original node.js version can be found [here](https://github.com/phxgg/yeet-bot).

### Build jar

Run the `fatJar` task.

### Run

Use java 16+

```
java -DspotifyClientId=<SPOTIFY_CLIENT_ID> -DspotifyClientSecret=<SPOTIFY_CLIENT_SECRET> -Dprefix=! -DbotToken=<DISCORD_BOT_TOKEN> -jar ~/jyeet-bot-1.0-SNAPSHOT-all.jar
```

### IPv6 Block (/64 subnet)

Run bot with the `-Dipv6Block=<IPV6_BLOCK>/64` parameter.

### TODO

* **REVIEW CODE:** Add and remove TrackBoxButtonClick event listeners only when needed and generally review the code.
* Let admins set a specific channel for the bot to listen to commands. Also, only reply to that channel.
* Implement the `!previous` command.
* IPv6 /64 block -> YouTube IP rotator for rate limiting.
* Fix message dispatcher in general.
