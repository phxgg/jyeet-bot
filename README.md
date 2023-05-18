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

### TODO

* > __Warning__
  > * Add and remove TrackBoxButtonClick event listeners only when needed and generally review the code.
  > * Review the message dispatcher code in general.

* Maybe add a `-DapiEnable` variable when running the application, to enable/disalbe API usage.
Right now it's enabled by default, but if an API is not running it will throw an exception each time it tries to connect to it.
It will display lots of errors in the console, but it will not affect the bot's functionality.
 
* Let admins set a specific channel for the bot to listen to commands. Also, only reply to that channel.

* Implement the `/previous` command.

* Implement the `/loop` command.

* IPv6 /64 block -> YouTube IP rotator for rate limiting.<br>
  > __Note__
  > This feature already works, but an IPv6 Block has not been implemented for usage in the current live Discord Bot.

