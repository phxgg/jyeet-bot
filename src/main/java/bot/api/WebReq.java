package bot.api;

import bot.dto.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

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
            return new Gson().toJson(new Response(404, "Not Found - Could not make a connection.", e.getMessage(), null));
//            throw new RuntimeException(e);
        }

        return response.body();
    }

    public static String Post(String url, HashMap<String, ?> data) {
        var objectMapper = new ObjectMapper();
        String requestBody = null;
        try {
            requestBody = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(URI.create(API).resolve(url))
                .setHeader("x-key", headers.get("x-key"))
                .setHeader("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return new Gson().toJson(new Response(404, "Not Found - Could not make a connection.", e.getMessage(), null));
//            throw new RuntimeException(e);
        }

        return response.body();
    }
}
