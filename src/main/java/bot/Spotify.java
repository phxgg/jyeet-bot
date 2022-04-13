package bot;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Spotify {
    private static final String clientId = "34040d4b2975409187928f90c596cca6";
    private static final String clientSecret = "c568993de30842e78c598306469aa613";
    private static final String redirectUri = "http://127.0.0.1/index.html";
    private static HashMap<String, SpotifyURLType> schemes = new HashMap<>();

    private static SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();
    private static final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
            .build();

    public Spotify() {
        schemes.put("https://open.spotify.com/track/", SpotifyURLType.Track);
        schemes.put("https://open.spotify.com/playlist/", SpotifyURLType.Playlist);
        schemes.put("https://open.spotify.com/album/", SpotifyURLType.Album);
        schemes.put("spotify:track:", SpotifyURLType.Track);
        schemes.put("spotify:playlist:", SpotifyURLType.Playlist);
        schemes.put("spotify:album:", SpotifyURLType.Album);

        try {
            final ClientCredentials clientCredentials = clientCredentialsRequest.execute();

            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            System.out.println("Expires in: " + clientCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public SpotifyApi getApi() {
        return spotifyApi;
    }

    public boolean isSpotifyURL(String url) {
        for (String scheme : schemes.keySet()) {
            if (url.startsWith(scheme)) {
                return true;
            }
        }

        return false;
    }

    public SpotifyURLType getURLType(String url) {
        for (String scheme : schemes.keySet()) {
            if (url.startsWith(scheme)) {
                return schemes.get(scheme);
            }
        }

        return null;
    }

    public String getIdFromURL(String url) {
        // Track: https://open.spotify.com/track/1WCEAGGRD066z2Q89ObXTq?si=f5be07a00a334d23
        // Playlist: https://open.spotify.com/playlist/4jR7VFTefyhcmDHpaCIvnq?si=312a317bd8f6433f

        String id = null;

        for (String scheme : schemes.keySet()) {
            if (url.startsWith(scheme)) {
                id = url.replace(scheme, "");
                id = id.split("\\?")[0];
                break;
            }
        }

        return id;
    }
}
