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
package bot.legacy.sources.spotify.loaders;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import bot.legacy.sources.spotify.SpotifyAudioSourceManager;
//import dao.exceptions.ChuuServiceException;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.Nullable;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyPlaylistLoader extends Loader {
    private static final String URL_PATTERN = "https?://(?:open\\.)?spotify\\.com(?:/user/[a-zA-Z0-9_]+)?";
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("^(?:" + URL_PATTERN + "|spotify)([/:])playlist\\1([a-zA-Z0-9]+)");
    private final SpotifyAudioSourceManager sourceManager;

    public SpotifyPlaylistLoader(YoutubeAudioSourceManager youtubeAudioSourceManager, SpotifyAudioSourceManager sourceManager) {
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
        var playlistId = matcher.group(2);

        Playlist execute;
        PlaylistTrack[] b;
        try {
            execute = spotifyApi.getPlaylist(playlistId).build().execute();
            b = spotifyApi.getPlaylistsItems(playlistId).build().execute().getItems();
        } catch (IOException | SpotifyWebApiException | ParseException exception) {
            try {
                throw new Exception(exception);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            throw new ChuuServiceException(exception);
        }
        PlaylistTrack[] items = execute.getTracks().getItems();

        check(items.length != 0, "Album $albumId is missing track items!");
        List<AudioTrack> audioTracks = fetchAlbumTracks(manager, spotifyApi, b);
        String name = execute.getName();
        var albumName = name == null || name.isBlank() ? "Untitled Album" : name;

        return new BasicAudioPlaylist(albumName, audioTracks, null, false);
    }

    private List<AudioTrack> fetchAlbumTracks(AudioPlayerManager manager,
                                              SpotifyApi spotifyApi, PlaylistTrack[] track) {
        var tasks = new ArrayList<CompletableFuture<AudioTrack>>();
        for (PlaylistTrack plTrack : track) {
            IPlaylistItem track1 = plTrack.getTrack();
            if (track1 instanceof Track tr) {
                AlbumSimplified album = tr.getAlbum();
                String name = album.getName() == null || album.getName().isBlank() ? "Untitled Album" : album.getName();
                String url = Arrays.stream(album.getImages()).max(Comparator.comparingInt((Image x) -> x.getHeight() * x.getWidth())).map(Image::getUrl).orElse(null);
                String songName = tr.getName();
                String artistName = tr.getArtists()[0].getName();
                CompletableFuture<AudioTrack> task = queueYoutubeSearch(manager, "ytsearch:" + songName + " " + artistName).thenApply(ai -> {
                    if (ai instanceof AudioPlaylist ap) {
                        return new SpotifyAudioTrack((YoutubeAudioTrack) ap.getTracks().get(0), artistName, name, songName, url, this.sourceManager);
                    } else {
                        return new SpotifyAudioTrack((YoutubeAudioTrack) ai, artistName, name, songName, url, this.sourceManager);
                    }
                });
                tasks.add(task);
            }
        }
        try {
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).get();
        } catch (Exception ignored) {
        }

        return tasks.stream().filter(t -> !t.isCompletedExceptionally()).map(x -> {
            try {
                return x.get();
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

}