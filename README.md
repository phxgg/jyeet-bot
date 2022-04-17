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

* **INVESTIGATE** TrackBox buttons works but we keep getting the following exception when using buttons:
```
18:35:03.959 [JDA MainWS-ReadThread] ERROR net.dv8tion.jda.api.requests.RestAction - RestAction queue returned failure
java.lang.IllegalStateException: This interaction has already been acknowledged or replied to. You can only reply or acknowledge an interaction once!
	at net.dv8tion.jda.internal.requests.restaction.interactions.InteractionCallbackImpl.tryAck(InteractionCallbackImpl.java:77)
	at net.dv8tion.jda.internal.requests.restaction.interactions.InteractionCallbackImpl.queue(InteractionCallbackImpl.java:84)
	at net.dv8tion.jda.api.requests.RestAction.queue(RestAction.java:573)
	at net.dv8tion.jda.api.requests.RestAction.queue(RestAction.java:539)
	at bot.music.TrackBoxButtonClick.onButtonInteraction(TrackBoxButtonClick.java:28)
	at net.dv8tion.jda.api.hooks.ListenerAdapter.onEvent(ListenerAdapter.java:359)
	at net.dv8tion.jda.api.hooks.InterfacedEventManager.handle(InterfacedEventManager.java:96)
	at net.dv8tion.jda.internal.hooks.EventManagerProxy.handleInternally(EventManagerProxy.java:88)
	at net.dv8tion.jda.internal.hooks.EventManagerProxy.handle(EventManagerProxy.java:70)
	at net.dv8tion.jda.internal.JDAImpl.handleEvent(JDAImpl.java:164)
	at net.dv8tion.jda.internal.handle.InteractionCreateHandler.handleAction(InteractionCreateHandler.java:112)
	at net.dv8tion.jda.internal.handle.InteractionCreateHandler.handleInternally(InteractionCreateHandler.java:69)
	at net.dv8tion.jda.internal.handle.SocketHandler.handle(SocketHandler.java:36)
	at net.dv8tion.jda.internal.requests.WebSocketClient.onDispatch(WebSocketClient.java:952)
	at net.dv8tion.jda.internal.requests.WebSocketClient.onEvent(WebSocketClient.java:839)
	at net.dv8tion.jda.internal.requests.WebSocketClient.handleEvent(WebSocketClient.java:817)
	at net.dv8tion.jda.internal.requests.WebSocketClient.onBinaryMessage(WebSocketClient.java:991)
	at com.neovisionaries.ws.client.ListenerManager.callOnBinaryMessage(ListenerManager.java:385)
	at com.neovisionaries.ws.client.ReadingThread.callOnBinaryMessage(ReadingThread.java:276)
	at com.neovisionaries.ws.client.ReadingThread.handleBinaryFrame(ReadingThread.java:996)
	at com.neovisionaries.ws.client.ReadingThread.handleFrame(ReadingThread.java:755)
	at com.neovisionaries.ws.client.ReadingThread.main(ReadingThread.java:108)
	at com.neovisionaries.ws.client.ReadingThread.runMain(ReadingThread.java:64)
	at com.neovisionaries.ws.client.WebSocketThread.run(WebSocketThread.java:45)
18:35:03.959 [JDA MainWS-ReadThread] DEBUG com.sedmelluq.discord.lavaplayer.player.AudioPlayer - Firing an event with class PlayerResumeEvent
```

* Implement a database to save/load prefixes and other settings for each guild.
* Implement the `!previous` command.
* When queue has finished, keep bot in voice channel for 1 minute. Disconnect if no track has been added inbetween this minute.
* IPv6 /64 block -> YouTube IP rotator for rate limiting.
* Fix message dispatcher. Only reply the channel we sent the first command.
* toLowerCase commands
