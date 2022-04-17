# jyeet-bot

Discord Music Bot re-coded in Java.<br>
Original node.js version can be found [here](https://github.com/phxgg/yeet-bot).

### Run

Use java 16+

```
java -DspotifyClientId=<SPOTIFY_CLIENT_ID> -DspotifyClientSecret=<SPOTIFY_CLIENT_SECRET> -Dprefix=! -DbotToken=<DISCORD_BOT_TOKEN> -jar ~/jyeet-bot-1.0-SNAPSHOT-all.jar
```

### IPv6 Block (/64 subnet)

Run bot with the `-Dipv6Block=<IPV6_BLOCK>/64` parameter.

### TODO

* **REVIEW CODE:** Add and remove TrackBoxButtonClick event listeners only when needed and generally review the code.
* When queue has finished, keep bot in voice channel for 1 minute. Disconnect if no track has been added inbetween this minute.
* Do voice channel checks (like if user is/isn't in a voice channel) before loading the track/playlist/etc..
* Implement a database to save/load prefixes and other settings for each guild.
* Implement the `!previous` command.
* IPv6 /64 block -> YouTube IP rotator for rate limiting.
* Fix message dispatcher. Only reply the channel we sent the first command.
* toLowerCase commands
