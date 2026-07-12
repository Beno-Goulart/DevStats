package devstats.models;

import java.util.Map;

public class ImageField extends DynamicField {

    public ImageField() {
    }

    public ImageField(int type, String name, String url) {
        super(type, name, Map.of("url", url));
    }

}
