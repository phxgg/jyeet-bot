package bot.api;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientSingleton {
    private static HttpClient instance;

    private HttpClientSingleton() {
    }

    public static synchronized HttpClient getInstance() {
        if (instance == null) {
            instance = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
        return instance;
    }
}
