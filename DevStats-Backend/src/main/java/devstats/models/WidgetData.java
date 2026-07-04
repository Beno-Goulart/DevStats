package devstats.models;

import java.util.ArrayList;
import java.util.List;

public class WidgetData {

    private List<DynamicField> dynamic = new ArrayList<>();

    public WidgetData() {
    }

    public WidgetData(List<DynamicField> dynamic) {
        this.dynamic = dynamic;
    }

    public List<DynamicField> getDynamic() {
        return dynamic;
    }

    public void setDynamic(List<DynamicField> dynamic) {
        this.dynamic = dynamic;
    }

}
