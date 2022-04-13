package bot.apis.spotify;

public class SpotifySingleton {
    private static Spotify instance;
    private static String clientSecret;
    private static String clientId;

    private SpotifySingleton() {
    }

    //Not pretty
    public static void init(String _clientSecret, String _clientId) {
        clientSecret = _clientSecret;
        clientId = _clientId;
    }


    public static synchronized Spotify getInstance() {
        if (instance == null) {
            instance = new Spotify(clientSecret, clientId);
        }
        return instance;
    }

}