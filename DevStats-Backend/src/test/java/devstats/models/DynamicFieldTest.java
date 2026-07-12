package devstats.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicFieldTest {

    @Test
    void constructorSetsFields() {
        DynamicField field = new DynamicField(1, "test_name", "test_value");
        assertEquals(1, field.getType());
        assertEquals("test_name", field.getName());
        assertEquals("test_value", field.getValue());
    }

    @Test
    void defaultConstructorCreatesEmptyObject() {
        DynamicField field = new DynamicField();
        assertEquals(0, field.getType());
        assertNull(field.getName());
        assertNull(field.getValue());
    }

    @Test
    void settersWork() {
        DynamicField field = new DynamicField();
        field.setType(3);
        field.setName("image");
        field.setValue("url");
        assertEquals(3, field.getType());
        assertEquals("image", field.getName());
        assertEquals("url", field.getValue());
    }
}
