package devstats.models;

public class DynamicField {

    private int type;

    private String name;

    private Object value;

    public DynamicField() {
    }

    public DynamicField(int type, String name, Object value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
