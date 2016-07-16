package ubongo.server;

public enum ResourceAction {

    CANCEL ("cancel"), RESUME ("resume"), RUN ("run"), STOP ("stop");

    private String name;

    ResourceAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
