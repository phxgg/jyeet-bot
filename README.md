# jyeet-bot

Discord Music Bot in Java.<br />
Node.js version [here](https://github.com/phxgg/discord-music-bot).

**YEEET bot website:** [yeet-web](https://github.com/phxgg/yeet-web)

### Build jar

Run the `fatJar` task.

### Run

You will need a [Spotify Developer App](https://developer.spotify.com/dashboard) to run the bot.

Use **Java 17**

```bash
$ java -Xms256m -Xmx512m -DspotifyClientId=<SPOTIFY_CLIENT_ID> -DspotifyClientSecret=<SPOTIFY_CLIENT_SECRET> -Dprefix=! -DbotToken=<DISCORD_BOT_TOKEN> -jar ~/jyeet-bot-1.0-SNAPSHOT-all.jar
```

### Use [yeet-api](https://github.com/phxgg/yeet-bot-api)

Run bot with the `-DapiUrl=http://localhost:1010 -DapiKey=<YOUR_API_SECRET>` parameters.

### IPv6 Block (/64 subnet)

Run bot with the `-Dipv6Block=<IPV6_BLOCK>/64` parameter.

### Notes

The code is ugly and needs a lot of refactoring. I'm aware of that.

> __Note__
> 
> IPv6 /64 block -> YouTube IP rotator for rate limiting.
> 
> This feature already works, but an IPv6 Block has not been implemented for usage in the current live Discord Bot.

