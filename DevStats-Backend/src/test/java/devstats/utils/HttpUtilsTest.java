package devstats.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilsTest {

    @Test
    void encodeFormBodyProducesCorrectFormat() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", "abc123");

        String result = HttpUtils.encodeFormBody(params);

        assertTrue(result.contains("grant_type=authorization_code"));
        assertTrue(result.contains("code=abc123"));
        assertTrue(result.contains("&"));
    }

    @Test
    void encodeFormBodyEncodesSpecialCharacters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", "value with spaces & symbols=ok");

        String result = HttpUtils.encodeFormBody(params);

        assertFalse(result.contains(" "));
        assertTrue(result.contains("key="));
    }

    @Test
    void encodeFormBodyHandlesEmptyMap() {
        String result = HttpUtils.encodeFormBody(Map.of());
        assertEquals("", result);
    }

    @Test
    void encodeFormBodyHandlesSingleParam() {
        Map<String, String> params = Map.of("single", "value");
        String result = HttpUtils.encodeFormBody(params);
        assertEquals("single=value", result);
    }
}
