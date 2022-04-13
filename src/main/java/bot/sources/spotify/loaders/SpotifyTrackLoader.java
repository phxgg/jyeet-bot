/*
 * MIT License
 *
 * Copyright (c) 2020 Melms Media LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 From Octave bot https://github.com/Stardust-Discord/Octave/ Modified for integrating with JAVA and the current bot
 */
package bot.sources.spotify.loaders;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import bot.sources.spotify.SpotifyAudioSourceManager;
//import dao.exceptions.ChuuServiceException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.Nullable;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SpotifyTrackLoader extends Loader {
    private final Pattern PLAYLIST_PATTERN = Pattern.compile("^(?:https?://(?:open\\.)?spotify\\.com|spotify)([/:])track\\1([a-zA-Z0-9]+)");
    private final SpotifyAudioSourceManager sourceManager;

    public SpotifyTrackLoader(YoutubeAudioSourceManager youtubeAudioSourceManager, SpotifyAudioSourceManager sourceManager) {
        super(youtubeAudioSourceManager);
        this.sourceManager = sourceManager;
    }


    @Override
    public Pattern pattern() {
        return PLAYLIST_PATTERN;
    }

    @Nullable
    @Override
    public AudioItem load(AudioPlayerManager manager, SpotifyApi spotifyApi, Matcher matcher) {
        var albumId = matcher.group(2);

        Track execute;
        try {
            execute = spotifyApi.getTrack(albumId).build().execute();
        } catch (Exception exception) {
            try {
                throw new Exception(exception);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            throw new ChuuServiceException(exception);
        }
        String song = execute.getName();
        String artist = execute.getArtists()[0].getName();
        AlbumSimplified album = execute.getAlbum();
        String name = album.getName();
        String url = Arrays.stream(album.getImages()).max(Comparator.comparingInt((Image x) -> x.getHeight() * x.getWidth())).map(Image::getUrl).orElse(null);
        AudioItem ai = doYoutubeSearch(manager, "ytmsearch:" + song + " " + artist);
        if (ai instanceof AudioPlaylist ap) {
            return new SpotifyAudioTrack((YoutubeAudioTrack) ap.getTracks().get(0), artist, name, song, url, this.sourceManager);
        } else {
            return new SpotifyAudioTrack((YoutubeAudioTrack) ai, artist, name, song, url, this.sourceManager);
        }
    }
}