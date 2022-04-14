package bot.apis.spotify;

public class SpotifySingleton {
    private static Spotify instance;
    private static String clientId;
    private static String clientSecret;

    private SpotifySingleton() {
    }

    public static void Init(String _clientId, String _clientSecret) {
        clientId = _clientId;
        clientSecret = _clientSecret;
    }


    public static synchronized Spotify getInstance() {
        if (instance == null) {
            instance = new Spotify(clientSecret, clientId);
        }
        return instance;
    }

}