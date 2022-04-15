package bot.legacy;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class GetAuthorizationCode {
    private static final String clientId = "34040d4b2975409187928f90c596cca6";
    private static final String clientSecret = "c568993de30842e78c598306469aa613";
    private static final URI redirectUri = SpotifyHttpManager.makeUri("http://127.0.0.1/index.html");
    private static final String code = "AQDphetKhXIStrRtW0-Da8Ar9ITFG4K2UbTbqclDaE3K2pZ8WRilhEbJRgf7lPCVsOQoLa5zV5ZalYF_VOzaYX-89c8A4PjQdTGdVycrWm8-z2FZknVHbPfMIZj-NhQW_0qymBogFfIkJ-GS7AKZESlDeugkgWWnKLrLxrtmmw";

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(redirectUri)
            .build();
    private static final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code)
            .build();

    public static void authorizationCode_Sync() {
        try {
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            System.out.println("REFRESH TOKEN: " + authorizationCodeCredentials.getRefreshToken());

            System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        authorizationCode_Sync();
//        authorizationCode_Async();
    }
}