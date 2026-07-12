package devstats.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageFieldTest {

    @Test
    void constructorSetsFields() {
        ImageField field = new ImageField(3, "avatar", "https://example.com/img.png");
        assertEquals(3, field.getType());
        assertEquals("avatar", field.getName());
        assertEquals("https://example.com/img.png", field.getValue());
    }

    @Test
    void defaultConstructorCreatesEmptyObject() {
        ImageField field = new ImageField();
        assertEquals(0, field.getType());
        assertNull(field.getName());
        assertNull(field.getValue());
    }

    @Test
    void imageFieldIsDynamicField() {
        ImageField field = new ImageField(3, "img", "url");
        assertInstanceOf(DynamicField.class, field);
    }
}
