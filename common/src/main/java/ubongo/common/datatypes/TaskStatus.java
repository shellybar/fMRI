package ubongo.common.datatypes;

public enum TaskStatus {
    CREATED ("Created"),
    NEW ("New"),
    PENDING ("Pending"),
    PROCESSING ("Processing"),
    COMPLETED ("Completed"),
    FAILED ("Failed"),
    CANCELED ("Canceled"),
    STOPPED ("Stopped"),
    STOPPED_FAILURE ("StopFailed");

    private final String name;

    TaskStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
