package bot.legacy.sources.spotify.loaders;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import bot.legacy.sources.MetadataTrack;
import bot.legacy.sources.spotify.SpotifyAudioSourceManager;

public final class SpotifyAudioTrack extends YoutubeAudioTrack implements MetadataTrack {
    private final String album;
    private final String image;
    private final SpotifyAudioSourceManager sourceManager;

    public SpotifyAudioTrack(YoutubeAudioTrack baseAudioTack, String artist, String album,
                             String song, String image, SpotifyAudioSourceManager sourceManager) {
        super(
                new AudioTrackInfo(
                        song,
                        artist,
                        baseAudioTack.getInfo().length,
                        baseAudioTack.getIdentifier(),
                        baseAudioTack.getInfo().isStream,
                        baseAudioTack.getInfo().uri),
                (YoutubeAudioSourceManager) baseAudioTack.getSourceManager()
        );

        this.album = album;
        this.image = image;
        this.sourceManager = sourceManager;
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }

    @Override
    public String getAlbum() {
        return album;
    }

    @Override
    public String getImage() {
        return image;
    }
}