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

* The bot has been tested very few times. It is expected that there will be bugs
and unexpected results. Do some more testing...
* ipv6block -> youtube ip rotator for rate limiting.
* Fix message dispatcher. Only reply the channel we sent the first command.
* <s>Use https://github.com/Topis-Lavalink-Plugins/Topis-Source-Managers for source managers.
    This will fix rate limiting issues for a not so big bot.</s>
* <s>Make bot leave channel after some time when queue is empty.
  **EDIT:** Currently exists instantly when queue is empty.</s>
* <s>Make bot leave channel when nobody is in the channel.
  **EDIT:** This kinda works, but the `destroyPlayer()` function gets called twice.
  Find what's wrong in the `onGuildVoiceLeave()` and `onGuildVoiceUpdate` events.</s>
* <s>Delete trackbox when the track is finished.</s>
* <s>Spotify refresh token to keep using the spotifyApi. **FIXED**</s>
