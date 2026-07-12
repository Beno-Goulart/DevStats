package devstats.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpUtils {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpUtils() {
    }

    public static String get(String url, Map<String, String> headers) throws Exception {
        HttpRequest request = buildRequest(url, "GET", null, headers);
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        validateResponse(response, url);
        return response.body();
    }

    public static String patch(String url, String json, Map<String, String> headers) throws Exception {
        HttpRequest request = buildRequest(url, "PATCH", json, headers);
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        validateResponse(response, url);
        return response.body();
    }

    public static String post(String url, String formBody, Map<String, String> headers) throws Exception {
        HttpRequest request = buildRequest(url, "POST", formBody, headers);
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        validateResponse(response, url);
        return response.body();
    }

    public static void delete(String url, Map<String, String> headers) throws Exception {
        HttpRequest request = buildRequest(url, "DELETE", null, headers);
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.debug("DELETE {} retornou {}: {}", url, response.statusCode(), response.body());
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HttpUtils.class);

    public static <T> T getJson(String url, Map<String, String> headers, Class<T> type) throws Exception {
        String body = get(url, headers);
        return JsonUtils.fromJson(body, type);
    }

    public static <T> T postJson(String url, String formBody, Map<String, String> headers, Class<T> type) throws Exception {
        String body = post(url, formBody, headers);
        return JsonUtils.fromJson(body, type);
    }

    public static String encodeFormBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static HttpRequest buildRequest(String url, String method, String body, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        if (headers != null) {
            headers.forEach(builder::header);
        }

        if (body != null) {
            if ("PATCH".equals(method)) {
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.POST(HttpRequest.BodyPublishers.ofString(body));
            }
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }

        return builder.build();
    }

    private static void validateResponse(HttpResponse<String> response, String url) {
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException(
                    "HTTP request failed: " + code + " from " + url + "\n" + response.body()
            );
        }
    }
}
