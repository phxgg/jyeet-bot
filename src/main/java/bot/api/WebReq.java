package bot.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.Future;

public class WebReq {
    private static final String API = "http://localhost:1010";
    private static final HashMap<String, String> headers = new HashMap<>() {{
        put("x-key", System.getProperty("apiKey"));
    }};

    public static String Get(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API).resolve(url))
                .setHeader("x-key", headers.get("x-key"))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response.body();
    }

    // TODO: Fix request body, it seems like it's not being sent correctly
    public static String Post(String url, HashMap<String, ?> data) {
//        var values = new HashMap<String, String>() {{
//            put("name", "John Doe");
//            put("occupation", "gardener");
//        }};

        var objectMapper = new ObjectMapper();
        byte[] requestBody = null;
        try {
            requestBody = objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API).resolve(url))
                .header("x-key", headers.get("x-key"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response.body();
    }
}
