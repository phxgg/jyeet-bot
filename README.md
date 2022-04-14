# jyeet-bot

Discord Music Bot re-coded in Java.<br>
Original node.js version can be found [here](https://github.com/phxgg/yeet-bot).

### TODO

* Review the whole code and make it pretty.
* The bot has been tested very few times. It is expected that there will be bugs
and unexpected results. Do some more testing...
* Fix message dispatcher. Only reply the channel we sent the first command.
* Delete trackbox when the track is finished.
* Spotify refresh token to keep using the spotifyApi.
* Make bot leave channel after some time when queue is empty.
**EDIT:** Currently exists instantly when queue is empty.
* Make bot leave channel when nobody is in the channel.
**EDIT:** This kinda works, but the `destroyPlayer()` function gets called twice.
Find what's wrong in the `onGuildVoiceLeave()` and `onGuildVoiceUpdate` events.
