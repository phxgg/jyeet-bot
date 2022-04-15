package bot.legacy;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;

public class GetAuthorizationRefreshCode {
    private static final String clientId = "34040d4b2975409187928f90c596cca6";
    private static final String clientSecret = "c568993de30842e78c598306469aa613";
    private static final String refreshToken = "AQCL2_YiaweSI0PUms6Qif1KBBVOcfzQ6pQjjuLR-I3RBPsx4bgc_7_4EqTMH7Jpu1mZCSmtu9xYLCsTYtK6xI3y33qJjEpavacuij4jDFV_43gX_lpny7HTSaD7mywLdsg";

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();
    private static final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
            .build();

    public static void authorizationCodeRefresh_Sync() {
        try {
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

            System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        authorizationCodeRefresh_Sync();
    }
}