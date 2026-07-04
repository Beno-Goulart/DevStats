package devstats.models;

public class WidgetPayload {

    private String username;

    private WidgetData data;

    public WidgetPayload() {
    }

    public WidgetPayload(String username, WidgetData data) {
        this.username = username;
        this.data = data;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public WidgetData getData() {
        return data;
    }

    public void setData(WidgetData data) {
        this.data = data;
    }

}
