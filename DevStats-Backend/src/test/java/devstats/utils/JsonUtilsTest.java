package devstats.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void toJsonSerializesObject() throws Exception {
        TestObject obj = new TestObject("hello", 42);
        String json = JsonUtils.toJson(obj);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"hello\""));
        assertTrue(json.contains("\"value\""));
        assertTrue(json.contains("42"));
    }

    @Test
    void fromJsonDeserializesObject() throws Exception {
        String json = "{\"name\":\"world\",\"value\":10}";
        TestObject obj = JsonUtils.fromJson(json, TestObject.class);
        assertEquals("world", obj.name);
        assertEquals(10, obj.value);
    }

    @Test
    void fromJsonIgnoresUnknownProperties() throws Exception {
        String json = "{\"name\":\"test\",\"value\":5,\"unknown\":\"field\"}";
        TestObject obj = JsonUtils.fromJson(json, TestObject.class);
        assertEquals("test", obj.name);
        assertEquals(5, obj.value);
    }

    @Test
    void parseTreeReturnsJsonNode() throws Exception {
        String json = "{\"key\":\"value\"}";
        JsonNode node = JsonUtils.parseTree(json);
        assertEquals("value", node.get("key").asText());
    }

    @Test
    void parseTreeHandlesNestedJson() throws Exception {
        String json = "{\"outer\":{\"inner\":123}}";
        JsonNode node = JsonUtils.parseTree(json);
        assertEquals(123, node.path("outer").path("inner").asInt());
    }

    @Test
    void fromJsonHandlesEmptyObject() throws Exception {
        String json = "{}";
        TestObject obj = JsonUtils.fromJson(json, TestObject.class);
        assertNull(obj.name);
        assertEquals(0, obj.value);
    }

    public static class TestObject {
        public String name;
        public int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
