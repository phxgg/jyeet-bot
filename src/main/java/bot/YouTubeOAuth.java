package bot;

import dev.lavalink.youtube.YoutubeAudioSourceManager;

public class YouTubeOAuth {
    public static void main(String[] args) {
        YoutubeAudioSourceManager yasm = new YoutubeAudioSourceManager();
        yasm.useOauth2(null, false);
    }
}
